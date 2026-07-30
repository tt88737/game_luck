package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.PurchaseOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PurchaseReversalResult {
    private PurchaseOrder order;
    private String processResult;
}
