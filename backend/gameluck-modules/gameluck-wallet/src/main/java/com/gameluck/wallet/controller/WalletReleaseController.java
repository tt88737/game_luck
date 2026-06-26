package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletReleaseBo;
import com.gameluck.wallet.domain.vo.WalletReleaseVo;
import com.gameluck.wallet.service.IWalletReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet release admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/release")
public class WalletReleaseController extends BaseController {

    private final IWalletReleaseService walletReleaseService;

    @SaCheckPermission("wallet:release:list")
    @GetMapping("/list")
    public TableDataInfo<WalletReleaseVo> list(WalletReleaseBo bo, PageQuery pageQuery) {
        return walletReleaseService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:release:query")
    @GetMapping("/{id}")
    public R<WalletReleaseVo> getInfo(@PathVariable Long id) {
        return R.ok(walletReleaseService.queryById(id));
    }
}
