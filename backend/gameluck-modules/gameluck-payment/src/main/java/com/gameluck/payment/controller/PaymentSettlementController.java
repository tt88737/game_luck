package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentSettlementCloseBo;
import com.gameluck.payment.domain.bo.PaymentSettlementCreateBo;
import com.gameluck.payment.domain.bo.PaymentSettlementQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementDetailVo;
import com.gameluck.payment.domain.vo.PaymentSettlementItemVo;
import com.gameluck.payment.service.IPaymentSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/settlement")
public class PaymentSettlementController {
    private final IPaymentSettlementService settlementService;

    @SaCheckPermission("payment:settlement:list")
    @GetMapping("/list")
    public TableDataInfo<PaymentSettlementBatchVo> list(PaymentSettlementQueryBo bo, PageQuery pageQuery) {
        return settlementService.queryPage(bo, pageQuery);
    }

    @SaCheckPermission("payment:settlement:create")
    @Log(title = "Payment settlement create", businessType = BusinessType.INSERT,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping
    public R<PaymentSettlementDetailVo> create(@Validated @RequestBody PaymentSettlementCreateBo bo) {
        return R.ok(settlementService.create(bo));
    }

    @SaCheckPermission("payment:settlement:query")
    @GetMapping("/{batchId}")
    public R<PaymentSettlementDetailVo> detail(@PathVariable Long batchId) {
        return R.ok(settlementService.queryDetail(batchId));
    }

    @SaCheckPermission("payment:settlement:query")
    @GetMapping("/{batchId}/items")
    public TableDataInfo<PaymentSettlementItemVo> items(@PathVariable Long batchId,
        @RequestParam(required = false) String eventType, PageQuery pageQuery) {
        return settlementService.queryItems(batchId, eventType, pageQuery);
    }

    @SaCheckPermission("payment:settlement:calculate")
    @Log(title = "Payment settlement calculate", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{batchId}/calculate")
    public R<PaymentSettlementDetailVo> calculate(@PathVariable Long batchId) {
        return R.ok(settlementService.calculate(batchId));
    }

    @SaCheckPermission("payment:settlement:close")
    @Log(title = "Payment settlement close", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{batchId}/close")
    public R<PaymentSettlementDetailVo> close(@PathVariable Long batchId,
        @Validated @RequestBody PaymentSettlementCloseBo bo) {
        return R.ok(settlementService.close(batchId, bo));
    }
}
