# Phase 2 Player Client API and H5 Design

## 1. Document Info

| Item | Value |
| --- | --- |
| Date | 2026-07-09 |
| Phase | Phase 2 |
| Scope | Player-facing client API and H5 integration |
| Primary Surfaces | Spring Boot backend, Vue3 H5 |
| Deferred Surfaces | Flutter App real integration, Cocos game runtime, real payment, KYC provider |

## 2. Goal

Build the first player-facing closed loop after Phase 1: H5 can load tenant bootstrap data, log in with a demo member session, show wallet balances, show a game lobby, and display clear disabled states for features that are not live yet.

This phase turns the current static H5 scaffold into a backend-backed client shell without adding high-risk real-money operations.

## 3. Non-Goals

- No real payment provider integration.
- No KYC provider integration.
- No production third-party game provider integration.
- No App Store or Google Play release workflow.
- No full Flutter App API integration in this slice.
- No Cocos game package implementation.
- No complex CMS or promotion rule engine.

## 4. Current Context

The backend already contains business modules for member, wallet, game, payment, promotion, redemption, and report under `backend/gameluck-modules`.

The H5 project already contains routes for:

| Route | Current Purpose |
| --- | --- |
| `/` | Static brand home |
| `/login` | Static login page |
| `/register` | Static register page |
| `/wallet` | Static wallet page |
| `/games` | Static game list page |
| `/promotions` | Static promotions page |
| `/redemptions` | Static redemption page |
| `/help` | Static help page |

Phase 2 should reuse these routes and replace demo data with API-driven state.

## 5. Recommended Approach

Use a thin client API layer under `/api/client/**`.

The backend keeps admin endpoints unchanged. Client endpoints expose a narrow, read-heavy and demo-safe surface:

- site bootstrap
- login and logout session
- current member profile
- wallet balances
- wallet ledger list
- game lobby list
- game launch placeholder

The first implementation may use local demo credentials and existing wallet/member tables. It must still preserve tenant context, idempotency boundaries for wallet operations, and i18n message rules.

## 6. Architecture

```mermaid
flowchart LR
  H5[Vue3 H5] --> ApiClient[H5 API Client]
  ApiClient --> ClientApi[/api/client/**]
  ClientApi --> Member[member module]
  ClientApi --> Wallet[wallet module]
  ClientApi --> Game[game module]
  ClientApi --> Bootstrap[bootstrap config service]
  Wallet --> DB[(MySQL)]
  Member --> DB
  Game --> DB
```

### Backend Boundary

Client controllers should live outside the admin controller namespace and use `/api/client/**`.

Recommended package:

```text
backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client
backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/client
```

If repeated client-session utilities are needed, create a small shared package in `gameluck-common-core` only when two or more modules require the same code.

### H5 Boundary

H5 should add a small API client and app state layer:

```text
h5/src/api/client.ts
h5/src/stores/session.ts
h5/src/types/client.ts
```

No heavy state library is needed for this phase. Vue `reactive` state is enough.

## 7. API Contract

### 7.1 Bootstrap

```http
GET /api/client/bootstrap
```

Request headers:

```text
X-Channel-Code: h5
X-Brand-Code: demo
Content-Language: zh_CN or en_US
```

Response:

```json
{
  "tenantId": "000000",
  "brandCode": "demo",
  "channelCode": "h5",
  "brandName": "GameLuck",
  "theme": {
    "logoText": "GameLuck",
    "primaryColor": "#1f7a4d"
  },
  "features": {
    "walletEnabled": true,
    "gameEnabled": true,
    "promotionEnabled": true,
    "redemptionEnabled": false,
    "paymentEnabled": false,
    "kycEnabled": false
  },
  "currencies": [
    {
      "currencyCode": "GC",
      "currencyName": "Gold Coin",
      "decimalScale": 2,
      "playable": true,
      "rechargeable": false,
      "withdrawable": false
    },
    {
      "currencyCode": "SC",
      "currencyName": "Sweep Coin",
      "decimalScale": 2,
      "playable": true,
      "rechargeable": false,
      "withdrawable": false
    }
  ]
}
```

Rules:

- Missing channel defaults to `h5`.
- Unknown brand defaults to `demo` in local development.
- Disabled features must be returned explicitly so H5 can show unavailable states.

### 7.2 Login

```http
POST /api/client/auth/login
```

Request:

```json
{
  "username": "demo_player",
  "password": "Demo123456"
}
```

Response:

```json
{
  "accessToken": "client-session-token",
  "expiresIn": 7200,
  "member": {
    "memberId": "1001",
    "memberNo": "M1001",
    "username": "demo_player",
    "status": "normal",
    "kycStatus": "not_required"
  }
}
```

Rules:

- Phase 2 can support one seeded local demo account first.
- Do not reuse admin login sessions for H5 players.
- Failed login returns a localized backend message.
- Token storage on H5 uses `localStorage` for this phase.

### 7.3 Current Member

```http
GET /api/client/member/me
```

Rules:

- Requires client token.
- Returns the member identity and status used by H5.
- If token is missing or expired, return 401 with localized message.

### 7.4 Wallet Accounts

```http
GET /api/client/wallet/accounts
```

Response:

```json
[
  {
    "currencyCode": "GC",
    "currencyName": "Gold Coin",
    "availableBalance": "1000.00",
    "frozenBalance": "0.00",
    "decimalScale": 2,
    "playable": true,
    "withdrawable": false
  }
]
```

Rules:

- Requires client token.
- Only returns accounts for the current member and current tenant.
- H5 must not pass arbitrary `memberId`.

### 7.5 Wallet Ledgers

```http
GET /api/client/wallet/ledgers?currencyCode=GC&pageNum=1&pageSize=20
```

Response:

```json
{
  "records": [
    {
      "ledgerId": "90001",
      "currencyCode": "GC",
      "direction": "credit",
      "amount": "1000.00",
      "afterAvailable": "1000.00",
      "bizType": "demo_seed",
      "createdAt": "2026-07-09T12:00:00+08:00"
    }
  ],
  "total": 1
}
```

Rules:

- Requires client token.
- Filter by current tenant and current member.
- Page size is capped at 50.

### 7.6 Game Lobby

```http
GET /api/client/games?currencyCode=GC
```

Response:

```json
[
  {
    "providerCode": "mock",
    "gameCode": "mock-slot-001",
    "gameName": "Mock Slot",
    "status": "enabled",
    "supportedCurrencies": ["GC", "SC"],
    "thumbnailUrl": "",
    "maintenance": false
  }
]
```

Rules:

- Public read is allowed after bootstrap.
- If currency is disabled, return an empty list with `code=200`.
- Mock games are enough for Phase 2.

### 7.7 Game Launch Placeholder

```http
POST /api/client/games/launch
```

Request:

```json
{
  "providerCode": "mock",
  "gameCode": "mock-slot-001",
  "currencyCode": "GC"
}
```

Response:

```json
{
  "sessionNo": "GS202607090001",
  "launchMode": "placeholder",
  "launchUrl": "",
  "message": "Game launch is not live yet."
}
```

Rules:

- Requires client token.
- This endpoint does not debit wallet balance in Phase 2.
- H5 shows a controlled placeholder state instead of navigating to an external game.

## 8. H5 User Experience

### Shell

The top navigation should keep the existing routes but become session-aware:

- Logged out: show Login.
- Logged in: show member name and Logout.
- Disabled features: show a disabled state inside the page rather than hiding all navigation.

### Home

Home loads bootstrap data and shows:

- brand name
- enabled currencies
- feature status
- quick links to wallet and games

### Login

Login submits to `/api/client/auth/login`, stores the returned token, loads `/api/client/member/me`, and redirects to `/wallet`.

Error states:

- invalid username or password
- network error
- backend localized error message

### Wallet

Wallet requires login. If logged out, show a login-required state with a login action.

When logged in, show:

- balance cards by currency
- frozen balance
- ledger table or mobile list
- empty ledger state
- loading and error states

### Games

Games reads bootstrap currencies and calls `/api/client/games`.

Each game shows:

- name
- provider
- supported currencies
- maintenance or enabled state
- Launch button

Launch button calls `/api/client/games/launch` and shows the placeholder response.

## 9. Error Handling

Backend responses follow the existing `R` response shape.

H5 must handle:

| Case | H5 Behavior |
| --- | --- |
| 401 | Clear token, show login-required state |
| 403 | Show permission/feature unavailable state |
| Network error | Show retry action |
| Backend business error | Show backend message |
| Empty list | Show empty state, not an error |

Backend messages must use existing i18n rules and pass `pnpm --dir admin-ui check:i18n`.

## 10. Security and Tenant Rules

- Client APIs must not accept `tenantId` or `memberId` from H5 request bodies for authenticated member-owned data.
- Tenant context is resolved from current local default, host, brand header, or existing tenant context utilities.
- Member context is resolved from the client token.
- Admin `SaCheckPermission` annotations remain on admin endpoints only.
- Client endpoints must not expose admin-only fields.

## 11. Data Setup

Phase 2 needs a local seed script or idempotent SQL section for:

- demo member `demo_player`
- GC and SC wallet accounts for demo member
- initial GC/SC balances
- at least one mock game

The seed must be safe to rerun by using unique keys or delete-and-insert for demo-only records.

## 12. Testing Strategy

Backend:

- Unit tests for client login success and failure.
- Unit tests for wallet account filtering by current member.
- Unit tests for game launch placeholder.
- Compile `gameluck-admin` with local profile.

H5:

- Type check and build.
- API client tests can be added if the project test runner is introduced; otherwise verify with production build and local browser smoke.

Runtime smoke:

1. Start backend with local profile.
2. Start H5 dev server.
3. Load home page and confirm bootstrap data renders.
4. Log in as `demo_player`.
5. Open wallet and confirm balances render from backend.
6. Open games and launch mock game, confirming placeholder state.

## 13. Acceptance Criteria

- `GET /api/client/bootstrap` returns demo brand, feature, and currency data.
- H5 home page renders bootstrap data from backend.
- `POST /api/client/auth/login` returns a client token for seeded demo credentials.
- H5 can log in, persist token, and show logged-in shell state.
- `GET /api/client/wallet/accounts` returns only the current member's balances.
- H5 wallet page renders balances and ledger state from backend.
- `GET /api/client/games` returns at least one mock game.
- H5 games page renders backend games and shows launch placeholder.
- Missing or expired token produces a controlled login-required state.
- Verification passes:
  - `pnpm --dir admin-ui check:i18n`
  - `npm run build --prefix h5`
  - `C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests`

## 14. Risks and Controls

| Risk | Control |
| --- | --- |
| Client token design grows into a second auth framework | Keep it minimal and isolated under client auth utilities |
| H5 starts encoding wallet rules | H5 only displays API data and feature flags |
| Demo seed pollutes production assumptions | Name seed records clearly as demo and keep SQL local/dev scoped |
| Client APIs leak admin data | Use dedicated client VO objects |
| Phase 2 expands too far | Keep payment, KYC, real game launch, and Flutter integration out of this slice |

## 15. Implementation Order

1. Add backend client bootstrap API and tests.
2. Add client auth/session API and demo seed data.
3. Add client wallet account and ledger APIs.
4. Add client game lobby and launch placeholder APIs.
5. Add H5 API client, session state, and shell login/logout behavior.
6. Wire H5 home, login, wallet, and games pages to backend.
7. Run build and backend compile verification.
