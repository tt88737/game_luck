package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PaymentSettlementItemVo {
    private String id;
    private String batchId;
    private String webhookEventId;
    private String providerEventId;
    private String paymentSessionId;
    private String sessionNo;
    private String providerSessionNo;
    private String purchaseOrderId;
    private String purchaseOrderNo;
    private String eventType;
    private Date receivedTime;
    private String currencyCode;
    private String sourceAmount;
    private String grossPayment;
    private String refundAmount;
    private String chargebackAmount;
    private String feeAmount;
    private String netContribution;
    private String sourceSnapshotJson;
    private Date createTime;
}
