package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.PurchaseReversalItem;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Purchase asset reversal item admin view object.
 */
@Data
@AutoMapper(target = PurchaseReversalItem.class)
public class PurchaseReversalItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
