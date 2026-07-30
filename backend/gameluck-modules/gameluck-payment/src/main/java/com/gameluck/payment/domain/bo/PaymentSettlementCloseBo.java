package com.gameluck.payment.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentSettlementCloseBo {
    @NotNull
    private Integer version;
    @NotBlank
    private String remark;
}
