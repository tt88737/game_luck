package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletTransaction;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet transaction view object.
 */
@Data
@AutoMapper(target = WalletTransaction.class)
public class WalletTransactionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String transactionNo;

    private String idempotencyKey;

    private Long memberId;

    private String currencyCode;

    private String operation;

    private String sourceType;

    private String businessNo;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private BigDecimal frozenBefore;

    private BigDecimal frozenAfter;

    private String releaseMode;

    private BigDecimal requiredTurnover;

    private String requestHash;

    private String status;

    private String failCode;

    private String failReason;

    private Long operatorId;

    private String remark;

    private Date createTime;

    private Date updateTime;
}
