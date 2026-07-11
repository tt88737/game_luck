package com.gameluck.wallet.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Wallet default rule template view object.
 */
@Data
public class WalletRuleTemplateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String currencyCode;

    private String sourceType;

    private String sourceLabel;

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

    private Boolean exists;

    private Boolean willCreate;
}
