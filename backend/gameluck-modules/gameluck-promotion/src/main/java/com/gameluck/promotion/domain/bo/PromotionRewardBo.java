package com.gameluck.promotion.domain.bo;

import com.gameluck.promotion.domain.PromotionReward;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Promotion reward query and form object.
 */
@Data
@AutoMapper(target = PromotionReward.class, reverseConvertGenerate = false)
public class PromotionRewardBo {

    private Long id;

    private String tenantId;

    private String promotionNo;

    @NotBlank(message = "{promotion.reward.name.required}")
    private String promotionName;

    private String currencyCode;

    @NotNull(message = "{promotion.reward.amount.required}")
    @DecimalMin(value = "0.000001", message = "{promotion.reward.amount.positive}")
    private BigDecimal rewardAmount;

    private String status;

    private Date startTime;

    private Date endTime;

    private String remark;

    private Date beginTime;

    private Date endQueryTime;
}
