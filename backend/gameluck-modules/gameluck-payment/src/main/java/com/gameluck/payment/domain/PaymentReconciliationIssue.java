package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("gl_payment_reconciliation_issue")
public class PaymentReconciliationIssue {
    @TableId(value = "id")
    private Long id;
    private String tenantId;
    private Long batchId;
    private Long lineId;
    private String issueType;
    private String status;
    private Long paymentSessionId;
    private String sessionNo;
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private Long webhookEventId;
    private Long reversalId;
    private String providerEventType;
    private String platformEventType;
    private String providerCurrencyCode;
    private String platformCurrencyCode;
    private BigDecimal providerAmount;
    private BigDecimal platformAmount;
    private String providerStatus;
    private String platformStatus;
    private String diagnosticSnapshotJson;
    private String resolutionType;
    private String resolutionRemark;
    private Long resolvedBy;
    private Date resolvedTime;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
