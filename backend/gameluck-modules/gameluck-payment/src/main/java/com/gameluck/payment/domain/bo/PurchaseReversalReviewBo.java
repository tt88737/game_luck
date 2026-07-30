package com.gameluck.payment.domain.bo;

import lombok.Data;

import java.util.Date;

@Data
public class PurchaseReversalReviewBo {
    private String reversalNo;
    private String purchaseOrderNo;
    private Long memberId;
    private String memberNo;
    private String reversalType;
    private String dispositionStatus;
    private Date beginTime;
    private Date endTime;
}
