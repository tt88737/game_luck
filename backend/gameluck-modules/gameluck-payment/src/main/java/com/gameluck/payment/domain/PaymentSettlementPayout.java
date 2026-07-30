package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("gl_payment_settlement_payout")
public class PaymentSettlementPayout {
    @TableId(value = "id") private Long id;
    private String tenantId;
    private String payoutNo;
    private Long settlementBatchId;
    private String settlementNo;
    private String providerCode;
    private String currencyCode;
    private BigDecimal payoutAmount;
    private String settlementEvidenceJson;
    private String payoutPurpose;
    private String payeeReference;
    private String status;
    private Long makerId;
    private String makerName;
    private Long submitterId;
    private String submitterName;
    private Long reviewerId;
    private String reviewerName;
    private String decisionReason;
    private Integer version;
    private Date submittedTime;
    private Date reviewedTime;
    private Date createTime;
    private Date updateTime;
}
