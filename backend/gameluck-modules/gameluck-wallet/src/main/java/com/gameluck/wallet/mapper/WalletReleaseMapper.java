package com.gameluck.wallet.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.wallet.domain.WalletRelease;
import com.gameluck.wallet.domain.vo.WalletReleaseVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Wallet release mapper.
 */
public interface WalletReleaseMapper extends BaseMapperPlus<WalletRelease, WalletReleaseVo> {

    List<WalletRelease> selectLockedByMemberForUpdate(@Param("tenantId") String tenantId,
                                                      @Param("memberId") Long memberId,
                                                      @Param("currencyCode") String currencyCode,
                                                      @Param("releaseStatus") String releaseStatus);
}
