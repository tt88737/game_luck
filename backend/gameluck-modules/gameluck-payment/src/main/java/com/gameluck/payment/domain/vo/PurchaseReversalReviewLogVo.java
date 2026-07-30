package com.gameluck.payment.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class PurchaseReversalReviewLogVo {
    private String operationNo;
    private String requestKey;
    private String operationType;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private String reviewNote;
    private String snapshotJson;
    private Date createTime;
}
