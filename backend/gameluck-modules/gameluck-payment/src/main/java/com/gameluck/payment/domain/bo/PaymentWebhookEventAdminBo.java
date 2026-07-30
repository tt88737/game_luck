package com.gameluck.payment.domain.bo;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.Date;

@Data
public class PaymentWebhookEventAdminBo {
    @Size(max = 128, message = "{payment.provider.admin.filter.length.invalid}") private String providerEventId;
    @Size(max = 64, message = "{payment.provider.admin.filter.length.invalid}") private String purchaseOrderNo;
    @Size(max = 64, message = "{payment.provider.admin.filter.length.invalid}") private String sessionNo;
    @Size(max = 128, message = "{payment.provider.admin.filter.length.invalid}") private String providerSessionNo;
    @Pattern(regexp = "^(|PAYMENT_SUCCEEDED|PAYMENT_FAILED|PAYMENT_CANCELLED|REFUND_SUCCEEDED|CHARGEBACK_CREATED)$",
        message = "{payment.provider.admin.filter.enum.invalid}") private String eventType;
    @Pattern(regexp = "^(|RECEIVED|PROCESSED|FAILED|IGNORED)$",
        message = "{payment.provider.admin.filter.enum.invalid}") private String status;
    @Size(max = 64, message = "{payment.provider.admin.filter.length.invalid}")
    @Pattern(regexp = "^[A-Z0-9_]*$", message = "{payment.provider.admin.filter.format.invalid}") private String providerCode;
    private Date beginTime;
    private Date endTime;

    @AssertTrue(message = "{payment.provider.admin.time.range.invalid}")
    public boolean isTimeRangeValid() { return beginTime == null || endTime == null || !beginTime.after(endTime); }
}
