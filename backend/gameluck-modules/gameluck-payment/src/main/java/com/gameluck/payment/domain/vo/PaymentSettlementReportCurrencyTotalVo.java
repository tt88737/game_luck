package com.gameluck.payment.domain.vo;

import lombok.Data;

@Data
public class PaymentSettlementReportCurrencyTotalVo {
    private String currencyCode;
    private Long batchCount;
    private Long eventCount;
    private Long paymentEventCount;
    private Long refundEventCount;
    private Long chargebackEventCount;
    private String grossPayment;
    private String refundAmount;
    private String chargebackAmount;
    private String totalFee;
    private String netSettlement;
}
