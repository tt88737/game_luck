package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("gl_simulated_payment_dispatch")
public class SimulatedPaymentDispatch {

    @TableId(value = "id")
    private Long id;
    private String tenantId;
    private String providerSessionNo;
    private String providerEventId;
    private String action;
    private Date occurredTime;
    private Date createTime;
}
