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

/**
 * Wallet source rule config gl_wallet_rule.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_wallet_rule")
public class WalletRule extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
