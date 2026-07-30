package com.gameluck.wallet.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.wallet.domain.WalletTurnoverTask;
import com.gameluck.wallet.domain.vo.WalletTurnoverTaskVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Wallet turnover task mapper.
 */
public interface WalletTurnoverTaskMapper extends BaseMapperPlus<WalletTurnoverTask, WalletTurnoverTaskVo> {

    List<WalletTurnoverTask> selectPendingByMemberForUpdate(@Param("tenantId") String tenantId,
                                                            @Param("memberId") Long memberId,
                                                            @Param("currencyCode") String currencyCode,
                                                            @Param("status") String status);

    int cancelPendingByPurchase(@Param("tenantId") String tenantId,
                                @Param("memberId") Long memberId,
                                @Param("businessNo") String businessNo,
                                @Param("pendingStatus") String pendingStatus,
                                @Param("cancelledStatus") String cancelledStatus,
                                @Param("remark") String remark,
                                @Param("now") java.util.Date now);
}
