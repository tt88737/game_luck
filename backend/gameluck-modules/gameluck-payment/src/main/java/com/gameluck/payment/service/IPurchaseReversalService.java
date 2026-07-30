package com.gameluck.payment.service;

import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.domain.vo.PurchaseReversalResult;

import java.util.Date;

public interface IPurchaseReversalService {
    PurchaseReversalResult reverse(PurchaseOrder order, PurchasePaymentCallbackBo callback, Date processingTime);
}
