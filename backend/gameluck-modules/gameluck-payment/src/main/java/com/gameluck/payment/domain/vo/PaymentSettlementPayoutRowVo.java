package com.gameluck.payment.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class PaymentSettlementPayoutRowVo {
    private String id;
    private String payoutNo;
    private String settlementBatchId;
    private String settlementNo;
    private String providerCode;
    private String currencyCode;
    private String payoutAmount;
    private String payoutPurpose;
    private String payeeReference;
    private String status;
    private String makerId;
    private String makerName;
    private String submitterId;
    private String submitterName;
    private String reviewerId;
    private String reviewerName;
    private String decisionReason;
    private Integer version;
    private Date submittedTime;
    private Date reviewedTime;
    private Date createTime;
    private Date updateTime;
}
