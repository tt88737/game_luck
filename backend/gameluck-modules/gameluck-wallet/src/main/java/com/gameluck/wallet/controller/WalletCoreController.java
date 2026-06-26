package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletDebitBo;
import com.gameluck.wallet.domain.bo.WalletTurnoverBo;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary wallet core test controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/core")
public class WalletCoreController extends BaseController {

    private final IWalletCoreService walletCoreService;

    @SaCheckPermission("wallet:core:test")
    @PostMapping("/credit")
    public R<WalletTransaction> credit(@Validated @RequestBody WalletCreditBo bo) {
        return R.ok(walletCoreService.credit(bo));
    }

    @SaCheckPermission("wallet:core:test")
    @PostMapping("/debit")
    public R<WalletTransaction> debit(@Validated @RequestBody WalletDebitBo bo) {
        return R.ok(walletCoreService.debit(bo));
    }

    @SaCheckPermission("wallet:core:test")
    @PostMapping("/turnover")
    public R<Integer> turnover(@Validated @RequestBody WalletTurnoverBo bo) {
        return R.ok(walletCoreService.addValidTurnover(bo));
    }
}
