package com.gameluck.payment.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PurchaseReversalReviewVo {
    private Long id;
    private String reversalNo;
    private String purchaseOrderNo;
    private Long memberId;
    private String memberNo;
    private String reversalType;
    private String status;
    private String dispositionStatus;
    private String reason;
    private String reviewReason;
    private String riskLevel;
    private Integer retryCount;
    private Date lastRetryTime;
    private Date resolvedTime;
    private Date createTime;
    private List<PurchaseReversalItemVo> items = new ArrayList<>();
}
