package com.gameluck.payment.domain.vo;

import com.gameluck.payment.enums.PaymentProviderEventType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public record SimulatedCheckoutVo(
    String sessionNo,
    String orderNo,
    String providerCode,
    String providerSessionNo,
    String payCurrencyCode,
    BigDecimal payAmount,
    String status,
    Date expireTime,
    String checkoutUrl,
    List<PaymentProviderEventType> allowedActions,
    String latestProviderEventId,
    String latestWebhookStatus
) {
}
