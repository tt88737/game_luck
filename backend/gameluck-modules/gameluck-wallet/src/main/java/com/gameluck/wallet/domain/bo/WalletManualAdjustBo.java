package com.gameluck.wallet.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Wallet manual adjustment request.
 */
@Data
public class WalletManualAdjustBo {

    @NotBlank(message = "{wallet.business.no.required}")
    private String adjustmentNo;

    @NotNull(message = "{member.id.required}")
    private Long memberId;

    @NotBlank(message = "{wallet.currency.required}")
    private String currencyCode;

    @NotNull(message = "{wallet.amount.required}")
    @DecimalMin(value = "0.000001", message = "{wallet.manual.adjust.amount.positive}")
    private BigDecimal amount;

    private String strategy;

    private Boolean turnoverRequired;

    private String turnoverMode;

    @DecimalMin(value = "0", message = "{wallet.required.turnover.nonnegative}")
    private BigDecimal requiredTurnover;

    @DecimalMin(value = "0", message = "{wallet.required.turnover.nonnegative}")
    private BigDecimal turnoverMultiplier;

    @NotBlank(message = "{wallet.manual.adjust.reason.required}")
    private String reason;
}
