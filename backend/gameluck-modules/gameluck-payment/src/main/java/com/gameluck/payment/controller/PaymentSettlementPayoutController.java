package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCommandBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCreateBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutEditBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutDetailVo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutRowVo;
import com.gameluck.payment.service.IPaymentSettlementPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/settlement-payout")
public class PaymentSettlementPayoutController {
    private final IPaymentSettlementPayoutService payoutService;

    @SaCheckPermission("payment:settlementPayout:list")
    @GetMapping("/list")
    public TableDataInfo<PaymentSettlementPayoutRowVo> list(PaymentSettlementPayoutQueryBo bo, PageQuery pageQuery) {
        return payoutService.queryPage(bo, pageQuery);
    }

    @SaCheckPermission("payment:settlementPayout:query")
    @GetMapping("/{id}")
    public R<PaymentSettlementPayoutDetailVo> detail(@PathVariable Long id) {
        return R.ok(payoutService.queryDetail(id));
    }

    @SaCheckPermission("payment:settlementPayout:create")
    @Log(title = "Settlement payout create", businessType = BusinessType.INSERT,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping
    public R<PaymentSettlementPayoutDetailVo> create(@Validated @RequestBody PaymentSettlementPayoutCreateBo bo) {
        return R.ok(payoutService.create(bo));
    }

    @SaCheckPermission("payment:settlementPayout:create")
    @Log(title = "Settlement payout edit", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping("/{id}")
    public R<PaymentSettlementPayoutDetailVo> edit(@PathVariable Long id,
                                                    @Validated @RequestBody PaymentSettlementPayoutEditBo bo) {
        return R.ok(payoutService.edit(id, bo));
    }

    @SaCheckPermission("payment:settlementPayout:submit")
    @Log(title = "Settlement payout submit", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{id}/submit")
    public R<PaymentSettlementPayoutDetailVo> submit(@PathVariable Long id,
                                                      @Validated @RequestBody PaymentSettlementPayoutCommandBo bo) {
        return R.ok(payoutService.submit(id, bo));
    }

    @SaCheckPermission("payment:settlementPayout:approve")
    @Log(title = "Settlement payout approve", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{id}/approve")
    public R<PaymentSettlementPayoutDetailVo> approve(@PathVariable Long id,
                                                       @Validated @RequestBody PaymentSettlementPayoutCommandBo bo) {
        return R.ok(payoutService.approve(id, bo));
    }

    @SaCheckPermission("payment:settlementPayout:approve")
    @Log(title = "Settlement payout reject", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{id}/reject")
    public R<PaymentSettlementPayoutDetailVo> reject(@PathVariable Long id,
                                                      @Validated @RequestBody PaymentSettlementPayoutCommandBo bo) {
        return R.ok(payoutService.reject(id, bo));
    }

    @SaCheckPermission("payment:settlementPayout:cancel")
    @Log(title = "Settlement payout cancel", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{id}/cancel")
    public R<PaymentSettlementPayoutDetailVo> cancel(@PathVariable Long id,
                                                      @Validated @RequestBody PaymentSettlementPayoutCommandBo bo) {
        return R.ok(payoutService.cancel(id, bo));
    }
}
