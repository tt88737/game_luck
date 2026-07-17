package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletExchangeRuleBo;
import com.gameluck.wallet.domain.vo.WalletExchangeRuleVo;
import com.gameluck.wallet.service.IWalletExchangeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet exchange rule admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/exchange-rule")
public class WalletExchangeRuleController extends BaseController {

    private final IWalletExchangeRuleService walletExchangeRuleService;

    @SaCheckPermission("wallet:exchangeRule:list")
    @GetMapping("/list")
    public TableDataInfo<WalletExchangeRuleVo> list(WalletExchangeRuleBo bo, PageQuery pageQuery) {
        return walletExchangeRuleService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:exchangeRule:query")
    @GetMapping("/{id}")
    public R<WalletExchangeRuleVo> getInfo(@PathVariable Long id) {
        return R.ok(walletExchangeRuleService.queryById(id));
    }

    @SaCheckPermission("wallet:exchangeRule:add")
    @Log(title = "钱包兑换规则", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody WalletExchangeRuleBo bo) {
        return toAjax(walletExchangeRuleService.insertByBo(bo));
    }

    @SaCheckPermission("wallet:exchangeRule:edit")
    @Log(title = "钱包兑换规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody WalletExchangeRuleBo bo) {
        return toAjax(walletExchangeRuleService.updateByBo(bo));
    }
}
