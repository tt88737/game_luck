package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.bo.WalletFreezeBo;
import com.gameluck.wallet.domain.vo.WalletFreezeVo;

import java.util.List;

/**
 * Wallet freeze service.
 */
public interface IWalletFreezeService {

    TableDataInfo<WalletFreezeVo> queryPageList(WalletFreezeBo bo, PageQuery pageQuery);

    WalletFreezeVo queryById(Long id);

    List<WalletFreezeVo> queryList(WalletFreezeBo bo);
}
