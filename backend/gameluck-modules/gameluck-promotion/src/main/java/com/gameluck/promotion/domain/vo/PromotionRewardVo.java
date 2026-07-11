package com.gameluck.promotion.domain.vo;

import com.gameluck.promotion.domain.PromotionReward;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Promotion reward view object.
 */
@Data
@AutoMapper(target = PromotionReward.class)
public class PromotionRewardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String promotionNo;

    private String promotionName;

    private String promotionType;

    private String currencyCode;

    private BigDecimal rewardAmount;

    private String claimCycle;

    private Integer dailyClaimLimit;

    private String rewardItems;

    private String status;

    private Date startTime;

    private Date endTime;

    private String remark;

    private Date createTime;

    private Date updateTime;
}
