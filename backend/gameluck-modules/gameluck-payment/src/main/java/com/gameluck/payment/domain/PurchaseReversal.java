package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Purchase asset reversal case.
 */
@Data
@TableName("gl_purchase_reversal")
public class PurchaseReversal {

    @TableId(value = "id")
    private Long id;

    private String tenantId;
    private String reversalNo;
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private Long memberId;
    private String eventKey;
    private String reversalType;
    private String status;
    private String reason;
    private String reviewReason;
    private String dispositionStatus;
    private Long reviewedBy;
    private String reviewedName;
    private String reviewNote;
    private Date resolvedTime;
    private Integer retryCount;
    private Date lastRetryTime;
    private Integer version;
    private Date completedTime;
    private Date createTime;
    private Date updateTime;
}
