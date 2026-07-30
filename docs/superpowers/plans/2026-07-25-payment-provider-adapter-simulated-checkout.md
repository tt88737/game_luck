# Payment Provider Adapter And Simulated Checkout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a provider-neutral payment adapter, hosted simulated checkout, signed webhook ingestion, operational review surfaces, and end-to-end payment/refund/chargeback verification without integrating a real supplier.

**Architecture:** Keep the adapter boundary inside `gameluck-payment`. Persist payment sessions and immutable webhook events, verify exact raw-body HMAC signatures at the HTTP boundary, and delegate business transitions to the existing purchase payment and reversal services. Use separate transactions so failed webhook attempts remain auditable while downstream order/wallet mutations roll back.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, MySQL 8, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, Vite, HMAC-SHA256, Playwright-compatible browser acceptance.

**Execution constraint:** Preserve the shared dirty `main` worktree. Do not create Git commits unless the user explicitly requests one; use review checkpoints in place of commit steps.

---

## File Map

- Persistence: `PaymentSession.java`, `PaymentWebhookEvent.java`, status/type enums, their mappers, and `backend/script/sql/gameluck_wallet.sql`.
- Provider boundary: `PaymentProviderAdapter.java`, `PaymentProviderRegistry.java`, provider request/result records, `PaymentProviderProperties.java`, and `SimulatedPaymentProviderAdapter.java`.
- Session application service: `IPaymentSessionService.java`, `PaymentSessionServiceImpl.java`, client BO/VO types, and `ClientPurchaseController.java`.
- Webhook boundary: `PaymentWebhookController.java`, `IPaymentWebhookService.java`, `PaymentWebhookServiceImpl.java`, and `PaymentWebhookBusinessProcessor.java`.
- Simulated checkout: `SimulatedPaymentController.java`, `ISimulatedPaymentService.java`, `SimulatedPaymentServiceImpl.java`, H5 `SimulatedCheckoutView.vue`, and H5 API/types/router files.
- Admin operations: `PaymentSessionController.java`, `PaymentWebhookEventController.java`, Admin APIs, views, labels, i18n, permissions, and menu SQL.
- Existing integration points: `ClientPurchaseService.java`, `PurchasePaymentEventServiceImpl.java`, `IPurchaseReversalService.java`, and purchase H5 views.

### Task 1: Add Payment Session And Webhook Persistence

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentSessionStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentWebhookEventStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PaymentProviderEventType.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentSession.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PaymentWebhookEvent.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSessionMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentWebhookEventMapper.java`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentProviderPersistenceContractTest.java`

- [x] **Step 1: Write the failing persistence contract test**

Assert exact enum values:

```java
assertArrayEquals(new String[]{"CREATED", "PENDING", "SUCCEEDED", "FAILED", "CANCELLED", "EXPIRED"},
    Arrays.stream(PaymentSessionStatus.values()).map(Enum::name).toArray(String[]::new));
assertArrayEquals(new String[]{"RECEIVED", "PROCESSED", "FAILED", "IGNORED"},
    Arrays.stream(PaymentWebhookEventStatus.values()).map(Enum::name).toArray(String[]::new));
assertArrayEquals(new String[]{"PAYMENT_SUCCEEDED", "PAYMENT_FAILED", "PAYMENT_CANCELLED", "REFUND_SUCCEEDED", "CHARGEBACK_CREATED"},
    Arrays.stream(PaymentProviderEventType.values()).map(Enum::name).toArray(String[]::new));
```

Reflectively require every field from the approved design. Read `gameluck_wallet.sql` by walking upward from `user.dir`; assert both tables, all tenant-scoped unique keys, `raw_body LONGTEXT`, and the required lookup indexes exist.

- [x] **Step 2: Run the test and verify RED**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentProviderPersistenceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: test compilation fails because the new enums and entities do not exist.

- [x] **Step 3: Add exact schema and domain types**

Use `DECIMAL(20,6)` for session amount, `DATETIME` for expiry/completion/processing timestamps, and `LONGTEXT` for immutable raw payload. Add idempotent `information_schema` guards matching the existing wallet SQL style.

Mapper contracts must include:

```java
PaymentSession selectByRequestKey(String tenantId, String requestKey);
PaymentSession selectBySessionNo(String tenantId, String sessionNo);
PaymentSession selectBySessionNoForUpdate(String tenantId, String sessionNo);
PaymentSession selectActiveByOrderNoForUpdate(String tenantId, String purchaseOrderNo, Date now);
PaymentWebhookEvent selectByProviderEventId(String tenantId, String providerCode, String providerEventId);
PaymentWebhookEvent selectByIdForUpdate(String tenantId, Long id);
```

The active-session query treats only `CREATED` and `PENDING` with `expire_time > now` as active.

- [x] **Step 4: Run the persistence contract GREEN**

Run Step 2 unchanged. Expected: all persistence contract tests pass with zero failures and errors.

- [x] **Step 5: Review checkpoint**

Confirm no plaintext provider secret column exists, every unique key starts with `tenant_id`, and no existing Phase 38-42 schema is altered destructively.

### Task 2: Build Provider Configuration, Registry, And HMAC Verification

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/provider/PaymentProviderAdapter.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/provider/PaymentProviderRegistry.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/provider/PaymentProviderSessionRequest.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/provider/PaymentProviderSessionResult.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/provider/PaymentWebhookEnvelope.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/provider/PaymentWebhookVerificationResult.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/config/PaymentProviderProperties.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/provider/SimulatedPaymentProviderAdapter.java`
- Modify: `backend/gameluck-admin/src/main/resources/application.yml`
- Modify: `backend/gameluck-admin/src/main/resources/application-local.yml`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/provider/PaymentProviderRegistryTest.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/provider/SimulatedPaymentProviderAdapterTest.java`

- [x] **Step 1: Write failing registry and signature tests**

Cover enabled `SIMULATED` resolution, unknown/disabled rejection, exact signature success, tampered body failure, stale timestamp failure, and lowercase hexadecimal digest output. Fix the test clock to avoid timing races.

Desired adapter contract:

```java
public interface PaymentProviderAdapter {
    String providerCode();
    PaymentProviderSessionResult createSession(PaymentProviderSessionRequest request);
    PaymentWebhookVerificationResult verifyWebhook(String timestamp, String signature, byte[] rawBody, Instant now);
    PaymentWebhookEnvelope parseWebhook(byte[] rawBody);
}
```

- [x] **Step 2: Run provider tests and verify RED**

Run the Task 1 command with `-Dtest=PaymentProviderRegistryTest,SimulatedPaymentProviderAdapterTest`.

Expected: compilation fails on the absent provider contracts.

- [x] **Step 3: Implement the minimal provider boundary**

Bind configuration under:

```yaml
payment:
  providers:
    simulated:
      enabled: ${PAYMENT_SIMULATED_ENABLED:true}
      secret: ${PAYMENT_SIMULATED_SECRET:local-simulated-payment-secret}
      checkout-base-url: ${PAYMENT_SIMULATED_CHECKOUT_BASE_URL:http://127.0.0.1:5174/simulated-checkout}
      webhook-base-url: ${PAYMENT_WEBHOOK_BASE_URL:http://127.0.0.1:8080/payment/webhooks}
      session-ttl-minutes: 15
      signature-tolerance-seconds: 300
```

Use `Mac.getInstance("HmacSHA256")`, UTF-8 timestamp/body composition, `MessageDigest.isEqual`, and an injected `Clock`. Never log or expose the configured secret.

- [x] **Step 4: Run provider tests GREEN**

Expected: registry and signature tests pass, including tampering and stale timestamp cases.

- [x] **Step 5: Review checkpoint**

Search for the literal local secret outside configuration/test fixtures and verify no API VO contains a secret field.

### Task 3: Create Idempotent Payment Sessions And Client APIs

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/bo/ClientPaymentSessionCreateBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/vo/ClientPaymentSessionVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSessionService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSessionServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/controller/ClientPurchaseController.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/vo/ClientPurchaseOrderVo.java`
- Modify: backend i18n message bundles under `backend/gameluck-admin/src/main/resources/i18n/`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSessionServiceImplTest.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`

- [x] **Step 1: Write failing session behavior tests**

Cover:

- same request key returns the same session;
- same key with a different order/member/provider conflicts;
- an existing unexpired active session is reused under a new request key only when it belongs to the same order/member/provider;
- expired/failed/cancelled sessions allow a new session;
- credited/refunded/chargeback orders reject new sessions;
- provider failure writes no session row;
- member ownership and tenant isolation are enforced.

Desired service methods:

```java
ClientPaymentSessionVo create(Long memberId, String orderNo, ClientPaymentSessionCreateBo bo);
ClientPaymentSessionVo get(Long memberId, String sessionNo);
```

- [x] **Step 2: Run session tests and verify RED**

Run Task 1 Maven command with `-Dtest=PaymentSessionServiceImplTest,ClientPurchaseServiceTest`.

Expected: compilation fails on missing BO/VO/service types.

- [x] **Step 3: Split order creation from simulated fulfillment**

Change `ClientPurchaseService.pay(...)` so it creates and returns a `PENDING` purchase order without calling `PurchasePaymentCallbackBo.simulatedSuccess(order)`. Preserve purchase compliance, limits, grant validation, and order idempotency. Keep the existing route temporarily for compatibility but return the pending order; H5 will create a payment session next.

Add endpoints:

```java
@PostMapping("/orders/{orderNo}/payment-sessions")
public R<ClientPaymentSessionVo> createPaymentSession(...)

@GetMapping("/payment-sessions/{sessionNo}")
public R<ClientPaymentSessionVo> paymentSession(...)
```

- [x] **Step 4: Implement guarded session creation**

Lock the purchase order, validate ownership and `PENDING` status, check request replay and active session, invoke the adapter, then insert one `PENDING` session. Copy amount/currency from the locked order and update its provider/session identifiers only after provider creation succeeds.

- [x] **Step 5: Run session and purchase tests GREEN**

Update existing purchase assertions from immediate `CREDITED` to `PENDING`; retain separate payment-event tests for fulfillment. Expected: all named suites pass.

- [x] **Step 6: Review checkpoint**

Verify no client-supplied amount, currency, member ID, checkout URL, or provider session ID is trusted.

### Task 4: Ingest Signed Webhooks With Durable Failure State

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentWebhookController.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentWebhookService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentWebhookServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentWebhookBusinessProcessor.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentWebhookAckVo.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSessionMapper.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchasePaymentEventServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentWebhookServiceImplTest.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentWebhookControllerContractTest.java`

- [x] **Step 1: Write failing webhook tests**

Cover valid ingestion, HTTP 401 on invalid signature, same-event replay, amount/currency mismatch, success mapping, failed/cancelled mapping, refund mapping, chargeback mapping, late contradictory event as `IGNORED`, and a forced business exception that leaves the event `FAILED` while rolling back order/wallet changes.

Controller must receive exact bytes:

```java
@PostMapping("/payment/webhooks/{providerCode}")
public ResponseEntity<PaymentWebhookAckVo> receive(
    @PathVariable String providerCode,
    @RequestHeader("X-Payment-Timestamp") String timestamp,
    @RequestHeader("X-Payment-Signature") String signature,
    @RequestBody byte[] rawBody)
```

- [x] **Step 2: Run webhook tests and verify RED**

Run Task 1 Maven command with `-Dtest=PaymentWebhookServiceImplTest,PaymentWebhookControllerContractTest`.

Expected: compilation fails because the service/controller do not exist.

- [x] **Step 3: Implement the transaction boundary**

Use three explicit stages:

```java
// transaction A: insert immutable RECEIVED event or return existing replay
PaymentWebhookEvent receiveVerified(...);

// transaction B: lock event/session/order and invoke existing business services
WebhookProcessingOutcome processBusiness(Long eventId);

// transaction C (REQUIRES_NEW): record FAILED metadata after transaction B rolls back
void recordFailure(Long eventId, String failureReason);
```

Do not catch and update `FAILED` inside the same transaction that performs wallet/order changes.

- [x] **Step 4: Map events to existing business commands**

Create `PurchasePaymentCallbackBo` with stable `eventKey = providerCode + ":" + providerEventId`, order/provider identifiers, raw request body, and mapped `PurchasePaymentEventType`. Update session state through a guarded update after business success. Mark contradictory terminal events `IGNORED` without calling the business service.

- [x] **Step 5: Run webhook tests GREEN**

Expected: invalid signatures create no event, replay creates no duplicate event/business effect, and forced failure persists exactly one `FAILED` event.

- [x] **Step 6: Run Phase 41/42 reversal regression**

Run payment tests:

```text
PurchasePaymentEventServiceImplTest,
PurchaseReversalServiceImplTest,
PurchaseReversalReviewServiceImplTest
```

Expected: refund and chargeback behavior remains unchanged.

- [x] **Step 7: Review checkpoint**

Confirm adapters and controllers have no direct wallet mapper/service calls; only `PurchasePaymentEventServiceImpl` and reversal services reach wallet behavior.

### Task 5: Add Hosted Simulated Checkout Actions

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/SimulatedPaymentController.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/ISimulatedPaymentService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/SimulatedPaymentServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/SimulatedPaymentActionBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/SimulatedCheckoutVo.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/provider/SimulatedPaymentProviderAdapter.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/SimulatedPaymentServiceImplTest.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/SimulatedPaymentControllerContractTest.java`

- [x] **Step 1: Write failing action/state tests**

Cover pending actions (`PAYMENT_SUCCEEDED`, `PAYMENT_FAILED`, `PAYMENT_CANCELLED`), post-success actions (`REFUND_SUCCEEDED`, `CHARGEBACK_CREATED`), invalid terminal actions, expiration, unique event IDs, and replay of the exact previous event ID/body/signature.

- [x] **Step 2: Run simulated Provider tests and verify RED**

Run Task 1 Maven command with `-Dtest=SimulatedPaymentServiceImplTest,SimulatedPaymentControllerContractTest`.

- [x] **Step 3: Implement real HTTP webhook dispatch**

Use Spring `RestClient` to POST the generated raw JSON bytes to `${payment.providers.simulated.webhook-base-url}/SIMULATED` with timestamp/signature headers. Do not call `IPaymentWebhookService` directly. Persist no separate secret or mutable event payload.

Expose:

```text
GET  /payment/simulated/checkout/{providerSessionNo}
POST /payment/simulated/checkout/{providerSessionNo}/actions
POST /payment/simulated/checkout/{providerSessionNo}/replay
```

The GET response supplies only display-safe checkout data and allowed actions.

- [x] **Step 4: Run simulated Provider tests GREEN**

Expected: action guards and exact replay behavior pass; mocked HTTP server receives the signed request.

- [x] **Step 5: Review checkpoint**

Confirm checkout actions cannot target another session through request fields and no action accepts amount, currency, order number, or member ID from the browser.

### Task 6: Add Admin Session And Webhook Operations

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSessionController.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentWebhookEventController.java`
- Create: payment session/event Admin BO and VO files under `domain/bo` and `domain/vo`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentProviderAdminService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentProviderAdminServiceImpl.java`
- Modify: backend i18n bundles
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentProviderAdminControllerContractTest.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentProviderAdminServiceImplTest.java`

- [x] **Step 1: Write failing Admin contracts**

Require tenant-scoped paginated list/detail filters and exact permissions:

```text
payment:paymentSession:list
payment:paymentSession:query
payment:webhookEvent:list
payment:webhookEvent:query
payment:webhookEvent:retry
```

Retry must use `@Log(title = "Payment webhook event", businessType = BusinessType.UPDATE)` and reject any event not currently `FAILED`.

- [x] **Step 2: Run Admin tests and verify RED**

Run Task 1 Maven command with the two new Admin test classes.

- [x] **Step 3: Implement read projections and retry**

Session filters: session/order/provider session, member ID/member number, provider, status, currency, and creation range. Event filters: provider event/order/session, event type, status, provider, and received range. Detail includes cross-links but never a plaintext secret.

Retry locks the failed event and calls the same business processing stage from Task 4; success changes it to `PROCESSED` or `IGNORED` and increments processing count.

- [x] **Step 4: Add idempotent menu and dictionary SQL**

Add two Payment Center menus after purchase orders and before reversal review, with four query/list permissions and one retry permission. Add exact dictionaries for session status, webhook status, and provider event type using delete-and-insert idempotent patterns already present in the SQL file.

- [x] **Step 5: Run Admin tests GREEN**

Expected: all controller/service contracts pass and retry cannot mutate raw payload fields.

- [x] **Step 6: Review checkpoint**

Scan controllers for mapper injection and ensure all data access remains in the service layer.

### Task 7: Build Admin Payment Operations Pages

**Files:**
- Create: `admin-ui/src/api/payment/paymentSession/index.ts`
- Create: `admin-ui/src/api/payment/paymentSession/types.ts`
- Create: `admin-ui/src/api/payment/paymentWebhookEvent/index.ts`
- Create: `admin-ui/src/api/payment/paymentWebhookEvent/types.ts`
- Create: `admin-ui/src/views/payment/payment-session/index.vue`
- Create: `admin-ui/src/views/payment/payment-webhook-event/index.vue`
- Modify: `admin-ui/src/utils/businessLabels.ts`
- Modify: `admin-ui/src/utils/i18nTitle.ts`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`
- Create: `admin-ui/scripts/check-payment-provider-admin-contract.mjs`

- [x] **Step 1: Write the failing frontend contract check**

Require exact API paths, TypeScript fields, permission strings, business label groups, i18n keys, raw-payload read-only rendering, and retry visibility restricted to `FAILED` events.

- [x] **Step 2: Run the contract and verify RED**

```powershell
node admin-ui/scripts/check-payment-provider-admin-contract.mjs
```

Expected: failure on absent API/view files.

- [x] **Step 3: Add typed APIs and business labels**

Use `PageQuery`, string monetary fields, ISO-compatible date strings, and explicit status unions. Add status tags without cross-currency totals.

- [x] **Step 4: Implement dense operational pages**

Payment sessions: filterable table, per-row detail drawer, purchase-order link, exact amount/currency pair, provider/session identifiers, timestamps, and status.

Webhook events: filterable table, event detail drawer, formatted read-only raw payload, signature digest, attempts/failure reason, linked order/session/reversal, and permission-scoped retry confirmation.

Keep desktop tables horizontally scrollable on mobile; use one-column descriptions below 768px and no nested cards.

- [x] **Step 5: Run frontend checks GREEN**

```powershell
node admin-ui/scripts/check-payment-provider-admin-contract.mjs
corepack pnpm --dir admin-ui check:i18n
corepack pnpm --dir admin-ui exec eslint --rule 'prettier/prettier: off' src/api/payment/paymentSession src/api/payment/paymentWebhookEvent src/views/payment/payment-session/index.vue src/views/payment/payment-webhook-event/index.vue src/utils/businessLabels.ts src/utils/i18nTitle.ts
```

Expected: all commands exit 0.

- [x] **Step 6: Browser acceptance checkpoint**

Capture desktop and 390px screenshots for a pending session, processed payment webhook, failed webhook, and retry result. Verify no page-level overflow, clipped commands, editable raw payload, or overlapping controls.

### Task 8: Build H5 Checkout And Payment Result Flow

**Files:**
- Modify: `h5/src/api/client.ts`
- Modify: `h5/src/types/client.ts`
- Modify: `h5/src/router/index.ts`
- Modify: `h5/src/views/PurchaseView.vue`
- Create: `h5/src/views/SimulatedCheckoutView.vue`
- Create: `h5/src/views/PurchaseResultView.vue`
- Modify: `h5/src/style.css`
- Create: `h5/scripts/check-payment-session-contract.mjs`

- [x] **Step 1: Write the failing H5 contract check**

Require order creation followed by session creation, redirect to backend-provided `checkoutUrl`, platform-only status polling, routes for `/simulated-checkout/:providerSessionNo` and `/purchase-result/:sessionNo`, all five simulated actions, and no query-string status trust.

- [x] **Step 2: Run contract and current build to verify RED**

```powershell
node h5/scripts/check-payment-session-contract.mjs
npm --prefix h5 run build
```

Expected: contract fails because the session API and views do not exist.

- [x] **Step 3: Add types and API wrappers**

Add `ClientPaymentSession`, `SimulatedCheckout`, allowed-action unions, and wrappers for client session create/get plus simulated checkout get/action/replay.

- [x] **Step 4: Update purchase and result UX**

Purchase submit creates a pending order, creates a session with a stable browser-generated request key, and redirects to `checkoutUrl`. Result view polls session state with bounded intervals, renders success/failure/cancel/expiry states, refreshes wallet only after `SUCCEEDED`, and provides a safe return to purchases.

- [x] **Step 5: Implement the hosted simulated checkout**

Display actual order number, amount, currency, provider session, expiry, state, and only server-authorized action buttons. After an action, show the webhook acknowledgement and navigate to the platform result URL. Include replay as a secondary test action, not a primary payment command.

- [x] **Step 6: Run H5 checks GREEN**

Run Step 2 unchanged. Expected: contract and build exit 0.

- [x] **Step 7: Browser acceptance checkpoint**

At desktop and 390px, verify purchase-to-checkout redirect, success result, failure result, cancellation, refresh/re-entry, expired state, and readable non-overlapping controls.

### Task 9: Run Backend Regression And Build Deliverables

**Files:**
- Modify only if regression exposes a defect covered by a new failing test.

- [x] **Step 1: Run focused Phase 38-43 regression**

Run under `-Plocal`, `MAVEN_OPTS=-Djdk.attach.allowAttachSelf=true -Xmx768m`, and `-DforkCount=0`:

```text
ClientPurchaseServiceTest,
PurchaseOrderServiceImplTest,
PurchasePaymentEventServiceImplTest,
PurchaseReversalContractTest,
PurchaseReversalServiceImplTest,
PurchaseReversalReviewContractTest,
PurchaseReversalReviewServiceImplTest,
PurchaseReversalReviewControllerContractTest,
PaymentProviderPersistenceContractTest,
PaymentProviderRegistryTest,
SimulatedPaymentProviderAdapterTest,
PaymentSessionServiceImplTest,
PaymentWebhookServiceImplTest,
PaymentWebhookControllerContractTest,
SimulatedPaymentServiceImplTest,
SimulatedPaymentControllerContractTest,
PaymentProviderAdminServiceImplTest,
PaymentProviderAdminControllerContractTest
```

Expected: zero failures, errors, and skips.

- [x] **Step 2: Run wallet/member cross-module regression**

Include `WalletCoreServiceImplTest`, `WalletTurnoverTaskServiceImplTest`, and `MemberProfileServiceImplTest` with the payment suites that touch fulfillment/reversal. Expected: zero failures, errors, and skips.

- [x] **Step 3: Build all deliverables**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-admin -am '-DskipTests' package
$env:NODE_OPTIONS='--max-old-space-size=1536'; corepack pnpm --dir admin-ui build:dev
npm --prefix h5 run build
```

Temporarily stop only project-owned `8080/5173/5174` processes if Windows commit space requires it, then restore them. Expected: backend 36-module package, Admin build, and H5 build all exit 0; the existing large-chunk advisory is acceptable.

- [x] **Step 4: Integrity scan**

Search for direct wallet access from adapters/controllers, plaintext secrets, query-string payment success trust, mutable webhook raw payload, missing tenant predicates, unfinished marker strings, and cross-currency numeric totals. Run `git diff --check`.

### Task 10: SQL Idempotency And Runtime Acceptance

**Files:**
- Modify after verification: `docs/superpowers/plans/2026-07-25-payment-provider-adapter-simulated-checkout.md`
- Modify after verification: `progress.md`
- Modify after verification: `task_plan.md` using encoding-preserving byte replacement

- [x] **Step 1: Import SQL twice and verify exact schema**

Apply `gameluck_wallet.sql` twice to local `gameluck_vue`. Verify exactly one session table, one webhook table, all required unique keys/indexes, two Admin menus, five permissions, three dictionary types, and no duplicate dictionary rows.

- [x] **Step 2: Runtime-smoke payment success**

Create a purchase, create a session, complete the hosted checkout, and verify one signed webhook event, one purchase payment event, one set of grant snapshots, one wallet credit per grant currency, one turnover task where applicable, session `SUCCEEDED`, and order `CREDITED`.

- [x] **Step 3: Verify replay and duplicate-success protection**

Replay the exact Provider event and send a new Provider event ID with the same payment-success result. Confirm both return HTTP 200, no duplicate credit/snapshot/turnover occurs, same-event replay stays one webhook row, and the second event is `PROCESSED` or `IGNORED` according to the implemented terminal rule without changing balances.

- [x] **Step 4: Runtime-smoke failure, cancellation, and expiry**

Use fresh sessions for each outcome. Confirm no wallet transaction or grant snapshot is created; session/order states match the event; an expired session rejects simulated success and a replacement session can be created.

- [x] **Step 5: Runtime-smoke refund and chargeback**

From separate credited orders, trigger refund and chargeback through the simulated Provider. Verify the existing reversal, wallet, turnover, member-risk, and review-case behavior. For a shortfall chargeback, confirm the case appears in the Phase 42 workbench.

- [x] **Step 6: Verify invalid signatures and durable retry**

Send invalid, stale, and tampered webhooks and confirm HTTP 401 with no business mutation. Force one valid event into a controlled processing failure, verify event `FAILED` and downstream rollback, then use the Admin retry permission to reach the correct terminal state with processing count incremented and an operation log.

- [x] **Step 7: Browser acceptance**

Capture desktop and 390px evidence for hosted checkout, H5 payment result, Admin payment session, processed webhook, failed webhook, and retry result. Verify actual data, allowed actions, cross-links, status polling, and no page-level overflow.

- [x] **Step 8: Restore services and run final checks**

Start backend with only `--spring.profiles.active=local` and restore Admin/H5 dev servers. Run frontend contracts, targeted semantic ESLint, i18n, `git diff --check`, and verify HTTP 200 on ports `8080`, `5173`, and `5174`.

- [x] **Step 9: Record completion without committing**

Append exact test counts, build results, SQL counts, runtime identifiers, event/replay evidence, screenshots, and service PIDs to `progress.md`. Mark every plan checkbox complete and change Phase 43 from `planned` to `completed` in `task_plan.md` without rewriting its mixed encoding. Do not run `git commit`.
