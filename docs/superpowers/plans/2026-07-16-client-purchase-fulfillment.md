# Client Purchase Fulfillment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first C-side purchase loop: show enabled purchase offers, simulate a successful payment, create purchase order snapshots, credit wallet balances, and show the result in H5.

**Architecture:** Keep purchase logic inside `gameluck-payment`. Add C-side purchase controller/service/VOs beside existing client modules. Use `IWalletCoreService.credit()` for all balance changes and reuse `PurchaseOfferServiceImpl.snapshotPaidOrderGrants()` for immutable wagering snapshots.

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, MySQL, Vue 3, Vite, Element Plus-free H5 UI, Vitest not required for this H5 slice, Maven, PowerShell.

---

## File Structure

### Backend Files To Create

- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/controller/ClientPurchaseController.java`
  - C-side `/api/client/purchase/**` HTTP entry.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
  - Lists enabled offers and performs simulated paid purchase fulfillment.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/bo/ClientPurchasePayBo.java`
  - Request body for simulated purchase.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/vo/ClientPurchaseOfferVo.java`
  - C-side offer card data.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/vo/ClientPurchaseGrantItemVo.java`
  - C-side grant item display data.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/vo/ClientPurchaseOrderVo.java`
  - C-side purchase result data.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderMapper.java`
  - MyBatis mapper for `gl_purchase_order`.
- `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`
  - TDD coverage for offer listing, simulated pay, idempotency, and wallet credit calls.

### Backend Files To Modify

- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseOfferService.java`
  - Add public method for snapshot/credit building if needed by interface.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseOfferServiceImpl.java`
  - Add `@Override` for interface methods if exposed.
- `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
  - Add client purchase validation and error messages.

### H5 Files To Modify

- `h5/src/types/client.ts`
  - Add purchase offer, grant item, order result interfaces.
- `h5/src/api/client.ts`
  - Add `purchaseOffers()` and `payPurchaseOffer()`.
- `h5/src/router/index.ts`
  - Add `/purchase`.
- `h5/src/App.vue`
  - Add purchase navigation entry.
- `h5/src/i18n/messages.ts`
  - Add Chinese and English copy for purchase navigation/page states.
- `h5/src/views/HomeView.vue`
  - Add purchase feature tile if payment is enabled.

### H5 Files To Create

- `h5/src/views/PurchaseView.vue`
  - C-side purchase/store page.

---

## Task 1: Backend Client Purchase Service

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/bo/ClientPurchasePayBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/vo/ClientPurchaseOfferVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/vo/ClientPurchaseGrantItemVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/domain/vo/ClientPurchaseOrderVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderMapper.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseOfferService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseOfferServiceImpl.java`

- [ ] **Step 1: Write failing service tests**

Create `ClientPurchaseServiceTest` with these tests:

```java
package com.gameluck.payment.client.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.client.domain.bo.ClientPurchasePayBo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOfferVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOrderVo;
import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.service.impl.PurchaseOfferServiceImpl;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientPurchaseServiceTest {

    @Test
    void enabledOffersExposeBusinessFieldsWithoutFundPropertyCode() {
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper itemMapper = mock(PurchaseOfferGrantItemMapper.class);
        when(offerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(offer(100L)));
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            grant(100L, "PURCHASE_GRANT", "GC", "10000", "NONE", "0"),
            grant(100L, "PURCHASE_BONUS", "SC", "1", "MULTIPLIER", "10")
        ));
        ClientPurchaseService service = service(offerMapper, itemMapper, mock(PurchaseOrderMapper.class), mock(IWalletCoreService.class));

        List<ClientPurchaseOfferVo> rows = service.offers();

        assertEquals(1, rows.size());
        assertEquals("Starter Pack", rows.get(0).getOfferName());
        assertEquals(2, rows.get(0).getGrantItems().size());
        assertEquals(new BigDecimal("10.000000"), rows.get(0).getPayAmount());
        assertEquals("SC requires 10x wagering.", rows.get(0).getWageringText());
    }

    @Test
    void simulatedPayCreatesOrderSnapshotsAndCreditsWallet() {
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper itemMapper = mock(PurchaseOfferGrantItemMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        when(offerMapper.selectById(100L)).thenReturn(offer(100L));
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            grant(100L, "PURCHASE_GRANT", "GC", "10000", "NONE", "0"),
            grant(100L, "PURCHASE_BONUS", "SC", "1", "MULTIPLIER", "10")
        ));
        when(orderMapper.selectByIdempotencyKey("000000", "idem-1")).thenReturn(null);
        when(orderMapper.insert(any(PurchaseOrder.class))).thenReturn(1);
        when(orderMapper.updateById(any(PurchaseOrder.class))).thenReturn(1);
        when(walletCoreService.credit(any())).thenReturn(successTx("WT1"), successTx("WT2"));
        ClientPurchaseService service = service(offerMapper, itemMapper, orderMapper, walletCoreService);

        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");
        ClientPurchaseOrderVo result = service.pay("Bearer " + new ClientTokenService().issue(1001L), bo);

        assertEquals("CREDITED", result.getStatus());
        assertEquals(2, result.getGrantItems().size());
        ArgumentCaptor<WalletCreditBo> captor = ArgumentCaptor.forClass(WalletCreditBo.class);
        verify(walletCoreService, times(2)).credit(captor.capture());
        assertEquals("PURCHASE", captor.getAllValues().get(0).getSourceType());
        assertEquals(new BigDecimal("0.000000"), captor.getAllValues().get(0).getTurnoverRequiredAmount());
        assertEquals(new BigDecimal("10.000000"), captor.getAllValues().get(1).getTurnoverRequiredAmount());
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingOrderWithoutCreditingAgain() {
        PurchaseOrder existing = new PurchaseOrder();
        existing.setId(1L);
        existing.setPurchaseOrderNo("PO202607160001");
        existing.setOfferId(100L);
        existing.setOfferNo("PO-STARTER");
        existing.setMemberId(1001L);
        existing.setPayCurrencyCode("USD");
        existing.setPayAmount(new BigDecimal("10.000000"));
        existing.setStatus("CREDITED");
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        when(orderMapper.selectByIdempotencyKey("000000", "idem-1")).thenReturn(existing);
        ClientPurchaseService service = service(mock(PurchaseOfferMapper.class), mock(PurchaseOfferGrantItemMapper.class), orderMapper, mock(IWalletCoreService.class));

        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");
        ClientPurchaseOrderVo result = service.pay("Bearer " + new ClientTokenService().issue(1001L), bo);

        assertEquals("CREDITED", result.getStatus());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void disabledOfferCannotBePurchased() {
        PurchaseOffer disabled = offer(100L);
        disabled.setStatus("1");
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        when(offerMapper.selectById(100L)).thenReturn(disabled);
        ClientPurchaseService service = service(offerMapper, mock(PurchaseOfferGrantItemMapper.class), mock(PurchaseOrderMapper.class), mock(IWalletCoreService.class));
        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), bo));

        assertEquals("client.purchase.offer.not.available", ex.getMessage());
    }

    private ClientPurchaseService service(PurchaseOfferMapper offerMapper, PurchaseOfferGrantItemMapper itemMapper,
                                          PurchaseOrderMapper orderMapper, IWalletCoreService walletCoreService) {
        PurchaseOfferServiceImpl purchaseOfferService = new PurchaseOfferServiceImpl(offerMapper, itemMapper, mock(com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper.class));
        return new ClientPurchaseService(new ClientTokenService(), offerMapper, itemMapper, orderMapper, purchaseOfferService, walletCoreService);
    }

    private PurchaseOffer offer(Long id) {
        PurchaseOffer offer = new PurchaseOffer();
        offer.setId(id);
        offer.setTenantId("000000");
        offer.setOfferNo("PO-STARTER");
        offer.setOfferName("Starter Pack");
        offer.setOfferType("STANDARD");
        offer.setPayCurrencyCode("USD");
        offer.setPayAmount(new BigDecimal("10.000000"));
        offer.setPurchaseLimitType("NONE");
        offer.setStatus("0");
        return offer;
    }

    private PurchaseOfferGrantItem grant(Long offerId, String grantType, String currencyCode, String amount, String wageringMode, String multiplier) {
        PurchaseOfferGrantItem item = new PurchaseOfferGrantItem();
        item.setOfferId(offerId);
        item.setGrantType(grantType);
        item.setCurrencyCode(currencyCode);
        item.setGrantAmount(new BigDecimal(amount + ".000000"));
        item.setFundPropertyCode(grantType + "_" + currencyCode);
        item.setWageringMode(wageringMode);
        item.setWageringRequiredAmount(BigDecimal.ZERO);
        item.setWageringMultiplier(new BigDecimal(multiplier + ".0000"));
        item.setGameScopeType("ALL");
        return item;
    }

    private WalletTransaction successTx(String transactionNo) {
        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionNo(transactionNo);
        tx.setStatus(WalletTransactionStatus.SUCCESS.name());
        return tx;
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=ClientPurchaseServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation failure because `ClientPurchaseService` and client purchase VO/BO classes do not exist.

- [ ] **Step 3: Create purchase order mapper**

Create `PurchaseOrderMapper.java`:

```java
package com.gameluck.payment.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.payment.domain.PurchaseOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PurchaseOrderMapper extends BaseMapperPlus<PurchaseOrder, PurchaseOrder> {

    @Select("select * from gl_purchase_order where tenant_id = #{tenantId} and idempotency_key = #{idempotencyKey} limit 1")
    PurchaseOrder selectByIdempotencyKey(@Param("tenantId") String tenantId, @Param("idempotencyKey") String idempotencyKey);
}
```

- [ ] **Step 4: Create client purchase BO and VOs**

Create `ClientPurchasePayBo.java`:

```java
package com.gameluck.payment.client.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientPurchasePayBo {
    @NotNull(message = "{client.purchase.offer.id.required}")
    private Long offerId;

    @NotBlank(message = "{client.purchase.idempotency.key.required}")
    private String idempotencyKey;
}
```

Create `ClientPurchaseGrantItemVo.java`:

```java
package com.gameluck.payment.client.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ClientPurchaseGrantItemVo {
    private String grantType;
    private String currencyCode;
    private BigDecimal grantAmount;
    private String wageringMode;
    private BigDecimal requiredTurnover;
    private BigDecimal wageringMultiplier;
    private String gameScopeType;
    private String gameScopeValue;
}
```

Create `ClientPurchaseOfferVo.java`:

```java
package com.gameluck.payment.client.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ClientPurchaseOfferVo {
    private Long offerId;
    private String offerNo;
    private String offerName;
    private String offerType;
    private String payCurrencyCode;
    private BigDecimal payAmount;
    private List<ClientPurchaseGrantItemVo> grantItems;
    private String limitText;
    private String wageringText;
}
```

Create `ClientPurchaseOrderVo.java`:

```java
package com.gameluck.payment.client.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ClientPurchaseOrderVo {
    private Long orderId;
    private String orderNo;
    private Long offerId;
    private String offerNo;
    private String offerName;
    private String payCurrencyCode;
    private BigDecimal payAmount;
    private String status;
    private List<ClientPurchaseGrantItemVo> grantItems;
    private Date createdAt;
    private Date creditedAt;
}
```

- [ ] **Step 5: Expose snapshot method on purchase offer interface**

Modify `IPurchaseOfferService.java`:

```java
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import java.util.List;

List<WalletCreditBo> snapshotPaidOrderGrants(PurchaseOrder order, List<PurchaseOfferGrantItem> items);
```

Add `@Override` to `PurchaseOfferServiceImpl.snapshotPaidOrderGrants`.

- [ ] **Step 6: Implement client purchase service**

Create `ClientPurchaseService.java` with:

```java
package com.gameluck.payment.client.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.client.domain.bo.ClientPurchasePayBo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseGrantItemVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOfferVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOrderVo;
import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.service.IPurchaseOfferService;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ClientPurchaseService {
    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String ENABLED = "0";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CREDITED = "CREDITED";
    private static final String STATUS_FAILED = "FAILED";
    private static final int MONEY_SCALE = 6;

    private final ClientTokenService clientTokenService;
    private final PurchaseOfferMapper offerMapper;
    private final PurchaseOfferGrantItemMapper grantItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final IPurchaseOfferService purchaseOfferService;
    private final IWalletCoreService walletCoreService;

    public List<ClientPurchaseOfferVo> offers() {
        String tenantId = currentTenantId();
        Date now = new Date();
        List<PurchaseOffer> offers = offerMapper.selectList(Wrappers.<PurchaseOffer>lambdaQuery()
            .eq(PurchaseOffer::getTenantId, tenantId)
            .eq(PurchaseOffer::getStatus, ENABLED)
            .and(q -> q.isNull(PurchaseOffer::getStartTime).or().le(PurchaseOffer::getStartTime, now))
            .and(q -> q.isNull(PurchaseOffer::getEndTime).or().ge(PurchaseOffer::getEndTime, now))
            .orderByAsc(PurchaseOffer::getSortOrder)
            .orderByDesc(PurchaseOffer::getCreateTime));
        if (offers.isEmpty()) {
            return List.of();
        }
        List<Long> offerIds = offers.stream().map(PurchaseOffer::getId).toList();
        Map<Long, List<PurchaseOfferGrantItem>> grouped = grantItemMapper.selectList(Wrappers.<PurchaseOfferGrantItem>lambdaQuery()
                .in(PurchaseOfferGrantItem::getOfferId, offerIds)
                .orderByAsc(PurchaseOfferGrantItem::getSortOrder))
            .stream().collect(Collectors.groupingBy(PurchaseOfferGrantItem::getOfferId));
        return offers.stream().map(offer -> toOfferVo(offer, grouped.getOrDefault(offer.getId(), List.of()))).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ClientPurchaseOrderVo pay(String authorization, ClientPurchasePayBo bo) {
        Long memberId = clientTokenService.resolveMemberId(authorization);
        String tenantId = currentTenantId();
        PurchaseOrder exists = orderMapper.selectByIdempotencyKey(tenantId, bo.getIdempotencyKey());
        if (exists != null) {
            if (!memberId.equals(exists.getMemberId()) || !bo.getOfferId().equals(exists.getOfferId())) {
                throw new ServiceException(MessageUtils.message("wallet.idempotency.conflict"));
            }
            return toOrderVo(exists, List.of(), null);
        }
        PurchaseOffer offer = requireAvailableOffer(tenantId, bo.getOfferId());
        List<PurchaseOfferGrantItem> items = grantItemMapper.selectList(Wrappers.<PurchaseOfferGrantItem>lambdaQuery()
            .eq(PurchaseOfferGrantItem::getTenantId, tenantId)
            .eq(PurchaseOfferGrantItem::getOfferId, offer.getId())
            .orderByAsc(PurchaseOfferGrantItem::getSortOrder));
        if (items.isEmpty()) {
            throw new ServiceException(MessageUtils.message("payment.purchase.order.grant.required"));
        }
        Date now = new Date();
        PurchaseOrder order = buildOrder(tenantId, memberId, offer, bo.getIdempotencyKey(), now);
        orderMapper.insert(order);
        order.setStatus(STATUS_PAID);
        order.setPaidTime(now);
        orderMapper.updateById(order);
        try {
            List<WalletCreditBo> credits = purchaseOfferService.snapshotPaidOrderGrants(order, items);
            for (WalletCreditBo credit : credits) {
                WalletTransaction tx = walletCoreService.credit(credit);
                if (!WalletTransactionStatus.SUCCESS.name().equals(tx.getStatus())) {
                    throw new ServiceException(StringUtils.blankToDefault(tx.getFailReason(), MessageUtils.message("client.purchase.credit.failed")));
                }
            }
            order.setStatus(STATUS_CREDITED);
            order.setCreditedTime(new Date());
            orderMapper.updateById(order);
            return toOrderVo(order, items, offer);
        } catch (RuntimeException ex) {
            order.setStatus(STATUS_FAILED);
            order.setFailReason(ex.getMessage());
            orderMapper.updateById(order);
            throw ex;
        }
    }

    private PurchaseOffer requireAvailableOffer(String tenantId, Long offerId) {
        PurchaseOffer offer = offerMapper.selectById(offerId);
        Date now = new Date();
        if (offer == null || !tenantId.equals(offer.getTenantId()) || !ENABLED.equals(offer.getStatus())
            || (offer.getStartTime() != null && offer.getStartTime().after(now))
            || (offer.getEndTime() != null && offer.getEndTime().before(now))) {
            throw new ServiceException(MessageUtils.message("client.purchase.offer.not.available"));
        }
        return offer;
    }

    private PurchaseOrder buildOrder(String tenantId, Long memberId, PurchaseOffer offer, String idempotencyKey, Date now) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(IdUtil.getSnowflakeNextId());
        order.setTenantId(tenantId);
        order.setPurchaseOrderNo("PO" + IdUtil.getSnowflakeNextIdStr());
        order.setOfferId(offer.getId());
        order.setOfferNo(offer.getOfferNo());
        order.setMemberId(memberId);
        order.setPayCurrencyCode(offer.getPayCurrencyCode());
        order.setPayAmount(offer.getPayAmount());
        order.setStatus(STATUS_PENDING);
        order.setIdempotencyKey(idempotencyKey);
        return order;
    }

    private ClientPurchaseOfferVo toOfferVo(PurchaseOffer offer, List<PurchaseOfferGrantItem> items) {
        ClientPurchaseOfferVo vo = new ClientPurchaseOfferVo();
        vo.setOfferId(offer.getId());
        vo.setOfferNo(offer.getOfferNo());
        vo.setOfferName(offer.getOfferName());
        vo.setOfferType(offer.getOfferType());
        vo.setPayCurrencyCode(offer.getPayCurrencyCode());
        vo.setPayAmount(offer.getPayAmount());
        vo.setGrantItems(items.stream().map(this::toGrantVo).toList());
        vo.setLimitText("NONE".equals(offer.getPurchaseLimitType()) ? "No purchase limit." : offer.getPurchaseLimitType());
        vo.setWageringText(wageringText(items));
        return vo;
    }

    private ClientPurchaseOrderVo toOrderVo(PurchaseOrder order, List<PurchaseOfferGrantItem> items, PurchaseOffer offer) {
        ClientPurchaseOrderVo vo = new ClientPurchaseOrderVo();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getPurchaseOrderNo());
        vo.setOfferId(order.getOfferId());
        vo.setOfferNo(order.getOfferNo());
        vo.setOfferName(offer == null ? null : offer.getOfferName());
        vo.setPayCurrencyCode(order.getPayCurrencyCode());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setGrantItems(items.stream().map(this::toGrantVo).toList());
        vo.setCreditedAt(order.getCreditedTime());
        return vo;
    }

    private ClientPurchaseGrantItemVo toGrantVo(PurchaseOfferGrantItem item) {
        BigDecimal required = requiredTurnover(item);
        ClientPurchaseGrantItemVo vo = new ClientPurchaseGrantItemVo();
        vo.setGrantType(item.getGrantType());
        vo.setCurrencyCode(item.getCurrencyCode());
        vo.setGrantAmount(item.getGrantAmount());
        vo.setWageringMode(item.getWageringMode());
        vo.setRequiredTurnover(required);
        vo.setWageringMultiplier(item.getWageringMultiplier());
        vo.setGameScopeType(item.getGameScopeType());
        vo.setGameScopeValue(item.getGameScopeValue());
        return vo;
    }

    private BigDecimal requiredTurnover(PurchaseOfferGrantItem item) {
        String mode = StringUtils.blankToDefault(item.getWageringMode(), "NONE");
        if ("FIXED".equals(mode)) {
            return normalize(item.getWageringRequiredAmount());
        }
        if ("MULTIPLIER".equals(mode)) {
            return normalize(item.getGrantAmount().multiply(item.getWageringMultiplier() == null ? BigDecimal.ZERO : item.getWageringMultiplier()));
        }
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String wageringText(List<PurchaseOfferGrantItem> items) {
        return items.stream()
            .filter(item -> requiredTurnover(item).compareTo(BigDecimal.ZERO) > 0)
            .findFirst()
            .map(item -> item.getCurrencyCode() + " requires " + normalize(item.getWageringMultiplier()).stripTrailingZeros().toPlainString() + "x wagering.")
            .orElse("No wagering required.");
    }

    private BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }
}
```

- [ ] **Step 7: Run GREEN test**

Run the same Maven command.

Expected: `ClientPurchaseServiceTest` compiles and passes.

- [ ] **Step 8: Commit backend service slice**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderMapper.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseOfferService.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseOfferServiceImpl.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java
git commit -m "feat: add client purchase fulfillment service"
```

---

## Task 2: Backend Client Purchase Controller And Messages

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/controller/ClientPurchaseController.java`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`

- [ ] **Step 1: Create controller**

```java
package com.gameluck.payment.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.payment.client.domain.bo.ClientPurchasePayBo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOfferVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOrderVo;
import com.gameluck.payment.client.service.ClientPurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/api/client/purchase")
public class ClientPurchaseController {

    private final ClientPurchaseService clientPurchaseService;

    @GetMapping("/offers")
    public R<List<ClientPurchaseOfferVo>> offers() {
        return R.ok(clientPurchaseService.offers());
    }

    @PostMapping("/orders/pay")
    public R<ClientPurchaseOrderVo> pay(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @Valid @RequestBody ClientPurchasePayBo bo) {
        return R.ok(clientPurchaseService.pay(authorization, bo));
    }
}
```

- [ ] **Step 2: Add i18n keys**

Append to all three backend message files.

Chinese/default:

```properties
client.purchase.offer.id.required=购买产品ID不能为空
client.purchase.idempotency.key.required=购买幂等键不能为空
client.purchase.offer.not.available=购买产品不可用
client.purchase.credit.failed=购买奖励入账失败
```

English:

```properties
client.purchase.offer.id.required=Purchase offer id is required.
client.purchase.idempotency.key.required=Purchase idempotency key is required.
client.purchase.offer.not.available=Purchase offer is not available.
client.purchase.credit.failed=Purchase grant credit failed.
```

- [ ] **Step 3: Run backend targeted tests**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment,gameluck-modules/gameluck-wallet -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=ClientPurchaseServiceTest,PurchaseOfferServiceImplTest,WalletCoreServiceImplTest,WalletTurnoverTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: build success.

- [ ] **Step 4: Compile admin module**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -DskipTests "-Dprofiles.active=local"
```

Expected: build success.

- [ ] **Step 5: Commit controller slice**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/controller/ClientPurchaseController.java backend/gameluck-admin/src/main/resources/i18n/messages.properties backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties
git commit -m "feat: expose client purchase endpoints"
```

---

## Task 3: H5 Purchase API And Page

**Files:**
- Modify: `h5/src/types/client.ts`
- Modify: `h5/src/api/client.ts`
- Modify: `h5/src/router/index.ts`
- Modify: `h5/src/App.vue`
- Modify: `h5/src/i18n/messages.ts`
- Modify: `h5/src/views/HomeView.vue`
- Create: `h5/src/views/PurchaseView.vue`

- [ ] **Step 1: Add H5 types**

Add to `h5/src/types/client.ts`:

```ts
export interface ClientPurchaseGrantItem {
  grantType: string
  currencyCode: string
  grantAmount: string
  wageringMode: string
  requiredTurnover: string
  wageringMultiplier: string
  gameScopeType: string
  gameScopeValue?: string
}

export interface ClientPurchaseOffer {
  offerId: number
  offerNo: string
  offerName: string
  offerType: string
  payCurrencyCode: string
  payAmount: string
  grantItems: ClientPurchaseGrantItem[]
  limitText: string
  wageringText: string
}

export interface ClientPurchaseOrder {
  orderId: number
  orderNo: string
  offerId: number
  offerNo: string
  offerName: string
  payCurrencyCode: string
  payAmount: string
  status: string
  grantItems: ClientPurchaseGrantItem[]
  creditedAt?: string
}
```

- [ ] **Step 2: Add H5 API methods**

Modify imports and `clientApi` in `h5/src/api/client.ts`:

```ts
ClientPurchaseOffer,
ClientPurchaseOrder,
```

```ts
purchaseOffers: () => request<ClientPurchaseOffer[]>('/api/client/purchase/offers'),
payPurchaseOffer: (offerId: number, idempotencyKey: string) =>
  request<ClientPurchaseOrder>('/api/client/purchase/orders/pay', {
    method: 'POST',
    body: JSON.stringify({ offerId, idempotencyKey }),
  }),
```

- [ ] **Step 3: Add route**

Modify `h5/src/router/index.ts`:

```ts
import PurchaseView from '../views/PurchaseView.vue'
```

```ts
{ path: '/purchase', name: 'purchase', component: PurchaseView },
```

- [ ] **Step 4: Add H5 purchase page**

Create `h5/src/views/PurchaseView.vue` with a C-side operational purchase layout:

```vue
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { clientApi, getClientToken } from '../api/client'
import type { ClientPurchaseOffer, ClientPurchaseOrder, WalletAccount } from '../types/client'

const loading = ref(true)
const walletLoading = ref(true)
const error = ref('')
const success = ref('')
const payingId = ref<number | null>(null)
const offers = ref<ClientPurchaseOffer[]>([])
const accounts = ref<WalletAccount[]>([])
const lastOrder = ref<ClientPurchaseOrder | null>(null)

const loggedIn = computed(() => Boolean(getClientToken()))
const gcBalance = computed(() => accounts.value.find((item) => item.currencyCode === 'GC')?.availableBalance || '0')
const scBalance = computed(() => accounts.value.find((item) => item.currencyCode === 'SC')?.availableBalance || '0')

async function load() {
  loading.value = true
  error.value = ''
  try {
    offers.value = await clientApi.purchaseOffers()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '购买产品加载失败'
  } finally {
    loading.value = false
  }
  await loadWallet()
}

async function loadWallet() {
  if (!loggedIn.value) {
    walletLoading.value = false
    return
  }
  walletLoading.value = true
  try {
    accounts.value = await clientApi.walletAccounts()
  } finally {
    walletLoading.value = false
  }
}

function grantText(offer: ClientPurchaseOffer) {
  return offer.grantItems.map((item) => `${item.grantAmount} ${item.currencyCode}`).join(' + ')
}

function idempotencyKey(offerId: number) {
  return `h5-${offerId}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function pay(offer: ClientPurchaseOffer) {
  if (!loggedIn.value) return
  payingId.value = offer.offerId
  error.value = ''
  success.value = ''
  try {
    const result = await clientApi.payPurchaseOffer(offer.offerId, idempotencyKey(offer.offerId))
    lastOrder.value = result
    success.value = `购买成功，订单 ${result.orderNo} 已入账`
    await loadWallet()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '购买失败'
  } finally {
    payingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <main class="page purchase-page">
    <section class="balance-strip">
      <div>
        <small>GC</small>
        <strong>{{ walletLoading ? '...' : gcBalance }}</strong>
      </div>
      <div>
        <small>SC</small>
        <strong>{{ walletLoading ? '...' : scBalance }}</strong>
      </div>
      <RouterLink class="text-link" to="/wallet">查看钱包</RouterLink>
    </section>

    <section v-if="!loggedIn" class="notice-panel">
      <h1>登录后购买</h1>
      <p>登录后可以购买 GC 套餐并领取 SC 赠送奖励。</p>
      <RouterLink class="btn primary" to="/login">去登录</RouterLink>
    </section>

    <section v-if="error" class="state error">{{ error }}</section>
    <section v-if="success" class="state success">{{ success }}</section>

    <section class="section-head">
      <div>
        <small>Purchase</small>
        <h1>购买</h1>
      </div>
    </section>

    <section v-if="loading" class="state">购买产品加载中...</section>
    <section v-else-if="!offers.length" class="state">暂无可购买产品</section>

    <section v-else class="offer-grid">
      <article v-for="offer in offers" :key="offer.offerId" class="offer-card">
        <small>{{ offer.offerType }}</small>
        <h2>{{ offer.offerName }}</h2>
        <p class="pay-line">支付 {{ offer.payAmount }} {{ offer.payCurrencyCode }}</p>
        <p class="grant-line">获得 {{ grantText(offer) }}</p>
        <p class="rule-line">{{ offer.wageringText }}</p>
        <button class="btn primary" :disabled="!loggedIn || payingId === offer.offerId" @click="pay(offer)">
          {{ payingId === offer.offerId ? '购买中...' : '立即购买' }}
        </button>
      </article>
    </section>

    <section v-if="lastOrder" class="order-result">
      <small>最近订单</small>
      <h2>{{ lastOrder.orderNo }}</h2>
      <p>{{ lastOrder.status }}</p>
      <p>{{ lastOrder.grantItems.map((item) => `${item.grantAmount} ${item.currencyCode}`).join(' + ') }}</p>
    </section>
  </main>
</template>
```

- [ ] **Step 5: Add H5 styles**

In the same file, add scoped CSS that matches existing H5 page density:

```vue
<style scoped>
.purchase-page {
  display: grid;
  gap: 18px;
}
.balance-strip {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
}
.balance-strip div,
.offer-card,
.order-result,
.notice-panel,
.state {
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
}
.balance-strip small,
.section-head small,
.offer-card small,
.order-result small {
  color: rgba(255, 255, 255, 0.62);
}
.balance-strip strong {
  display: block;
  margin-top: 4px;
  font-size: 22px;
}
.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
}
.section-head h1,
.offer-card h2,
.order-result h2 {
  margin: 4px 0 0;
}
.offer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}
.offer-card {
  display: grid;
  gap: 10px;
}
.pay-line {
  font-size: 18px;
  font-weight: 700;
}
.grant-line,
.rule-line,
.notice-panel p,
.order-result p {
  margin: 0;
  color: rgba(255, 255, 255, 0.76);
}
.state.error {
  border-color: rgba(255, 99, 99, 0.55);
}
.state.success {
  border-color: rgba(65, 211, 137, 0.55);
}
.text-link {
  color: #8bd3ff;
  text-decoration: none;
}
@media (max-width: 640px) {
  .balance-strip {
    grid-template-columns: 1fr 1fr;
  }
  .balance-strip .text-link {
    grid-column: 1 / -1;
  }
}
</style>
```

- [ ] **Step 6: Add navigation and home tile**

In `h5/src/App.vue`, add purchase route links near wallet:

```vue
<RouterLink to="/purchase">{{ t('navPurchase') }}</RouterLink>
```

In `h5/src/views/HomeView.vue`, add a feature tile:

```vue
<RouterLink class="feature-tile" to="/purchase">
  <strong>{{ t('navPurchase') }}</strong>
  <small>{{ sessionState.bootstrap?.features.paymentEnabled ? '购买入口可见' : '购买暂未开放' }}</small>
</RouterLink>
```

- [ ] **Step 7: Add i18n keys**

Add to both locales in `h5/src/i18n/messages.ts`:

```ts
navPurchase: '购买',
```

```ts
navPurchase: 'Purchase',
```

- [ ] **Step 8: Run H5 build**

```powershell
npm --prefix h5 run build
```

Expected: build success.

- [ ] **Step 9: Commit H5 slice**

```powershell
git add h5/src/types/client.ts h5/src/api/client.ts h5/src/router/index.ts h5/src/App.vue h5/src/i18n/messages.ts h5/src/views/HomeView.vue h5/src/views/PurchaseView.vue
git commit -m "feat: add h5 purchase page"
```

---

## Task 4: Local Runtime Smoke

**Files:**
- No planned file changes unless defects are found.

- [ ] **Step 1: Import seed SQL if needed**

If local DB does not have enabled purchase offers, insert one via B-side or SQL. Expected existing B-side can create:

```text
Starter Pack
10 USD
10000 GC no wagering
1 SC 10x wagering
status = enabled
```

- [ ] **Step 2: Run full targeted verification**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment,gameluck-modules/gameluck-wallet,gameluck-modules/gameluck-promotion -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=ClientPurchaseServiceTest,PurchaseOfferServiceImplTest,DepositOrderServiceImplTest,WalletCoreServiceImplTest,WalletTurnoverTaskServiceImplTest,PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir admin-ui build:dev
npm --prefix h5 run build
```

Expected: all pass. Existing large Vite chunk warning is acceptable.

- [ ] **Step 3: API smoke**

With backend running and a client token:

```powershell
$offers = Invoke-RestMethod -Uri "http://localhost:8080/api/client/purchase/offers" -Headers @{"X-Channel-Code"="h5";"X-Brand-Code"="demo"}
$offers.data | ConvertTo-Json -Depth 6
```

Then login/register demo user and call:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/client/purchase/orders/pay" `
  -Method Post `
  -Headers @{"Content-Type"="application/json";"Authorization"="Bearer <token>";"X-Channel-Code"="h5";"X-Brand-Code"="demo"} `
  -Body '{"offerId":<offerId>,"idempotencyKey":"smoke-purchase-001"}'
```

Expected:

```text
code = 200
data.status = CREDITED
data.grantItems contains GC and SC
```

- [ ] **Step 4: DB verification**

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "SELECT purchase_order_no,status,pay_currency_code,pay_amount FROM gl_purchase_order ORDER BY create_time DESC LIMIT 5; SELECT currency_code,amount,required_turnover FROM gl_wallet_transaction WHERE source_type='PURCHASE' ORDER BY create_time DESC LIMIT 5; SELECT currency_code,required_turnover,release_status FROM gl_wallet_release WHERE source_type='PURCHASE' ORDER BY create_time DESC LIMIT 5; SELECT currency_code,required_turnover,completed_turnover,status FROM gl_wallet_turnover_task WHERE source_type='PURCHASE' ORDER BY create_time DESC LIMIT 5;"
```

Expected:

```text
purchase order status CREDITED
GC and SC wallet CREDIT rows
SC release locked if required_turnover > 0
SC turnover task exists if required_turnover > 0
```

- [ ] **Step 5: Commit fixes if needed**

If runtime smoke exposes defects, stage only the files changed to fix those defects. Do not stage unrelated dirty worktree files.

```powershell
git status --short
git commit -m "fix: stabilize client purchase fulfillment"
```

---

## Self-Review

Spec coverage:

- C-side offer list covered by Task 1 and Task 3.
- Simulated paid purchase covered by Task 1 and Task 2.
- Wallet credit and wagering snapshot covered by Task 1.
- H5 page and states covered by Task 3.
- Runtime/API/DB verification covered by Task 4.

Placeholder scan:

- No unfinished marker text or open placeholder steps.

Type consistency:

- `ClientPurchasePayBo.offerId/idempotencyKey` matches H5 request body.
- `ClientPurchaseOfferVo` and H5 `ClientPurchaseOffer` use matching names.
- `ClientPurchaseOrderVo` and H5 `ClientPurchaseOrder` use matching names.
- Backend status uses existing `PENDING/PAID/CREDITED/FAILED`.
