package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.bo.WalletExchangeOrderBo;
import com.gameluck.wallet.domain.vo.WalletExchangeOrderVo;
import com.gameluck.wallet.service.IWalletExchangeOrderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet exchange order admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/exchange-order")
public class WalletExchangeOrderController extends BaseController {

    private final IWalletExchangeOrderAdminService walletExchangeOrderAdminService;

    @SaCheckPermission("wallet:exchangeOrder:list")
    @GetMapping("/list")
    public TableDataInfo<WalletExchangeOrderVo> list(WalletExchangeOrderBo bo, PageQuery pageQuery) {
        return walletExchangeOrderAdminService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("wallet:exchangeOrder:query")
    @GetMapping("/{id}")
    public R<WalletExchangeOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(walletExchangeOrderAdminService.queryById(id));
    }
}
