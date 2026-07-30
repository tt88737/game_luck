package com.gameluck.payment.domain.vo;

import lombok.Data;
import java.util.Date;

@Data
public class PaymentReconciliationLineVo {
    private String id;
    private String batchId;
    private Long sourceRowNumber;
    private String providerRecordId;
    private String eventType;
    private String providerSessionNo;
    private String purchaseOrderNo;
    private String currencyCode;
    private String amount;
    private Date occurredTime;
    private String status;
    private String parseError;
    private String rawFieldsJson;
    private Date createTime;
}
