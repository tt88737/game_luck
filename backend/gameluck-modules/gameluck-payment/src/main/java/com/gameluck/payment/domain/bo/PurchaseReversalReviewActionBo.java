package com.gameluck.payment.domain.bo;

import lombok.Data;

@Data
public class PurchaseReversalReviewActionBo {
    private String requestKey;
    private String reviewNote;
}
