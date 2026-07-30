package com.gameluck.payment.service.reconciliation;

import java.util.List;

public record ReconciliationParseResult(
    String sha256Digest,
    long totalCount,
    long validCount,
    long invalidCount,
    List<ReconciliationParsedLine> lines,
    String fileErrorCode
) {
    public ReconciliationParseResult {
        lines = List.copyOf(lines);
    }
}
