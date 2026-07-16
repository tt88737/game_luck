package com.gameluck.payment.client.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * C-side purchase offer view.
 */
@Data
public class ClientPurchaseOfferVo {

    private Long offerId;

    private String offerNo;

    private String offerName;

    private String offerType;

    private String payCurrencyCode;

    private BigDecimal payAmount;

    private List<ClientPurchaseGrantItemVo> grantItems;

    private String limitText;

    private String wageringText;
}
