package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("gl_payment_settlement_item")
public class PaymentSettlementItem {
    @TableId(value = "id") private Long id;
    private String tenantId;
    private Long batchId;
    private Long webhookEventId;
    private String providerEventId;
    private Long paymentSessionId;
    private String sessionNo;
    private String providerSessionNo;
    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private String eventType;
    private Date receivedTime;
    private String currencyCode;
    private BigDecimal sourceAmount;
    private BigDecimal grossPayment;
    private BigDecimal refundAmount;
    private BigDecimal chargebackAmount;
    private BigDecimal feeAmount;
    private BigDecimal netContribution;
    private String sourceSnapshotJson;
    private Date createTime;
}
