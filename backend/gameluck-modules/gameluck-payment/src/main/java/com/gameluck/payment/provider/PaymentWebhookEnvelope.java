package com.gameluck.payment.provider;

import com.gameluck.payment.enums.PaymentProviderEventType;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentWebhookEnvelope(
    Long tenantId,
    String providerEventId,
    PaymentProviderEventType eventType,
    String providerSessionNo,
    String purchaseOrderNo,
    String payCurrencyCode,
    BigDecimal payAmount,
    Instant occurredTime
) {
}
