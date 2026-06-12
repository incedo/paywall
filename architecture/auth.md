# Authentication & Authorization — OIDC + Ory via BFF

**Status**: AGREED
**Last Updated**: 2026-04-18
**Depends On**: architecture/tech-stack.md, architecture/module-structure.md

---

## 1. Overview

Authentication and authorization use **OpenID Connect (OIDC)** backed by the **Ory** open-source identity stack. The CRM never stores passwords or manages login flows directly — it validates OIDC tokens issued by Ory Hydra and reads user identity from Ory Kratos.

The OIDC dance is wired as a **Backend-for-Frontend (BFF)**. The backend owns PKCE, state, and code exchange; every client (web/desktop/Android/iOS) only asks the backend to start a flow and then either receives an HttpOnly session cookie (web) or drains tokens from a one-shot exchange key (mobile/desktop). This is the only shape that satisfies the Kotlin Multiplatform rule in `CLAUDE.md`: every client capability must either exist in commonMain already or be expressible as a thin HTTP adapter to the backend.

### Why Ory?
- Open-source, self-hosted — no vendor lock-in.
- Kratos handles identity (registration, login, recovery, settings, profile).
- Hydra handles OAuth2/OIDC (code exchange, RS256 JWT issuance, consent).
- Clean separation: the CRM is a pure OIDC relying party.
- Docker-native — single-command local bring-up via `scripts/start-ory.sh`.

### Why BFF (and not direct SPA-PKCE)?
A direct SPA flow needs SHA-256, URL-safe random bytes, session storage, and an OS-native browser-tab integration on every target. Implementing all four on wasm + JVM (desktop + Android) + Kotlin/Native (iOS) is four parallel native integrations. BFF moves the crypto to one place the backend already owns, collapses per-platform work to "open a URL + poll one HTTP endpoint," and keeps the frontend code identical across targets.

---

## 2. Ory Component Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                      CRM Frontend (KMP)                            │
│                                                                    │
│  Compose screens (CrmTheme tokens only, no third-party UI):        │
│     LoginScreen          RegistrationScreen    RecoveryScreen      │
│     RecoveryCodeScreen   ResetPasswordScreen   SettingsScreen      │
│     CallbackScreen                                                 │
│                                                                    │
│  Platform launchers (expect/actual):                               │
│     wasmJs  → BffAuth.beginLoginRedirect (whole-window navigate)   │
│     jvm     → DesktopAuth / Desktop.browse + /exchange polling     │
│     android → AndroidAuth / CustomTabsIntent + polling             │
│     iOS     → IosAuth / ASWebAuthenticationSession + polling       │
└─────────────────────────────────┬──────────────────────────────────┘
                                  │ HTTP (cookie or Bearer)
                                  ▼
┌────────────────────────────────────────────────────────────────────┐
│                      CRM Backend (Ktor)                            │
│                                                                    │
│  BffAuthController                 AuthConfig (JWT realm)          │
│   GET  /api/v1/auth/login           RS256 verify via Hydra JWKS    │
│   GET  /api/v1/auth/callback        cookie ↔ bearer fallback       │
│   GET  /api/v1/auth/mobile-callback                                │
│   GET  /api/v1/auth/exchange        HydraAuthController            │
│   POST /api/v1/auth/logout           /auth/login/{challenge,accept}│
│                                      /auth/consent/{challenge,accept}│
│  BffAuthService (SHA256 + PKCE)                                    │
│                                    HttpKratosAdmin                 │
│  VerifierStore (pluggable)          POST  /admin/identities        │
│   ├─ InMemoryVerifierStore          PATCH /admin/identities/{id}   │
│   └─ RedisVerifierStore                                            │
└──────────┬────────────────────┬────────────────────┬───────────────┘
           │ Admin + /oauth2    │ Admin              │ Set/get
           ▼                    ▼                    ▼
┌──────────────────┐    ┌──────────────────┐   ┌──────────────────┐
│ Ory Hydra v2.3.0 │    │ Ory Kratos v1.3.1│   │ Redis 7-alpine   │
│ :4444 public     │◀──▶│ :4433 public     │   │ :6379            │
│ :4445 admin      │    │ :4434 admin      │   │ (verifier TTLs)  │
└────────┬─────────┘    └────────┬─────────┘   └──────────────────┘
         │                       │
         └─────────┬─────────────┘
                   ▼
         ┌──────────────────┐
         │ PostgreSQL 16    │
         │ dbs: kratos,hydra│
         └──────────────────┘
```

---

## 3. Token & Claims Strategy

- **Algorithm**: RS256. Hydra signs access tokens with its own key; the backend pulls Hydra's JWKS from `OIDC_JWKS_URL` (`/.well-known/jwks.json`) with a 10-hour in-process cache (Auth0 `jwks-rsa` library).
- **Issuer**: `OIDC_ISSUER_URL` (default `http://localhost:4444/` in dev) is pinned in the verifier; tokens with a different `iss` are rejected.
- **Audience**: not enforced — Hydra doesn't populate `aud` on access tokens by default. Add a `requiredAudience` check later if we ever split the API into multiple resource servers.
- **Top-level claims**: Hydra is configured with `OAUTH2_ALLOWED_TOP_LEVEL_CLAIMS=email,name,role`, and the consent handler injects those into `session.access_token` so they land as JWT claims. `AuthConfig.kt`'s `validate {}` block extracts `sub`/`email`/`name`/`role` identically in both HMAC (dev) and RS256 (Ory) modes.
- **Leeway**: 10 s clock skew tolerance in ORY mode.
- **Dev mode (HMAC256)**: a parallel verifier is wired when `AUTH_MODE=DEV` (default) or no `OIDC_JWKS_URL` is set. `/auth/dev-login` mints short-lived HMAC tokens. In `AUTH_MODE=ORY` that endpoint returns 403 so a production build cannot accidentally issue self-signed tokens.

---

## 4. End-to-end Flows

### 4.1 Web (Wasm)

```
User → :8081/                       (admin app)
App → AuthStateHolder.checkSession
  GET /api/v1/auth/me → 401 (no cookie)
App → push AdminConfig.Login
Login screen (no login_challenge in URL)
  LaunchedEffect { beginPlatformLogin() }
  → BffAuth.beginLoginRedirect()
    → window.location = /api/v1/auth/login
Backend /auth/login
  → generate verifier + state, store verifier
  → 302 to Hydra /oauth2/auth?code_challenge=...&state=...
Hydra
  → no session → 302 to URLS_LOGIN=:8081/login?login_challenge=X
App re-boots at /login?login_challenge=X
  → AuthStateHolder 401 again, pushed to AdminConfig.Login
  → LoginScreen renders form (challenge present)
  → user submits email/password → KratosClient.submitLogin
    → Kratos sets session cookie in browser
  → HydraLoginBridge.acceptLogin(challenge, subject, claims)
    → backend PUT /admin/oauth2/auth/requests/login/accept
    → returns { redirect_to: /oauth2/auth/... }
  → redirectTo(redirect_to)
Hydra issues code → 302 to /api/v1/auth/callback?code=X&state=Y
Backend /callback
  → VerifierStore.takeVerifier(state)
  → POST Hydra /oauth2/token (code + verifier) → tokens
  → Set-Cookie: crm_session=<JWT> HttpOnly SameSite=Lax
  → 302 to POST_LOGIN_REDIRECT (:8081/)
App boots, checkSession succeeds via cookie.
```

### 4.2 Desktop (JVM)

```
App → Login screen → LaunchedEffect { beginPlatformLogin() }
  → BffAuth.beginLoginMobile() → GET /auth/login?mode=exchange
    → { authorizeUrl, exchangeKey }
  → Desktop.browse(authorizeUrl)   (system browser opens)
  → loop: BffAuth.completeLoginMobile(exchangeKey)
        → GET /auth/exchange?key=<state>
        → 410 Gone (not yet) → delay 1.5s → retry
User authenticates in system browser; Hydra → /api/v1/auth/mobile-callback
  → Backend stores tokens under exchangeKey, responds { ok: true }
Next poll by the desktop app:
  → GET /auth/exchange drains tokens
  → ApiClient.authToken = access_token
Desktop is now authenticated; AuthStateHolder.checkSession passes.
```

### 4.3 Android

Identical to desktop but `Desktop.browse()` is replaced with `CustomTabsIntent.launchUrl(context, ...)`; the `Context` is registered globally via `AndroidAuthContext.register(this)` from `MainActivity.onCreate`.

### 4.4 iOS

`ASWebAuthenticationSession` opens the authorize URL with a `crm-app` callback scheme. The polling loop on `/auth/exchange` is identical to desktop/android. Dismissal of the web sheet is a UX nicety — correctness comes from polling, not from intercepting the callback URL.

---

## 5. Backend Surface (controllers + ports)

| File | Role |
|---|---|
| `backend/src/main/kotlin/crm/backend/config/AuthConfig.kt` | JWT realm config; AUTH_MODE switch; cookie↔bearer fallback via `authHeader {}` |
| `backend/src/main/kotlin/crm/backend/config/Plugins.kt` | CORS with explicit origins + `allowCredentials=true` (required for cookie auth) |
| `backend/src/main/kotlin/crm/backend/user/BffAuthController.kt` | `/auth/login`, `/callback`, `/mobile-callback`, `/exchange`, `/logout` |
| `backend/src/main/kotlin/crm/backend/user/HydraAuthController.kt` | `/auth/login/{challenge,accept}`, `/auth/consent/{challenge,accept}` bridge to Hydra Admin |
| `backend/src/main/kotlin/crm/backend/user/HttpKratosAdmin.kt` | `KratosAdminPort` HTTP adapter (create identity, role patch, state patch) |
| `backend/src/main/kotlin/crm/backend/user/VerifierStore.kt` | Pluggable store — InMemory (default) or Redis (when `REDIS_URL` set) |
| `backend/src/main/kotlin/crm/backend/user/AuthController.kt` | `/auth/dev-login` (AUTH_MODE=DEV only) + `/auth/me` |

---

## 6. Frontend Surface (commonMain + per-platform actuals)

| File | Target | Role |
|---|---|---|
| `frontend/common/src/commonMain/kotlin/crm/frontend/state/auth/AuthState.kt` | common | `AuthStateHolder` — Checking/Authenticated/Unauthenticated/LoginRedirecting state machine |
| `frontend/common/src/commonMain/kotlin/crm/frontend/api/BffAuth.kt` | common | `beginLoginRedirect` / `beginLoginMobile` / `completeLoginMobile` / `logout` |
| `frontend/common/src/commonMain/kotlin/crm/frontend/api/KratosClient.kt` | common | Typed wrapper for Kratos Self-Service API (login/register/recovery/recovery-code/settings) |
| `frontend/common/src/commonMain/kotlin/crm/frontend/api/HydraLoginBridge.kt` | common | Typed wrapper for `/auth/login/{challenge,accept}` |
| `frontend/common/src/commonMain/kotlin/crm/frontend/api/PlatformLogin.kt` | common | `expect suspend fun beginPlatformLogin(): Boolean` |
| `frontend/common/src/wasmJsMain/.../PlatformLogin.wasmJs.kt` | Wasm | `BffAuth.beginLoginRedirect()` + `awaitCancellation()` |
| `frontend/common/src/jvmMain/.../PlatformLogin.jvm.kt` | JVM | `Desktop.browse()` + poll `/auth/exchange` |
| `frontend/common/src/androidMain/.../PlatformLogin.android.kt` | Android | `CustomTabsIntent` via registered `AndroidAuthContext` + poll |
| `frontend/common/src/iosMain/.../PlatformLogin.ios.kt` | iOS | `UIApplication.openURL` + poll (frontend:ios has a richer `IosAuth` using ASWebAuthenticationSession) |
| `frontend/common/src/commonMain/kotlin/crm/frontend/screen/auth/*Screen.kt` | common | Compose screens using `CrmTheme` tokens only |

---

## 7. Configuration (env vars)

| Variable | Default | Used by |
|---|---|---|
| `AUTH_MODE` | `DEV` (or `ORY` if `OIDC_JWKS_URL` set) | AuthConfig |
| `OIDC_ISSUER_URL` | `http://localhost:4444/` | JWT verifier |
| `OIDC_JWKS_URL` | `http://localhost:4444/.well-known/jwks.json` | JWT verifier |
| `OIDC_CLIENT_ID` | `crm-frontend` | BFF authorize URL |
| `OIDC_REDIRECT_URI` | `http://localhost:8080/api/v1/auth/callback` | BFF web redirect |
| `OIDC_MOBILE_REDIRECT_URI` | `http://localhost:8080/api/v1/auth/mobile-callback` | BFF mobile/desktop redirect |
| `POST_LOGIN_REDIRECT` | `http://localhost:8081/` | after `/callback` |
| `KRATOS_ADMIN_URL` | unset | swaps `NoOpKratosAdmin` → `HttpKratosAdmin` |
| `HYDRA_ADMIN_URL` | unset | gates `hydraAuthRoutes` + `bffAuthRoutes` |
| `HYDRA_PUBLIC_URL` | falls back to issuer | BFF authorize/token endpoints |
| `REDIS_URL` | unset | swaps verifier store to Redis |
| `CORS_ALLOWED_ORIGINS` | `localhost:8081,8082,3000` | CORS allowlist (comma-separated) |
| `JWT_SECRET` / `JWT_ISSUER` / `JWT_AUDIENCE` | dev values | Dev-mode HMAC only |

---

## 8. Identity Schema & Roles

Kratos identity schema: `docker/ory/kratos/identity.schema.json`.

```json
{
  "traits": {
    "email":  { "identifier": true, "verification": { "via": "email" } },
    "name":   "string",
    "role":   "enum [ADMIN, MANAGER, USER, PROSPECT, CUSTOMER]"
  }
}
```

`UserRole` lives in `shared/src/commonMain/kotlin/crm/user/domain/UserRole.kt`. `CrmPrincipal` (backend) reads the role from the JWT claim; `PortalRoleUpgradeReactor` transitions PROSPECT → CUSTOMER on `CustomerActivated` by calling `KratosAdminPort.updateRole`.

---

## 9. Security Posture

**Cookies (web)**
- `crm_session` is `HttpOnly`, `SameSite=Lax`, `Path=/`, not marked `Secure` in dev (HTTP). Production MUST set `secure=true` and serve over TLS.
- Cookie holds the access token directly (~1h TTL); no server-side session table. Refresh is deferred — user re-authenticates when the token expires.

**PKCE**
- S256 challenge method.
- 10-minute TTL on the verifier (matches Hydra login-request lifespan).
- `ConcurrentHashMap` in-memory by default; `Redis` with `GETDEL` drain-once semantics when `REDIS_URL` is set.

**CORS**
- Explicit origin allowlist — wildcard origins are forbidden with credentialed requests.

**Dev-login**
- Env-gated: `AUTH_MODE=ORY` ⇒ `/auth/dev-login` returns 403.

---

## 10. Infra & Startup

Canonical compose file: `docker/docker-compose.yml`. Services:

| Service | Image | Ports |
|---|---|---|
| postgres | `postgres:16-alpine` | 5432 |
| kratos-migrate | `oryd/kratos:v1.3.1` | — |
| kratos | `oryd/kratos:v1.3.1` | 4433 (public), 4434 (admin) |
| hydra-migrate | `oryd/hydra:v2.3.0` | — |
| hydra | `oryd/hydra:v2.3.0` | 4444 (public), 4445 (admin) |
| oidc-client-init | `curlimages/curl:8.10.1` | one-shot; runs `docker/setup-oidc-client.sh` |
| mailslurper | `oryd/mailslurper:latest-smtps` | 4436 (UI), 1025 (SMTP) |
| redis | `redis:7-alpine` | 6379 |

Bring the whole thing up:

```bash
./scripts/start-ory.sh --all
```

This runs the stack + backend (`AUTH_MODE=ory`, `REDIS_URL=redis://localhost:6379`) + admin web (:8081) + portal (:8082). Seed admin: `admin@crm.local` / `Admin123!`.

---

## 11. Deliberately out of scope

- Email verification UI (Kratos config already allows it; screen deferred).
- Social OIDC providers (Google, GitHub) — `methods.oidc.enabled: false` in Kratos config.
- Refresh-token rotation on the frontend (current tokens are 1h; user re-authenticates on expiry).
- Multi-tenant identity — single identity pool today.
- Session introspection endpoint (backend uses JWKS verification only, no `/oauth2/introspect`).
- Redis Cluster / Sentinel — single-node Redis only.

---

## 12. References

- Ory Kratos docs: https://www.ory.sh/docs/kratos
- Ory Hydra docs: https://www.ory.sh/docs/hydra
- OAuth2 PKCE (RFC 7636)
- `CLAUDE.md` §Multiplatform Rule
- `TODO.md` Phase 4 — Real adapters
