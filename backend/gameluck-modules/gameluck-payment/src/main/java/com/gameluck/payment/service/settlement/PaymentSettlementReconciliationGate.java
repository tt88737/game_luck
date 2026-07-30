package com.gameluck.payment.service.settlement;

import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.domain.PaymentReconciliationIssue;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PaymentSettlementReconciliationGate {
    private final PaymentReconciliationBatchMapper batchMapper;
    private final PaymentReconciliationIssueMapper issueMapper;

    public PaymentSettlementReconciliationGate(PaymentReconciliationBatchMapper batchMapper,
        PaymentReconciliationIssueMapper issueMapper) {
        this.batchMapper = batchMapper;
        this.issueMapper = issueMapper;
    }

    public SettlementReconciliationEvidence evaluate(String tenantId, String providerCode, String currencyCode,
        Instant periodStart, Instant periodEnd) {
        LocalDate first = periodStart.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate last = periodEnd.minusNanos(1).atZone(ZoneOffset.UTC).toLocalDate();
        List<PaymentReconciliationBatch> batches = new ArrayList<>(batchMapper.selectCompletedForSettlement(
            tenantId, providerCode, first, last));
        batches.sort(Comparator.comparing(PaymentSettlementReconciliationGate::statementDate)
            .thenComparing(PaymentReconciliationBatch::getId));
        List<Long> ids = batches.stream().map(PaymentReconciliationBatch::getId).toList();
        Set<LocalDate> coveredSet = new LinkedHashSet<>();
        batches.forEach(batch -> coveredSet.add(statementDate(batch)));
        List<LocalDate> expected = first.datesUntil(last.plusDays(1)).toList();
        List<LocalDate> missing = expected.stream().filter(date -> !coveredSet.contains(date)).toList();
        int open = 0, resolved = 0, ignored = 0;
        List<PaymentReconciliationIssue> issues = ids.isEmpty()
            ? List.of() : issueMapper.selectByReconciliationBatches(tenantId, ids);
        for (PaymentReconciliationIssue issue : issues) {
            if (!currencyCode.equals(issue.getProviderCurrencyCode())
                && !currencyCode.equals(issue.getPlatformCurrencyCode())) continue;
            if ("OPEN".equals(issue.getStatus())) open++;
            else if ("RESOLVED".equals(issue.getStatus())) resolved++;
            else if ("IGNORED".equals(issue.getStatus())) ignored++;
        }
        return new SettlementReconciliationEvidence(ids, List.copyOf(coveredSet), missing,
            open, resolved, ignored);
    }

    private static LocalDate statementDate(PaymentReconciliationBatch batch) {
        if (batch.getStatementDate() instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return batch.getStatementDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
