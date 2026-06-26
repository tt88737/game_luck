package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletAccountBo;
import com.gameluck.wallet.domain.vo.WalletAccountVo;
import com.gameluck.wallet.service.IWalletAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet account admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/account")
public class WalletAccountController extends BaseController {

    private final IWalletAccountService walletAccountService;

    @SaCheckPermission("wallet:account:list")
    @GetMapping("/list")
    public TableDataInfo<WalletAccountVo> list(WalletAccountBo bo, PageQuery pageQuery) {
        return walletAccountService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:account:query")
    @GetMapping("/{id}")
    public R<WalletAccountVo> getInfo(@PathVariable Long id) {
        return R.ok(walletAccountService.queryById(id));
    }
}
