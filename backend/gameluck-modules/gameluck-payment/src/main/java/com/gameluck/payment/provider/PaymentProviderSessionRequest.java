package com.gameluck.payment.provider;

import java.math.BigDecimal;

public record PaymentProviderSessionRequest(
    Long tenantId,
    String purchaseOrderNo,
    String payCurrencyCode,
    BigDecimal payAmount,
    String requestKey
) {
}
