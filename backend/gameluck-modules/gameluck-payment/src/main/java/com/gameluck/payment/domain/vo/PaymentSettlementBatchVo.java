package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PaymentSettlementBatchVo {
    private String id;
    private String settlementNo;
    private String providerCode;
    private String currencyCode;
    private Date periodStart;
    private Date periodEnd;
    private String status;
    private String paymentFeeRate;
    private String paymentFixedFee;
    private String chargebackFixedFee;
    private Integer eventCount;
    private Integer paymentCount;
    private Integer refundCount;
    private Integer chargebackCount;
    private String grossPayment;
    private String refundAmount;
    private String chargebackAmount;
    private String totalFee;
    private String netSettlement;
    private Integer reconciliationCoverageCount;
    private Integer openIssueCount;
    private String failureReason;
    private String creatorId;
    private String creatorName;
    private String calculatorId;
    private String calculatorName;
    private String closerId;
    private String closerName;
    private String closeRemark;
    private Date calculatedTime;
    private Date closedTime;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
