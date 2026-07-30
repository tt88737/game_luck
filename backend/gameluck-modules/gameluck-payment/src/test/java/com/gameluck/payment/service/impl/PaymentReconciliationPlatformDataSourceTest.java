package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.PaymentReconciliationLine;
import com.gameluck.payment.mapper.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentReconciliationPlatformDataSourceTest {
    @Test
    void fiveHundredAndOneLinesUseTwelveBulkQueriesAndMarkPriorCompletedEvidence() {
        PaymentSessionMapper sessions = mock(PaymentSessionMapper.class);
        PurchaseOrderMapper orders = mock(PurchaseOrderMapper.class);
        PaymentWebhookEventMapper webhooks = mock(PaymentWebhookEventMapper.class);
        PurchasePaymentEventMapper events = mock(PurchasePaymentEventMapper.class);
        PurchaseReversalMapper reversals = mock(PurchaseReversalMapper.class);
        PaymentReconciliationLineMapper reconciliationLines = mock(PaymentReconciliationLineMapper.class);
        when(sessions.selectReconciliationCandidatesBatch(anyString(), anyString(), anyList())).thenReturn(List.of());
        when(orders.selectReconciliationByOrderNos(anyString(), anyList())).thenReturn(List.of());
        when(webhooks.selectReconciliationByProviderEventIds(anyString(), anyString(), anyList())).thenReturn(List.of());
        when(events.selectReconciliationLatestCandidates(anyString(), anyList())).thenReturn(List.of());
        when(reversals.selectReconciliationLatestCandidates(anyString(), anyList())).thenReturn(List.of());
        when(reconciliationLines.selectProviderIdsFromOtherCompletedBatches(
            eq("tenant-a"), anyLong(), anyList())).thenAnswer(invocation -> {
                List<String> ids = invocation.getArgument(2);
                return ids.contains("event-1") ? List.of("event-1") : List.of();
            });
        PaymentReconciliationPlatformDataSource source = new PaymentReconciliationPlatformDataSource(
            sessions, orders, webhooks, events, reversals, reconciliationLines);
        List<PaymentReconciliationLine> lines = LongStream.rangeClosed(1, 501).mapToObj(this::line).toList();

        var firstPage = source.prefetch("tenant-a", "SIMULATED", lines.subList(0, 500));
        assertEquals(500, firstPage.size());
        assertEquals(true, firstPage.get(1L).duplicatePriorStatementEvidence());
        assertEquals(1, source.prefetch("tenant-a", "SIMULATED", lines.subList(500, 501)).size());

        verify(sessions, times(2)).selectReconciliationCandidatesBatch(eq("tenant-a"), eq("SIMULATED"), anyList());
        verify(orders, times(2)).selectReconciliationByOrderNos(eq("tenant-a"), anyList());
        verify(webhooks, times(2)).selectReconciliationByProviderEventIds(eq("tenant-a"), eq("SIMULATED"), anyList());
        verify(events, times(2)).selectReconciliationLatestCandidates(eq("tenant-a"), anyList());
        verify(reversals, times(2)).selectReconciliationLatestCandidates(eq("tenant-a"), anyList());
        verify(reconciliationLines, times(2)).selectProviderIdsFromOtherCompletedBatches(
            eq("tenant-a"), anyLong(), anyList());
        verify(sessions, never()).selectReconciliationCandidates(anyString(), anyString(), anyString());
        verify(orders, never()).selectByOrderNo(anyString(), anyString());
        verify(webhooks, never()).selectByProviderEventId(anyString(), anyString(), anyString());
        verify(events, never()).selectByPurchaseOrderNo(anyString(), anyString());
        verify(reversals, never()).selectByPurchaseOrderNo(anyString(), anyString());
    }

    private PaymentReconciliationLine line(long id) {
        PaymentReconciliationLine line = new PaymentReconciliationLine();
        line.setId(id); line.setBatchId(44L); line.setProviderSessionNo("session-" + id);
        line.setProviderRecordId("event-" + id); line.setPurchaseOrderNo("order-" + id);
        return line;
    }
}
