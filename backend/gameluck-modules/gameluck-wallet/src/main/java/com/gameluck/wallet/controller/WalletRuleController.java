package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletRuleBo;
import com.gameluck.wallet.domain.vo.WalletRuleTemplateVo;
import com.gameluck.wallet.domain.vo.WalletRuleVo;
import com.gameluck.wallet.service.IWalletRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Wallet source rule admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/rule")
public class WalletRuleController extends BaseController {

    private final IWalletRuleService walletRuleService;

    @SaCheckPermission("wallet:rule:list")
    @GetMapping("/list")
    public TableDataInfo<WalletRuleVo> list(WalletRuleBo bo, PageQuery pageQuery) {
        return walletRuleService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:rule:list")
    @GetMapping("/default/preview")
    public R<List<WalletRuleTemplateVo>> previewDefaultRules() {
        return R.ok(walletRuleService.previewMissingDefaultRules(TenantHelper.getTenantId()));
    }

    @SaCheckPermission("wallet:rule:seed")
    @Log(title = "钱包规则", businessType = BusinessType.INSERT)
    @PostMapping("/default/seed")
    public R<Integer> seedDefaultRules() {
        return R.ok(walletRuleService.seedMissingDefaultRules(TenantHelper.getTenantId()));
    }

    @SaCheckPermission("wallet:rule:query")
    @GetMapping("/{id}")
    public R<WalletRuleVo> getInfo(@PathVariable Long id) {
        return R.ok(walletRuleService.queryById(id));
    }

    @SaCheckPermission("wallet:rule:add")
    @Log(title = "钱包规则", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody WalletRuleBo bo) {
        return toAjax(walletRuleService.insertByBo(bo));
    }

    @SaCheckPermission("wallet:rule:edit")
    @Log(title = "钱包规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody WalletRuleBo bo) {
        return toAjax(walletRuleService.updateByBo(bo));
    }
}
