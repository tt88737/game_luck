package com.gameluck.payment.domain.bo;

import lombok.Data;

@Data
public class PaymentSettlementPayoutCreateBo {
    private String settlementBatchId;
    private String payoutPurpose;
    private String payeeReference;
}
