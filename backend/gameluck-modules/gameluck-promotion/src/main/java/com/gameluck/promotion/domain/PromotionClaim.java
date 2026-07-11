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

/**
 * Promotion claim gl_promotion_claim.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_promotion_claim")
public class PromotionClaim extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String claimNo;

    private Long promotionId;

    private String promotionNo;

    private String promotionName;

    private String promotionType;

    private Long memberId;

    private String currencyCode;

    private BigDecimal rewardAmount;

    private java.time.LocalDate claimDate;

    private String rewardSnapshot;

    private String status;

    private String walletTransactionNo;

    private String idempotencyKey;

    private String failReason;

    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
