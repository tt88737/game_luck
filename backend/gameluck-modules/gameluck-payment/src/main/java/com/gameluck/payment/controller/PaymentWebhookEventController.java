package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentWebhookEventAdminBo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventAdminVo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventDetailVo;
import com.gameluck.payment.domain.vo.PaymentWebhookRetryResultVo;
import com.gameluck.payment.service.IPaymentProviderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/webhook-event")
public class PaymentWebhookEventController {
    private final IPaymentProviderAdminService adminService;

    @SaCheckPermission("payment:webhookEvent:list")
    @GetMapping("/list")
    public TableDataInfo<PaymentWebhookEventAdminVo> list(@Validated PaymentWebhookEventAdminBo bo, PageQuery pageQuery) {
        return adminService.queryWebhookPage(bo, pageQuery);
    }

    @SaCheckPermission("payment:webhookEvent:query")
    @GetMapping("/{id}")
    public R<PaymentWebhookEventDetailVo> getInfo(@PathVariable Long id) {
        return R.ok(adminService.queryWebhookById(id));
    }

    @SaCheckPermission("payment:webhookEvent:retry")
    @Log(title = "Payment webhook event", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/retry")
    public R<PaymentWebhookRetryResultVo> retry(@PathVariable Long id) {
        return R.ok(adminService.retryWebhookEvent(id));
    }
}
