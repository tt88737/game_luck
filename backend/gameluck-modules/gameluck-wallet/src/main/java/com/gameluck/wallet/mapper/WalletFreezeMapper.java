package com.gameluck.wallet.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.wallet.domain.WalletFreeze;
import com.gameluck.wallet.domain.vo.WalletFreezeVo;
import org.apache.ibatis.annotations.Param;

/**
 * Wallet freeze mapper.
 */
public interface WalletFreezeMapper extends BaseMapperPlus<WalletFreeze, WalletFreezeVo> {

    WalletFreeze selectByFreezeNoForUpdate(@Param("tenantId") String tenantId,
                                           @Param("freezeNo") String freezeNo);
}
