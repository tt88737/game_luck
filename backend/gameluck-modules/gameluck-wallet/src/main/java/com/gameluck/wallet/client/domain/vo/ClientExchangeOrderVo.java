package com.gameluck.wallet.client.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Client exchange order result.
 */
@Data
public class ClientExchangeOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String exchangeOrderNo;

    private Long exchangeRuleId;

    private String fromCurrencyCode;

    private BigDecimal fromAmount;

    private String toCurrencyCode;

    private BigDecimal toAmount;

    private BigDecimal feeAmount;

    private String status;

    private String failReason;
}
