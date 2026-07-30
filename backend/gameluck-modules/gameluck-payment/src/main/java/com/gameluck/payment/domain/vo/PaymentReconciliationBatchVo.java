package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PaymentReconciliationBatchVo {
    private String id;
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
    private String creatorId;
    private String creatorName;
    private Date createTime;
    private Date updateTime;
}
