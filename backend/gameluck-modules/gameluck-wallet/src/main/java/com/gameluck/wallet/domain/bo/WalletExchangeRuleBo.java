package com.gameluck.wallet.domain.bo;

import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.wallet.domain.WalletExchangeRule;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet exchange rule business object.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WalletExchangeRule.class, reverseConvertGenerate = false)
public class WalletExchangeRuleBo extends BaseEntity {

    private Long id;

    private String tenantId;

    @NotBlank(message = "{wallet.exchange.rule.name.required}")
    private String ruleName;

    @NotBlank(message = "{wallet.exchange.rule.from.currency.required}")
    private String fromCurrencyCode;

    @NotBlank(message = "{wallet.exchange.rule.to.currency.required}")
    private String toCurrencyCode;

    private String rateType;

    @NotNull(message = "{wallet.exchange.rule.rate.required}")
    @DecimalMin(value = "0.00000001", message = "{wallet.exchange.rule.rate.positive}")
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
}
