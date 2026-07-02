package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.payment.domain.bo.DepositOrderBo;
import com.gameluck.payment.domain.vo.DepositOrderVo;
import com.gameluck.payment.service.IDepositOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deposit order admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/deposit")
public class DepositOrderController extends BaseController {

    private final IDepositOrderService depositOrderService;

    @SaCheckPermission("payment:deposit:list")
    @GetMapping("/list")
    public TableDataInfo<DepositOrderVo> list(DepositOrderBo bo, PageQuery pageQuery) {
        return depositOrderService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("payment:deposit:query")
    @GetMapping("/{id}")
    public R<DepositOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(depositOrderService.queryById(id));
    }

    @SaCheckPermission("payment:deposit:add")
    @Log(title = "充值订单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody DepositOrderBo bo) {
        return toAjax(depositOrderService.insertByBo(bo));
    }

    @SaCheckPermission("payment:deposit:simulate")
    @Log(title = "模拟支付成功", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/simulate-success")
    public R<DepositOrderVo> simulateSuccess(@PathVariable Long id) {
        return R.ok(depositOrderService.simulateSuccess(id));
    }

    @SaCheckPermission("payment:deposit:cancel")
    @Log(title = "充值订单取消", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        return toAjax(depositOrderService.cancel(id));
    }
}
