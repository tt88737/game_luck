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

    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @NotBlank(message = "币种不能为空")
    private String currencyCode;

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @NotBlank(message = "业务单号不能为空")
    private String businessNo;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;

    @NotNull(message = "有效流水不能为空")
    @DecimalMin(value = "0.000001", message = "有效流水必须大于0")
    private BigDecimal validTurnoverAmount;
}
