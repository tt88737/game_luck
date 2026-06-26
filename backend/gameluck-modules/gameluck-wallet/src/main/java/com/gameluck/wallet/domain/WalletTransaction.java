package com.gameluck.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet transaction ledger gl_wallet_transaction.
 */
@Data
@TableName("gl_wallet_transaction")
public class WalletTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
