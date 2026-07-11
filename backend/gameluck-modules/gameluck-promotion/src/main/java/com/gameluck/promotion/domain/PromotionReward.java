package com.gameluck.promotion.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Promotion reward gl_promotion_reward.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_promotion_reward")
public class PromotionReward extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
