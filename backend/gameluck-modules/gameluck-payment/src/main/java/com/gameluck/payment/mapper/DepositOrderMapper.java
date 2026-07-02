package com.gameluck.payment.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.payment.domain.DepositOrder;
import com.gameluck.payment.domain.vo.DepositOrderVo;
import org.apache.ibatis.annotations.Param;

/**
 * Deposit order mapper.
 */
public interface DepositOrderMapper extends BaseMapperPlus<DepositOrder, DepositOrderVo> {

    DepositOrder selectByIdForUpdate(@Param("id") Long id);
}
