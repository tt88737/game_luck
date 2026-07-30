package com.gameluck.payment.domain.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentReconciliationResolutionBo {
    @NotBlank
    private String resolutionType;
    @NotBlank
    private String remark;
    @NotNull
    @Min(0)
    private Integer expectedVersion;
}
