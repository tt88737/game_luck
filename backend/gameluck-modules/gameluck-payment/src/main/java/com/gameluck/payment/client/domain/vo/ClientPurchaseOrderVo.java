package com.gameluck.payment.client.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * C-side purchase order result view.
 */
@Data
public class ClientPurchaseOrderVo {

    private Long orderId;

    private String orderNo;

    private Long offerId;

    private String offerNo;

    private String offerName;

    private String payCurrencyCode;

    private BigDecimal payAmount;

    private String status;

    private String providerCode;

    private String providerOrderNo;

    private String paymentSessionNo;

    private List<ClientPurchaseGrantItemVo> grantItems;

    private Date createdAt;

    private Date creditedAt;
}
