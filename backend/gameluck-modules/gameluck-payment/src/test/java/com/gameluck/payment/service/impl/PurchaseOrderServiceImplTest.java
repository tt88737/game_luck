package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;
import com.gameluck.payment.domain.PurchasePaymentEvent;
import com.gameluck.payment.domain.PurchaseReversal;
import com.gameluck.payment.domain.PurchaseReversalItem;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.domain.bo.PurchaseOrderBo;
import com.gameluck.payment.domain.vo.PurchaseOrderDetailVo;
import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchasePaymentEventMapper;
import com.gameluck.payment.mapper.PurchaseReversalItemMapper;
import com.gameluck.payment.mapper.PurchaseReversalMapper;
import com.gameluck.payment.service.IPurchasePaymentEventService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseOrderServiceImplTest {

    @Test
    @Tag("local")
    void queryDetailByIdLoadsSnapshotsEventsAndReversalItems() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchaseOrderGrantSnapshotMapper snapshotMapper = mock(PurchaseOrderGrantSnapshotMapper.class);
        PurchasePaymentEventMapper eventMapper = mock(PurchasePaymentEventMapper.class);
        PurchaseReversalMapper reversalMapper = mock(PurchaseReversalMapper.class);
        PurchaseReversalItemMapper reversalItemMapper = mock(PurchaseReversalItemMapper.class);
        PurchaseOrderServiceImpl service = service(orderMapper, snapshotMapper, eventMapper, reversalMapper,
            reversalItemMapper, mock(IPurchasePaymentEventService.class));
        PurchaseOrder order = creditedOrder();
        Date completedTime = new Date(1_720_000_000_000L);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(snapshotMapper.selectByPurchaseOrderNo("000000", "PO1001")).thenReturn(List.of(snapshot("GC"), snapshot("SC")));
        when(eventMapper.selectByPurchaseOrderNo("000000", "PO1001")).thenReturn(List.of(event("evt-1"), event("evt-2")));
        when(reversalMapper.selectByPurchaseOrderNo("000000", "PO1001"))
            .thenReturn(reversal("RV1001", completedTime));
        when(reversalItemMapper.selectByReversalNo("000000", "RV1001"))
            .thenReturn(List.of(reversalItem("GC", "10.00000000"), reversalItem("SC", "20.00000000")));

        PurchaseOrderDetailVo detail = service.queryById(10L);

        assertEquals("PO1001", detail.getPurchaseOrderNo());
        assertEquals(2, detail.getGrantSnapshots().size());
        assertEquals(2, detail.getPaymentEvents().size());
        assertEquals("evt-1", detail.getPaymentEvents().get(0).getEventKey());
        assertEquals("RV1001", detail.getReversal().getReversalNo());
        assertEquals("CHARGEBACK", detail.getReversal().getReversalType());
        assertEquals("REVIEW_REQUIRED", detail.getReversal().getStatus());
        assertEquals("provider dispute", detail.getReversal().getReason());
        assertEquals("full recovery requires review", detail.getReversal().getReviewReason());
        assertEquals(completedTime, detail.getReversal().getCompletedTime());
        assertEquals(2, detail.getReversal().getItems().size());
        assertEquals("GC", detail.getReversal().getItems().get(0).getCurrencyCode());
        assertEquals(new BigDecimal("10.00000000"), detail.getReversal().getItems().get(0).getRequiredAmount());
        assertEquals(new BigDecimal("8.00000000"), detail.getReversal().getItems().get(0).getAvailableAmount());
        assertEquals(BigDecimal.ZERO, detail.getReversal().getItems().get(0).getRecoveredAmount());
        assertEquals(new BigDecimal("2.00000000"), detail.getReversal().getItems().get(0).getShortfallAmount());
        assertEquals("REVIEW_REQUIRED", detail.getReversal().getItems().get(0).getStatus());
        assertEquals("WT-RV-GC", detail.getReversal().getItems().get(0).getWalletTransactionNo());
        assertEquals("SC", detail.getReversal().getItems().get(1).getCurrencyCode());
    }

    @Test
    @Tag("local")
    void queryDetailByIdLeavesReversalNullWhenNoRecoveryExists() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchaseReversalMapper reversalMapper = mock(PurchaseReversalMapper.class);
        PurchaseOrderServiceImpl service = service(orderMapper, mock(PurchaseOrderGrantSnapshotMapper.class),
            mock(PurchasePaymentEventMapper.class), reversalMapper, mock(PurchaseReversalItemMapper.class),
            mock(IPurchasePaymentEventService.class));
        when(orderMapper.selectById(10L)).thenReturn(creditedOrder());
        when(reversalMapper.selectByPurchaseOrderNo("000000", "PO1001")).thenReturn(null);

        PurchaseOrderDetailVo detail = service.queryById(10L);

        assertNull(detail.getReversal());
    }

    @Test
    @Tag("local")
    void manualActionRequiresReason() {
        PurchaseOrderServiceImpl service = service(mock(PurchaseOrderMapper.class), mock(PurchaseOrderGrantSnapshotMapper.class),
            mock(PurchasePaymentEventMapper.class), mock(PurchaseReversalMapper.class),
            mock(PurchaseReversalItemMapper.class), mock(IPurchasePaymentEventService.class));
        PurchaseOrderBo bo = new PurchaseOrderBo();
        bo.setReason(" ");

        assertReasonRequired(() -> service.markFailed(10L, bo.getReason()));
        assertReasonRequired(() -> service.cancel(10L, bo.getReason()));
        assertReasonRequired(() -> service.refund(10L, bo.getReason()));
        assertReasonRequired(() -> service.chargeback(10L, bo.getReason()));
    }

    @Test
    @Tag("local")
    void manualCancelCreatesPaymentEventCommand() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        IPurchasePaymentEventService eventService = mock(IPurchasePaymentEventService.class);
        PurchaseOrderServiceImpl service = service(orderMapper, mock(PurchaseOrderGrantSnapshotMapper.class),
            mock(PurchasePaymentEventMapper.class), mock(PurchaseReversalMapper.class),
            mock(PurchaseReversalItemMapper.class), eventService);
        PurchaseOrder order = pendingOrder();
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(eventService.applyEvent(any())).thenReturn(cancelledOrder());

        service.cancel(10L, "provider cancelled before settlement");

        ArgumentCaptor<PurchasePaymentCallbackBo> captor = ArgumentCaptor.forClass(PurchasePaymentCallbackBo.class);
        verify(eventService).applyEvent(captor.capture());
        PurchasePaymentCallbackBo command = captor.getValue();
        assertEquals(PurchasePaymentEventType.CANCELLED, command.getEventType());
        assertEquals("MANUAL_ADMIN", command.getProviderCode());
        assertEquals("PO1001", command.getProviderOrderNo());
        assertEquals("provider cancelled before settlement", command.getFailReason());
        assertEquals(true, command.getRequestBody().contains("provider cancelled before settlement"));
    }

    @Test
    @Tag("local")
    void manualChargebackCreatesPaymentEventCommand() {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        IPurchasePaymentEventService eventService = mock(IPurchasePaymentEventService.class);
        PurchaseOrderServiceImpl service = service(orderMapper, mock(PurchaseOrderGrantSnapshotMapper.class),
            mock(PurchasePaymentEventMapper.class), mock(PurchaseReversalMapper.class),
            mock(PurchaseReversalItemMapper.class), eventService);
        PurchaseOrder order = creditedOrder();
        order.setProviderOrderNo("SIMPO1001");
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(eventService.applyEvent(any())).thenReturn(chargebackOrder());

        service.chargeback(10L, "provider reported chargeback");

        ArgumentCaptor<PurchasePaymentCallbackBo> captor = ArgumentCaptor.forClass(PurchasePaymentCallbackBo.class);
        verify(eventService).applyEvent(captor.capture());
        PurchasePaymentCallbackBo command = captor.getValue();
        assertEquals(PurchasePaymentEventType.CHARGEBACK, command.getEventType());
        assertEquals("MANUAL_ADMIN", command.getProviderCode());
        assertEquals("SIMPO1001", command.getProviderOrderNo());
        assertEquals("provider reported chargeback", command.getFailReason());
        assertEquals(true, command.getRequestBody().contains("provider reported chargeback"));
    }

    private PurchaseOrderServiceImpl service(PurchaseOrderMapper orderMapper,
                                             PurchaseOrderGrantSnapshotMapper snapshotMapper,
                                             PurchasePaymentEventMapper eventMapper,
                                             PurchaseReversalMapper reversalMapper,
                                             PurchaseReversalItemMapper reversalItemMapper,
                                             IPurchasePaymentEventService eventService) {
        return new PurchaseOrderServiceImpl(orderMapper, snapshotMapper, eventMapper, reversalMapper, reversalItemMapper,
            eventService, mock(JdbcTemplate.class));
    }

    private void assertReasonRequired(Runnable runnable) {
        ServiceException exception = assertThrows(ServiceException.class, runnable::run);
        assertEquals("payment.purchase.manual.reason.required", exception.getMessage());
    }

    private PurchaseOrder pendingOrder() {
        PurchaseOrder order = creditedOrder();
        order.setStatus(PurchaseOrderStatus.PENDING.name());
        return order;
    }

    private PurchaseOrder creditedOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(10L);
        order.setTenantId("000000");
        order.setPurchaseOrderNo("PO1001");
        order.setOfferId(9001L);
        order.setOfferNo("OF1001");
        order.setMemberId(1001L);
        order.setPayCurrencyCode("USD");
        order.setPayAmount(new BigDecimal("9.990000"));
        order.setStatus(PurchaseOrderStatus.CREDITED.name());
        order.setProviderCode("SIMULATED");
        return order;
    }

    private PurchaseOrder cancelledOrder() {
        PurchaseOrder order = pendingOrder();
        order.setStatus(PurchaseOrderStatus.CANCELLED.name());
        return order;
    }

    private PurchaseOrder chargebackOrder() {
        PurchaseOrder order = creditedOrder();
        order.setStatus(PurchaseOrderStatus.CHARGEBACK.name());
        return order;
    }

    private PurchaseOrderGrantSnapshot snapshot(String currencyCode) {
        PurchaseOrderGrantSnapshot snapshot = new PurchaseOrderGrantSnapshot();
        snapshot.setPurchaseOrderNo("PO1001");
        snapshot.setCurrencyCode(currencyCode);
        snapshot.setGrantAmount(BigDecimal.ONE);
        return snapshot;
    }

    private PurchasePaymentEvent event(String eventKey) {
        PurchasePaymentEvent event = new PurchasePaymentEvent();
        event.setEventKey(eventKey);
        event.setPurchaseOrderNo("PO1001");
        event.setEventType(PurchasePaymentEventType.PAY_SUCCESS.name());
        return event;
    }

    private PurchaseReversal reversal(String reversalNo, Date completedTime) {
        PurchaseReversal reversal = new PurchaseReversal();
        reversal.setReversalNo(reversalNo);
        reversal.setPurchaseOrderNo("PO1001");
        reversal.setReversalType("CHARGEBACK");
        reversal.setStatus("REVIEW_REQUIRED");
        reversal.setReason("provider dispute");
        reversal.setReviewReason("full recovery requires review");
        reversal.setCompletedTime(completedTime);
        return reversal;
    }

    private PurchaseReversalItem reversalItem(String currencyCode, String requiredAmount) {
        PurchaseReversalItem item = new PurchaseReversalItem();
        item.setReversalNo("RV1001");
        item.setCurrencyCode(currencyCode);
        item.setRequiredAmount(new BigDecimal(requiredAmount));
        item.setAvailableAmount(new BigDecimal("8.00000000"));
        item.setRecoveredAmount(BigDecimal.ZERO);
        item.setShortfallAmount(new BigDecimal("2.00000000"));
        item.setWalletTransactionNo("WT-RV-" + currencyCode);
        item.setStatus("REVIEW_REQUIRED");
        return item;
    }
}
