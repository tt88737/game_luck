package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PaymentReconciliationActionLogVo {
    private String id;
    private String batchId;
    private String issueId;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private String operatorId;
    private String operatorName;
    private String remark;
    private Date createTime;
}
