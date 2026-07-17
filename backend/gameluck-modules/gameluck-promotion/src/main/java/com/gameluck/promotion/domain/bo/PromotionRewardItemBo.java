package com.gameluck.promotion.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromotionRewardItemBo {

    @NotBlank(message = "{promotion.reward.currency.required}")
    private String currencyCode;

    @NotNull(message = "{promotion.reward.amount.required}")
    @DecimalMin(value = "0.000001", message = "{promotion.reward.amount.positive}")
    private BigDecimal rewardAmount;

    private String fundPropertyCode;

    @DecimalMin(value = "0", message = "{wallet.required.turnover.nonnegative}")
    private BigDecimal turnoverMultiplier;

    @DecimalMin(value = "0", message = "{wallet.required.turnover.nonnegative}")
    private BigDecimal turnoverRequiredAmount;

    private String gameScopeType;

    private String gameScopeValue;

    private Integer turnoverExpireDays;
}
