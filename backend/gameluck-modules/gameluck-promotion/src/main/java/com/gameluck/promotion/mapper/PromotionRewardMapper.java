package com.gameluck.promotion.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.promotion.domain.PromotionReward;
import com.gameluck.promotion.domain.vo.PromotionRewardVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Promotion reward mapper.
 */
public interface PromotionRewardMapper extends BaseMapperPlus<PromotionReward, PromotionRewardVo> {

    PromotionReward selectByIdForUpdate(@Param("id") Long id);

    PromotionReward selectActiveDailyLoginReward(@Param("tenantId") String tenantId);

    List<PromotionReward> selectClientActiveRewards(@Param("tenantId") String tenantId);
}
