package com.gameluck.promotion.domain.vo;

import com.gameluck.promotion.domain.PromotionClaim;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * Promotion claim view object.
 */
@Data
@AutoMapper(target = PromotionClaim.class)
public class PromotionClaimVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private LocalDate claimDate;

    private String rewardSnapshot;

    private String status;

    private String walletTransactionNo;

    private String idempotencyKey;

    private String failReason;

    private String remark;

    private Date createTime;

    private Date updateTime;
}
