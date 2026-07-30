package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Purchase order grant snapshot gl_purchase_order_grant_snapshot.
 */
@Data
@TableName("gl_purchase_order_grant_snapshot")
public class PurchaseOrderGrantSnapshot {

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private Long purchaseOrderId;

    private String purchaseOrderNo;

    private Long memberId;

    private String grantType;

    private String currencyCode;

    private BigDecimal grantAmount;

    private String fundPropertyCode;

    private String walletTransactionNo;

    private String turnoverTaskNo;

    private String wageringMode;

    private BigDecimal wageringMultiplier;

    private Integer wageringExpireDays;

    private BigDecimal requiredTurnover;

    private String gameScopeType;

    private String gameScopeValue;

    private String ruleSnapshot;
}
