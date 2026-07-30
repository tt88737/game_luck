package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("gl_payment_reconciliation_line")
public class PaymentReconciliationLine {
    @TableId(value = "id")
    private Long id;
    private String tenantId;
    private Long batchId;
    private Long sourceRowNumber;
    private String providerRecordId;
    private String eventType;
    private String providerSessionNo;
    private String purchaseOrderNo;
    private String currencyCode;
    private BigDecimal amount;
    private Date occurredTime;
    private String status;
    private String parseError;
    private String rawFieldsJson;
    private Date createTime;
}
