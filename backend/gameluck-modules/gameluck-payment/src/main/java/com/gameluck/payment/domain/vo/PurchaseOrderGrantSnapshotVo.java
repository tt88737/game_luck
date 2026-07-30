package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Purchase order grant snapshot admin view object.
 */
@Data
@AutoMapper(target = PurchaseOrderGrantSnapshot.class)
public class PurchaseOrderGrantSnapshotVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private BigDecimal requiredTurnover;

    private String gameScopeType;

    private String gameScopeValue;

    private String ruleSnapshot;
}
