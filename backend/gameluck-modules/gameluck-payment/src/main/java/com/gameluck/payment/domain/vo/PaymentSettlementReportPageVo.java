package com.gameluck.payment.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class PaymentSettlementReportPageVo {
    private List<PaymentSettlementReportRowVo> rows;
    private Long total;
    private List<PaymentSettlementReportCurrencyTotalVo> currencyTotals;
    private String generatedAt;
}
