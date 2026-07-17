package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.client.domain.vo.ClientWalletCurrencyVo;
import com.gameluck.wallet.domain.bo.WalletCurrencyPolicyBo;
import com.gameluck.wallet.domain.vo.WalletCurrencyPolicyVo;

import java.util.List;

/**
 * Wallet currency visibility and action policy service.
 */
public interface IWalletCurrencyPolicyService {

    List<ClientWalletCurrencyVo> listClientCurrencies(String tenantId, Long memberId, String channel);

    TableDataInfo<WalletCurrencyPolicyVo> queryPageList(WalletCurrencyPolicyBo bo, PageQuery pageQuery);

    WalletCurrencyPolicyVo queryById(Long id);

    int insertByBo(WalletCurrencyPolicyBo bo);

    int updateByBo(WalletCurrencyPolicyBo bo);
}
