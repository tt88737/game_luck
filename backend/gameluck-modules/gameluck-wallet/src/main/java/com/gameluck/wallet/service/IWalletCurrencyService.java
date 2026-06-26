package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.bo.WalletCurrencyBo;
import com.gameluck.wallet.domain.vo.WalletCurrencyVo;

import java.util.List;

/**
 * Wallet currency service.
 */
public interface IWalletCurrencyService {

    TableDataInfo<WalletCurrencyVo> queryPageList(WalletCurrencyBo bo, PageQuery pageQuery);

    WalletCurrencyVo queryById(Long id);

    List<WalletCurrencyVo> queryList(WalletCurrencyBo bo);

    int updateByBo(WalletCurrencyBo bo);
}
