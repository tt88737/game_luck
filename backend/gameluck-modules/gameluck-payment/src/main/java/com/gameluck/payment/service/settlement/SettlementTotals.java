package com.gameluck.payment.service.settlement;

import java.math.BigDecimal;

public record SettlementTotals(
    int eventCount,
    int paymentCount,
    int refundCount,
    int chargebackCount,
    BigDecimal grossPayment,
    BigDecimal refundAmount,
    BigDecimal chargebackAmount,
    BigDecimal totalFee,
    BigDecimal netSettlement
) { }
