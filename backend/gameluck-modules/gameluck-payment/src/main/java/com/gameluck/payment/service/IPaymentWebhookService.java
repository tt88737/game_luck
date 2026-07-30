package com.gameluck.payment.service;

import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;

public interface IPaymentWebhookService {

    PaymentWebhookAckVo receive(String providerCode, String timestamp, String signature, byte[] rawBody);
}
