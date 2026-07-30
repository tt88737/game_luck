package com.gameluck.payment.service.reconciliation;

import com.gameluck.payment.enums.PaymentReconciliationIssueType;

import java.util.List;
import java.util.Optional;

public record ReconciliationMatchResult(
    boolean matched,
    Optional<PaymentReconciliationIssueType> primaryIssueType,
    List<ReconciliationDifference> differences,
    String diagnosticSnapshotJson
) {
    public ReconciliationMatchResult {
        primaryIssueType = primaryIssueType == null ? Optional.empty() : primaryIssueType;
        differences = List.copyOf(differences);
    }
}
