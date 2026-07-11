package com.gameluck.wallet.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Wallet credit request.
 */
@Data
public class WalletCreditBo {

    @NotBlank(message = "{wallet.idempotency.key.required}")
    private String idempotencyKey;

    @NotNull(message = "{member.id.required}")
    private Long memberId;

    @NotBlank(message = "{wallet.currency.required}")
    private String currencyCode;

    @NotBlank(message = "{wallet.source.type.required}")
    private String sourceType;

    @NotBlank(message = "{wallet.business.no.required}")
    private String businessNo;

    @NotNull(message = "{wallet.amount.required}")
    @DecimalMin(value = "0.000001", message = "{wallet.amount.positive}")
    private BigDecimal amount;

    private String releaseMode;

    @DecimalMin(value = "0", message = "{wallet.required.turnover.nonnegative}")
    private BigDecimal requiredTurnover;

    private Boolean manualAdjustOverride;

    private Long operatorId;

    private String remark;
}
