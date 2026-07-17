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
 * Wallet currency exchange order.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_wallet_exchange_order")
public class WalletExchangeOrder extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String exchangeOrderNo;

    private Long memberId;

    private Long exchangeRuleId;

    private String fromCurrencyCode;

    private BigDecimal fromAmount;

    private String toCurrencyCode;

    private BigDecimal toAmount;

    private BigDecimal feeAmount;

    private String debitTransactionNo;

    private String creditTransactionNo;

    private String turnoverTaskNo;

    private String ruleSnapshot;

    private String status;

    private String failReason;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
