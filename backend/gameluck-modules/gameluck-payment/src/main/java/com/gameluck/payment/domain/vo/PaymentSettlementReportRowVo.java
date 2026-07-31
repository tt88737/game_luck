package com.gameluck.payment.domain.vo;

import lombok.Data;

@Data
public class PaymentSettlementReportRowVo {
    private String settlementDate;
    private String providerCode;
    private String currencyCode;
    private Integer batchCount;
    private Integer eventCount;
    private Integer paymentCount;
    private Integer refundCount;
    private Integer chargebackCount;
    private String grossPayment;
    private String refundAmount;
    private String chargebackAmount;
    private String totalFee;
    private String netSettlement;
    private Boolean negativeNet;
    private String earliestPeriodStart;
    private String latestPeriodEnd;
    private String latestCloseTime;
}
