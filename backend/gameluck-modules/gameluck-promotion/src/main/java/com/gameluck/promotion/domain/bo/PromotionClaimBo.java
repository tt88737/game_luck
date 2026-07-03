package com.gameluck.promotion.domain.bo;

import com.gameluck.promotion.domain.PromotionClaim;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * Promotion claim query and form object.
 */
@Data
@AutoMapper(target = PromotionClaim.class, reverseConvertGenerate = false)
public class PromotionClaimBo {

    private Long id;

    private String tenantId;

    private String claimNo;

    private Long promotionId;

    private String promotionNo;

    @NotNull(message = "memberId is required")
    private Long memberId;

    private String currencyCode;

    private String status;

    private String remark;

    private Date beginTime;

    private Date endTime;
}
