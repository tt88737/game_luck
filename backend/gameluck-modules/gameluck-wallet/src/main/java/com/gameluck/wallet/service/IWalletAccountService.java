package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.bo.WalletAccountBo;
import com.gameluck.wallet.domain.vo.WalletAccountVo;

import java.util.List;

/**
 * Wallet account service.
 */
public interface IWalletAccountService {

    TableDataInfo<WalletAccountVo> queryPageList(WalletAccountBo bo, PageQuery pageQuery);

    WalletAccountVo queryById(Long id);

    List<WalletAccountVo> queryList(WalletAccountBo bo);
}
