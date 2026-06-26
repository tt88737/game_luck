package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletTransactionBo;
import com.gameluck.wallet.domain.vo.WalletTransactionVo;
import com.gameluck.wallet.service.IWalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet transaction admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/transaction")
public class WalletTransactionController extends BaseController {

    private final IWalletTransactionService walletTransactionService;

    @SaCheckPermission("wallet:transaction:list")
    @GetMapping("/list")
    public TableDataInfo<WalletTransactionVo> list(WalletTransactionBo bo, PageQuery pageQuery) {
        return walletTransactionService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:transaction:query")
    @GetMapping("/{id}")
    public R<WalletTransactionVo> getInfo(@PathVariable Long id) {
        return R.ok(walletTransactionService.queryById(id));
    }
}
