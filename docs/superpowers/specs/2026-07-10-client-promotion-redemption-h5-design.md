# Phase 3 Client Promotion And Redemption H5 Design

## Goal

Extend the player H5 closed loop from login, wallet, and game launch into promotion reward claiming and redemption request submission.

This phase gives a logged-in demo player two additional end-to-end flows:

- View available promotion rewards and claim a reward into the wallet.
- View redemption history and submit a simulated SC redemption request that freezes wallet balance for review.

The scope remains a local/demo player-client implementation. Real KYC, payment provider, payout provider, geofencing, fraud review, and third-party promotion engines are out of scope.

## Current Context

Phase 2 already added:

- `/api/client/bootstrap`
- `/api/client/auth/login`
- `/api/client/member/me`
- `/api/client/wallet/accounts`
- `/api/client/wallet/ledgers`
- `/api/client/games`
- `/api/client/games/launch`
- H5 session state and backend-backed home, login, wallet, and games pages.

Existing backend modules already contain admin-side promotion and redemption capabilities:

- `gameluck-promotion` has reward and claim domain objects, mappers, services, and claim wallet-credit logic.
- `gameluck-redemption` has redemption order domain objects, mappers, services, and approve/reject wallet settlement/release logic.
- `gameluck-wallet` has freeze, release, credit, and ledger behavior.

Phase 3 adds a narrow `/api/client/**` layer over the existing modules without exposing admin endpoints or admin BO/VO shapes directly to H5.

## API Design

### Promotion APIs

Add a client promotion package under:

```text
backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/
```

Endpoints:

```text
GET  /api/client/promotions
POST /api/client/promotions/claim
```

`GET /api/client/promotions` returns a list of active demo rewards visible to the player.

Response item fields:

```text
promotionId
promotionNo
promotionName
currencyCode
rewardAmount
status
claimStatus
claimNo
walletTransactionNo
canClaim
```

`POST /api/client/promotions/claim` accepts:

```text
promotionId
```

The service resolves the member id from the H5 client token. It then calls the existing promotion claim behavior with the current member id. The claim must be idempotent per tenant, promotion, and member. If the reward has already been claimed, the API returns the existing claim state instead of creating a second wallet credit.

### Redemption APIs

Add a client redemption package under:

```text
backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/
```

Endpoints:

```text
GET  /api/client/redemptions
POST /api/client/redemptions/request
```

`GET /api/client/redemptions` returns the current player's recent redemption orders.

Response item fields:

```text
orderId
orderNo
currencyCode
amount
status
walletFreezeNo
reviewRemark
createdAt
```

`POST /api/client/redemptions/request` accepts:

```text
currencyCode
amount
```

Rules:

- Only logged-in players can submit.
- Only `SC` is accepted in this phase.
- Amount must be positive.
- KYC is not implemented; the demo flow is allowed so the wallet-freeze behavior can be verified.
- The service creates a redemption order with `PENDING` status and freezes the submitted SC amount through wallet-center.

## Data And Seed Design

Extend the demo seed SQL only where necessary:

```text
backend/script/sql/gameluck_client_demo.sql
```

Seed one active promotion reward suitable for H5 smoke testing:

```text
tenant_id = 000000
promotion_no = PR-DEMO-DAILY-SC
promotion_name = 每日 SC 奖励
currency_code = SC
reward_amount = 8.000000
status = ACTIVE
```

The seed must remain idempotent and must not duplicate claims or redemption orders on repeated imports.

Redemption requests are created by runtime smoke, not pre-seeded, because the smoke needs to prove wallet freeze and order creation.

## H5 Design

Update H5 API types and client methods:

```text
h5/src/types/client.ts
h5/src/api/client.ts
```

Add typed methods:

```text
promotions()
claimPromotion(promotionId)
redemptions()
requestRedemption(currencyCode, amount)
```

### Promotions Page

Replace static `h5/src/data/demo.ts` promotion data with backend data in:

```text
h5/src/views/PromotionsView.vue
```

States:

- Logged out: show "请先登录" and a login button.
- Loading: show compact loading text.
- Error: show translated error message.
- Empty: show no rewards available.
- Ready: list rewards with amount, currency, claim status, and a claim button.
- Success: after claim, show a success message and refresh promotion list.
- Already claimed: disable the claim button and show claimed state.

### Redemptions Page

Replace static redemption data with backend wallet and redemption APIs in:

```text
h5/src/views/RedemptionsView.vue
```

States:

- Logged out: show "请先登录" and a login button.
- Loading: show compact loading text.
- Error: show translated error message.
- Ready: show available SC balance, amount input, disabled submit state for invalid amount, and recent redemption records.
- Success: after request, show success message and refresh wallet/redemption data.
- Insufficient balance: backend wallet freeze failure is surfaced as a translated failure.

The page should default the amount input to a small demo value such as `1.00`, so smoke testing is fast and predictable.

## Backend Security And Error Handling

Client controllers must use `@SaIgnore` because H5 player APIs use `ClientTokenService` instead of the admin Sa-Token login gate.

Token-protected endpoints require `Authorization: Bearer <clientToken>`.

Visible backend messages must use `MessageUtils.message(...)` and i18n bundle keys. No raw visible Chinese or English text should be hardcoded in backend Java.

Add or reuse i18n keys for:

```text
client.promotion.claim.success
client.promotion.already.claimed
client.promotion.not.available
client.redemption.currency.unsupported
client.redemption.amount.required
client.redemption.amount.positive
client.redemption.request.success
```

Frontend visible text can remain local Chinese text in H5 Vue files, consistent with the current H5 app.

## Testing Strategy

Backend tests:

- Client promotion service test:
  - Lists active promotion with current member claim state.
  - Claims active promotion and returns wallet transaction number.
  - Re-claim returns existing claim state without another wallet credit.

- Client redemption service test:
  - Lists only current member redemption orders.
  - Creates pending SC redemption request.
  - Rejects unsupported currency.
  - Rejects non-positive amount.

Build and guard verification:

```powershell
pnpm --dir admin-ui check:i18n
npm run build --prefix h5
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Runtime smoke:

1. Import or verify demo seed SQL.
2. Start backend with `local` profile.
3. Start H5 dev server.
4. Login with `demo_player / Demo123456`.
5. Open promotions page, claim the demo reward, and verify success state.
6. Open wallet page and verify SC balance increased.
7. Open redemption page, submit a small SC redemption request, and verify the new pending record.
8. Open wallet page and verify frozen SC increased or available SC decreased according to wallet freeze behavior.

## Acceptance Criteria

- H5 promotions page is fully backend-backed.
- H5 redemptions page is fully backend-backed for listing and request submission.
- Demo reward claim updates wallet through existing wallet-center credit logic.
- Demo redemption request freezes SC through existing wallet-center freeze logic.
- Repeated promotion claim does not duplicate wallet credit.
- Client APIs do not require admin Sa-Token login.
- i18n guard passes.
- H5 build passes.
- Backend compile passes.
- Browser smoke confirms the claim and redemption flows in Chinese UI.

## Out Of Scope

- Real KYC verification.
- Real payout or payment provider.
- Redemption approval from H5.
- Promotion targeting, segmentation, or rules engine.
- Third-party CRM/campaign provider integration.
- App/Flutter integration for these flows.
