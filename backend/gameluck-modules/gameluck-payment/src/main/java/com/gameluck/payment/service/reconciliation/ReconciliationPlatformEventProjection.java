package com.gameluck.payment.service.reconciliation;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Objects;

public record ReconciliationPlatformEventProjection(
    Long id,
    String providerEventId,
    String eventType,
    String providerSessionNo,
    String purchaseOrderNo,
    String currency,
    BigDecimal amount,
    Instant occurredTime,
    String status,
    Long paymentSessionId,
    Long purchaseOrderId,
    Long reversalId,
    Instant receivedTime
) {
    public ReconciliationPlatformEventProjection(Long id, String providerEventId, String eventType,
                                                  Instant receivedTime) {
        this(id, providerEventId, eventType, null, null, null, null, receivedTime, null,
            null, null, null, receivedTime);
    }
    public ReconciliationPlatformEventProjection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(providerEventId, "providerEventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(receivedTime, "receivedTime");
    }
}
