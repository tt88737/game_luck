package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletFundPropertyTemplateBo;
import com.gameluck.wallet.domain.vo.WalletFundPropertyTemplateVo;
import com.gameluck.wallet.service.IWalletFundPropertyTemplateService;
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
@RequestMapping("/wallet/fund-property")
public class WalletFundPropertyTemplateController extends BaseController {

    private final IWalletFundPropertyTemplateService walletFundPropertyTemplateService;

    @SaCheckPermission("wallet:fundProperty:list")
    @GetMapping("/list")
    public TableDataInfo<WalletFundPropertyTemplateVo> list(WalletFundPropertyTemplateBo bo, PageQuery pageQuery) {
        return walletFundPropertyTemplateService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:fundProperty:query")
    @GetMapping("/{id}")
    public R<WalletFundPropertyTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(walletFundPropertyTemplateService.queryById(id));
    }

    @SaCheckPermission("wallet:fundProperty:add")
    @Log(title = "资金属性", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody WalletFundPropertyTemplateBo bo) {
        return toAjax(walletFundPropertyTemplateService.insertByBo(bo));
    }

    @SaCheckPermission("wallet:fundProperty:edit")
    @Log(title = "资金属性", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody WalletFundPropertyTemplateBo bo) {
        return toAjax(walletFundPropertyTemplateService.updateByBo(bo));
    }
}
