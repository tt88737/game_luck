package com.gameluck.payment.domain.bo;

import lombok.Data;

@Data
public class PaymentSettlementPayoutEditBo {
    private Integer version;
    private String payoutPurpose;
    private String payeeReference;
}
