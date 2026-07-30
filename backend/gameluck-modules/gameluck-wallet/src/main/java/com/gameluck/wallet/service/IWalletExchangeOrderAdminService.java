package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.bo.WalletExchangeOrderBo;
import com.gameluck.wallet.domain.vo.WalletExchangeOrderVo;

import java.util.List;

/**
 * Wallet exchange order admin query service.
 */
public interface IWalletExchangeOrderAdminService {

    TableDataInfo<WalletExchangeOrderVo> queryPageList(WalletExchangeOrderBo bo, PageQuery pageQuery);

    WalletExchangeOrderVo queryById(Long id);

    List<WalletExchangeOrderVo> queryList(WalletExchangeOrderBo bo);
}
