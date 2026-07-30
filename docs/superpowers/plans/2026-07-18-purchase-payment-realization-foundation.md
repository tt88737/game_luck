# Purchase Payment Realization Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Make C-side purchase orders gateway-ready by adding provider/session fields, payment event idempotency, and a shared payment-event processing service while preserving the current simulated purchase demo.

**Architecture:** Keep order creation in `ClientPurchaseService`, but move "payment succeeded -> snapshot grants -> credit wallet -> mark credited" into `PurchasePaymentEventServiceImpl`. The C-side simulated pay endpoint will create a pending simulated order and immediately apply a simulated `PAY_SUCCESS` event through that same service.

**Tech Stack:** Spring Boot, Java 17, MyBatis Plus, Lombok, JUnit 5, Mockito, MySQL, Maven local profile.

---

## File Structure

- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchaseOrderStatus.java`
  - Stable purchase order status enum.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchasePaymentEventType.java`
  - Stable payment event type enum.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchasePaymentEventStatus.java`
  - Event processing status enum.
- Modify `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseOrder.java`
  - Add provider/session/callback/refund/chargeback fields.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchasePaymentEvent.java`
  - Entity for `gl_purchase_payment_event`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PurchasePaymentCallbackBo.java`
  - Internal callback/event command object.
- Modify `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderMapper.java`
  - Add row-lock query by purchase order number.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchasePaymentEventMapper.java`
  - Event mapper with lookup by event key.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchasePaymentEventService.java`
  - Event processing service interface.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchasePaymentEventServiceImpl.java`
  - Idempotent event processing implementation.
- Modify `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
  - Create simulated provider/session fields and call event service.
- Create `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchasePaymentEventServiceImplTest.java`
  - Unit coverage for event idempotency/status behavior.
- Modify `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`
  - Update constructor mocks and preserve C-side behavior assertions.
- Modify `backend/script/sql/gameluck_wallet.sql`
  - Add idempotent columns/indexes/table.
- Modify `progress.md`
  - Record implementation and verification results.
- Modify `task_plan.md`
  - Mark Phase 38 complete only after verification passes.

Do not commit in this workspace unless the user explicitly asks for a git commit.

---

### Task 1: Payment Status And Event Domain

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchaseOrderStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchasePaymentEventType.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/PurchasePaymentEventStatus.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseOrder.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchasePaymentEvent.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PurchasePaymentCallbackBo.java`

- [x] **Step 1: Add purchase order status enum**

Create `PurchaseOrderStatus.java`:

```java
package com.gameluck.payment.enums;

public enum PurchaseOrderStatus {
    CREATED,
    PENDING,
    PAID,
    CREDITED,
    FAILED,
    CANCELLED,
    REFUNDED,
    CHARGEBACK
}
```

- [x] **Step 2: Add payment event type enum**

Create `PurchasePaymentEventType.java`:

```java
package com.gameluck.payment.enums;

public enum PurchasePaymentEventType {
    PAY_SUCCESS,
    PAY_FAILED,
    CANCELLED,
    REFUNDED,
    CHARGEBACK
}
```

- [x] **Step 3: Add payment event status enum**

Create `PurchasePaymentEventStatus.java`:

```java
package com.gameluck.payment.enums;

public enum PurchasePaymentEventStatus {
    RECEIVED,
    PROCESSED,
    IGNORED,
    FAILED
}
```

- [x] **Step 4: Extend purchase order entity**

In `PurchaseOrder.java`, add these fields after `idempotencyKey` and before `failReason`:

```java
private String providerCode;

private String providerOrderNo;

private String paymentSessionNo;

private String callbackEventKey;
```

Add these fields after `creditedTime`:

```java
private Date cancelTime;

private Date refundTime;

private Date chargebackTime;
```

- [x] **Step 5: Add purchase payment event entity**

Create `PurchasePaymentEvent.java`:

```java
package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("gl_purchase_payment_event")
public class PurchasePaymentEvent {

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String eventKey;

    private String purchaseOrderNo;

    private String providerCode;

    private String providerOrderNo;

    private String eventType;

    private String eventStatus;

    private String requestHash;

    private String requestBody;

    private String processResult;

    private Date processTime;

    private Date createTime;
}
```

- [x] **Step 6: Add callback command object**

Create `PurchasePaymentCallbackBo.java`:

```java
package com.gameluck.payment.domain.bo;

import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchasePaymentCallbackBo {

    private String tenantId;

    private String eventKey;

    private String purchaseOrderNo;

    private String providerCode;

    private String providerOrderNo;

    private PurchasePaymentEventType eventType;

    private String requestBody;

    private String failReason;

    public static PurchasePaymentCallbackBo simulatedSuccess(PurchaseOrder order) {
        return PurchasePaymentCallbackBo.builder()
            .tenantId(order.getTenantId())
            .eventKey("purchase:simulated:pay-success:" + order.getPurchaseOrderNo())
            .purchaseOrderNo(order.getPurchaseOrderNo())
            .providerCode(order.getProviderCode())
            .providerOrderNo(order.getProviderOrderNo())
            .eventType(PurchasePaymentEventType.PAY_SUCCESS)
            .requestBody("{\"source\":\"SIMULATED\"}")
            .build();
    }
}
```

### Task 2: Mapper And Event Service Tests

**Files:**
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchasePaymentEventMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchasePaymentEventServiceImplTest.java`

- [x] **Step 1: Add order row-lock query**

In `PurchaseOrderMapper.java`, add:

```java
@Select("select * from gl_purchase_order where tenant_id = #{tenantId} and purchase_order_no = #{purchaseOrderNo} limit 1 for update")
PurchaseOrder selectByOrderNoForUpdate(@Param("tenantId") String tenantId, @Param("purchaseOrderNo") String purchaseOrderNo);
```

- [x] **Step 2: Add payment event mapper**

Create `PurchasePaymentEventMapper.java`:

```java
package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gameluck.payment.domain.PurchasePaymentEvent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PurchasePaymentEventMapper extends BaseMapper<PurchasePaymentEvent> {

    @Select("select * from gl_purchase_payment_event where tenant_id = #{tenantId} and event_key = #{eventKey} limit 1")
    PurchasePaymentEvent selectByEventKey(@Param("tenantId") String tenantId, @Param("eventKey") String eventKey);
}
```

- [x] **Step 3: Create failing event service tests**

Create `PurchasePaymentEventServiceImplTest.java` with these concrete tests:

- `paySuccessCreditsWalletOnceWhenEventRepeated`
  - Arrange a `PENDING` purchase order with `purchaseOrderNo=PO1001`.
  - Arrange two grant items, `GC` purchase grant and `SC` purchase bonus.
  - First call uses event key `evt-1`; expect order status `CREDITED`, event status `PROCESSED`, two wallet credits, and `callbackEventKey=evt-1`.
  - Second call uses the same event key and same request body; arrange `eventMapper.selectByEventKey` to return the already stored event and order mapper to return the credited order; expect zero additional wallet credits.
- `sameEventKeyWithDifferentPayloadIsRejected`
  - Arrange existing event key `evt-conflict` with request hash for body `{"amount":"10.00"}`.
  - Call `applyEvent` with the same event key and body `{"amount":"11.00"}`.
  - Expect `ServiceException` message key `payment.purchase.event.idempotency.conflict` and no wallet credit.
- `payFailedMarksPendingOrderFailedWithoutWalletCredit`
  - Arrange a `PENDING` order and a `PAY_FAILED` callback with fail reason `provider declined`.
  - Expect status `FAILED`, fail reason persisted, event status `PROCESSED`, and no wallet credit.
- `cancelledMarksPendingOrderCancelledWithoutWalletCredit`
  - Arrange a `PENDING` order and a `CANCELLED` callback.
  - Expect status `CANCELLED`, `cancelTime` populated, event status `PROCESSED`, and no wallet credit.
- `refundAndChargebackAreRecordOnlyForCreditedOrders`
  - Arrange a `CREDITED` order.
  - Apply `REFUNDED`; expect status `REFUNDED`, `refundTime` populated, event status `PROCESSED`, and no wallet credit.
  - Arrange another `CREDITED` order.
  - Apply `CHARGEBACK`; expect status `CHARGEBACK`, `chargebackTime` populated, event status `PROCESSED`, and no wallet credit.

Use Mockito mocks for:

- `PurchaseOrderMapper`
- `PurchasePaymentEventMapper`
- `PurchaseOfferGrantItemMapper`
- `IPurchaseOfferService`
- `IWalletCoreService`

Expected first run: compilation fails because `IPurchasePaymentEventService` and `PurchasePaymentEventServiceImpl` do not exist yet.

- [x] **Step 4: Run failing tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -Plocal -DskipTests=false "-Dtest=PurchasePaymentEventServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: compilation failure for missing event service types.

### Task 3: Implement Payment Event Service

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchasePaymentEventService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchasePaymentEventServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchasePaymentEventServiceImplTest.java`

- [x] **Step 1: Add service interface**

Create `IPurchasePaymentEventService.java`:

```java
package com.gameluck.payment.service;

import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;

public interface IPurchasePaymentEventService {

    PurchaseOrder applyEvent(PurchasePaymentCallbackBo bo);
}
```

- [x] **Step 2: Implement event service**

Create `PurchasePaymentEventServiceImpl.java`:

Key rules:

- Resolve blank tenant to `000000`.
- Compute `requestHash` from event type, order number, provider code, provider order number, request body, and fail reason.
- If existing event key has same hash, return current order by locked order number without wallet credit.
- If existing event key has different hash, throw `ServiceException(MessageUtils.message("payment.purchase.event.idempotency.conflict"))`.
- Insert event as `RECEIVED` before processing new events.
- Use `selectByOrderNoForUpdate(...)` before changing order state.
- For `PAY_SUCCESS`, allow only `CREATED`, `PENDING`, `PAID`, and `CREDITED`.
- For `PAY_SUCCESS`, credit wallet only if order is not already `CREDITED`.
- For `PAY_FAILED` and `CANCELLED`, allow only `CREATED` and `PENDING`.
- For `REFUNDED` and `CHARGEBACK`, allow only `PAID` and `CREDITED`, and do not call wallet.

- [x] **Step 3: Run event service tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -Plocal -DskipTests=false "-Dtest=PurchasePaymentEventServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: event service tests pass.

### Task 4: Refactor C-Side Simulated Purchase

**Files:**
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`

- [x] **Step 1: Inject event service**

In `ClientPurchaseService`, replace direct wallet-credit fields with:

```java
private final IPurchasePaymentEventService purchasePaymentEventService;
```

Keep `IPurchaseOfferService` because offer listing and snapshot construction still use it.

- [x] **Step 2: Build provider/session fields**

In `buildOrder(...)`, set:

```java
order.setStatus(PurchaseOrderStatus.PENDING.name());
order.setProviderCode("SIMULATED");
order.setProviderOrderNo("SIM" + order.getPurchaseOrderNo());
order.setPaymentSessionNo("PS" + IdUtil.getSnowflakeNextIdStr());
```

- [x] **Step 3: Replace immediate wallet credit block**

After inserting the order, replace direct `PAID`/wallet credit/`CREDITED` logic with:

```java
PurchaseOrder credited = purchasePaymentEventService.applyEvent(PurchasePaymentCallbackBo.simulatedSuccess(order));
return toOrderVo(credited, items, offer);
```

The try/catch that marks failed wallet credit moves into the event service.

- [x] **Step 4: Update C-side tests**

Update `ClientPurchaseServiceTest.service(...)` to accept/mock `IPurchasePaymentEventService`.

In `simulatedPayCreatesOrderSnapshotsAndCreditsWallet`, either:

- keep wallet assertions in the new event service test, and assert C-side service calls `purchasePaymentEventService.applyEvent(...)`; or
- instantiate the real `PurchasePaymentEventServiceImpl` with mocks if the test remains focused on end-to-end service behavior.

Preferred split:

- `ClientPurchaseServiceTest` verifies:
  - inserted order has provider/session fields;
  - simulated callback command is sent to event service;
  - returned status is `CREDITED`.
- `PurchasePaymentEventServiceImplTest` verifies wallet credit details.

- [x] **Step 5: Run client purchase tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -Plocal -DskipTests=false "-Dtest=ClientPurchaseServiceTest,PurchasePaymentEventServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: payment tests pass.

### Task 5: SQL Migration

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`

- [x] **Step 1: Add idempotent purchase order column migrations**

Append idempotent `ALTER TABLE gl_purchase_order` statements for:

- `provider_code`
- `provider_order_no`
- `payment_session_no`
- `callback_event_key`
- `cancel_time`
- `refund_time`
- `chargeback_time`

- [x] **Step 2: Add indexes if missing**

Append idempotent index creation for:

- `idx_gl_purchase_order_02 (tenant_id, provider_code, provider_order_no)`
- `idx_gl_purchase_order_03 (tenant_id, payment_session_no)`

- [x] **Step 3: Add payment event table**

Append:

```sql
CREATE TABLE IF NOT EXISTS gl_purchase_payment_event (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  event_key VARCHAR(128) NOT NULL COMMENT 'Payment event idempotency key',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order no',
  provider_code VARCHAR(64) NOT NULL COMMENT 'Provider code',
  provider_order_no VARCHAR(128) DEFAULT NULL COMMENT 'Provider order no',
  event_type VARCHAR(32) NOT NULL COMMENT 'PAY_SUCCESS,PAY_FAILED,CANCELLED,REFUNDED,CHARGEBACK',
  event_status VARCHAR(32) NOT NULL COMMENT 'RECEIVED,PROCESSED,IGNORED,FAILED',
  request_hash VARCHAR(128) NOT NULL COMMENT 'Normalized request hash',
  request_body TEXT DEFAULT NULL COMMENT 'Raw or normalized request body',
  process_result VARCHAR(500) DEFAULT NULL COMMENT 'Process result',
  process_time DATETIME DEFAULT NULL COMMENT 'Process time',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_payment_event_01 (tenant_id, event_key),
  KEY idx_gl_purchase_payment_event_01 (tenant_id, purchase_order_no),
  KEY idx_gl_purchase_payment_event_02 (tenant_id, provider_code, provider_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase payment event';
```

- [x] **Step 4: Import SQL and verify schema**

Run:

```powershell
cmd /c "C:\tools\mysql-8.0.46-winx64\bin\mysql.exe -uroot -proot gameluck_vue < backend\script\sql\gameluck_wallet.sql"
C:\tools\mysql-8.0.46-winx64\bin\mysql.exe -uroot -proot -N -B gameluck_vue -e "show tables like 'gl_purchase_payment_event';"
```

Expected: table exists and new purchase order columns exist.

### Task 6: Verification And Runtime Smoke

**Files:**
- Modify: `progress.md`
- Modify: `task_plan.md`

- [x] **Step 1: Run focused payment tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -Plocal -DskipTests=false "-Dtest=ClientPurchaseServiceTest,PurchasePaymentEventServiceImplTest,PurchaseOfferServiceImplTest,DepositOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: all specified payment tests pass.

- [x] **Step 2: Package backend**

If the current Java backend process locks `backend/gameluck-admin/target/gameluck-admin.jar`, verify its command line points to this local jar, stop only that process, then run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 3: Restart backend and check health**

Run backend from refreshed jar and verify:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/ -UseBasicParsing
```

Expected: HTTP `200`.

- [x] **Step 4: Runtime smoke**

Use direct HTTP + MySQL:

1. Register a new H5 user in `US/CA`.
2. List purchase offers and pick an enabled offer.
3. POST `/api/client/purchase/orders/pay` with idempotency key `phase38-{timestamp}`.
4. Expected response `code=200`, order status `CREDITED`.
5. DB checks:
   - one row in `gl_purchase_order` for the idempotency key;
   - `provider_code='SIMULATED'`;
   - `provider_order_no` and `payment_session_no` are populated;
   - exactly one row in `gl_purchase_payment_event` for the purchase order;
   - order status is `CREDITED`.
6. Repeat the same C-side request with the same idempotency key.
7. Expected: same order returned, purchase order count unchanged, payment event count unchanged.

- [x] **Step 5: Whitespace check**

Run:

```powershell
git diff --check
```

Expected: exit code `0`; CRLF warnings are acceptable.

- [x] **Step 6: Record completion**

Update `progress.md` with:

- Files created and modified.
- SQL import result.
- Focused test result.
- Backend package result.
- Runtime smoke result.
- `git diff --check` result.

Update `task_plan.md` Phase 38 from `in_progress` to `complete` only after all required verification passes.

## Self Review

- Spec coverage: tasks cover order fields, event table, idempotent event processing, C-side simulated purchase refactor, SQL, tests, package, and runtime smoke.
- Scope control: no real payment provider, webhook signature verification, refund reversal, chargeback clawback, or Admin order page is included.
- Placeholder scan: no TODO/TBD placeholders are left.
- Type consistency: enum, BO, entity, mapper, and service names are consistent across tasks.
