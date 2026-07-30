package com.gameluck.payment.service;

import com.gameluck.payment.domain.bo.SimulatedPaymentActionBo;
import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.domain.vo.SimulatedCheckoutVo;

public interface ISimulatedPaymentService {

    SimulatedCheckoutVo getCheckout(String providerSessionNo);

    PaymentWebhookAckVo executeAction(String providerSessionNo, SimulatedPaymentActionBo action);

    PaymentWebhookAckVo replay(String providerSessionNo);
}
