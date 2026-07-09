package com.gameluck.wallet.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.vo.WalletTransactionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Wallet transaction mapper.
 */
public interface WalletTransactionMapper extends BaseMapperPlus<WalletTransaction, WalletTransactionVo> {

    WalletTransaction selectByIdempotencyKey(@Param("tenantId") String tenantId,
                                             @Param("idempotencyKey") String idempotencyKey);

    List<WalletTransaction> selectClientLedgers(@Param("tenantId") String tenantId,
                                                @Param("memberId") Long memberId,
                                                @Param("currencyCode") String currencyCode,
                                                @Param("offset") int offset,
                                                @Param("pageSize") int pageSize);

    Long countClientLedgers(@Param("tenantId") String tenantId,
                            @Param("memberId") Long memberId,
                            @Param("currencyCode") String currencyCode);
}
