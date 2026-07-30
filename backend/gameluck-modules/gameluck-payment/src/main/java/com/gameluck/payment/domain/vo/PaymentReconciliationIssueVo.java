package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PaymentReconciliationIssueVo {
    private String id;
    private String batchId;
    private String lineId;
    private String issueType;
    private String status;
    private String paymentSessionId;
    private String sessionNo;
    private String purchaseOrderId;
    private String purchaseOrderNo;
    private String webhookEventId;
    private String reversalId;
    private String providerEventType;
    private String platformEventType;
    private String providerCurrencyCode;
    private String platformCurrencyCode;
    private String providerAmount;
    private String platformAmount;
    private String providerStatus;
    private String platformStatus;
    private String diagnosticSnapshotJson;
    private String resolutionType;
    private String resolutionRemark;
    private String resolvedBy;
    private Date resolvedTime;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
