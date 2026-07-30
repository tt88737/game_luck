package com.gameluck.payment.service.reconciliation;

import java.math.BigDecimal;
import java.time.Instant;

public record ReconciliationParsedLine(
    long sourceLineNumber,
    String providerRecordId,
    String eventType,
    String providerSessionNo,
    String purchaseOrderNo,
    String currency,
    BigDecimal amount,
    Instant occurredTime,
    String sourceFieldsJson,
    Status status,
    String parseErrorCode
) {
    public enum Status {
        VALID,
        INVALID
    }
}
