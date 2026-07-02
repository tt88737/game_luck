package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletRule;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet source rule view object.
 */
@Data
@AutoMapper(target = WalletRule.class)
public class WalletRuleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String currencyCode;

    private String sourceType;

    private String ruleName;

    private String creditEnabled;

    private String debitEnabled;

    private String withdrawEnabled;

    private String exchangeEnabled;

    private String releaseMode;

    private String turnoverRequired;

    private BigDecimal defaultRequiredTurnover;

    private String status;

    private Integer sortOrder;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private Integer version;
}
