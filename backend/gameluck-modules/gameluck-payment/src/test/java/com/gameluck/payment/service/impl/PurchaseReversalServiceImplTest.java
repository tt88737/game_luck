package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;
import com.gameluck.payment.domain.PurchaseReversal;
import com.gameluck.payment.domain.PurchaseReversalItem;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.domain.vo.PurchaseReversalResult;
import com.gameluck.payment.enums.PurchaseOrderStatus;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.mapper.PurchaseReversalItemMapper;
import com.gameluck.payment.mapper.PurchaseReversalMapper;
import com.gameluck.wallet.domain.vo.WalletBatchDebitLineResult;
import com.gameluck.wallet.domain.vo.WalletBatchDebitResult;
import com.gameluck.wallet.service.IWalletCoreService;
import com.gameluck.wallet.service.IWalletTurnoverTaskService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseReversalServiceImplTest {

    @Test
    @Tag("local")
    void memberRiskAuditContractExposesDedicatedFields() {
        MemberProfile member = new MemberProfile();
        Date now = new Date(1000);
        member.setRiskReason("chargeback");
        member.setRiskSource("PURCHASE_CHARGEBACK:PRV1:EV1");
        member.setRiskUpdatedTime(now);

        assertEquals("chargeback", member.getRiskReason());
        assertEquals("PURCHASE_CHARGEBACK:PRV1:EV1", member.getRiskSource());
        assertEquals(now, member.getRiskUpdatedTime());
    }

    @Test
    @Tag("local")
    void chargebackSuccessRaisesMemberRiskWithAuditSource() {
        Fixture fixture = fixture(List.of(snapshot("SC", "10")),
            walletResult("COMPLETED", line("SC", "10", "20", "10", "0", "WT-SC")));
        when(fixture.memberMapper.selectByIdForUpdate("000000", 1001L)).thenReturn(member());
        when(fixture.memberMapper.updateChargebackRisk(any(), any(), any(), any(), any())).thenReturn(1);

        fixture.service.reverse(order(), callback(PurchasePaymentEventType.CHARGEBACK), new Date(1000));

        verifyChargebackRisk(fixture, new Date(1000));
    }

    @Test
    @Tag("local")
    void chargebackReviewStillRaisesMemberRiskWithAuditSource() {
        Fixture fixture = fixture(List.of(snapshot("SC", "10")),
            walletResult("REVIEW_REQUIRED", line("SC", "10", "4", "0", "6", null)));
        when(fixture.memberMapper.selectByIdForUpdate("000000", 1001L)).thenReturn(member());
        when(fixture.memberMapper.updateChargebackRisk(any(), any(), any(), any(), any())).thenReturn(1);

        fixture.service.reverse(order(), callback(PurchasePaymentEventType.CHARGEBACK), new Date(1000));

        verifyChargebackRisk(fixture, new Date(1000));
    }

    @Test
    @Tag("local")
    void refundSuccessAggregatesSnapshotsAndCompletesRecovery() {
        Fixture fixture = fixture(List.of(snapshot("sc", "3"), snapshot("SC", "7"), snapshot("GC", "5")),
            walletResult("COMPLETED", line("GC", "5", "20", "5", "0", "WT-GC"),
                line("SC", "10", "30", "10", "0", "WT-SC")));

        PurchaseReversalResult result = fixture.service.reverse(order(), callback(PurchasePaymentEventType.REFUNDED), new Date(1000));

        assertEquals("OK", result.getProcessResult());
        assertEquals(PurchaseOrderStatus.REFUNDED.name(), result.getOrder().getStatus());
        ArgumentCaptor<com.gameluck.wallet.domain.bo.WalletBatchDebitBo> captor =
            ArgumentCaptor.forClass(com.gameluck.wallet.domain.bo.WalletBatchDebitBo.class);
        verify(fixture.walletCore).batchDebit(captor.capture());
        assertEquals("GC", captor.getValue().getLines().get(0).getCurrencyCode());
        assertEquals(new BigDecimal("10.000000"), captor.getValue().getLines().get(1).getAmount());
        verify(fixture.turnover).cancelPendingByPurchase(any(), any(), any(), any(), any());
        verify(fixture.itemMapper, org.mockito.Mockito.times(2)).updateById(any(PurchaseReversalItem.class));
        verify(fixture.memberMapper, never()).updateChargebackRisk(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("local")
    void refundInsufficiencyCreatesReviewWithoutCancellingTurnover() {
        Fixture fixture = fixture(List.of(snapshot("SC", "10")),
            walletResult("REVIEW_REQUIRED", line("SC", "10", "4", "0", "6", null)));

        PurchaseReversalResult result = fixture.service.reverse(order(), callback(PurchasePaymentEventType.REFUNDED), new Date(1000));

        assertEquals("REVIEW_REQUIRED", result.getProcessResult());
        assertEquals(PurchaseOrderStatus.REFUND_REVIEW.name(), result.getOrder().getStatus());
        verify(fixture.turnover, never()).cancelPendingByPurchase(any(), any(), any(), any(), any());
        verify(fixture.memberMapper, never()).updateChargebackRisk(any(), any(), any(), any(), any());
        ArgumentCaptor<PurchaseReversalItem> item = ArgumentCaptor.forClass(PurchaseReversalItem.class);
        verify(fixture.itemMapper).updateById(item.capture());
        assertEquals(new BigDecimal("6.000000"), item.getValue().getShortfallAmount());
    }

    @Test
    @Tag("local")
    void rejectsMissingGrantSnapshots() {
        Fixture fixture = fixture(List.of(), null);
        assertThrows(RuntimeException.class,
            () -> fixture.service.reverse(order(), callback(PurchasePaymentEventType.REFUNDED), new Date()));
        verify(fixture.reversalMapper, never()).insert(any(PurchaseReversal.class));
    }

    @Test
    @Tag("local")
    void rejectsNonPositiveSnapshotBeforeCreatingRecoveryData() {
        Fixture fixture = fixture(List.of(snapshot("SC", "0")), null);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> fixture.service.reverse(order(), callback(PurchasePaymentEventType.REFUNDED), new Date()));

        assertEquals("payment.purchase.reversal.amount.invalid", exception.getMessage());
        verify(fixture.reversalMapper, never()).insert(any(PurchaseReversal.class));
        verify(fixture.itemMapper, never()).insert(any(PurchaseReversalItem.class));
        verify(fixture.walletCore, never()).batchDebit(any());
    }

    @Test
    @Tag("local")
    void sameEventReplayReturnsOriginalReviewOutcomeWithoutSideEffects() {
        Fixture fixture = fixture(List.of(snapshot("SC", "10")), null);
        PurchaseOrder reviewed = order();
        reviewed.setStatus(PurchaseOrderStatus.REFUND_REVIEW.name());
        PurchaseReversal existing = new PurchaseReversal();
        existing.setEventKey("EV1");
        existing.setStatus("REVIEW_REQUIRED");
        when(fixture.reversalMapper.selectByEventKey("000000", "EV1")).thenReturn(existing);

        PurchaseReversalResult result = fixture.service.reverse(
            reviewed, callback(PurchasePaymentEventType.REFUNDED), new Date());

        assertEquals("REVIEW_REQUIRED", result.getProcessResult());
        assertEquals(PurchaseOrderStatus.REFUND_REVIEW.name(), result.getOrder().getStatus());
        verify(fixture.reversalMapper, never()).insert(any(PurchaseReversal.class));
        verify(fixture.walletCore, never()).batchDebit(any());
        verify(fixture.turnover, never()).cancelPendingByPurchase(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("local")
    void differentEventKeyAgainstReviewOrderIsRejectedWithoutSideEffects() {
        Fixture fixture = fixture(List.of(snapshot("SC", "10")), null);
        PurchaseOrder reviewed = order();
        reviewed.setStatus(PurchaseOrderStatus.REFUND_REVIEW.name());

        ServiceException exception = assertThrows(ServiceException.class,
            () -> fixture.service.reverse(reviewed,
                PurchasePaymentCallbackBo.builder().tenantId("000000").eventKey("EV2")
                    .purchaseOrderNo("PO1").eventType(PurchasePaymentEventType.REFUNDED).build(),
                new Date()));

        assertEquals("payment.purchase.order.status.invalid", exception.getMessage());
        verify(fixture.reversalMapper, never()).insert(any(PurchaseReversal.class));
        verify(fixture.walletCore, never()).batchDebit(any());
    }

    private static Fixture fixture(List<PurchaseOrderGrantSnapshot> snapshots, WalletBatchDebitResult walletResult) {
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        PurchaseOrderGrantSnapshotMapper snapshotMapper = mock(PurchaseOrderGrantSnapshotMapper.class);
        PurchaseReversalMapper reversalMapper = mock(PurchaseReversalMapper.class);
        PurchaseReversalItemMapper itemMapper = mock(PurchaseReversalItemMapper.class);
        IWalletCoreService walletCore = mock(IWalletCoreService.class);
        IWalletTurnoverTaskService turnover = mock(IWalletTurnoverTaskService.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        when(snapshotMapper.selectByPurchaseOrderNo("000000", "PO1")).thenReturn(snapshots);
        when(walletCore.batchDebit(any())).thenReturn(walletResult);
        return new Fixture(new PurchaseReversalServiceImpl(orderMapper, snapshotMapper, reversalMapper, itemMapper,
            walletCore, turnover, memberMapper), reversalMapper, itemMapper, walletCore, turnover, memberMapper);
    }

    private static PurchaseOrder order() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setTenantId("000000");
        order.setPurchaseOrderNo("PO1");
        order.setMemberId(1001L);
        order.setStatus(PurchaseOrderStatus.CREDITED.name());
        return order;
    }

    private static PurchasePaymentCallbackBo callback(PurchasePaymentEventType type) {
        return PurchasePaymentCallbackBo.builder().tenantId("000000").eventKey("EV1").purchaseOrderNo("PO1")
            .eventType(type).failReason("provider reason").build();
    }

    private static PurchaseOrderGrantSnapshot snapshot(String currency, String amount) {
        PurchaseOrderGrantSnapshot snapshot = new PurchaseOrderGrantSnapshot();
        snapshot.setCurrencyCode(currency);
        snapshot.setGrantAmount(new BigDecimal(amount));
        return snapshot;
    }

    private static WalletBatchDebitResult walletResult(String status, WalletBatchDebitLineResult... lines) {
        WalletBatchDebitResult result = new WalletBatchDebitResult();
        result.setStatus(status);
        result.setLines(List.of(lines));
        return result;
    }

    private static WalletBatchDebitLineResult line(String currency, String required, String available,
                                                    String recovered, String shortfall, String transactionNo) {
        WalletBatchDebitLineResult line = new WalletBatchDebitLineResult();
        line.setCurrencyCode(currency);
        line.setRequiredAmount(new BigDecimal(required));
        line.setAvailableAmount(new BigDecimal(available));
        line.setRecoveredAmount(new BigDecimal(recovered));
        line.setShortfallAmount(new BigDecimal(shortfall));
        line.setWalletTransactionNo(transactionNo);
        return line;
    }

    private static MemberProfile member() {
        MemberProfile member = new MemberProfile();
        member.setId(1001L);
        member.setTenantId("000000");
        member.setRiskLevel("NORMAL");
        return member;
    }

    private static void verifyChargebackRisk(Fixture fixture, Date expectedTime) {
        ArgumentCaptor<String> source = ArgumentCaptor.forClass(String.class);
        verify(fixture.memberMapper).updateChargebackRisk(
            org.mockito.ArgumentMatchers.eq("000000"), org.mockito.ArgumentMatchers.eq(1001L),
            org.mockito.ArgumentMatchers.eq("payment.purchase.chargeback.risk.reason"), source.capture(),
            org.mockito.ArgumentMatchers.eq(expectedTime));
        org.junit.jupiter.api.Assertions.assertTrue(source.getValue().startsWith("PURCHASE_CHARGEBACK:PRV"));
        org.junit.jupiter.api.Assertions.assertTrue(source.getValue().endsWith(":EV1"));
    }

    private record Fixture(PurchaseReversalServiceImpl service, PurchaseReversalMapper reversalMapper,
                           PurchaseReversalItemMapper itemMapper, IWalletCoreService walletCore,
                           IWalletTurnoverTaskService turnover, MemberProfileMapper memberMapper) {
    }
}
