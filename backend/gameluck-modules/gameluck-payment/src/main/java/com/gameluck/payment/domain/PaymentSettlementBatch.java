package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("gl_payment_settlement_batch")
public class PaymentSettlementBatch {
    @TableId(value = "id") private Long id;
    private String tenantId;
    private String settlementNo;
    private String providerCode;
    private String currencyCode;
    private Date periodStart;
    private Date periodEnd;
    private String status;
    private BigDecimal paymentFeeRate;
    private BigDecimal paymentFixedFee;
    private BigDecimal chargebackFixedFee;
    private Integer eventCount;
    private Integer paymentCount;
    private Integer refundCount;
    private Integer chargebackCount;
    private BigDecimal grossPayment;
    private BigDecimal refundAmount;
    private BigDecimal chargebackAmount;
    private BigDecimal totalFee;
    private BigDecimal netSettlement;
    private Integer reconciliationCoverageCount;
    private Integer openIssueCount;
    private String evidenceSnapshotJson;
    private String failureReason;
    private Long creatorId;
    private String creatorName;
    private Long calculatorId;
    private String calculatorName;
    private Long closerId;
    private String closerName;
    private String closeRemark;
    private Date calculatedTime;
    private Date closedTime;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
