package com.gameluck.wallet.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.vo.WalletAccountVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Wallet account mapper.
 */
public interface WalletAccountMapper extends BaseMapperPlus<WalletAccount, WalletAccountVo> {

    WalletAccount selectByBizKeyForUpdate(@Param("tenantId") String tenantId,
                                          @Param("memberId") Long memberId,
                                          @Param("currencyCode") String currencyCode);

    List<WalletAccount> selectClientAccounts(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);
}
