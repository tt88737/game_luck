package com.gameluck.payment.domain.vo;

public record PaymentWebhookAckVo(String providerEventId, String status) {
}
