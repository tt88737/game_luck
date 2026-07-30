package com.gameluck.payment.domain.bo;

import lombok.Data;

@Data
public class PaymentSettlementPayoutCommandBo {
    private Integer version;
    private String reason;
}
