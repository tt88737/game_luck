package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("gl_purchase_payment_event")
public class PurchasePaymentEvent {

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String eventKey;

    private String purchaseOrderNo;

    private String providerCode;

    private String providerOrderNo;

    private String eventType;

    private String eventStatus;

    private String requestHash;

    private String requestBody;

    private String processResult;

    private Date processTime;

    private Date createTime;
}
