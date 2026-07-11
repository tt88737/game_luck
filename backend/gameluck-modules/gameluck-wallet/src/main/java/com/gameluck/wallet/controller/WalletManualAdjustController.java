package com.gameluck.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletManualAdjustBo;
import com.gameluck.wallet.service.IWalletManualAdjustService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet manual adjustment controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/manual-adjust")
public class WalletManualAdjustController extends BaseController {

    private final IWalletManualAdjustService walletManualAdjustService;

    @SaCheckPermission("wallet:manualAdjust:add")
    @Log(title = "人工调账", businessType = BusinessType.INSERT)
    @PostMapping
    public R<WalletTransaction> adjust(@Validated @RequestBody WalletManualAdjustBo bo) {
        return R.ok(walletManualAdjustService.adjust(bo));
    }
}
