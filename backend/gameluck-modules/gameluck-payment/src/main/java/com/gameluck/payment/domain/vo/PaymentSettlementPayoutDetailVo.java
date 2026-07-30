package com.gameluck.payment.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentSettlementPayoutDetailVo extends PaymentSettlementPayoutRowVo {
    private String settlementEvidenceJson;
    private List<PaymentSettlementPayoutActionLogVo> actionLogs;
}
