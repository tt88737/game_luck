package com.gameluck.payment.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentWebhookEventDetailVo extends PaymentWebhookEventAdminVo {
    private String rawBody;

    /** One-way signature digest retained as evidence; never a provider verification secret. */
    private String signatureDigest;
}
