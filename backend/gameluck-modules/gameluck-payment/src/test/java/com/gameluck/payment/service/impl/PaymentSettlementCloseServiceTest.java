package com.gameluck.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.domain.PaymentReconciliationIssue;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.bo.PaymentSettlementCloseBo;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.service.settlement.PaymentSettlementReconciliationGate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentSettlementCloseServiceTest {
    @Test
    void reconciliationSqlDateKeepsItsCalendarDateOutsideUtc() {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            Fixture f = fixture();
            PaymentReconciliationBatch batch = new PaymentReconciliationBatch();
            batch.setId(29L);
            batch.setStatementDate(Date.from(LocalDate.parse("2026-07-29")
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
            when(f.reconciliationBatches.selectCompletedForSettlement("tenant-a", "SIMULATED",
                LocalDate.parse("2026-07-29"), LocalDate.parse("2026-07-29"))).thenReturn(List.of(batch));
            when(f.issues.selectByReconciliationBatches("tenant-a", List.of(29L))).thenReturn(List.of());

            var evidence = f.gate.evaluate("tenant-a", "SIMULATED", "USD",
                Instant.parse("2026-07-29T15:00:00Z"), Instant.parse("2026-07-29T15:30:00Z"));

            assertEquals(List.of(LocalDate.parse("2026-07-29")), evidence.coveredDates());
            assertTrue(evidence.missingDates().isEmpty());
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void evidenceRequiresEveryTouchedUtcDateAndCurrentCurrencyIssues() throws Exception {
        Fixture f = fixture();
        when(f.reconciliationBatches.selectCompletedForSettlement("tenant-a", "SIMULATED",
            LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-03")))
            .thenReturn(List.of(reconciliation(11L, "2026-07-01"), reconciliation(12L, "2026-07-02")));
        when(f.issues.selectByReconciliationBatches("tenant-a", List.of(11L, 12L)))
            .thenReturn(List.of(issue("OPEN", "EUR", "USD"), issue("RESOLVED", "USD", "USD"),
                issue("IGNORED", "USD", null)));

        var evidence = f.gate.evaluate("tenant-a", "SIMULATED", "USD",
            Instant.parse("2026-07-01T12:00:00Z"), Instant.parse("2026-07-03T00:00:01Z"));
        assertEquals(List.of(LocalDate.parse("2026-07-03")), evidence.missingDates());
        assertEquals(1, evidence.openIssueCount());
        assertEquals(1, evidence.resolvedIssueCount());
        assertEquals(1, evidence.ignoredIssueCount());
        assertEquals("{\"completedBatchIds\":[11,12],\"coveredDates\":[\"2026-07-01\",\"2026-07-02\"],\"missingDates\":[\"2026-07-03\"],\"openIssueCount\":1,\"resolvedIssueCount\":1,\"ignoredIssueCount\":1}",
            f.mapper.writeValueAsString(evidence));
    }

    @Test
    void rejectsMissingCoverageOrOpenIssueAndAuditsOnlyRejection() {
        Fixture f = fixture();
        when(f.settlements.selectByTenantAndId("tenant-a", 7L)).thenReturn(settlement("CALCULATED", 2));
        when(f.reconciliationBatches.selectCompletedForSettlement(anyString(), anyString(), any(), any()))
            .thenReturn(List.of());
        PaymentSettlementCloseBo bo = new PaymentSettlementCloseBo(); bo.setVersion(2); bo.setRemark("close July");
        assertThrows(ServiceException.class, () -> f.service.close("tenant-a", 7L, bo));
        verify(f.logs).insert(argThat(log -> "CLOSE_REJECTED".equals(log.getActionType())
            && "CALCULATED".equals(log.getBeforeStatus()) && "CALCULATED".equals(log.getAfterStatus())));
        verify(f.settlements, never()).closeCalculated(anyString(), anyLong(), anyInt(), anyInt(), anyInt(),
            anyString(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void closesOnceWithFreshEvidenceRemarkAndOperator() {
        Fixture f = fixture();
        when(f.settlements.selectByTenantAndId("tenant-a", 7L)).thenReturn(settlement("CALCULATED", 2));
        when(f.reconciliationBatches.selectCompletedForSettlement(anyString(), anyString(), any(), any()))
            .thenReturn(List.of(reconciliation(11L, "2026-07-01"), reconciliation(12L, "2026-07-02")));
        when(f.issues.selectByReconciliationBatches("tenant-a", List.of(11L, 12L))).thenReturn(List.of());
        when(f.settlements.closeCalculated(eq("tenant-a"), eq(7L), eq(2), eq(2), eq(0), anyString(),
            eq(88L), eq("operator"), eq("close July"), any())).thenReturn(1);
        PaymentSettlementCloseBo bo = new PaymentSettlementCloseBo(); bo.setVersion(2); bo.setRemark(" close July ");
        PaymentSettlementBatch result = f.service.close("tenant-a", 7L, bo);
        assertEquals("CLOSED", result.getStatus());
        verify(f.logs).insert(argThat(log -> "CLOSE".equals(log.getActionType())));

        when(f.settlements.closeCalculated(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), anyString(),
            anyLong(), anyString(), anyString(), any())).thenReturn(0);
        assertThrows(ServiceException.class, () -> f.service.close("tenant-a", 7L, bo));
        verify(f.logs, times(1)).insert(argThat(log -> "CLOSE".equals(log.getActionType())));
    }

    @Test
    void rejectsBlankRemarkStaleVersionAndTerminalReplayWithoutReadingOrMutatingReconciliation() {
        Fixture f = fixture();
        PaymentSettlementCloseBo blank = new PaymentSettlementCloseBo(); blank.setVersion(2); blank.setRemark("  ");
        when(f.settlements.selectByTenantAndId("tenant-a", 7L)).thenReturn(settlement("CALCULATED", 2));
        assertThrows(ServiceException.class, () -> f.service.close("tenant-a", 7L, blank));
        verifyNoInteractions(f.reconciliationBatches, f.issues, f.logs);

        PaymentSettlementCloseBo replay = new PaymentSettlementCloseBo(); replay.setVersion(3); replay.setRemark("again");
        when(f.settlements.selectByTenantAndId("tenant-a", 7L)).thenReturn(settlement("CLOSED", 3));
        assertThrows(ServiceException.class, () -> f.service.close("tenant-a", 7L, replay));
        verifyNoInteractions(f.reconciliationBatches, f.issues, f.logs);
    }

    @Test
    void recalculatesEvidenceSoAnIssueOpenedImmediatelyBeforeCloseBlocksIt() {
        Fixture f = fixture();
        List<PaymentReconciliationBatch> coverage = List.of(
            reconciliation(11L, "2026-07-01"), reconciliation(12L, "2026-07-02"));
        when(f.reconciliationBatches.selectCompletedForSettlement("tenant-a", "SIMULATED",
            LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-02"))).thenReturn(coverage);
        when(f.issues.selectByReconciliationBatches("tenant-a", List.of(11L, 12L)))
            .thenReturn(List.of(), List.of(issue("OPEN", "USD", null)));
        assertEquals(0, f.gate.evaluate("tenant-a", "SIMULATED", "USD",
            Instant.parse("2026-07-01T12:00:00Z"), Instant.parse("2026-07-02T12:00:00Z")).openIssueCount());

        when(f.settlements.selectByTenantAndId("tenant-a", 7L)).thenReturn(settlement("CALCULATED", 2));
        PaymentSettlementCloseBo bo = new PaymentSettlementCloseBo(); bo.setVersion(2); bo.setRemark("close");
        assertThrows(ServiceException.class, () -> f.service.close("tenant-a", 7L, bo));
        verify(f.settlements, never()).closeCalculated(anyString(), anyLong(), anyInt(), anyInt(), anyInt(),
            anyString(), anyLong(), anyString(), anyString(), any());
        verify(f.logs).insert(argThat(log -> "CLOSE_REJECTED".equals(log.getActionType())));
        verify(f.reconciliationBatches, times(2)).selectCompletedForSettlement("tenant-a", "SIMULATED",
            LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-02"));
        verify(f.issues, times(2)).selectByReconciliationBatches("tenant-a", List.of(11L, 12L));
        verifyNoMoreInteractions(f.reconciliationBatches, f.issues);
    }

    @Test
    void mapperContractsAndTransactionsKeepCloseTenantScopedAndReconciliationReadOnly() throws Exception {
        String batchesSql = PaymentReconciliationBatchMapper.class.getMethod("selectCompletedForSettlement",
            String.class, String.class, LocalDate.class, LocalDate.class).getAnnotation(Select.class).value()[0];
        assertTrue(batchesSql.contains("tenant_id=#{tenantId}"));
        assertTrue(batchesSql.contains("provider_code=#{providerCode}"));
        assertTrue(batchesSql.contains("status='COMPLETED'"));
        String issuesSql = PaymentReconciliationIssueMapper.class.getMethod("selectByReconciliationBatches",
            String.class, List.class).getAnnotation(Select.class).value()[0];
        assertTrue(issuesSql.contains("tenant_id=#{tenantId}"));
        assertFalse((batchesSql + issuesSql).toLowerCase().matches(".*\\b(update|delete|insert)\\b.*"));
        assertNotNull(PaymentSettlementCloseService.class
            .getMethod("close", String.class, Long.class, PaymentSettlementCloseBo.class)
            .getAnnotation(Transactional.class));
        assertEquals(Propagation.REQUIRES_NEW, PaymentSettlementCloseRejectionRecorder.class
            .getMethod("record", String.class, Long.class, long.class, String.class, String.class, Date.class)
            .getAnnotation(Transactional.class).propagation());
    }

    private static Fixture fixture() {
        PaymentSettlementBatchMapper settlements = mock(PaymentSettlementBatchMapper.class);
        PaymentSettlementActionLogMapper logs = mock(PaymentSettlementActionLogMapper.class);
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationIssueMapper issues = mock(PaymentReconciliationIssueMapper.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        PaymentSettlementReconciliationGate gate = new PaymentSettlementReconciliationGate(batches, issues);
        PaymentReconciliationOperatorProvider operator = mock(PaymentReconciliationOperatorProvider.class);
        when(operator.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(88L, "operator"));
        PaymentSettlementCloseRejectionRecorder rejectionRecorder =
            new PaymentSettlementCloseRejectionRecorder(logs);
        return new Fixture(settlements, logs, batches, issues, mapper, gate,
            new PaymentSettlementCloseService(settlements, logs, gate, operator, rejectionRecorder, mapper));
    }

    private static PaymentSettlementBatch settlement(String status, int version) {
        PaymentSettlementBatch value = new PaymentSettlementBatch(); value.setId(7L); value.setTenantId("tenant-a");
        value.setProviderCode("SIMULATED"); value.setCurrencyCode("USD"); value.setStatus(status); value.setVersion(version);
        value.setPeriodStart(Date.from(Instant.parse("2026-07-01T12:00:00Z")));
        value.setPeriodEnd(Date.from(Instant.parse("2026-07-02T12:00:00Z"))); return value;
    }

    private static PaymentReconciliationBatch reconciliation(long id, String date) {
        PaymentReconciliationBatch value = new PaymentReconciliationBatch(); value.setId(id);
        value.setStatementDate(Date.from(LocalDate.parse(date).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
        return value;
    }

    private static PaymentReconciliationIssue issue(String status, String providerCurrency, String platformCurrency) {
        PaymentReconciliationIssue value = new PaymentReconciliationIssue(); value.setStatus(status);
        value.setProviderCurrencyCode(providerCurrency); value.setPlatformCurrencyCode(platformCurrency); return value;
    }

    private record Fixture(PaymentSettlementBatchMapper settlements, PaymentSettlementActionLogMapper logs,
        PaymentReconciliationBatchMapper reconciliationBatches, PaymentReconciliationIssueMapper issues,
        ObjectMapper mapper, PaymentSettlementReconciliationGate gate, PaymentSettlementCloseService service) { }
}
