package com.gameluck.payment.domain.bo;

import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchasePaymentCallbackBo {

    private String tenantId;

    private String eventKey;

    private String purchaseOrderNo;

    private String providerCode;

    private String providerOrderNo;

    private PurchasePaymentEventType eventType;

    private String requestBody;

    private String failReason;

    public static PurchasePaymentCallbackBo simulatedSuccess(PurchaseOrder order) {
        return PurchasePaymentCallbackBo.builder()
            .tenantId(order.getTenantId())
            .eventKey("purchase:simulated:pay-success:" + order.getPurchaseOrderNo())
            .purchaseOrderNo(order.getPurchaseOrderNo())
            .providerCode(order.getProviderCode())
            .providerOrderNo(order.getProviderOrderNo())
            .eventType(PurchasePaymentEventType.PAY_SUCCESS)
            .requestBody("{\"source\":\"SIMULATED\"}")
            .build();
    }
}
