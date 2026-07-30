package com.gameluck.payment.domain.bo;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.Date;

@Data
public class PaymentSessionAdminBo {
    @Size(max = 64, message = "{payment.provider.admin.filter.length.invalid}") private String sessionNo;
    @Size(max = 64, message = "{payment.provider.admin.filter.length.invalid}") private String purchaseOrderNo;
    @Size(max = 128, message = "{payment.provider.admin.filter.length.invalid}") private String providerSessionNo;
    @Positive(message = "{payment.provider.admin.member.id.invalid}") private Long memberId;
    @Size(max = 64, message = "{payment.provider.admin.filter.length.invalid}") private String memberNo;
    @Size(max = 64, message = "{payment.provider.admin.filter.length.invalid}")
    @Pattern(regexp = "^[A-Z0-9_]*$", message = "{payment.provider.admin.filter.format.invalid}") private String providerCode;
    @Pattern(regexp = "^(|CREATED|PENDING|SUCCEEDED|FAILED|CANCELLED|EXPIRED)$",
        message = "{payment.provider.admin.filter.enum.invalid}") private String status;
    @Size(max = 32, message = "{payment.provider.admin.filter.length.invalid}")
    @Pattern(regexp = "^[A-Z0-9_]*$", message = "{payment.provider.admin.filter.format.invalid}") private String payCurrencyCode;
    private Date beginTime;
    private Date endTime;

    @AssertTrue(message = "{payment.provider.admin.time.range.invalid}")
    public boolean isTimeRangeValid() { return beginTime == null || endTime == null || !beginTime.after(endTime); }
}
