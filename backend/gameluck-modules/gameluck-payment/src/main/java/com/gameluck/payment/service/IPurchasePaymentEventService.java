package com.gameluck.payment.service;

import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;

public interface IPurchasePaymentEventService {

    PurchaseOrder applyEvent(PurchasePaymentCallbackBo bo);
}
