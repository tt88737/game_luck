package com.gameluck.payment.domain.bo;

import lombok.Data;

@Data
public class PaymentSettlementQueryBo {
    private String providerCode;
    private String currencyCode;
    private String status;
}
