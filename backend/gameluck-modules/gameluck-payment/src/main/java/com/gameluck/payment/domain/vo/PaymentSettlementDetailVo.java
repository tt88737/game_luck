package com.gameluck.payment.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentSettlementDetailVo extends PaymentSettlementBatchVo {
    private String evidenceSnapshotJson;
    private List<PaymentSettlementActionLogVo> actionLogs;
}
