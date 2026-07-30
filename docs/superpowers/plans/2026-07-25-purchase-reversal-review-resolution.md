# Purchase Reversal Review Resolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a dedicated Admin review workbench that resolves refund and chargeback recovery shortfalls through either atomic full recovery retry or audited per-currency loss acceptance.

**Architecture:** Extend the existing purchase reversal aggregate with a separate disposition state and immutable review-operation log. A payment-owned review service locks each case, delegates all-or-nothing multi-currency debit to the wallet module, and updates the order, turnover tasks, case, items, and audit log in one transaction; Admin remains a typed projection over that service.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, pnpm, Playwright-compatible browser acceptance.

**Execution constraint:** Do not create Git commits unless the user explicitly requests one. Commit steps are replaced with review checkpoints because the shared `main` worktree contains unrelated uncommitted changes.

---

## File Map

- Reversal disposition model: `PurchaseReversalDispositionStatus.java`, existing `PurchaseReversal.java`, and `PurchaseReversalMapper.java`.
- Review audit model: `PurchaseReversalReviewOperationType.java`, `PurchaseReversalReviewLog.java`, and `PurchaseReversalReviewLogMapper.java`.
- Review query and commands: `PurchaseReversalReviewBo.java`, `PurchaseReversalReviewActionBo.java`, `PurchaseReversalReviewVo.java`, `PurchaseReversalReviewDetailVo.java`, `PurchaseReversalReviewLogVo.java`, and existing reversal item/order/event/member projections.
- Review transaction boundary: `IPurchaseReversalReviewService.java` and `PurchaseReversalReviewServiceImpl.java`.
- Wallet read-only preflight: `IWalletCoreService.java`, `WalletCoreServiceImpl.java`, `WalletBatchDebitPreviewResult.java`, and `WalletBatchDebitPreviewLineResult.java`.
- Admin HTTP boundary: `PurchaseReversalReviewController.java`.
- Admin frontend: `admin-ui/src/api/payment/purchaseReversalReview/` and `admin-ui/src/views/payment/purchase-reversal-review/index.vue`.
- Existing order link: `admin-ui/src/api/payment/purchaseOrder/types.ts` and `admin-ui/src/views/payment/purchase-order/index.vue`.
- Schema, menu, dictionary, messages: `backend/script/sql/gameluck_wallet.sql`, `backend/script/sql/gameluck_platform_dict.sql`, backend i18n bundles, and Admin language/label helpers.

### Task 1: Add Disposition And Review Audit Persistence

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchaseReversalDispositionStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchaseReversalReviewOperationType.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseReversalReviewLog.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseReversalReviewLogMapper.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseReversal.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseReversalMapper.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseReversalServiceImpl.java`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PurchaseReversalReviewContractTest.java`

- [x] **Step 1: Write the failing persistence contract test**

Assert the new enums contain exactly:

```java
PENDING_REVIEW, RECOVERY_COMPLETED, LOSS_ACCEPTED
RETRY_INSUFFICIENT, RETRY_COMPLETED, LOSS_ACCEPTED
```

Reflectively require `dispositionStatus`, `reviewedBy`, `reviewedName`, `reviewNote`, `resolvedTime`, `retryCount`, `lastRetryTime`, and `version` on `PurchaseReversal`. Read `gameluck_wallet.sql` from an ancestor of `user.dir` and require `gl_purchase_reversal_review_log`, tenant-scoped unique keys for `request_key` and `operation_no`, and every approved review field.

- [x] **Step 2: Run the contract test and verify RED**

Run:

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PurchaseReversalReviewContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: test compilation fails because the disposition enums, audit entity, and review fields do not exist.

- [x] **Step 3: Add the exact schema and domain model**

Add the approved fields to `gl_purchase_reversal` with idempotent `information_schema` migrations. Define `gl_purchase_reversal_review_log` with `snapshot_json LONGTEXT`, unique keys `(tenant_id,request_key)` and `(tenant_id,operation_no)`, plus indexes `(tenant_id,reversal_no,create_time)` and `(tenant_id,create_time)`.

Add mapper methods:

```java
PurchaseReversal selectByReversalNoForUpdate(String tenantId, String reversalNo);
PurchaseReversalReviewLog selectByRequestKey(String tenantId, String requestKey);
List<PurchaseReversalReviewLog> selectByReversalNo(String tenantId, String reversalNo);
```

Use `SELECT ... FOR UPDATE` for the case lock and deterministic `create_time asc, id asc` for logs.

- [x] **Step 4: Initialize disposition during first reversal**

In `PurchaseReversalServiceImpl`, set `dispositionStatus=PENDING_REVIEW` only when the wallet result is `REVIEW_REQUIRED`; leave it null for automatically completed cases. Do not change the original event key or chargeback-risk behavior.

- [x] **Step 5: Run contract and existing reversal tests GREEN**

Run Task 1 Step 2 with `-Dtest=PurchaseReversalReviewContractTest,PurchaseReversalServiceImplTest`.

Expected: both suites pass with zero failures and errors.

### Task 2: Build Review Query And Detail Projection

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PurchaseReversalReviewBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseReversalReviewVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseReversalReviewDetailVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseReversalReviewLogVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseReversalReviewService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseReversalReviewServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseReversalMapper.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseReversalReviewServiceImplTest.java`

- [x] **Step 1: Write failing tenant-safe query tests**

Cover default `PENDING_REVIEW`, explicit disposition/type/member/order/reversal/time filters, newest-first pagination, and tenant isolation. Detail must return the case, ordered items, purchase order, grant snapshots, payment events, member risk fields, and ordered review logs. Assert the shortfall summary is a list/map by currency, never one numeric total.

- [x] **Step 2: Run query tests RED**

Run the Maven command from Task 1 with `-Dtest=PurchaseReversalReviewServiceImplTest`.

Expected: compilation fails because the query BO, VOs, interface, and service do not exist.

- [x] **Step 3: Define the public query contract**

```java
TableDataInfo<PurchaseReversalReviewVo> queryPageList(PurchaseReversalReviewBo bo, PageQuery pageQuery);
PurchaseReversalReviewDetailVo queryByReversalNo(String reversalNo);
```

`PurchaseReversalReviewVo` exposes identifiers, member number, reversal type/status/disposition, risk level, retry fields, timestamps, and `List<PurchaseReversalItemVo> items`. The detail adds purchase order, grant snapshots, events, member risk audit, and logs.

- [x] **Step 4: Implement query projection**

Use MyBatis-Plus wrappers scoped by `TenantHelper.getTenantId()`, `MemberNoQueryHelper`, current mappers, and `BeanUtil`. Avoid N+1 member-number lookup by filling the page in one helper call. Do not expose review mutation through the query code path.

- [x] **Step 5: Run query tests GREEN**

Run the command from Step 2. Expected: all list/detail cases pass.

### Task 3: Implement Atomic Full-Recovery Retry

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletBatchDebitPreviewResult.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletBatchDebitPreviewLineResult.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletCoreService.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletCoreServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PurchaseReversalReviewActionBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseReversalReviewActionResultVo.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseReversalReviewService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseReversalReviewServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseReversalItemMapper.java`
- Test: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletCoreServiceImplTest.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseReversalReviewServiceImplTest.java`

- [x] **Step 1: Write failing retry tests**

Add tests named:

```text
retryKeepsCasePendingAndWritesSnapshotWhenAnyCurrencyIsInsufficient
retryCompletesEveryCurrencyAndFinalizesRefund
retryCompletesChargebackWithoutRepeatingRiskUpdate
retryReplaysSameRequestKeyWithoutSecondDebitOrLog
retryRejectsRequestKeyBoundToAnotherOperation
retryRejectsDifferentRequestAfterTerminalDisposition
```

Verify an insufficient SC causes zero GC/SC writes, updates every item snapshot, increments retry count once, and writes exactly one `RETRY_INSUFFICIENT` log.

Also add wallet tests `previewBatchDebitReturnsEveryAvailableAndShortfallWithoutWrites` and `previewBatchDebitDoesNotWriteWhenEveryBalanceIsSufficient`. Both must verify zero account updates and zero wallet transaction inserts.

- [x] **Step 2: Run retry tests RED**

Run Task 2 test command. Expected: compilation/assertion failures because `retry` is absent.

- [x] **Step 3: Add the retry contract**

Add a wallet-owned read-only preflight contract:

```java
WalletBatchDebitPreviewResult previewBatchDebit(WalletBatchDebitBo bo);
```

The preview result contains `boolean sufficient` and ordered lines with `currencyCode`, `requiredAmount`, `availableAmount`, and `shortfallAmount`. It never exposes a transaction number or recovered amount because it never writes.

Add the review service contract:

```java
PurchaseReversalReviewActionResultVo retry(String reversalNo, PurchaseReversalReviewActionBo bo);
```

Require a trimmed non-empty `requestKey` of at most 128 characters; review note is optional and capped at 500 characters. Resolve the operator from `LoginHelper.getUserId()` and `LoginHelper.getUsername()`.

- [x] **Step 4: Implement locked idempotent retry**

First implement `previewBatchDebit` by extracting/reusing the existing normalization, deterministic sorting, validation, account-locking, and shortfall calculation from `batchDebit`. The preview path must return immediately after calculation for both sufficient and insufficient balances, with no account update and no transaction insert.

Within `@Transactional(rollbackFor = Exception.class)`:

1. Check an existing review log by request key; validate reversal and operation family before returning its recorded result.
2. Lock the reversal by tenant/reversal number and require `REVIEW_REQUIRED / PENDING_REVIEW`.
3. Lock the purchase order and require the matching review status.
4. Load all reversal items, require positive amounts, sort by currency, and call `walletCoreService.batchDebit` with stable keys `purchase-reversal-review:{reversalNo}:{currency}`.
5. On insufficiency, update every available/shortfall snapshot, retry count/time, and one JSON audit log; do not change order, turnover, recovered amount, or transaction number.
6. On success, update every item to completed, cancel pending turnover, set case `COMPLETED / RECOVERY_COMPLETED`, set reviewer/resolution fields, and set order to `REFUNDED` or `CHARGEBACK` with null fail reason.

Use a structured JSON serializer already available in the repository rather than concatenating JSON strings.

- [x] **Step 5: Run retry tests GREEN**

Run Task 2 test command. Expected: all query and retry tests pass with zero wallet mutation on insufficiency and one cancellation on success.

### Task 4: Implement Loss Acceptance And Concurrent Finalization

**Files:**
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseReversalReviewService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseReversalReviewServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseReversalMapper.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseReversalReviewServiceImplTest.java`

- [x] **Step 1: Write failing loss and concurrency tests**

Cover required note, refreshed per-currency balance snapshot, no debit, no turnover cancellation, order remaining in review, saved reviewer/time, one `LOSS_ACCEPTED` log, same-key replay, different-key terminal rejection, and two concurrent dispositions where only the lock winner can finalize.

- [x] **Step 2: Run tests RED**

Run the focused review service suite. Expected: failure because `acceptLoss` does not exist.

- [x] **Step 3: Add and implement loss acceptance**

```java
PurchaseReversalReviewActionResultVo acceptLoss(String reversalNo, PurchaseReversalReviewActionBo bo);
```

Require a 1-500 character note. After locking the case, call `walletCoreService.previewBatchDebit` with every required currency and update item available/shortfall values from its ordered result. Never call a wallet mapper from payment and never call `batchDebit` from loss acceptance. Set only disposition/reviewer/note/resolved time, preserve `status=REVIEW_REQUIRED` and order review status, and write a structured `LOSS_ACCEPTED` log.

- [x] **Step 4: Enforce finalization with the locked row plus guarded update**

Add a mapper update guarded by `tenant_id`, `reversal_no`, and `disposition_status='PENDING_REVIEW'`. Require exactly one affected row; zero means another transaction finalized first. Keep the row lock as the ordering boundary and the guarded update as defense in depth.

- [x] **Step 5: Run service tests GREEN**

Expected: all retry, loss, idempotency, and concurrency tests pass; Mockito verifies no wallet debit or turnover cancellation on loss acceptance.

### Task 5: Expose Controller, Permissions, Messages, And Dictionaries

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PurchaseReversalReviewController.java`
- Modify: all `backend/gameluck-admin/src/main/resources/i18n/messages*.properties`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: `backend/script/sql/gameluck_platform_dict.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PurchaseReversalReviewControllerContractTest.java`

- [x] **Step 1: Write a failing HTTP/permission contract test**

Reflectively require the four routes and exact permissions:

```text
GET  /list       payment:reversalReview:list
GET  /{no}       payment:reversalReview:query
POST /{no}/retry payment:reversalReview:retry
POST /{no}/accept-loss payment:reversalReview:acceptLoss
```

Require `@Log` on both mutations and validated request bodies.

- [x] **Step 2: Add controller and localized messages**

Add exact keys to every backend bundle for case missing, already resolved, request key required/conflict, note required, state mismatch, retry insufficient, retry completed, and loss accepted. Natural Chinese and English messages must preserve key parity.

- [x] **Step 3: Add idempotent menu and dictionary SQL**

Add “拒付审核” below purchase orders with four child permissions. Add dictionary types/rows for disposition status and review operation type using the existing `NOT EXISTS`/upsert conventions. Ensure menu IDs do not collide with existing rows.

- [x] **Step 4: Run contract, i18n, and SQL text checks**

Run the controller contract test, `pnpm --dir admin-ui check:i18n`, and targeted `rg` checks for every permission, status, operation type, and message key.

### Task 6: Add Typed Admin API And Business Labels

**Files:**
- Create: `admin-ui/src/api/payment/purchaseReversalReview/index.ts`
- Create: `admin-ui/src/api/payment/purchaseReversalReview/types.ts`
- Modify: `admin-ui/src/api/payment/purchaseOrder/types.ts`
- Modify: `admin-ui/src/utils/businessLabels.ts`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`

- [x] **Step 1: Add exact frontend contracts**

Define list/detail/action/log types matching backend field names and `PurchaseReversalReviewQuery`. Represent `items` as an array; do not define a numeric total loss field. Add disposition and reviewer fields to `PurchaseReversalVO` for the order-detail link.

- [x] **Step 2: Add API functions**

```typescript
listPurchaseReversalReview(query)
getPurchaseReversalReview(reversalNo)
retryPurchaseReversalReview(reversalNo, data)
acceptPurchaseReversalLoss(reversalNo, data)
```

Use the exact backend paths and `AxiosPromise` types.

- [x] **Step 3: Add labels and tag types**

Add labels for all disposition statuses and operation types. Use warning for pending, success for recovered, danger for accepted loss, and preserve existing reversal/order colors.

- [x] **Step 4: Run TypeScript-aware static checks**

Run targeted ESLint with the repository's established Prettier-rule exclusion and `pnpm --dir admin-ui check:i18n`. Expected: zero semantic lint errors and no missing keys.

### Task 7: Build The Review Workbench And Order Link

**Files:**
- Create: `admin-ui/src/views/payment/purchase-reversal-review/index.vue`
- Modify: `admin-ui/src/views/payment/purchase-order/index.vue`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`

- [x] **Step 1: Build the dense review list**

Use a segmented status filter, inline search form, bordered table, independent pagination, status tags, per-currency shortfall chips/text, and icon-only detail action with tooltip. Default to `PENDING_REVIEW`; show waiting duration only for pending cases.

- [x] **Step 2: Build the detail drawer**

Render un-nested full-width sections for case/order metadata, grant snapshots, recovery items, member risk, and operation history. Use `useWindowSize()` for one-column descriptions below 768px and table-owned horizontal scrolling.

- [x] **Step 3: Add complete action states**

For retry, generate one request key when the confirmation opens and retain it until the request finishes. On insufficient result, keep the drawer open and refresh latest shortfalls. For loss acceptance, require a note and show every currency/shortfall in the confirmation. Disable actions while submitting and hide them without their dedicated permission.

- [x] **Step 4: Add purchase-order navigation**

When `detail.reversal.dispositionStatus === 'PENDING_REVIEW'`, show “前往拒付审核” and route to `/payment/purchase-reversal-review?reversalNo=...`. On the review page, consume the query parameter, filter the list, and open the matching detail once.

- [x] **Step 5: Run Admin checks and responsive browser acceptance**

Run targeted ESLint, `check:i18n`, and `build:dev`. Verify pending, recovered, and loss-accepted cases at desktop and 390px mobile widths. Save:

```text
docs/implementation/phase42-reversal-review-pending.png
docs/implementation/phase42-reversal-review-recovered.png
docs/implementation/phase42-reversal-review-loss.png
```

Expected: no overlap, clipped commands, nested cards, or page-level horizontal overflow.

### Task 8: Full Regression, SQL Idempotency, And Runtime Acceptance

**Files:**
- Modify after verification: `docs/superpowers/plans/2026-07-25-purchase-reversal-review-resolution.md`
- Modify after verification: `progress.md`
- Modify after verification: `task_plan.md` with encoding-preserving byte replacement

- [x] **Step 1: Run focused backend regression**

Run all Phase 41 suites plus `PurchaseReversalReviewContractTest`, `PurchaseReversalReviewServiceImplTest`, and `PurchaseReversalReviewControllerContractTest` under `-Plocal` with `MAVEN_OPTS=-Djdk.attach.allowAttachSelf=true`.

Expected: every named suite runs with zero failures, errors, and skips.

- [x] **Step 2: Build backend and Admin**

Run backend `mvn -pl gameluck-admin -am -DskipTests package`. Run Admin `pnpm build:dev` with a controlled Node heap if Windows commit space requires it. Existing large-chunk advisory is acceptable; OOM or skipped build is not.

- [x] **Step 3: Import SQL twice**

Apply `gameluck_wallet.sql` and `gameluck_platform_dict.sql` twice through the established local MySQL command. Verify one review-log table, all reversal review columns, two unique log keys, one menu with four permissions, and exact dictionary rows without duplicates.

- [x] **Step 4: Runtime-smoke successful retry**

Use the Phase 41 `CHARGEBACK_REVIEW` order or create an equivalent fixture. Top up the missing currency, call retry once, and verify all currencies debit atomically, order becomes `CHARGEBACK`, case becomes `COMPLETED / RECOVERY_COMPLETED`, turnover becomes cancelled, risk remains high, and exactly one operation log exists.

- [x] **Step 5: Runtime-smoke accepted loss**

Create a second review case, call accept-loss with a reason, and verify wallet balances/transactions and turnover tasks remain unchanged; order stays in review; disposition becomes `LOSS_ACCEPTED`; each final shortfall and operator field is stored.

- [x] **Step 6: Verify replay and concurrent protection**

Replay both request keys and submit a different key after each terminal disposition. Confirm no duplicate debit/log, no status overwrite, and localized terminal rejection for different keys. Exercise two concurrent requests against a fresh case and confirm only one finalizes.

- [x] **Step 7: Run integrity checks**

Scan the design/plan/code for placeholders, cross-currency numeric totals, direct controller mapper access, and old “no operator action” assumptions. Run `git diff --check`; line-ending warnings are acceptable, whitespace errors are not.

- [x] **Step 8: Record completion without committing**

Mark all plan checkboxes complete, append exact test/build/SQL/runtime/UI evidence to `progress.md`, and change Phase 42 from `planned` or `in_progress` to `completed` in `task_plan.md` without rewriting its mixed encoding. Do not run `git commit`.
