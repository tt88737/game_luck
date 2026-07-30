package com.gameluck.payment.service.settlement;

import java.math.BigDecimal;
import java.time.Instant;

public record SettlementItemDraft(
    Long webhookEventId,
    String providerEventId,
    Long paymentSessionId,
    String sessionNo,
    String providerSessionNo,
    Long purchaseOrderId,
    String purchaseOrderNo,
    String eventType,
    Instant receivedTime,
    String currencyCode,
    BigDecimal sourceAmount,
    BigDecimal grossPayment,
    BigDecimal refundAmount,
    BigDecimal chargebackAmount,
    BigDecimal feeAmount,
    BigDecimal netContribution,
    String sourceSnapshotJson
) { }
