# Purchase Limit Enforcement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce the first usable C-side purchase limits for enabled purchase offers before wallet crediting happens.

**Architecture:** Keep enforcement inside `ClientPurchaseService` because this phase only guards the C-side purchase entry point. Use `gl_purchase_order` history as the source of truth, counting only successful `CREDITED` orders. Add mapper count queries for total member purchase, per-offer purchase, and per-offer current-day purchase.

**Tech Stack:** Java 17, Spring Boot, MyBatis annotation mapper, Maven, JUnit 5, Mockito, MySQL.

---

## File Structure

- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderMapper.java`
  - Add count queries used by purchase limit enforcement.
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
  - Enforce `FIRST_ONLY`, `TOTAL_ONCE`, and `DAILY_ONCE` before creating an order.
  - Reject unsupported `PERIOD_LIMIT` instead of silently bypassing it.
- Modify: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`
  - Add RED tests for the new limit behavior.
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
  - Add user-facing purchase limit messages.

## Task 1: Count Queries

- [ ] **Step 1: Add RED service tests first**

Add focused tests to `ClientPurchaseServiceTest`:

```java
@Test
void totalOnceOfferRejectsSecondCreditedPurchaseForSameMemberAndOffer() {
    PurchaseOffer offer = offer(100L);
    offer.setPurchaseLimitType("TOTAL_ONCE");
    PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
    PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
    when(offerMapper.selectById(100L)).thenReturn(offer);
    when(orderMapper.selectByIdempotencyKey("000000", "idem-total")).thenReturn(null);
    when(orderMapper.countCreditedByMemberAndOffer("000000", 1001L, 100L)).thenReturn(1L);
    ClientPurchaseService service = service(offerMapper, mock(PurchaseOfferGrantItemMapper.class), orderMapper, mock(IWalletCoreService.class));

    ClientPurchasePayBo bo = payBo(100L, "idem-total");
    ServiceException ex = assertThrows(ServiceException.class, () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), bo));

    assertEquals("client.purchase.limit.total.once", ex.getMessage());
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=ClientPurchaseServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because mapper methods and helpers do not exist yet.

- [ ] **Step 3: Add mapper methods**

Add to `PurchaseOrderMapper`:

```java
@Select("select count(1) from gl_purchase_order where tenant_id = #{tenantId} and member_id = #{memberId} and status = 'CREDITED'")
long countCreditedByMember(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);

@Select("select count(1) from gl_purchase_order where tenant_id = #{tenantId} and member_id = #{memberId} and offer_id = #{offerId} and status = 'CREDITED'")
long countCreditedByMemberAndOffer(@Param("tenantId") String tenantId, @Param("memberId") Long memberId, @Param("offerId") Long offerId);

@Select("select count(1) from gl_purchase_order where tenant_id = #{tenantId} and member_id = #{memberId} and offer_id = #{offerId} and status = 'CREDITED' and credited_time >= #{dayStart} and credited_time < #{nextDayStart}")
long countCreditedByMemberOfferAndCreditedTimeRange(@Param("tenantId") String tenantId, @Param("memberId") Long memberId, @Param("offerId") Long offerId, @Param("dayStart") java.util.Date dayStart, @Param("nextDayStart") java.util.Date nextDayStart);
```

## Task 2: Service Enforcement

- [ ] **Step 1: Add remaining RED tests**

Cover:

- `FIRST_ONLY`: any existing credited purchase by member blocks the offer.
- `DAILY_ONCE`: credited purchase for same offer today blocks the offer.
- `PERIOD_LIMIT`: unsupported configuration blocks purchase.
- `PENDING`, `PAID`, and `FAILED` are ignored because mapper queries count only `CREDITED`.

- [ ] **Step 2: Implement minimal enforcement**

In `ClientPurchaseService.pay`, after `requireAvailableOffer(...)` and before grant item loading, call:

```java
enforcePurchaseLimit(tenantId, memberId, offer);
```

Implement:

```java
private void enforcePurchaseLimit(String tenantId, Long memberId, PurchaseOffer offer) {
    String limitType = StringUtils.blankToDefault(offer.getPurchaseLimitType(), "NONE");
    if ("NONE".equals(limitType)) {
        return;
    }
    if ("FIRST_ONLY".equals(limitType)) {
        if (orderMapper.countCreditedByMember(tenantId, memberId) > 0) {
            throw new ServiceException(MessageUtils.message("client.purchase.limit.first.only"));
        }
        return;
    }
    if ("TOTAL_ONCE".equals(limitType)) {
        if (orderMapper.countCreditedByMemberAndOffer(tenantId, memberId, offer.getId()) > 0) {
            throw new ServiceException(MessageUtils.message("client.purchase.limit.total.once"));
        }
        return;
    }
    if ("DAILY_ONCE".equals(limitType)) {
        Date dayStart = cn.hutool.core.date.DateUtil.beginOfDay(new Date());
        Date nextDayStart = cn.hutool.core.date.DateUtil.offsetDay(dayStart, 1);
        if (orderMapper.countCreditedByMemberOfferAndCreditedTimeRange(tenantId, memberId, offer.getId(), dayStart, nextDayStart) > 0) {
            throw new ServiceException(MessageUtils.message("client.purchase.limit.daily.once"));
        }
        return;
    }
    throw new ServiceException(MessageUtils.message("client.purchase.limit.unsupported"));
}
```

- [ ] **Step 3: Add i18n keys**

Default / Chinese:

```properties
client.purchase.limit.first.only=该商品仅限首次购买用户购买
client.purchase.limit.total.once=该商品每个用户仅限购买一次
client.purchase.limit.daily.once=该商品每个用户每日仅限购买一次
client.purchase.limit.unsupported=该商品限购规则暂不支持
```

English:

```properties
client.purchase.limit.first.only=This offer is only available before the member's first purchase.
client.purchase.limit.total.once=This offer can only be purchased once per member.
client.purchase.limit.daily.once=This offer can only be purchased once per member per day.
client.purchase.limit.unsupported=This purchase limit rule is not supported yet.
```

## Task 3: Verification

- [ ] **Step 1: Run targeted payment tests**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=ClientPurchaseServiceTest,PurchaseOfferServiceImplTest,DepositOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- [ ] **Step 2: Run cross-module purchase regression**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment,gameluck-modules/gameluck-wallet,gameluck-modules/gameluck-promotion,gameluck-modules/gameluck-game -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=ClientPurchaseServiceTest,PurchaseOfferServiceImplTest,DepositOrderServiceImplTest,WalletCoreServiceImplTest,WalletTurnoverTaskServiceImplTest,PromotionRewardServiceImplTest,GameBetOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- [ ] **Step 3: Package backend**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: all commands pass. Existing CRLF warnings are acceptable.

## Self-Review

- `FIRST_ONLY`, `TOTAL_ONCE`, and `DAILY_ONCE` are enforced before order insertion and wallet credit.
- Existing idempotent repeat behavior remains first, so retrying the same completed request still returns the existing order.
- Only `CREDITED` orders count toward limits.
- `PERIOD_LIMIT` is not silently ignored.
