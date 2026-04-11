# DnXT Support Access / Login-As Pattern

## Overview

DnXT staff can access any tenant's application environment from the Global Admin Portal (`admin.dnxtcloud.com`) via a secure "Login As" mechanism. This pattern is **reusable across all DnXT products** — Operations, Reviewer, Publisher, EDMS, Planner, and any future module.

## How It Works

```
Global Admin (admin.dnxtcloud.com)
  ↓ Super Admin clicks "Access Tenant"
  ↓ POST /api/tenants/{id}/support-access
  ↓
  ↓ Global Admin backend → calls target service internal API
  ↓ POST /api/internal/support-token (X-Internal-Api-Key header)
  ↓
  ↓ Target service checks:
  ↓   1. API key valid (constant-time comparison)
  ↓   2. Rate limit not exceeded (5/min)
  ↓   3. support_access_enabled config = true (tenant can disable)
  ↓   4. Active sessions < 3 (concurrent limit)
  ↓
  ↓ Target service generates:
  ↓   - dnxt-support user (created on demand, no password, VIEW-only role)
  ↓   - 30-minute JWT with support claims (session_type, requested_by, jti)
  ↓   - One-time UUID code mapped to the JWT (5-min TTL)
  ↓   - Session record in ops_support_session table
  ↓
  ↓ Returns: {code: "uuid", expiresInSeconds: 300}
  ↓
  ↓ Global Admin builds redirect URL:
  ↓ https://{tenant}-{module}.dnxtcloud.com/support-login?code={uuid}
  ↓
  → Opens new browser tab to that URL
    → Target SPA reads ?code, replaceState() to remove from URL
    → POST /api/auth/support-exchange {code}
    → Code exchanged for JWT (single-use, removed from map)
    → JWT stored in localStorage, redirect to /dashboard
    → Support banner displayed: "DnXT Support Session — Read-only. All actions logged."
```

## Security Model

| Layer | Protection |
|-------|-----------|
| **Network** | Internal API only accessible via Azure Container Apps internal networking |
| **Authentication** | API key with constant-time comparison (MessageDigest.isEqual) |
| **Rate Limiting** | 5 requests/min on support-token endpoint |
| **Tenant Control** | `support_access_enabled` config — tenant admin can disable at any time |
| **Concurrent Sessions** | Max 3 active support sessions |
| **Token Security** | JWT never in URL — one-time code exchanged via POST |
| **Code Security** | UUID (128-bit entropy), single-use, 5-min TTL |
| **Token Expiry** | 30-minute hardcoded expiry (not configurable) |
| **Token Claims** | `session_type=support`, `requested_by`, `support_session_id`, `jti` |
| **Token Revocation** | JTI blacklist checked in JwtAuthFilter, tenant admin can revoke |
| **Password Protection** | dnxt-support has null passwordHash, AuthService blocks null-password logins |
| **User Visibility** | dnxt-support filtered from user list endpoints |
| **Audit Trail** | ops_support_audit table + Global Admin audit_log (both sides) |
| **Least Privilege** | DNXT_SUPPORT role has VIEW-only permissions (no CREATE/EDIT/DELETE/MANAGE) |
| **Browser History** | replaceState() removes code from URL immediately after read |

## Adding Login-As to a New Module

When onboarding a new module (Reviewer, Publisher, EDMS, Planner), follow these steps:

### Step 1: Backend — Target Service

**1a. Flyway Migration** (`V{N}__support_access.sql`)
```sql
-- Make password_hash nullable for system accounts
ALTER TABLE {module}_user ALTER COLUMN password_hash DROP NOT NULL;

-- Support access config
INSERT INTO {module}_config (config_key, config_value, category, description, is_secret)
VALUES ('support_access_enabled', 'true', 'SECURITY',
        'When enabled, DnXT staff can access via Global Admin Login-As', false);

-- Dedicated support role (VIEW-only)
INSERT INTO {module}_role (role_id, role_name, role_label, description, is_system)
VALUES ('role-dnxt-support', 'DNXT_SUPPORT', 'DnXT Support',
        'Restricted read-only access for DnXT support staff', true);

-- Grant only VIEW/EXPORT permissions
INSERT INTO {module}_role_permission (role_id, permission_id)
SELECT 'role-dnxt-support', p.permission_id
FROM {module}_permission p
WHERE p.action IN ('VIEW', 'VIEW_OWN', 'VIEW_ALL', 'EXPORT');

-- Support audit + session tables
CREATE TABLE {module}_support_audit (...);
CREATE TABLE {module}_support_session (...);
```

**1b. InternalController.java** — Copy from Operations, adapt package/imports
- `POST /api/internal/support-token` (API-key secured)
- `POST /api/auth/support-exchange` (public)
- `GET /api/internal/support-access-status` (API-key secured)
- `POST /api/internal/revoke-session` (API-key secured)
- `GET /api/internal/health` (public)
- `@Scheduled` cleanup

**1c. JwtTokenProvider.java** — Add `generateSupportToken()` method
- 30-min expiry, support claims: `session_type`, `requested_by`, `support_session_id`, `jti`
- `isBlacklisted()` and `blacklistToken()` for revocation

**1d. JwtAuthFilter.java** — Add blacklist check
- After token validation, check JTI against blacklist

**1e. AuthService.java** — Add null password guard
- Before BCrypt.matches(), check if passwordHash is null → reject

**1f. User list endpoints** — Filter out `dnxt-support`

**1g. Settings endpoints** — Add support access toggle + session management
- `GET /api/settings/support-access`
- `PUT /api/settings/support-access`
- `GET /api/settings/support-sessions`
- `POST /api/settings/support-sessions/{id}/revoke`

**1h. SecurityConfig.java** — Ensure patterns in permitAll:
- `/api/auth/**` (includes support-exchange)
- `/api/internal/**` (secured by API key, not Spring Security)

**1i. application.properties**
```properties
{module}.internal.api-key=${OPS_INTERNAL_API_KEY:}
```

### Step 2: Frontend — Target Service

**2a. SupportLoginPage.tsx** (new)
```tsx
// Reads ?code from URL
// Immediately replaceState() to remove code
// POST /api/auth/support-exchange {code}
// On success: store JWT, navigate to /dashboard
// On failure: show "expired or invalid" message
```

**2b. App.tsx** — Add route
```tsx
<Route path="/support-login" element={<SupportLoginPage />} />
```

**2c. Layout.tsx** — Support session banner
```tsx
{user?.username === 'dnxt-support' && (
  <div className="support-banner">
    ⚠ DnXT Support Session — Read-only. All actions logged.
  </div>
)}
```

### Step 3: Global Admin

**3a. Create `{Module}ProvisioningService.java`** — calls the module's internal API

**3b. Update `SupportAccessService.java`** — add method for the new module's internal URL

**3c. Update `TenantController.java`** — add support-access endpoint per module (or make it generic with a module parameter)

**3d. Update `TenantDetailPage.tsx`** — add "Access" button per enabled module

### Step 4: Azure Deployment

```bash
# Generate shared API key (one per module, or reuse)
openssl rand -hex 32

# Set on target service
az containerapp update --name {module-service} --resource-group dnxt-container-rg \
  --set-env-vars "OPS_INTERNAL_API_KEY={key}"

# Set on Global Admin
az containerapp update --name dnxt-global-admin --resource-group dnxt-container-rg \
  --set-env-vars "{MODULE}_INTERNAL_API_KEY={key}" \
    "{MODULE}_INTERNAL_URL=https://{module-service}.internal.jollyfield-1cab1a17.eastus.azurecontainerapps.io"
```

## Audit Events

| Event | Where Logged | Fields |
|-------|-------------|--------|
| SUPPORT_ACCESS_INITIATED | Global Admin `audit_log` | userId, tenantId, tenantSlug, IP |
| SUPPORT_TOKEN_GENERATED | Target `{module}_support_audit` | sessionId, requestedBy, reason, IP |
| SUPPORT_LOGIN | Target `{module}_support_audit` | sessionId, requestedBy, IP |
| SUPPORT_SESSION_REVOKED | Target `{module}_support_audit` | sessionId, revokedBy, IP |
| SUPPORT_ACCESS_BLOCKED | Global Admin `audit_log` | tenantId, reason (disabled by tenant) |

## Tenant Admin Controls

Tenant administrators can:
1. **Toggle support access** — Settings → Security → "Allow DnXT Support Team Access"
2. **View active sessions** — Settings → Security → "Active Support Sessions"
3. **Revoke sessions** — Click "Revoke" on any active session (immediately invalidates the JWT)
4. **View history** — Support audit log shows all past access with requestedBy, timestamps, IPs

## Key Files (Operations — reference implementation)

| File | Purpose |
|------|---------|
| `V53__support_access.sql` | Schema migration: config, role, audit/session tables |
| `InternalController.java` | Core: token generation, code exchange, revocation |
| `JwtTokenProvider.java` | Support token generation + blacklist |
| `JwtAuthFilter.java` | Blacklist check on every request |
| `AuthService.java` | Null password guard |
| `AdminController.java` | Filter support user from lists |
| `ConfigController.java` | Support access toggle + session management |
| `SecurityConfig.java` | Endpoint authorization rules |
| `SupportLoginPage.tsx` | Frontend code exchange page |
| `Layout.tsx` | Support session banner |
| `App.tsx` | /support-login route |
