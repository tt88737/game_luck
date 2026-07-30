package com.gameluck.payment.service.settlement;

import java.math.BigDecimal;
import java.time.Instant;

public record SettlementSourceEvent(
    Long webhookEventId,
    String providerCode,
    String providerEventId,
    String eventType,
    String webhookStatus,
    String providerSessionNo,
    Long sessionId,
    String sessionNo,
    String sessionPurchaseOrderNo,
    String sessionCurrencyCode,
    BigDecimal sessionAmount,
    Long orderId,
    String orderPurchaseOrderNo,
    String orderProviderCode,
    String orderCurrencyCode,
    BigDecimal orderAmount,
    Instant receivedTime
) { }
