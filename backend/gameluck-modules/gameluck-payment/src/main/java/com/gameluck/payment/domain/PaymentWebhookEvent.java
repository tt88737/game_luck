package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("gl_payment_webhook_event")
public class PaymentWebhookEvent {

    @TableId(value = "id")
    private Long id;

    private String tenantId;
    private String providerCode;
    private String providerEventId;
    private String eventType;
    private String providerSessionNo;
    private String sessionNo;
    private String purchaseOrderNo;
    private String rawBody;
    private String signatureDigest;
    private Date receivedTime;
    private String status;
    private String failureReason;
    private Integer processingCount;
    private Date lastProcessingTime;
    private Date createTime;
    private Date updateTime;
}
