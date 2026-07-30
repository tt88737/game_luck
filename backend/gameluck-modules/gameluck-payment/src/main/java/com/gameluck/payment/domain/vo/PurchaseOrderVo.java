package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.PurchaseOrder;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Purchase order admin view object.
 */
@Data
@AutoMapper(target = PurchaseOrder.class)
public class PurchaseOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String purchaseOrderNo;

    private Long offerId;

    private String offerNo;

    private Long memberId;

    private String memberNo;

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
