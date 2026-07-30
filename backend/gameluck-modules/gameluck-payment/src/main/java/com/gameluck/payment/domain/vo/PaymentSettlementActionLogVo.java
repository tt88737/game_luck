package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PaymentSettlementActionLogVo {
    private String id;
    private String batchId;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private String operatorId;
    private String operatorName;
    private String remark;
    private String evidenceSnapshotJson;
    private Date createTime;
}
