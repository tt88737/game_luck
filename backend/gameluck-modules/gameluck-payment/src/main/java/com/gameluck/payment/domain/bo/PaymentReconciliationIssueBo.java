package com.gameluck.payment.domain.bo;

import lombok.Data;

@Data
public class PaymentReconciliationIssueBo {
    private String issueType;
    private String status;
    private String purchaseOrderNo;
    private String sessionNo;
    private String providerRecordId;
}
