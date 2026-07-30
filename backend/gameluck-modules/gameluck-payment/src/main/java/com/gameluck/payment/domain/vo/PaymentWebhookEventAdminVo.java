package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PaymentWebhookEventAdminVo {
    private Long id;
    private String providerCode;
    private String providerEventId;
    private String eventType;
    private String providerSessionNo;
    private String sessionNo;
    private String purchaseOrderNo;
    private Date receivedTime;
    private String status;
    private String failureReason;
    private Integer processingCount;
    private Date lastProcessingTime;
    private Date createTime;
    private Date updateTime;
}
