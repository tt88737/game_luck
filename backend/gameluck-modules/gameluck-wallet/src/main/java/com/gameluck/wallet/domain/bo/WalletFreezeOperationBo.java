package com.gameluck.wallet.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Wallet freeze, unfreeze, and settle operation request.
 */
@Data
public class WalletFreezeOperationBo {

    private String freezeNo;

    @NotNull
    private Long memberId;

    @NotBlank
    private String currencyCode;

    @DecimalMin("0.000001")
    private BigDecimal amount;

    @NotBlank
    private String sourceType;

    @NotBlank
    private String businessNo;

    @NotBlank
    private String idempotencyKey;

    private Long operatorId;

    private String remark;
}
