package com.gameluck.payment.domain.bo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class PaymentSettlementPayoutCreateBo {
    @NotBlank
    private String settlementBatchId;
    @NotBlank
    @Size(max = 500)
    private String payoutPurpose;
    @NotBlank
    @Size(max = 128)
    private String payeeReference;
}
