package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("gl_payment_reconciliation_batch")
public class PaymentReconciliationBatch {
    @TableId(value = "id")
    private Long id;
    private String tenantId;
    private String providerCode;
    private Date statementDate;
    private String originalFileName;
    private String fileDigest;
    private Integer totalCount;
    private Integer validCount;
    private Integer invalidCount;
    private Integer matchedCount;
    private Integer discrepancyCount;
    private String status;
    private String failureReason;
    private Long creatorId;
    private String creatorName;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
