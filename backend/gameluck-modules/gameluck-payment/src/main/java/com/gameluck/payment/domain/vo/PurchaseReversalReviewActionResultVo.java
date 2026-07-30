package com.gameluck.payment.domain.vo;

import lombok.Data;

@Data
public class PurchaseReversalReviewActionResultVo {
    private String operationType;
    private String dispositionStatus;
    private boolean completed;
    private PurchaseReversalReviewDetailVo detail;
}
