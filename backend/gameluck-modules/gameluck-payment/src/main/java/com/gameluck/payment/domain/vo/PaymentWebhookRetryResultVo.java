package com.gameluck.payment.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class PaymentWebhookRetryResultVo {
    private Long eventId;
    private String providerEventId;
    private String status;
    private Integer processingCount;
    private Date lastProcessingTime;
    private String failureReason;
}
