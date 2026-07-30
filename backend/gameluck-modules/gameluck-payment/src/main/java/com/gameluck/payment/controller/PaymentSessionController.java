package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentSessionAdminBo;
import com.gameluck.payment.domain.vo.PaymentSessionAdminVo;
import com.gameluck.payment.service.IPaymentProviderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/payment-session")
public class PaymentSessionController {
    private final IPaymentProviderAdminService adminService;

    @SaCheckPermission("payment:paymentSession:list")
    @GetMapping("/list")
    public TableDataInfo<PaymentSessionAdminVo> list(@Validated PaymentSessionAdminBo bo, PageQuery pageQuery) {
        return adminService.querySessionPage(bo, pageQuery);
    }

    @SaCheckPermission("payment:paymentSession:query")
    @GetMapping("/{id}")
    public R<PaymentSessionAdminVo> getInfo(@PathVariable Long id) {
        return R.ok(adminService.querySessionById(id));
    }
}
