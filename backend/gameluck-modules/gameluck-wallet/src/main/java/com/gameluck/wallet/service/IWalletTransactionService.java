package com.gameluck.wallet.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.wallet.domain.bo.WalletTransactionBo;
import com.gameluck.wallet.domain.vo.WalletTransactionVo;

import java.util.List;

/**
 * Wallet transaction service.
 */
public interface IWalletTransactionService {

    TableDataInfo<WalletTransactionVo> queryPageList(WalletTransactionBo bo, PageQuery pageQuery);

    WalletTransactionVo queryById(Long id);

    List<WalletTransactionVo> queryList(WalletTransactionBo bo);
}
