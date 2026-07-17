package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletExchangeOrder;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet exchange order view object.
 */
@Data
@AutoMapper(target = WalletExchangeOrder.class)
public class WalletExchangeOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private Date createTime;

    private Date updateTime;

    private Integer version;
}
