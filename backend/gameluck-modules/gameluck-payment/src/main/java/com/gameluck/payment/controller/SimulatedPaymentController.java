package com.gameluck.payment.controller;

import com.gameluck.payment.domain.bo.SimulatedPaymentActionBo;
import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.domain.vo.SimulatedCheckoutVo;
import com.gameluck.payment.service.ISimulatedPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class SimulatedPaymentController {

    private final ISimulatedPaymentService simulatedPaymentService;

    @GetMapping("/payment/simulated/checkout/{providerSessionNo}")
    public SimulatedCheckoutVo checkout(@PathVariable String providerSessionNo) {
        return simulatedPaymentService.getCheckout(providerSessionNo);
    }

    @PostMapping("/payment/simulated/checkout/{providerSessionNo}/actions")
    public PaymentWebhookAckVo action(@PathVariable String providerSessionNo,
                                      @RequestBody SimulatedPaymentActionBo action) {
        return simulatedPaymentService.executeAction(providerSessionNo, action);
    }

    @PostMapping("/payment/simulated/checkout/{providerSessionNo}/replay")
    public PaymentWebhookAckVo replay(@PathVariable String providerSessionNo) {
        return simulatedPaymentService.replay(providerSessionNo);
    }
}
