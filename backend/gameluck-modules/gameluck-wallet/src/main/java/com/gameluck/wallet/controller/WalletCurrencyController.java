package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletCurrencyBo;
import com.gameluck.wallet.domain.vo.WalletCurrencyVo;
import com.gameluck.wallet.service.IWalletCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet currency admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/currency")
public class WalletCurrencyController extends BaseController {

    private final IWalletCurrencyService walletCurrencyService;

    @SaCheckPermission("wallet:currency:list")
    @GetMapping("/list")
    public TableDataInfo<WalletCurrencyVo> list(WalletCurrencyBo bo, PageQuery pageQuery) {
        return walletCurrencyService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:currency:query")
    @GetMapping("/{id}")
    public R<WalletCurrencyVo> getInfo(@PathVariable Long id) {
        return R.ok(walletCurrencyService.queryById(id));
    }

    @SaCheckPermission("wallet:currency:edit")
    @Log(title = "钱包币种", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody WalletCurrencyBo bo) {
        return toAjax(walletCurrencyService.updateByBo(bo));
    }
}
