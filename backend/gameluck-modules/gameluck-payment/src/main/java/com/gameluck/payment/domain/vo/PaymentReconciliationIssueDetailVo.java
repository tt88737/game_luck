package com.gameluck.payment.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentReconciliationIssueDetailVo extends PaymentReconciliationIssueVo {
    private List<PaymentReconciliationActionLogVo> actionLogs;
    private Long sourceRowNumber;
    private PaymentReconciliationLineVo sourceLine;
    private String canonicalOriginalFields;
    private boolean platformOnly;
}
