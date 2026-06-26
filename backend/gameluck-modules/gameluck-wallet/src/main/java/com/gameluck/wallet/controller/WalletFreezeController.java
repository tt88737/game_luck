package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletFreezeBo;
import com.gameluck.wallet.domain.vo.WalletFreezeVo;
import com.gameluck.wallet.service.IWalletFreezeService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet freeze admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/freeze")
public class WalletFreezeController extends BaseController {

    private final IWalletFreezeService walletFreezeService;

    @SaCheckPermission("wallet:freeze:list")
    @GetMapping("/list")
    public TableDataInfo<WalletFreezeVo> list(WalletFreezeBo bo, PageQuery pageQuery) {
        return walletFreezeService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:freeze:query")
    @GetMapping("/{id}")
    public R<WalletFreezeVo> getInfo(@PathVariable Long id) {
        return R.ok(walletFreezeService.queryById(id));
    }
}
