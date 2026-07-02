package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.bo.WalletRuleBo;
import com.gameluck.wallet.domain.vo.WalletRuleVo;

import java.util.List;

/**
 * Wallet source rule service.
 */
public interface IWalletRuleService {

    TableDataInfo<WalletRuleVo> queryPageList(WalletRuleBo bo, PageQuery pageQuery);

    WalletRuleVo queryById(Long id);

    List<WalletRuleVo> queryList(WalletRuleBo bo);

    Boolean insertByBo(WalletRuleBo bo);

    Boolean updateByBo(WalletRuleBo bo);

    WalletRuleVo resolveCreditRule(String tenantId, String currencyCode, String sourceType);
}
