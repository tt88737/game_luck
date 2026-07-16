package com.gameluck.payment.client.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * C-side purchase grant item view.
 */
@Data
public class ClientPurchaseGrantItemVo {

    private String grantType;

    private String currencyCode;

    private BigDecimal grantAmount;

    private String wageringMode;

    private BigDecimal requiredTurnover;

    private BigDecimal wageringMultiplier;

    private String gameScopeType;

    private String gameScopeValue;
}
