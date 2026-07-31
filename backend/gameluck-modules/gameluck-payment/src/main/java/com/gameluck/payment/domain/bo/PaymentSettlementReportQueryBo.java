package com.gameluck.payment.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PaymentSettlementReportQueryBo {
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private String providerCode;
    private String currencyCode;
}
