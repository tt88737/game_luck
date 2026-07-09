package com.gameluck.redemption.client.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClientRedemptionRequestBo {

    @NotNull(message = "{client.redemption.currency.required}")
    private String currencyCode;

    @NotNull(message = "{client.redemption.amount.required}")
    @DecimalMin(value = "0.000001", message = "{client.redemption.amount.positive}")
    private BigDecimal amount;
}
