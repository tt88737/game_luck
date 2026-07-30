package com.gameluck.wallet.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.wallet.domain.WalletExchangeOrder;
import com.gameluck.wallet.domain.vo.WalletExchangeOrderVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * Wallet exchange order mapper.
 */
public interface WalletExchangeOrderMapper extends BaseMapperPlus<WalletExchangeOrder, WalletExchangeOrderVo> {

    @Select("""
        SELECT COALESCE(SUM(from_amount), 0)
        FROM gl_wallet_exchange_order
        WHERE tenant_id = #{tenantId}
          AND member_id = #{memberId}
          AND exchange_rule_id = #{exchangeRuleId}
          AND status = 'SUCCESS'
          AND del_flag = '0'
          AND create_time >= CURRENT_DATE()
          AND create_time < DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY)
        """)
    BigDecimal sumSuccessFromAmountToday(@Param("tenantId") String tenantId,
                                         @Param("memberId") Long memberId,
                                         @Param("exchangeRuleId") Long exchangeRuleId);
}
