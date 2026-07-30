# Purchase Refund And Chargeback Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recover every asset granted by a refunded or charged-back purchase atomically, preserve exact review shortfalls when recovery is impossible, and expose the complete audit chain to administrators.

**Architecture:** The wallet module owns deterministic multi-currency locking, all-or-nothing debit, and pending-turnover cancellation. The payment module owns durable reversal cases/items and orchestrates order, event, wallet, turnover, and chargeback-risk changes in one transaction; the Admin purchase detail remains a read-only projection of those records.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, MySQL, Vue 3, TypeScript, Element Plus, Vitest/Playwright-compatible Admin checks.

**Execution constraint:** Do not create Git commits unless the user explicitly requests one. The commit steps normally required by the planning workflow are intentionally replaced with review checkpoints because the shared worktree contains uncommitted changes from several phases.

---

## File Map

- Wallet contracts and result types: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletBatchDebitBo.java`, `WalletBatchDebitLineBo.java`, `domain/vo/WalletBatchDebitResult.java`, `WalletBatchDebitLineResult.java`.
- Wallet behavior: `IWalletCoreService.java`, `WalletCoreServiceImpl.java`, `IWalletTurnoverTaskService.java`, `WalletTurnoverTaskServiceImpl.java`, `WalletTurnoverTaskMapper.java`, and `WalletTurnoverTaskMapper.xml`.
- Reversal persistence: new payment domain, VO, enums, and mappers for `gl_purchase_reversal` and `gl_purchase_reversal_item`.
- Reversal orchestration: new `IPurchaseReversalService.java` and `PurchaseReversalServiceImpl.java`; existing `PurchasePaymentEventServiceImpl.java` delegates refund and chargeback handling.
- Member audit: `MemberProfile.java`, `MemberProfileVo.java`, `MemberProfileBo.java`, `MemberProfileMapper.java`, and `MemberProfileMapper.xml`.
- Admin projection: `PurchaseOrderDetailVo.java`, `PurchaseOrderServiceImpl.java`, `admin-ui/src/api/payment/purchaseOrder/types.ts`, and `admin-ui/src/views/payment/purchase-order/index.vue`.
- Schema/localization: `backend/script/sql/gameluck_wallet.sql`, `backend/script/sql/gameluck_platform_dict.sql`, and all `messages*.properties` bundles under `backend/gameluck-admin/src/main/resources/i18n/`.

### Task 1: Define Reversal Schema And Payment Domain

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchaseReversalType.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchaseReversalStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseReversal.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseReversalItem.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseReversalMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseReversalItemMapper.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchaseOrderStatus.java`
- Modify: `backend/script/sql/gameluck_wallet.sql`

- [x] **Step 1: Add a schema/domain contract test that requires both tables, unique keys, review statuses, and exact money fields**

Add `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PurchaseReversalContractTest.java` with assertions for `REFUND_REVIEW`, `CHARGEBACK_REVIEW`, `PROCESSING`, `COMPLETED`, `REVIEW_REQUIRED`, and reflection checks for `requiredAmount`, `availableAmount`, `recoveredAmount`, and `shortfallAmount` as `BigDecimal`.

- [x] **Step 2: Run the contract test and verify RED**

Run: `mvn -pl gameluck-modules/gameluck-payment -am -DskipTests=false -Dtest=PurchaseReversalContractTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: test compilation fails because the reversal types and new order statuses do not exist.

- [x] **Step 3: Add the exact persistence model**

Use these enums:

```java
public enum PurchaseReversalType { REFUND, CHARGEBACK }
public enum PurchaseReversalStatus { PROCESSING, COMPLETED, REVIEW_REQUIRED }
```

Add `REFUND_REVIEW` and `CHARGEBACK_REVIEW` to `PurchaseOrderStatus`. Model both entities with the fields in the approved design and `@TableName("gl_purchase_reversal")` / `@TableName("gl_purchase_reversal_item")`. Add mapper methods:

```java
PurchaseReversal selectByEventKey(String tenantId, String eventKey);
PurchaseReversal selectByPurchaseOrderNo(String tenantId, String purchaseOrderNo);
List<PurchaseReversalItem> selectByReversalNo(String tenantId, String reversalNo);
```

Define both tables in `gameluck_wallet.sql` with `decimal(20,8)` monetary columns, unique keys `(tenant_id,event_key)`, `(tenant_id,reversal_no)`, and `(tenant_id,reversal_no,currency_code)`, plus indexes on purchase order number and member id.

- [x] **Step 4: Run the contract test and verify GREEN**

Run the command from Step 2.

Expected: `PurchaseReversalContractTest` passes with zero failures.

- [x] **Step 5: Review checkpoint**

Run: `git diff -- backend/gameluck-modules/gameluck-payment backend/script/sql/gameluck_wallet.sql`

Expected: only reversal persistence and the two new order statuses are present; no commit is created.

### Task 2: Add Atomic Multi-Currency Wallet Debit

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletBatchDebitBo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletBatchDebitLineBo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletBatchDebitResult.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletBatchDebitLineResult.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletCoreService.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletCoreServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletCoreServiceImplTest.java`

- [x] **Step 1: Write failing tests for success, insufficiency, missing account, sort order, and replay**

Add tests named `batchDebitDebitsEveryCurrencyWhenAllBalancesAreSufficient`, `batchDebitReturnsReviewWithoutWritesWhenOneCurrencyIsInsufficient`, `batchDebitTreatsMissingAccountAsFullShortfall`, and `batchDebitReplayReturnsOriginalTransactions`. Assert no `walletAccountMapper.updateById` or transaction insert occurs for either review case.

- [x] **Step 2: Run wallet tests and verify RED**

Run: `mvn -pl gameluck-modules/gameluck-wallet -am -DskipTests=false -Dtest=WalletCoreServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: test compilation fails because `batchDebit(WalletBatchDebitBo)` and its types do not exist.

- [x] **Step 3: Add the public wallet contract**

```java
public interface IWalletCoreService {
    WalletBatchDebitResult batchDebit(WalletBatchDebitBo bo);
}
```

`WalletBatchDebitBo` contains `tenantId`, `memberId`, `businessNo`, `sourceType`, `remark`, and `List<WalletBatchDebitLineBo> lines`. Each line contains uppercase `currencyCode`, positive `amount`, and `idempotencyKey`. The result has status `COMPLETED` or `REVIEW_REQUIRED` and one result line per currency with required, available, shortfall, and transaction number.

- [x] **Step 4: Implement deterministic preflight and all-or-nothing debit**

Normalize duplicate currencies by summing, reject non-positive totals, sort with `Comparator.comparing(WalletBatchDebitLineBo::getCurrencyCode)`, lock every account via `selectByBizKeyForUpdate`, and compute every result before writing. If any shortfall is positive, return `REVIEW_REQUIRED`; otherwise invoke the existing internal debit path for each locked account using source type `PURCHASE_REVERSAL`, retaining the caller transaction.

- [x] **Step 5: Run wallet tests and verify GREEN**

Run the command from Step 2.

Expected: all `WalletCoreServiceImplTest` tests pass, including unchanged single-currency tests.

### Task 3: Cancel Pending Purchase Turnover Tasks

**Files:**
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletTurnoverTaskService.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletTurnoverTaskServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/mapper/WalletTurnoverTaskMapper.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/resources/mapper/wallet/WalletTurnoverTaskMapper.xml`
- Test: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletTurnoverTaskServiceImplTest.java`

- [x] **Step 1: Write failing cancellation tests**

Test that `PENDING` rows matching tenant/member/purchase number become `CANCELLED`, `COMPLETED` rows remain unchanged, the remark contains the reversal number, and a repeat call returns zero.

- [x] **Step 2: Run the focused test and verify RED**

Run: `mvn -pl gameluck-modules/gameluck-wallet -am -DskipTests=false -Dtest=WalletTurnoverTaskServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation fails because `cancelPendingByPurchase(...)` is missing.

- [x] **Step 3: Implement the exact cancellation boundary**

```java
int cancelPendingByPurchase(String tenantId, Long memberId,
                            String purchaseOrderNo, String reversalNo, Date now);
```

Use one guarded SQL update with `status = 'PENDING'`, set `status = 'CANCELLED'`, `update_time = #{now}`, and `remark = CONCAT('Purchase reversal ', #{reversalNo})`. Do not select or reopen completed tasks.

- [x] **Step 4: Run the focused test and verify GREEN**

Run the command from Step 2.

Expected: all turnover tests pass and repeat cancellation changes zero rows.

### Task 4: Build The Purchase Reversal Orchestrator

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseReversalService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseReversalServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseReversalResult.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseReversalServiceImplTest.java`

- [x] **Step 1: Write failing orchestration tests**

Cover refund success, refund shortfall, aggregation of duplicate uppercase currency snapshots, missing snapshots, non-positive totals, same-event replay, and a different event key after terminal/review status. Assert success cancels turnover; review leaves wallet and turnover untouched.

- [x] **Step 2: Run tests and verify RED**

Run: `mvn -pl gameluck-modules/gameluck-payment -am -DskipTests=false -Dtest=PurchaseReversalServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation fails because the service and result type are absent.

- [x] **Step 3: Add the service contract and aggregation**

```java
public interface IPurchaseReversalService {
    PurchaseReversalResult reverse(PurchaseOrder order,
                                   PurchasePaymentCallbackBo callback,
                                   Date processingTime);
}
```

Return the updated order plus process result `OK` or `REVIEW_REQUIRED`. Resolve an existing reversal by `(tenantId,eventKey)` before validating order status. Otherwise require `PAID` or `CREDITED`, load grant snapshots, uppercase and sum by currency, reject empty/non-positive data, and insert a `PROCESSING` case plus item rows.

- [x] **Step 4: Implement completed and review outcomes**

Call `walletCoreService.batchDebit`. On `COMPLETED`, persist recovered amounts/transaction numbers, cancel pending turnover, set the case and items to `COMPLETED`, update the matching event time, clear `failReason`, and set the final order status. On `REVIEW_REQUIRED`, persist all available/shortfall values, keep recovered amounts zero, set review statuses, and store `MessageUtils.message("payment.purchase.reversal.review.required")` on both case and order.

- [x] **Step 5: Run tests and verify GREEN**

Run the command from Step 2.

Expected: all reversal service tests pass; Mockito verifies zero wallet mutation on review and one cancellation on success.

### Task 5: Integrate Payment Events And Process Results

**Files:**
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchasePaymentEventServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchasePaymentEventServiceImplTest.java`

- [x] **Step 1: Replace old refund/chargeback expectations with failing delegation tests**

Assert both event types call `purchaseReversalService.reverse`, `REVIEW_REQUIRED` still leaves the event `PROCESSED`, and same event replay returns before calling reversal again. Assert technical failures remain `FAILED` and rethrow.

- [x] **Step 2: Run tests and verify RED**

Run: `mvn -pl gameluck-modules/gameluck-payment -am -DskipTests=false -Dtest=PurchasePaymentEventServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: failures show the existing deferred-wallet branches and hard-coded `processResult = OK`.

- [x] **Step 3: Delegate reversal processing**

Inject `IPurchaseReversalService`. Make `processEvent` return a small internal result containing `PurchaseOrder order` and `String processResult`; PAY_SUCCESS/PAY_FAILED/CANCELLED return `OK`, while REFUNDED/CHARGEBACK return the reversal result. Remove both deferred-wallet strings.

- [x] **Step 4: Run tests and verify GREEN**

Run the command from Step 2.

Expected: payment event tests pass; review outcomes are `PROCESSED / REVIEW_REQUIRED` and technical exceptions are `FAILED`.

### Task 6: Persist Chargeback Risk Audit

**Files:**
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/MemberProfile.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/vo/MemberProfileVo.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/bo/MemberProfileBo.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/mapper/MemberProfileMapper.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/resources/mapper/member/MemberProfileMapper.xml`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseReversalServiceImpl.java`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseReversalServiceImplTest.java`

- [x] **Step 1: Add failing chargeback tests**

Test both completed and review-required chargebacks set `riskLevel = HIGH`, localized `riskReason`, `riskSource = PURCHASE_CHARGEBACK:{reversalNo}:{eventKey}`, and `riskUpdatedTime = processingTime`. Verify refund does not update risk and same-event replay does not update it twice.

- [x] **Step 2: Run tests and verify RED**

Run the Task 4 test command.

Expected: compilation or assertion failure because the three risk audit fields and guarded mapper update do not exist.

- [x] **Step 3: Add fields and a locked/guarded update**

Add `risk_reason varchar(500)`, `risk_source varchar(255)`, and `risk_updated_time datetime` to `gl_member_profile` using idempotent migration statements. Add:

```java
MemberProfile selectByIdForUpdate(String tenantId, Long memberId);
int updateChargebackRisk(String tenantId, Long memberId, String reason,
                         String source, Date riskUpdatedTime);
```

Lock the member during a new chargeback reversal, preserve all unrelated compliance fields, and perform the risk update for both wallet outcomes before returning.

- [x] **Step 4: Run tests and verify GREEN**

Run the Task 4 test command plus `mvn -pl gameluck-modules/gameluck-member -am -DskipTests=false -Dtest=MemberProfileServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`.

Expected: chargeback tests and existing member tests pass.

### Task 7: Expose Reversal Details In Admin Backend

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseReversalVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseReversalItemVo.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOrderDetailVo.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseOrderServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseOrderServiceImplTest.java`

- [x] **Step 1: Write a failing detail projection test**

Assert `getPurchaseOrderDetail` returns one reversal with its ordered item list, all reason/status/time fields, and no fabricated reversal when none exists.

- [x] **Step 2: Run tests and verify RED**

Run: `mvn -pl gameluck-modules/gameluck-payment -am -DskipTests=false -Dtest=PurchaseOrderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation fails because `PurchaseOrderDetailVo.reversal` is absent.

- [x] **Step 3: Add the read-only projection**

Add `private PurchaseReversalVo reversal;` to detail. `PurchaseReversalVo` exposes reversal number/type/status/reason/review reason/completed time and `List<PurchaseReversalItemVo> items`; item fields exactly match the design. Query by purchase order number and then by reversal number, preserving currency order.

- [x] **Step 4: Run tests and verify GREEN**

Run the command from Step 2.

Expected: all order service tests pass for both present and absent reversal data.

### Task 8: Render Recovery And Risk Audit In Admin UI

**Files:**
- Modify: `admin-ui/src/api/payment/purchaseOrder/types.ts`
- Modify: `admin-ui/src/views/payment/purchase-order/index.vue`
- Modify: `admin-ui/src/api/member/profile/types.ts`
- Modify: `admin-ui/src/views/member/profile/index.vue`

- [x] **Step 1: Add exact TypeScript contracts**

Define `PurchaseReversalVO` and `PurchaseReversalItemVO`, add `reversal?: PurchaseReversalVO` to `PurchaseOrderDetailVO`, and add `riskReason`, `riskSource`, and `riskUpdatedTime` to the existing member profile type.

- [x] **Step 2: Add status labels before markup changes**

Add labels/tags for `REFUND_REVIEW`, `CHARGEBACK_REVIEW`, `COMPLETED`, and `REVIEW_REQUIRED`. Use warning/danger tag types for review states and preserve existing final-status colors.

- [x] **Step 3: Add the read-only recovery section**

Below grant snapshots, render a non-nested section headed `资产追偿`, a compact description list for case metadata, and a stable-width table with currency, required, available, recovered, shortfall, item status, and wallet transaction number. Render shortfall with danger emphasis only when positive; render an empty state when no reversal exists.

- [x] **Step 4: Update confirmation and member-detail copy**

Change refund/chargeback warnings to state that the action attempts full asset recovery and may enter manual review. Display the three risk audit fields in member detail without adding any edit controls.

- [x] **Step 5: Run Admin static verification**

Run: `pnpm --dir admin-ui check:i18n`

Expected: exit code 0 with no missing translation keys.

Run: `pnpm --dir admin-ui build:dev`

Expected: build succeeds; only the repository's existing large-chunk advisory is acceptable.

### Task 9: Add Dictionaries And Localized Backend Messages

**Files:**
- Modify: `backend/script/sql/gameluck_platform_dict.sql`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: every localized sibling `backend/gameluck-admin/src/main/resources/i18n/messages_*.properties`

- [x] **Step 1: Add idempotent dictionary rows**

Insert order labels for `REFUND_REVIEW` and `CHARGEBACK_REVIEW`, reversal type labels for `REFUND`/`CHARGEBACK`, and reversal status labels for `PROCESSING`/`COMPLETED`/`REVIEW_REQUIRED`, using the file's existing `NOT EXISTS` convention and stable sort values.

- [x] **Step 2: Add backend message keys to every bundle**

Add equivalent localized entries for:

```properties
payment.purchase.reversal.snapshot.missing=Purchase grant snapshots are missing
payment.purchase.reversal.amount.invalid=Purchase reversal amount must be positive
payment.purchase.reversal.review.required=Full asset recovery requires manual review
payment.purchase.chargeback.risk.reason=Purchase chargeback triggered high-risk control
```

Use natural locale-specific translations in sibling bundles and retain these exact keys.

- [x] **Step 3: Verify key parity and SQL idempotency text**

Run: `pnpm --dir admin-ui check:i18n`

Expected: exit code 0.

Run: `rg -n "REFUND_REVIEW|CHARGEBACK_REVIEW|REVIEW_REQUIRED|payment.purchase.reversal|payment.purchase.chargeback.risk.reason" backend/script/sql backend/gameluck-admin/src/main/resources/i18n`

Expected: every status and all four message keys are present; dictionary inserts use idempotent guards.

### Task 10: Full Regression And Runtime Acceptance

**Files:**
- Modify after successful verification: `docs/superpowers/plans/2026-07-20-purchase-refund-chargeback-recovery.md`
- Modify after successful verification: `progress.md`
- Modify after successful verification: `task_plan.md` using encoding-preserving byte-level replacement if required
- Create: `docs/implementation/phase41-purchase-reversal-completed.png`
- Create: `docs/implementation/phase41-purchase-reversal-review.png`

- [x] **Step 1: Run focused backend regression**

Run: `mvn -pl gameluck-modules/gameluck-wallet,gameluck-modules/gameluck-member,gameluck-modules/gameluck-payment -am -DskipTests=false -Dtest=WalletCoreServiceImplTest,WalletTurnoverTaskServiceImplTest,MemberProfileServiceImplTest,PurchaseReversalContractTest,PurchaseReversalServiceImplTest,PurchasePaymentEventServiceImplTest,PurchaseOrderServiceImplTest,ClientPurchaseServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: all named suites pass with zero failures and zero errors.

- [x] **Step 2: Build backend and Admin**

Run: `mvn -pl gameluck-admin -am -DskipTests package`

Expected: `BUILD SUCCESS`.

Run: `pnpm --dir admin-ui build:dev`

Expected: successful build; the existing chunk-size advisory is acceptable.

- [x] **Step 3: Import SQL twice and verify idempotency**

Apply the updated `gameluck_wallet.sql` and `gameluck_platform_dict.sql` through the repository's established local MySQL command, then repeat the same import.

Expected: both runs succeed; reversal tables, member columns, unique keys, and dictionary rows exist exactly once.

- [x] **Step 4: Runtime-smoke a successful refund**

Against `http://127.0.0.1:8080`, create/pay a purchase whose granted currency balances remain sufficient, submit one `REFUNDED` event, and query order, reversal, items, wallet accounts, wallet transactions, and turnover tasks.

Expected: order `REFUNDED`; event `PROCESSED / OK`; reversal/items `COMPLETED`; every aggregated currency is fully debited; only `PENDING` purchase turnover tasks are `CANCELLED`; no balance is negative.

- [x] **Step 5: Replay the refund event**

Submit the identical event key/body again and compare row counts, balances, transaction numbers, and turnover statuses.

Expected: the same order/result returns and no count or balance changes.

- [x] **Step 6: Runtime-smoke an insufficient chargeback**

Create/pay another purchase, consume enough of one granted currency to create a known shortfall, submit `CHARGEBACK`, and query the same audit chain plus member risk fields.

Expected: order `CHARGEBACK_REVIEW`; event `PROCESSED / REVIEW_REQUIRED`; all item recovered amounts are zero; exact per-currency available/shortfall values are stored; no reversal debit transaction exists; turnover remains unchanged; member risk is `HIGH` with reason/source/time populated.

- [x] **Step 7: Verify conflicting event protection**

Submit a different refund/chargeback event key against each final/review order.

Expected: localized invalid-status failure; no second reversal, wallet debit, turnover update, or risk update occurs.

- [x] **Step 8: Verify Admin desktop and mobile layouts**

Open both runtime orders in the purchase detail UI, capture the completed and review screenshots at desktop width, then inspect a mobile viewport.

Expected: case metadata and every currency row are readable; review shortfalls are prominent; no text overlap, nested cards, clipped controls, or horizontal page overflow. Save the two desktop screenshots at the paths listed above.

- [x] **Step 9: Run final integrity checks**

Run: `$terms = @('T' + 'BD', 'T' + 'ODO', 'implement ' + 'later', 'fill in ' + 'details', 'deferred ' + 'wallet', 'wallet reversal is ' + 'deferred', 'wallet clawback is ' + 'deferred'); Select-String -Path 'docs/superpowers/plans/2026-07-20-purchase-refund-chargeback-recovery.md','backend/gameluck-modules/gameluck-payment/**/*.java' -Pattern $terms`

Expected: no plan placeholders and no old deferred-wallet implementation text.

Run: `git diff --check`

Expected: exit code 0; line-ending notices are acceptable, whitespace errors are not.

- [x] **Step 10: Record completion without committing**

Mark completed checkboxes in this plan, append exact test/build/runtime evidence to `progress.md`, and change Phase 41 in `task_plan.md` from `in_progress` to `completed` without rewriting its mixed-encoding content. Do not run `git commit` unless the user explicitly authorizes it.
