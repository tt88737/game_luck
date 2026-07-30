package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Aggregated currency line for a purchase asset reversal.
 */
@Data
@TableName("gl_purchase_reversal_item")
public class PurchaseReversalItem {

    @TableId(value = "id")
    private Long id;

    private String tenantId;
    private Long reversalId;
    private String reversalNo;
    private String purchaseOrderNo;
    private Long memberId;
    private String currencyCode;
    private BigDecimal requiredAmount;
    private BigDecimal availableAmount;
    private BigDecimal recoveredAmount;
    private BigDecimal shortfallAmount;
    private String walletTransactionNo;
    private String status;
    private Date createTime;
    private Date updateTime;
}
