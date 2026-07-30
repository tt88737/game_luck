package com.gameluck.payment.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class PaymentSettlementReportRowVo {
    private LocalDate reportDate;
    private String providerCode;
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
    private Boolean negativeNet;
    private Date earliestPeriodStart;
    private Date latestPeriodEnd;
    private Date latestCloseTime;
}
