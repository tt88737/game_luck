package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOptionVo;
import com.gameluck.wallet.domain.bo.WalletExchangeRuleBo;
import com.gameluck.wallet.domain.vo.WalletExchangeRuleVo;

import java.util.List;

/**
 * Wallet exchange rule service.
 */
public interface IWalletExchangeRuleService {

    TableDataInfo<WalletExchangeRuleVo> queryPageList(WalletExchangeRuleBo bo, PageQuery pageQuery);

    WalletExchangeRuleVo queryById(Long id);

    List<WalletExchangeRuleVo> queryList(WalletExchangeRuleBo bo);

    int insertByBo(WalletExchangeRuleBo bo);

    int updateByBo(WalletExchangeRuleBo bo);

    List<ClientExchangeOptionVo> listOptions(Long memberId, String channel);
}
