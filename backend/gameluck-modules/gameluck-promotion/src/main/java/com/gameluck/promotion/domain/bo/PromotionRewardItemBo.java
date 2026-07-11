package com.gameluck.promotion.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionRewardItemBo {

    @NotBlank(message = "{promotion.reward.currency.required}")
    private String currencyCode;

    @NotNull(message = "{promotion.reward.amount.required}")
    @DecimalMin(value = "0.000001", message = "{promotion.reward.amount.positive}")
    private BigDecimal rewardAmount;
}
