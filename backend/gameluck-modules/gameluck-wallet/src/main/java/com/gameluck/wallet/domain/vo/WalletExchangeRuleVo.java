package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletExchangeRule;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet exchange rule view object.
 */
@Data
@AutoMapper(target = WalletExchangeRule.class)
public class WalletExchangeRuleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

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

    private String gameScopeType;

    private String gameScopeValue;

    private String countryCode;

    private String stateCode;

    private String memberTag;

    private String channel;

    private String status;

    private Date startTime;

    private Date endTime;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private Integer version;
}
