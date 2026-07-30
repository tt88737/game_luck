package com.gameluck.payment.domain.bo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class PaymentSettlementPayoutEditBo {
    @NotNull
    private Integer version;
    @NotBlank
    @Size(max = 500)
    private String payoutPurpose;
    @NotBlank
    @Size(max = 128)
    private String payeeReference;
}
