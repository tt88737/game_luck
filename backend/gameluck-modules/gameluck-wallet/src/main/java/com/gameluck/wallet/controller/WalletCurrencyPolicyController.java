package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletCurrencyPolicyBo;
import com.gameluck.wallet.domain.vo.WalletCurrencyPolicyVo;
import com.gameluck.wallet.service.IWalletCurrencyPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/currency-policy")
public class WalletCurrencyPolicyController extends BaseController {

    private final IWalletCurrencyPolicyService walletCurrencyPolicyService;

    @SaCheckPermission("wallet:currencyPolicy:list")
    @GetMapping("/list")
    public TableDataInfo<WalletCurrencyPolicyVo> list(WalletCurrencyPolicyBo bo, PageQuery pageQuery) {
        return walletCurrencyPolicyService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:currencyPolicy:query")
    @GetMapping("/{id}")
    public R<WalletCurrencyPolicyVo> getInfo(@PathVariable Long id) {
        return R.ok(walletCurrencyPolicyService.queryById(id));
    }

    @SaCheckPermission("wallet:currencyPolicy:add")
    @Log(title = "钱包币种策略", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody WalletCurrencyPolicyBo bo) {
        return toAjax(walletCurrencyPolicyService.insertByBo(bo));
    }

    @SaCheckPermission("wallet:currencyPolicy:edit")
    @Log(title = "钱包币种策略", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody WalletCurrencyPolicyBo bo) {
        return toAjax(walletCurrencyPolicyService.updateByBo(bo));
    }
}
