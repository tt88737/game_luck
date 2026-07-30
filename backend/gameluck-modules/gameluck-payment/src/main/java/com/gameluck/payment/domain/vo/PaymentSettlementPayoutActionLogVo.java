package com.gameluck.payment.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class PaymentSettlementPayoutActionLogVo {
    private String id;
    private String payoutId;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private String operatorId;
    private String operatorName;
    private String reason;
    private String evidenceSnapshotJson;
    private Integer expectedVersion;
    private Integer resultVersion;
    private Date createTime;
}
