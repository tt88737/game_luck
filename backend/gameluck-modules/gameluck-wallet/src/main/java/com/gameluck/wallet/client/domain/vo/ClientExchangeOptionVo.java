package com.gameluck.wallet.client.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Client exchange option.
 */
@Data
public class ClientExchangeOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long exchangeRuleId;

    private String ruleName;

    private String fromCurrencyCode;

    private String toCurrencyCode;

    private String rateType;

    private BigDecimal rateValue;

    private BigDecimal minFromAmount;

    private BigDecimal maxFromAmount;

    private BigDecimal dailyFromLimit;

    private String feeType;

    private BigDecimal feeValue;

    private String turnoverRequired;

    private BigDecimal turnoverMultiplier;
}
