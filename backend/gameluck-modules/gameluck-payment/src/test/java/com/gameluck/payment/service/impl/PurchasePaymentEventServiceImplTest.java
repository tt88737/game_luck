package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchasePaymentEvent;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.domain.vo.PurchaseReversalResult;
import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.enums.PurchasePaymentEventStatus;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchasePaymentEventMapper;
import com.gameluck.payment.service.IPurchaseOfferService;
import com.gameluck.payment.service.IPurchaseReversalService;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchasePaymentEventServiceImplTest {

    @Test
    @Tag("local")
    void paySuccessCreditsWalletOnceWhenEventRepeated() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchasePaymentEventMapper eventMapper = mock(PurchasePaymentEventMapper.class);
        PurchaseOfferGrantItemMapper grantItemMapper = mock(PurchaseOfferGrantItemMapper.class);
        IPurchaseOfferService purchaseOfferService = mock(IPurchaseOfferService.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PurchasePaymentEventServiceImpl service = new PurchasePaymentEventServiceImpl(
            orderMapper, eventMapper, grantItemMapper, purchaseOfferService, walletCoreService,
            mock(IPurchaseReversalService.class));
        PurchaseOrder order = pendingOrder();
        when(eventMapper.selectByEventKey("000000", "evt-1")).thenReturn(null);
        when(orderMapper.selectByOrderNoForUpdate("000000", "PO1001")).thenReturn(order);
        when(purchaseOfferService.creditsFromOrderSnapshots(order)).thenReturn(List.of(credit("GC"), credit("SC")));
        when(walletCoreService.credit(any())).thenReturn(successTx("WT1"), successTx("WT2"));

        PurchaseOrder result = service.applyEvent(successCallback("evt-1", "{\"amount\":\"10.00\"}"));

        assertEquals(PurchaseOrderStatus.CREDITED.name(), result.getStatus());
        assertEquals("evt-1", result.getCallbackEventKey());
        verify(walletCoreService, times(2)).credit(any(WalletCreditBo.class));
        verify(grantItemMapper, never()).selectList(any());
        verify(purchaseOfferService, never()).prepareOrderGrantSnapshots(any(), any());
        ArgumentCaptor<PurchasePaymentEvent> eventCaptor = ArgumentCaptor.forClass(PurchasePaymentEvent.class);
        verify(eventMapper).updateById(eventCaptor.capture());
        PurchasePaymentEvent storedEvent = eventCaptor.getValue();
        assertEquals(PurchasePaymentEventStatus.PROCESSED.name(), storedEvent.getEventStatus());

        when(eventMapper.selectByEventKey("000000", "evt-1")).thenReturn(storedEvent);
        when(orderMapper.selectByOrderNoForUpdate("000000", "PO1001")).thenReturn(order);
        service.applyEvent(successCallback("evt-1", "{\"amount\":\"10.00\"}"));

        verify(walletCoreService, times(2)).credit(any(WalletCreditBo.class));
    }

    @Test
    @Tag("local")
    void sameEventKeyWithDifferentPayloadIsRejected() {
        PurchasePaymentEventMapper eventMapper = mock(PurchasePaymentEventMapper.class);
        PurchasePaymentEventServiceImpl service = new PurchasePaymentEventServiceImpl(
            mock(PurchaseOrderMapper.class), eventMapper, mock(PurchaseOfferGrantItemMapper.class),
            mock(IPurchaseOfferService.class), mock(IWalletCoreService.class), mock(IPurchaseReversalService.class));
        PurchasePaymentEvent existing = new PurchasePaymentEvent();
        existing.setTenantId("000000");
        existing.setEventKey("evt-conflict");
        existing.setPurchaseOrderNo("PO1001");
        existing.setRequestHash("different-hash");
        when(eventMapper.selectByEventKey("000000", "evt-conflict")).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.applyEvent(successCallback("evt-conflict", "{\"amount\":\"11.00\"}")));

        assertEquals("payment.purchase.event.idempotency.conflict", exception.getMessage());
    }

    @Test
    @Tag("local")
    void payFailedMarksPendingOrderFailedWithoutWalletCredit() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PurchasePaymentEventServiceImpl service = service(orderMapper, walletCoreService);
        PurchaseOrder order = pendingOrder();
        when(orderMapper.selectByOrderNoForUpdate("000000", "PO1001")).thenReturn(order);

        PurchaseOrder result = service.applyEvent(PurchasePaymentCallbackBo.builder()
            .tenantId("000000")
            .eventKey("evt-failed")
            .purchaseOrderNo("PO1001")
            .providerCode("SIMULATED")
            .providerOrderNo("SIMPO1001")
            .eventType(PurchasePaymentEventType.PAY_FAILED)
            .requestBody("{\"status\":\"failed\"}")
            .failReason("provider declined")
            .build());

        assertEquals(PurchaseOrderStatus.FAILED.name(), result.getStatus());
        assertEquals("provider declined", result.getFailReason());
        verify(walletCoreService, never()).credit(any());
    }

    @Test
    @Tag("local")
    void cancelledMarksPendingOrderCancelledWithoutWalletCredit() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PurchasePaymentEventServiceImpl service = service(orderMapper, walletCoreService);
        PurchaseOrder order = pendingOrder();
        when(orderMapper.selectByOrderNoForUpdate("000000", "PO1001")).thenReturn(order);

        PurchaseOrder result = service.applyEvent(PurchasePaymentCallbackBo.builder()
            .tenantId("000000")
            .eventKey("evt-cancel")
            .purchaseOrderNo("PO1001")
            .providerCode("SIMULATED")
            .providerOrderNo("SIMPO1001")
            .eventType(PurchasePaymentEventType.CANCELLED)
            .requestBody("{\"status\":\"cancelled\"}")
            .build());

        assertEquals(PurchaseOrderStatus.CANCELLED.name(), result.getStatus());
        assertNotNull(result.getCancelTime());
        verify(walletCoreService, never()).credit(any());
    }

    @Test
    @Tag("local")
    void refundReviewDelegatesRecoveryAndMarksEventProcessed() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchasePaymentEventMapper eventMapper = mock(PurchasePaymentEventMapper.class);
        IPurchaseReversalService reversalService = mock(IPurchaseReversalService.class);
        PurchaseOrder refunded = creditedOrder("PO1001");
        refunded.setStatus(PurchaseOrderStatus.REFUND_REVIEW.name());
        PurchasePaymentEventServiceImpl service = new PurchasePaymentEventServiceImpl(orderMapper, eventMapper,
            mock(PurchaseOfferGrantItemMapper.class), mock(IPurchaseOfferService.class),
            mock(IWalletCoreService.class), reversalService);
        when(orderMapper.selectByOrderNoForUpdate("000000", "PO1001")).thenReturn(refunded);
        when(reversalService.reverse(any(), any(), any()))
            .thenReturn(new PurchaseReversalResult(refunded, "REVIEW_REQUIRED"));

        PurchaseOrder refundResult = service.applyEvent(PurchasePaymentCallbackBo.builder()
            .tenantId("000000")
            .eventKey("evt-refund")
            .purchaseOrderNo("PO1001")
            .providerCode("SIMULATED")
            .providerOrderNo("SIMPO1001")
            .eventType(PurchasePaymentEventType.REFUNDED)
            .requestBody("{\"status\":\"refunded\"}")
            .build());

        assertEquals(PurchaseOrderStatus.REFUND_REVIEW.name(), refundResult.getStatus());
        verify(reversalService).reverse(any(), any(), any());
        ArgumentCaptor<PurchasePaymentEvent> event = ArgumentCaptor.forClass(PurchasePaymentEvent.class);
        verify(eventMapper).updateById(event.capture());
        assertEquals(PurchasePaymentEventStatus.PROCESSED.name(), event.getValue().getEventStatus());
        assertEquals("REVIEW_REQUIRED", event.getValue().getProcessResult());
    }

    private PurchasePaymentEventServiceImpl service(PurchaseOrderMapper orderMapper, IWalletCoreService walletCoreService) {
        PurchasePaymentEventMapper eventMapper = mock(PurchasePaymentEventMapper.class);
        when(eventMapper.selectByEventKey(any(), any())).thenReturn(null);
        return new PurchasePaymentEventServiceImpl(orderMapper, eventMapper, mock(PurchaseOfferGrantItemMapper.class),
            mock(IPurchaseOfferService.class), walletCoreService, mock(IPurchaseReversalService.class));
    }

    private PurchasePaymentCallbackBo successCallback(String eventKey, String body) {
        return PurchasePaymentCallbackBo.builder()
            .tenantId("000000")
            .eventKey(eventKey)
            .purchaseOrderNo("PO1001")
            .providerCode("SIMULATED")
            .providerOrderNo("SIMPO1001")
            .eventType(PurchasePaymentEventType.PAY_SUCCESS)
            .requestBody(body)
            .build();
    }

    private PurchaseOrder pendingOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setTenantId("000000");
        order.setPurchaseOrderNo("PO1001");
        order.setOfferId(100L);
        order.setMemberId(1001L);
        order.setStatus(PurchaseOrderStatus.PENDING.name());
        return order;
    }

    private PurchaseOrder creditedOrder(String orderNo) {
        PurchaseOrder order = pendingOrder();
        order.setPurchaseOrderNo(orderNo);
        order.setStatus(PurchaseOrderStatus.CREDITED.name());
        return order;
    }

    private PurchaseOfferGrantItem grant(String currencyCode, String grantType) {
        PurchaseOfferGrantItem item = new PurchaseOfferGrantItem();
        item.setTenantId("000000");
        item.setOfferId(100L);
        item.setCurrencyCode(currencyCode);
        item.setGrantType(grantType);
        item.setGrantAmount(new BigDecimal("1.000000"));
        item.setSortOrder(10);
        return item;
    }

    private WalletCreditBo credit(String currencyCode) {
        WalletCreditBo credit = new WalletCreditBo();
        credit.setCurrencyCode(currencyCode);
        credit.setAmount(new BigDecimal("1.000000"));
        return credit;
    }

    private WalletTransaction successTx(String transactionNo) {
        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionNo(transactionNo);
        tx.setStatus(WalletTransactionStatus.SUCCESS.name());
        return tx;
    }
}
