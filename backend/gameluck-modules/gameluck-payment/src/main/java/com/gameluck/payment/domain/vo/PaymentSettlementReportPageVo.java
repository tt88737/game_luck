package com.gameluck.payment.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PaymentSettlementReportPageVo {
    private List<PaymentSettlementReportRowVo> rows;
    private long total;
    private List<PaymentSettlementReportCurrencyTotalVo> currencyTotals;
    private Date generatedAt;
}
