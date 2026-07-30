package com.gameluck.payment.domain.bo;

import lombok.Data;

import java.util.Date;

@Data
public class PaymentSettlementPayoutQueryBo {
    private String payoutNo;
    private String settlementNo;
    private String status;
    private String providerCode;
    private String currencyCode;
    private Date createStart;
    private Date createEnd;
}
