package com.gameluck.payment.domain.bo;

import com.gameluck.payment.enums.PaymentProviderEventType;

public record SimulatedPaymentActionBo(PaymentProviderEventType action) {
}
