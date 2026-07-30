package com.gameluck.payment.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.payment.client.domain.bo.ClientPaymentSessionCreateBo;
import com.gameluck.payment.client.domain.bo.ClientPurchasePayBo;
import com.gameluck.payment.client.domain.vo.ClientPaymentSessionVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOfferVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOrderVo;
import com.gameluck.payment.client.service.ClientPurchaseService;
import com.gameluck.payment.service.IPaymentSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/api/client/purchase")
public class ClientPurchaseController {

    private final ClientPurchaseService clientPurchaseService;
    private final IPaymentSessionService paymentSessionService;
    private final ClientTokenService clientTokenService;

    @GetMapping("/offers")
    public R<List<ClientPurchaseOfferVo>> offers() {
        return R.ok(clientPurchaseService.offers());
    }

    @PostMapping("/orders/pay")
    public R<ClientPurchaseOrderVo> pay(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @Valid @RequestBody ClientPurchasePayBo bo) {
        return R.ok(clientPurchaseService.pay(authorization, bo));
    }

    @PostMapping("/orders/{orderNo}/payment-sessions")
    public R<ClientPaymentSessionVo> createPaymentSession(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable String orderNo, @Valid @RequestBody ClientPaymentSessionCreateBo bo) {
        return R.ok(paymentSessionService.create(clientTokenService.requireMemberId(authorization), orderNo, bo));
    }

    @GetMapping("/payment-sessions/{sessionNo}")
    public R<ClientPaymentSessionVo> paymentSession(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable String sessionNo) {
        return R.ok(paymentSessionService.get(clientTokenService.requireMemberId(authorization), sessionNo));
    }
}
