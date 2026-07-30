package com.gameluck.payment.provider;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentProviderSessionResult(
    String providerSessionNo,
    String checkoutUrl,
    Instant expireTime,
    String purchaseOrderNo,
    String payCurrencyCode,
    BigDecimal payAmount
) {
}
