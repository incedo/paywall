#!/usr/bin/env bash
# deploy-local.sh — Deploy the paywall stack to the local Kubernetes cluster.
#
# Prerequisites:
#   - kubectl configured to point at the local cluster (e.g. Docker Desktop, kind, k3d)
#   - Traefik installed with traefik.io/v1alpha1 CRDs
#   - /etc/hosts entries (or local DNS) for paywall.local and its subdomains:
#       127.0.0.1 paywall.local auth.paywall.local id.paywall.local login.paywall.local mail.paywall.local
#   - Local images built:
#       docker build -t paywall-backend:dev -f backend/Dockerfile .
#       docker build -t paywall-frontend:dev -f composeApp/Dockerfile .
#
# Usage:
#   ./k8s/deploy-local.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="paywall-dev"

# Colours for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Colour

info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ── 1. Namespace ──────────────────────────────────────────────────────────────
info "Applying namespace..."
kubectl apply -f "${SCRIPT_DIR}/base/namespace.yaml"

# ── 2. Full stack via kustomize overlay ───────────────────────────────────────
info "Applying local overlay (kustomize)..."
kubectl apply -k "${SCRIPT_DIR}/local/"

# ── 3. Wait for PostgreSQL ─────────────────────────────────────────────────────
info "Waiting for PostgreSQL to become ready (timeout 120s)..."
if kubectl wait \
    --for=condition=ready pod \
    -l app=postgres \
    -n "${NAMESPACE}" \
    --timeout=120s; then
  success "PostgreSQL is ready."
else
  error "PostgreSQL did not become ready within 120s."
  kubectl describe pod -l app=postgres -n "${NAMESPACE}" || true
  exit 1
fi

# ── 4. Wait for Kratos ────────────────────────────────────────────────────────
info "Waiting for Kratos to become ready (timeout 180s)..."
if kubectl wait \
    --for=condition=available deployment/kratos \
    -n "${NAMESPACE}" \
    --timeout=180s; then
  success "Kratos is ready."
else
  warn "Kratos deployment not yet available — it may still be running migrations."
  kubectl describe deployment kratos -n "${NAMESPACE}" || true
fi

# ── 5. Wait for Hydra ─────────────────────────────────────────────────────────
info "Waiting for Hydra to become ready (timeout 180s)..."
if kubectl wait \
    --for=condition=available deployment/hydra \
    -n "${NAMESPACE}" \
    --timeout=180s; then
  success "Hydra is ready."
else
  warn "Hydra deployment not yet available — it may still be running migrations."
  kubectl describe deployment hydra -n "${NAMESPACE}" || true
fi

# ── 6. Wait for OIDC client registration job ──────────────────────────────────
info "Waiting for OIDC client init job to complete (timeout 120s)..."
if kubectl wait \
    --for=condition=complete job/oidc-client-init \
    -n "${NAMESPACE}" \
    --timeout=120s 2>/dev/null; then
  success "OIDC client registered."
else
  warn "OIDC client init job not yet complete — check logs:"
  warn "  kubectl logs -l app=oidc-client-init -n ${NAMESPACE}"
fi

# ── 7. Print access URLs ──────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}============================================================${NC}"
echo -e "${GREEN}  paywall-dev is up!${NC}"
echo -e "${GREEN}============================================================${NC}"
echo ""
echo -e "  ${CYAN}Admin console (frontend):${NC}   http://paywall.local/"
echo -e "  ${CYAN}Decide API (backend):${NC}        http://paywall.local/api/v1/decide"
echo -e "  ${CYAN}Hydra (OAuth2/OIDC):${NC}         http://auth.paywall.local/"
echo -e "  ${CYAN}Kratos (identity):${NC}           http://id.paywall.local/"
echo -e "  ${CYAN}Login UI:${NC}                    http://login.paywall.local/login"
echo -e "  ${CYAN}MailSlurper (email):${NC}         http://mail.paywall.local/"
echo ""
echo -e "  ${YELLOW}Tip:${NC} Watch pods with:  kubectl get pods -n ${NAMESPACE} -w"
echo -e "  ${YELLOW}Tip:${NC} Backend logs:     kubectl logs -l app=backend -n ${NAMESPACE} -f"
echo ""
