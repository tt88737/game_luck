package com.gameluck.wallet.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Wallet valid turnover request.
 */
@Data
public class WalletTurnoverBo {

    @NotNull(message = "{member.id.required}")
    private Long memberId;

    @NotBlank(message = "{wallet.currency.required}")
    private String currencyCode;

    @NotBlank(message = "{wallet.source.type.required}")
    private String sourceType;

    @NotBlank(message = "{wallet.business.no.required}")
    private String businessNo;

    @NotBlank(message = "{wallet.idempotency.key.required}")
    private String idempotencyKey;

    @NotNull(message = "{wallet.valid.turnover.required}")
    @DecimalMin(value = "0.000001", message = "{wallet.valid.turnover.positive}")
    private BigDecimal validTurnoverAmount;
}
