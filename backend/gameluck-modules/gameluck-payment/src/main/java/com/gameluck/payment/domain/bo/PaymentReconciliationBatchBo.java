package com.gameluck.payment.domain.bo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PaymentReconciliationBatchBo {
    private String providerCode;
    private LocalDate statementDate;
    private String status;
    private String originalFileName;
}
