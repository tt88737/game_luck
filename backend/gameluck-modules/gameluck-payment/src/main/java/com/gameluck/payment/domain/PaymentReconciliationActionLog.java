package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("gl_payment_reconciliation_action_log")
public class PaymentReconciliationActionLog {
    @TableId(value = "id")
    private Long id;
    private String tenantId;
    private Long batchId;
    private Long issueId;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private Date createTime;
}
