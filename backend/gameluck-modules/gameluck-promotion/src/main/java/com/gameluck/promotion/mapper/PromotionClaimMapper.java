package com.gameluck.promotion.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import org.apache.ibatis.annotations.Param;

/**
 * Promotion claim mapper.
 */
public interface PromotionClaimMapper extends BaseMapperPlus<PromotionClaim, PromotionClaimVo> {

    PromotionClaim selectByPromotionAndMember(@Param("tenantId") String tenantId,
                                              @Param("promotionId") Long promotionId,
                                              @Param("memberId") Long memberId);
}
