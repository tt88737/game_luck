package com.gameluck.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementItemMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.service.settlement.PaymentSettlementCalculator;
import com.gameluck.payment.service.settlement.SettlementSourceEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentSettlementCalculationServiceTest {

    @Test
    void acquiresOneGuardedLeaseWithOperatorAndImmutableBatchSnapshot() {
        Fixture f = fixture();
        PaymentSettlementBatch batch = batch("CREATED", 0);
        when(f.batches.selectByTenantAndId("tenant-a", 7L)).thenReturn(batch);
        when(f.batches.transitionStatus(eq("tenant-a"), eq(7L), eq("CREATED"), eq("CALCULATING"), any())).thenReturn(1);
        PaymentSettlementCalculationService.CalculationLease lease = f.service.acquire("tenant-a", 7L);
        assertEquals(1, lease.version()); assertEquals(88L, lease.operatorId());
        assertEquals("SIMULATED", lease.providerCode()); assertEquals("USD", lease.currencyCode());
        verify(f.batches).transitionStatus(eq("tenant-a"), eq(7L), eq("CREATED"), eq("CALCULATING"), any());

        when(f.batches.transitionStatus(eq("tenant-a"), eq(7L), eq("CREATED"), eq("CALCULATING"), any())).thenReturn(0);
        assertThrows(ServiceException.class, () -> f.service.acquire("tenant-a", 7L));
    }

    @Test
    void calculatesInFixedChunksAndCompletesTotalsAtomically() {
        Fixture f = fixture();
        List<SettlementSourceEvent> first = events(1, 500);
        List<SettlementSourceEvent> second = events(501, 1);
        when(f.webhooks.selectSettlementSourceEvents(eq("tenant-a"), eq("SIMULATED"), any(), any(),
            isNull(), isNull(), eq(500))).thenReturn(first);
        when(f.webhooks.selectSettlementSourceEvents(eq("tenant-a"), eq("SIMULATED"), any(), any(),
            eq(first.get(499).receivedTime()), eq(500L), eq(500))).thenReturn(second);
        when(f.items.insertBatch(any())).thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());
        when(f.batches.completeCalculation(eq("tenant-a"), eq(7L), eq(1), eq(501), eq(501), eq(0), eq(0),
            eq(new BigDecimal("501.000000")), eq(new BigDecimal("0.000000")),
            eq(new BigDecimal("0.000000")), eq(new BigDecimal("0.000000")),
            eq(new BigDecimal("501.000000")), anyString(), eq(88L), eq("operator"), any())).thenReturn(1);

        PaymentSettlementBatch result = f.service.reconcile(lease());
        assertEquals("CALCULATED", result.getStatus()); assertEquals(501, result.getEventCount());
        ArgumentCaptor<List<com.gameluck.payment.domain.PaymentSettlementItem>> chunks = ArgumentCaptor.forClass(List.class);
        verify(f.items, times(2)).insertBatch(chunks.capture());
        assertEquals(List.of(500, 1), chunks.getAllValues().stream().map(List::size).toList());
        verify(f.logs).insert(argThat(log -> "CALCULATE".equals(log.getActionType())
            && "CALCULATING".equals(log.getBeforeStatus()) && "CALCULATED".equals(log.getAfterStatus())));
    }

    @Test
    void facadeRecordsStableFailureAfterOwnedCalculationRollsBack() {
        Fixture f = fixture();
        @SuppressWarnings("unchecked") ObjectProvider<PaymentSettlementCalculationService> proxies = mock(ObjectProvider.class);
        PaymentSettlementCalculationService proxy = mock(PaymentSettlementCalculationService.class);
        when(proxies.getObject()).thenReturn(proxy);
        when(proxy.acquire(7L)).thenReturn(lease());
        when(proxy.reconcile(any())).thenThrow(new IllegalStateException("SQL C:\\private\\rawBody signature stack"));
        PaymentSettlementCalculationService service = new PaymentSettlementCalculationService(f.batches, f.items,
            f.logs, f.webhooks, new PaymentSettlementCalculator(new ObjectMapper()), f.operator, f.failure,
            proxies, new ObjectMapper());
        ServiceException error = assertThrows(ServiceException.class, () -> service.calculate(7L));
        assertEquals("payment.settlement.calculate.failed", error.getMessage());
        verify(f.failure).recordFailure("tenant-a", 7L, "SETTLEMENT_CALCULATION_FAILED", 88L, "operator");
    }

    @Test
    void zeroEligibleEventsFailsWithoutWritingItemsOrTotals() {
        Fixture f = fixture();
        when(f.webhooks.selectSettlementSourceEvents(anyString(), anyString(), any(), any(),
            isNull(), isNull(), eq(500))).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> f.service.reconcile(lease()));
        verifyNoInteractions(f.items);
        verify(f.batches, never()).completeCalculation(anyString(), anyLong(), anyInt(), anyInt(), anyInt(),
            anyInt(), anyInt(), any(), any(), any(), any(), any(), anyString(), anyLong(), anyString(), any());
    }

    @Test
    void lifecycleMethodsDeclareProxyTransactionBoundaries() throws Exception {
        assertEquals(Propagation.REQUIRES_NEW, PaymentSettlementCalculationService.class
            .getMethod("acquire", String.class, Long.class).getAnnotation(Transactional.class).propagation());
        assertNotNull(PaymentSettlementCalculationService.class
            .getMethod("reconcile", PaymentSettlementCalculationService.CalculationLease.class)
            .getAnnotation(Transactional.class));
        assertEquals(Propagation.REQUIRES_NEW, PaymentSettlementFailureRecorder.class
            .getMethod("recordFailure", String.class, Long.class, String.class, Long.class, String.class)
            .getAnnotation(Transactional.class).propagation());
    }

    private static List<SettlementSourceEvent> events(int start, int count) {
        List<SettlementSourceEvent> result = new ArrayList<>();
        for (int value = start; value < start + count; value++) {
            result.add(new SettlementSourceEvent((long) value, "SIMULATED", "evt-" + value,
                "PAYMENT_SUCCEEDED", "PROCESSED", "ps-" + value, 1000L + value, "s-" + value,
                "o-" + value, "USD", BigDecimal.ONE, 2000L + value, "o-" + value,
                "SIMULATED", "USD", BigDecimal.ONE, Instant.parse("2026-07-01T00:00:00Z").plusSeconds(value)));
        }
        return result;
    }

    private static PaymentSettlementBatch batch(String status, int version) {
        PaymentSettlementBatch b = new PaymentSettlementBatch(); b.setId(7L); b.setTenantId("tenant-a");
        b.setProviderCode("SIMULATED"); b.setCurrencyCode("USD"); b.setStatus(status); b.setVersion(version);
        b.setPeriodStart(Date.from(Instant.parse("2026-07-01T00:00:00Z")));
        b.setPeriodEnd(Date.from(Instant.parse("2026-07-02T00:00:00Z")));
        b.setPaymentFeeRate(BigDecimal.ZERO); b.setPaymentFixedFee(BigDecimal.ZERO);
        b.setChargebackFixedFee(BigDecimal.ZERO); return b;
    }

    private static PaymentSettlementCalculationService.CalculationLease lease() {
        PaymentSettlementBatch b = batch("CALCULATING", 1);
        return new PaymentSettlementCalculationService.CalculationLease("tenant-a", 7L, 1, 88L,
            "operator", b.getProviderCode(), b.getCurrencyCode(), b.getPeriodStart().toInstant(),
            b.getPeriodEnd().toInstant(), b.getPaymentFeeRate(), b.getPaymentFixedFee(), b.getChargebackFixedFee());
    }

    private static Fixture fixture() {
        PaymentSettlementBatchMapper batches = mock(PaymentSettlementBatchMapper.class);
        PaymentSettlementItemMapper items = mock(PaymentSettlementItemMapper.class);
        PaymentSettlementActionLogMapper logs = mock(PaymentSettlementActionLogMapper.class);
        PaymentWebhookEventMapper webhooks = mock(PaymentWebhookEventMapper.class);
        PaymentReconciliationOperatorProvider operator = mock(PaymentReconciliationOperatorProvider.class);
        when(operator.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(88L, "operator"));
        PaymentSettlementFailureRecorder failure = mock(PaymentSettlementFailureRecorder.class);
        PaymentSettlementCalculationService service = new PaymentSettlementCalculationService(batches, items, logs,
            webhooks, new PaymentSettlementCalculator(new ObjectMapper()), operator, failure, null, new ObjectMapper());
        return new Fixture(batches, items, logs, webhooks, operator, failure, service);
    }

    private record Fixture(PaymentSettlementBatchMapper batches, PaymentSettlementItemMapper items,
        PaymentSettlementActionLogMapper logs, PaymentWebhookEventMapper webhooks,
        PaymentReconciliationOperatorProvider operator, PaymentSettlementFailureRecorder failure,
        PaymentSettlementCalculationService service) { }
}
