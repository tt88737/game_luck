package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("gl_payment_settlement_payout_action_log")
public class PaymentSettlementPayoutActionLog {
    @TableId(value = "id") private Long id;
    private String tenantId;
    private Long payoutId;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private String reason;
    private String evidenceSnapshotJson;
    private Integer expectedVersion;
    private Integer resultVersion;
    private Date createTime;
}
