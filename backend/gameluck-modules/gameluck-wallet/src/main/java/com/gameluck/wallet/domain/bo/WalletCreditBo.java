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

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;

    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @NotBlank(message = "币种不能为空")
    private String currencyCode;

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @NotBlank(message = "业务单号不能为空")
    private String businessNo;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.000001", message = "金额必须大于0")
    private BigDecimal amount;

    private String releaseMode;

    @DecimalMin(value = "0", message = "所需流水不能小于0")
    private BigDecimal requiredTurnover;

    private Long operatorId;

    private String remark;
}
