package com.gameluck.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet currency exchange rule.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_wallet_exchange_rule")
public class WalletExchangeRule extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
