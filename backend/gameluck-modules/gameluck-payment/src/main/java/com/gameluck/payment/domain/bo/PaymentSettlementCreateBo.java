package com.gameluck.payment.domain.bo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentSettlementCreateBo {
    private String providerCode;
    private String currencyCode;
    private Date periodStart;
    private Date periodEnd;
    private BigDecimal paymentFeeRate;
    private BigDecimal paymentFixedFee;
    private BigDecimal chargebackFixedFee;
}
