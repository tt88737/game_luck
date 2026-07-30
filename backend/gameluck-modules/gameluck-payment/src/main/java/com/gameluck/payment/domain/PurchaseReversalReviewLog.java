package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("gl_purchase_reversal_review_log")
public class PurchaseReversalReviewLog {

    @TableId(value = "id")
    private Long id;

    private String tenantId;
    private String operationNo;
    private Long reversalId;
    private String reversalNo;
    private String requestKey;
    private String operationType;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private String reviewNote;
    private String snapshotJson;
    private Date createTime;
}
