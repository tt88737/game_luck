package com.gameluck.wallet.client.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Client exchange order request.
 */
@Data
public class ClientExchangeOrderBo {

    @NotNull(message = "{wallet.exchange.rule.id.required}")
    private Long exchangeRuleId;

    @NotNull(message = "{wallet.exchange.amount.required}")
    @DecimalMin(value = "0.000001", message = "{wallet.exchange.amount.positive}")
    private BigDecimal fromAmount;

    private String idempotencyKey;
}
