# Admin Purchase Order Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Add B-side purchase order visibility, payment event audit trails, and controlled manual payment outcome marking without wallet reversal.

**Architecture:** Reuse the Phase 38 `IPurchasePaymentEventService` as the only path for manual payment outcome changes. Add Admin query/detail services around purchase orders, grant snapshots, and payment events, then expose a dense Element Plus table with detail and manual action dialogs.

**Tech Stack:** Spring Boot, Java 17, MyBatis Plus, Sa-Token permissions, GameLuck Admin UI Vue3, Element Plus, TypeScript, MySQL, Maven local profile.

---

## File Structure

- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PurchaseOrderBo.java`
  - Admin query object and manual action reason field.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchasePaymentEventVo.java`
  - Admin event row view.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOrderGrantSnapshotVo.java`
  - Admin grant snapshot row view.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOrderVo.java`
  - Admin purchase order list/detail base view.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOrderDetailVo.java`
  - Order detail with snapshots and payment events.
- Modify `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderMapper.java`
  - Add Admin VO page query support if existing BaseMapper VO support is insufficient.
- Modify `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderGrantSnapshotMapper.java`
  - Add snapshot query by order number.
- Modify `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchasePaymentEventMapper.java`
  - Add event query by order number ordered by create time.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseOrderService.java`
  - Admin service contract.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseOrderServiceImpl.java`
  - Admin query/detail/manual action implementation.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PurchaseOrderController.java`
  - Admin REST controller.
- Create `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseOrderServiceImplTest.java`
  - Unit tests for detail and manual actions.
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages*.properties`
  - Add manual reason validation message.
- Create `admin-ui/src/api/payment/purchaseOrder/index.ts`
  - Admin UI API calls.
- Create `admin-ui/src/api/payment/purchaseOrder/types.ts`
  - TypeScript API types.
- Create `admin-ui/src/views/payment/purchase-order/index.vue`
  - Admin page.
- Modify `admin-ui/src/utils/businessLabels.ts`
  - Add purchase order/event labels and tag types.
- Modify `admin-ui/src/lang/zh_CN.ts` and `admin-ui/src/lang/en_US.ts`
  - Add page copy.
- Modify `backend/script/sql/gameluck_platform_dict.sql`
  - Add purchase order/event dictionaries.
- Modify relevant menu SQL file used by this project
  - Add idempotent menu and permission rows.
- Modify `progress.md`
  - Record design, implementation, and verification.
- Modify `task_plan.md`
  - Mark Phase 39 complete only after verification passes.

Do not commit in this workspace unless the user explicitly asks for a git commit.

---

### Task 1: Backend Admin Model And Mapper Queries

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PurchaseOrderBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchasePaymentEventVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOrderGrantSnapshotVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOrderVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOrderDetailVo.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderGrantSnapshotMapper.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchasePaymentEventMapper.java`

- [x] **Step 1: Add `PurchaseOrderBo`**

Create a BO with fields: `tenantId`, `purchaseOrderNo`, `memberId`, `memberNo`, `offerId`, `offerNo`, `status`, `providerCode`, `providerOrderNo`, `paymentSessionNo`, `idempotencyKey`, `reason`, `beginTime`, `endTime`.

- [x] **Step 2: Add Admin VO classes**

Create VO classes for purchase order, grant snapshot, payment event, and detail. Use `@AutoMapper` for matching entity fields and plain list fields in `PurchaseOrderDetailVo`.

- [x] **Step 3: Add mapper lookup methods**

Add:

```java
List<PurchaseOrderGrantSnapshot> selectByPurchaseOrderNo(@Param("tenantId") String tenantId, @Param("purchaseOrderNo") String purchaseOrderNo);
List<PurchasePaymentEvent> selectByPurchaseOrderNo(@Param("tenantId") String tenantId, @Param("purchaseOrderNo") String purchaseOrderNo);
```

Order events by `create_time asc`.

### Task 2: Backend Admin Service Tests

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseOrderServiceImplTest.java`

- [x] **Step 1: Write detail aggregation test**

Test `queryDetailByIdLoadsSnapshotsAndEvents`: mock order, two snapshots, two events, then assert the detail VO contains all rows and member no is preserved when available.

- [x] **Step 2: Write manual action validation test**

Test `manualActionRequiresReason`: call each manual method with blank reason and assert `ServiceException` message `payment.purchase.manual.reason.required`.

- [x] **Step 3: Write manual event tests**

Test `manualCancelCreatesPaymentEventCommand` and `manualChargebackCreatesPaymentEventCommand`: mock a target order and capture `PurchasePaymentCallbackBo`. Assert event type, `MANUAL_ADMIN`, order no, provider order no fallback, and request body reason.

- [x] **Step 4: Run failing tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -Plocal -DskipTests=false "-Dtest=PurchaseOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected first run: compilation failure because `IPurchaseOrderService` and `PurchaseOrderServiceImpl` do not exist.

### Task 3: Backend Admin Service And Controller

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseOrderService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseOrderServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PurchaseOrderController.java`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`

- [x] **Step 1: Add service interface**

Methods:

```java
TableDataInfo<PurchaseOrderVo> queryPageList(PurchaseOrderBo bo, PageQuery pageQuery);
PurchaseOrderDetailVo queryById(Long id);
PurchaseOrderDetailVo markFailed(Long id, String reason);
PurchaseOrderDetailVo cancel(Long id, String reason);
PurchaseOrderDetailVo refund(Long id, String reason);
PurchaseOrderDetailVo chargeback(Long id, String reason);
```

- [x] **Step 2: Implement list and detail**

Use `LambdaQueryWrapper<PurchaseOrder>`, `MemberNoQueryHelper`, grant snapshot mapper, and payment event mapper. Keep ordering `create_time desc`.

- [x] **Step 3: Implement manual action helpers**

Load order by id, require nonblank reason, build a `PurchasePaymentCallbackBo`, call `purchasePaymentEventService.applyEvent(...)`, and return fresh detail.

- [x] **Step 4: Add controller**

Use permissions:

- `payment:purchaseOrder:list`
- `payment:purchaseOrder:query`
- `payment:purchaseOrder:manual`

Use `@Log(... BusinessType.UPDATE)` on manual endpoints.

- [x] **Step 5: Run backend tests**

Run the `PurchaseOrderServiceImplTest` Maven command again. Expected: pass.

### Task 4: SQL Dictionaries And Menus

**Files:**
- Modify: `backend/script/sql/gameluck_platform_dict.sql`
- Modify: project menu seed SQL used for local Admin menus

- [x] **Step 1: Add dictionaries**

Add idempotent dictionary rows for:

- `gl_purchase_order_status`
- `gl_purchase_payment_event_type`
- `gl_purchase_payment_event_status`

Include status values from the Phase 38 enums.

- [x] **Step 2: Add menu and permissions**

Add idempotent rows for route `payment/purchase-order/index` and permissions:

- `payment:purchaseOrder:list`
- `payment:purchaseOrder:query`
- `payment:purchaseOrder:manual`

- [x] **Step 3: Import SQL**

Run:

```powershell
cmd /c "C:\tools\mysql-8.0.46-winx64\bin\mysql.exe -uroot -proot gameluck_vue < backend\script\sql\gameluck_platform_dict.sql"
```

If menu SQL is separate, import it too.

### Task 5: Admin UI API And Page

**Files:**
- Create: `admin-ui/src/api/payment/purchaseOrder/index.ts`
- Create: `admin-ui/src/api/payment/purchaseOrder/types.ts`
- Create: `admin-ui/src/views/payment/purchase-order/index.vue`
- Modify: `admin-ui/src/utils/businessLabels.ts`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`

- [x] **Step 1: Add API types and calls**

Define `PurchaseOrderVO`, `PurchaseOrderDetailVO`, `PurchasePaymentEventVO`, `PurchaseOrderGrantSnapshotVO`, `PurchaseOrderQuery`, and `PurchaseManualActionForm`.

- [x] **Step 2: Add business labels**

Add label maps and tag type helpers for purchase order statuses, payment event types, and event statuses.

- [x] **Step 3: Build the page**

Create the dense query table, detail dialog, event/snapshot tables, and manual action dialog. Hide row actions with `v-hasPermi`.

- [x] **Step 4: Add i18n copy**

Add Chinese and English keys for filters, fields, statuses, event timeline, manual actions, validation, and confirmation copy.

- [x] **Step 5: Run Admin check**

Run the available Admin UI build/type command used in prior phases. If existing unrelated TypeScript errors remain, record exact errors and verify this new page has no direct errors.

### Task 6: Final Verification And Runtime Smoke

**Files:**
- Modify: `progress.md`
- Modify: `task_plan.md`
- Modify: `docs/superpowers/plans/2026-07-18-admin-purchase-order-operations.md`

- [x] **Step 1: Run focused backend tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -Plocal -DskipTests=false "-Dtest=PurchaseOrderServiceImplTest,PurchasePaymentEventServiceImplTest,ClientPurchaseServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

- [x] **Step 2: Package backend**

Stop only the local `gameluck-admin.jar` Java process if it locks the jar, then run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

- [x] **Step 3: Runtime smoke**

Use Admin API auth:

1. Create or reuse a C-side credited purchase order.
2. Confirm `GET /payment/purchase-order/list` returns it.
3. Confirm `GET /payment/purchase-order/{id}` includes snapshots and events.
4. Insert a DB-seeded `PENDING` purchase order for manual cancel.
5. Call `POST /payment/purchase-order/{id}/cancel` with reason.
6. Confirm status becomes `CANCELLED`.
7. Confirm one `MANUAL_ADMIN` event row exists.
8. Confirm latest operation log includes the manual endpoint.

- [x] **Step 4: UI smoke**

Run Admin dev server if needed and verify the route loads, filters render, details open, and manual dialog validates blank reason. Use Playwright/browser screenshots if available.

- [x] **Step 5: Whitespace check**

Run:

```powershell
git diff --check
```

Expected exit code `0`; CRLF warnings are acceptable.

- [x] **Step 6: Record completion**

Update `progress.md`, mark all checkboxes complete, and mark `task_plan.md` Phase 39 complete only after all required checks pass.

## Self Review

- Spec coverage: plan covers backend query/detail, manual event actions, Admin UI, dictionaries/menus, tests, runtime smoke, and docs.
- Scope control: wallet reversal and real provider callbacks remain excluded.
- Placeholder scan: no TODO/TBD placeholders are present.
- Type consistency: `PurchaseOrderBo`, `PurchaseOrderVo`, `PurchaseOrderDetailVo`, and manual service names match across tasks.
