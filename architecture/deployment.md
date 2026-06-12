# K8s Local Development — Rancher Desktop with WASM

**Status**: AGREED
**Last Updated**: 2026-06-12
**Depends On**: architecture/tech-stack.md, architecture/auth.md

---

> **Production target note**: Per requirements Doc 5 (INF-*), the production target is **Cloudflare** — Workers at the edge, with services running as containers behind them. This document describes the **cluster-shaped reference deployment** used for development. The deployable artifacts are the same in both: a static Wasm bundle for the SPA/admin console and a containerized Ktor origin for the paywall backend.

## 1. Overview

Local Kubernetes development uses **Rancher Desktop** with WASM support enabled. This deploys the full paywall stack (backend, frontend, Ory, PostgreSQL) into a local K8s cluster — the same way it runs in the cluster-shaped reference environment, but with dev-friendly defaults.

### Why K8s Locally (not just Docker Compose)?
- **Parity**: Catch K8s-specific issues early (networking, DNS, config, probes)
- **WASM pods**: Rancher Desktop supports WASM workloads via containerd shims — test the experimental WASM backend deployment path locally
- **Ingress**: Test real routing (`/api/*` → backend, `/*` → frontend, `/ory/*` → Ory)
- **Service mesh ready**: Same manifests can add Linkerd/Istio later

### Rancher Desktop Requirements
- Rancher Desktop installed with **containerd** runtime (not dockerd)
- WASM support enabled: Settings → Experimental → WebAssembly
- This installs `containerd-shim-spin-v2` and/or `containerd-shim-slight-v1`
- Traefik ingress controller (bundled with Rancher Desktop / k3s)

---

## 2. Architecture — Local Cluster

```
┌─────────────────────────────────────────────────────────────────┐
│                    Rancher Desktop (k3s)                         │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Ingress (Traefik)                       :80 / :443        │  │
│  │    paywall.local/*        → frontend-svc                    │  │
│  │    paywall.local/api/*    → backend-svc                     │  │
│  │    auth.paywall.local/*   → hydra-public-svc                │  │
│  │    id.paywall.local/*     → kratos-public-svc               │  │
│  │    login.paywall.local/*  → kratos-ui-svc                   │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Frontend    │  │  Backend     │  │  Backend (WASM)         │  │
│  │  Deployment  │  │  Deployment  │  │  Deployment             │  │
│  │  Nginx +     │  │  JVM + Ktor  │  │  Spin/WasmEdge + Ktor  │  │
│  │  WASM assets │  │  (Option A)  │  │  (Option B — experimental)│
│  └─────────────┘  └──────┬──────┘  └────────┬────────────────┘  │
│                          │                    │                   │
│  ┌───────────────────────┴────────────────────┘                  │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌───────────┐             │
│  │  │  PostgreSQL  │  │  Ory Kratos  │  │ Ory Hydra │             │
│  │  │  StatefulSet │  │  Deployment  │  │ Deployment│             │
│  │  └─────────────┘  └─────────────┘  └───────────┘             │
│  │                                                               │
│  │  ┌─────────────┐  ┌───────────────┐                          │
│  │  │  Kratos UI   │  │  Mailslurper   │                          │
│  │  │  Deployment  │  │  Deployment    │                          │
│  │  └─────────────┘  └───────────────┘                          │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Namespace: paywall-dev                                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Namespace & Hosts

All resources deploy into namespace `paywall-dev`. Local DNS via `/etc/hosts`:

```
# Add to /etc/hosts (or use Rancher Desktop's built-in DNS)
127.0.0.1  paywall.local
127.0.0.1  auth.paywall.local
127.0.0.1  id.paywall.local
127.0.0.1  login.paywall.local
127.0.0.1  mail.paywall.local
```

---

## 4. Manifest Structure

```
k8s/
  base/                              # Shared base manifests (Kustomize)
    kustomization.yaml
    namespace.yaml
    postgres/
      statefulset.yaml
      service.yaml
      configmap.yaml                 # init SQL
    ory/
      kratos-config.yaml             # ConfigMap from docker/ory/kratos/
      kratos-deployment.yaml
      kratos-service.yaml
      hydra-deployment.yaml
      hydra-service.yaml
      kratos-ui-deployment.yaml
      kratos-ui-service.yaml
      mailslurper-deployment.yaml
      mailslurper-service.yaml
      oidc-client-job.yaml           # Job: register OIDC client + seed admin
    paywall/
      backend-deployment.yaml        # JVM Ktor (Option A)
      backend-service.yaml
      frontend-deployment.yaml       # Nginx + WASM static assets
      frontend-service.yaml

  local/                             # Local overlay (Rancher Desktop specific)
    kustomization.yaml               # Patches for local dev
    ingress.yaml                     # Traefik IngressRoute
    paywall/
      backend-wasm-deployment.yaml   # WASM runtime (Option B — experimental)
      backend-wasm-runtimeclass.yaml # RuntimeClass for spin/wasmEdge
```

---

## 5. WASM Runtime Configuration

Rancher Desktop with WASM enabled provides containerd shims. To run WASM workloads:

### RuntimeClass (for WASM pods)
```yaml
apiVersion: node.k8s.io/v1
kind: RuntimeClass
metadata:
  name: wasmtime-spin-v2
handler: spin
```

### WASM Pod Annotation
```yaml
spec:
  runtimeClassName: wasmtime-spin-v2
  containers:
    - name: paywall-backend-wasm
      image: paywall-backend:wasm
      # WASM binary compiled from Kotlin → WASM
```

> **Note**: Kotlin server-side WASM is experimental. The manifests include both Option A (JVM) and Option B (WASM) deployments. Use Option A by default; Option B for experimentation.

---

## 6. Deployment Script

A single script deploys everything to the local cluster.

---

## 7. Development Workflow

### First-time Setup
```bash
# 1. Ensure Rancher Desktop is running with containerd + WASM enabled
# 2. Deploy the stack
./k8s/deploy-local.sh

# 3. Wait for all pods to be ready
kubectl -n paywall-dev get pods -w

# 4. Access the paywall platform
open http://paywall.local
# Login: admin@paywall.local / Admin123!
```

### Iterative Development
```bash
# Rebuild and redeploy just the backend
./gradlew :backend:jibDockerBuild   # Build container image
kubectl -n paywall-dev rollout restart deployment/paywall-backend

# Rebuild and redeploy just the frontend
./gradlew :frontend:wasmJsBrowserDistribution
docker build -t paywall-frontend:dev -f frontend/Dockerfile .
kubectl -n paywall-dev rollout restart deployment/paywall-frontend

# View logs
kubectl -n paywall-dev logs -f deployment/paywall-backend
kubectl -n paywall-dev logs -f deployment/paywall-frontend
```

### Switching Between Option A and Option B
```bash
# Use JVM backend (default, stable)
kubectl -n paywall-dev scale deployment/paywall-backend --replicas=1
kubectl -n paywall-dev scale deployment/paywall-backend-wasm --replicas=0

# Try WASM backend (experimental)
kubectl -n paywall-dev scale deployment/paywall-backend --replicas=0
kubectl -n paywall-dev scale deployment/paywall-backend-wasm --replicas=1
```

---

## 8. Completion Criteria

- [ ] All K8s manifests are valid (`kubectl apply --dry-run=client`)
- [ ] `deploy-local.sh` brings up all services in paywall-dev namespace
- [ ] PostgreSQL StatefulSet persists data across restarts
- [ ] Ory Kratos + Hydra initialize and pass health checks
- [ ] OIDC client registration Job completes successfully
- [ ] Seed admin user can log in via login.paywall.local
- [ ] Ingress routes correctly: `/api/*` → backend, `/*` → frontend
- [ ] Backend pod starts and connects to PostgreSQL + Ory
- [ ] Frontend pod serves WASM assets
- [ ] WASM RuntimeClass exists and Option B pod can start (experimental)

---

## 9. Open Questions

- **Q-1**: Use Kustomize overlays or Helm charts for environment differences? — **Decision**: Kustomize (simpler, no templating engine)
- **Q-2**: Container registry for local images — use Rancher Desktop's built-in registry or `nerdctl` direct load? — **Decision**: nerdctl direct load (load images directly into containerd, no registry)
- **Q-3**: TLS for local dev? Self-signed certs via cert-manager or plain HTTP? — **Decision**: Plain HTTP for local dev (TLS only for staging/production)
