package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.bo.WalletReleaseBo;
import com.gameluck.wallet.domain.vo.WalletReleaseVo;

import java.util.List;

/**
 * Wallet release service.
 */
public interface IWalletReleaseService {

    TableDataInfo<WalletReleaseVo> queryPageList(WalletReleaseBo bo, PageQuery pageQuery);

    WalletReleaseVo queryById(Long id);

    List<WalletReleaseVo> queryList(WalletReleaseBo bo);
}
