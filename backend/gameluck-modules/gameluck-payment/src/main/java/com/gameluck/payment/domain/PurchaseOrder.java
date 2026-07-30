package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Purchase order gl_purchase_order.
 */
@Data
@TableName("gl_purchase_order")
public class PurchaseOrder {

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String purchaseOrderNo;

    private Long offerId;

    private String offerNo;

    private String offerNameSnapshot;

    private Long memberId;

    private String payCurrencyCode;

    private BigDecimal payAmount;

    private String status;

    private String idempotencyKey;

    private String providerCode;

    private String providerOrderNo;

    private String paymentSessionNo;

    private String callbackEventKey;

    private String failReason;

    private Date paidTime;

    private Date creditedTime;

    private Date cancelTime;

    private Date refundTime;

    private Date chargebackTime;

    private Date createTime;

    private Date updateTime;
}
