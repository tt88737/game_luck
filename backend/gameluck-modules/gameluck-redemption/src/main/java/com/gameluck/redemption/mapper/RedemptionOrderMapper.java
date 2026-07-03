package com.gameluck.redemption.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.domain.vo.RedemptionOrderVo;
import org.apache.ibatis.annotations.Param;

/**
 * Redemption order mapper.
 */
public interface RedemptionOrderMapper extends BaseMapperPlus<RedemptionOrder, RedemptionOrderVo> {

    RedemptionOrder selectByIdForUpdate(@Param("id") Long id);
}
