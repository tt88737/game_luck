package com.gameluck.payment.service.settlement;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

public record SettlementReconciliationEvidence(
    List<Long> completedBatchIds,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") List<LocalDate> coveredDates,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") List<LocalDate> missingDates,
    int openIssueCount,
    int resolvedIssueCount,
    int ignoredIssueCount
) { }
