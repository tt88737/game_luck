package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Purchase offer grant item gl_purchase_offer_grant_item.
 */
@Data
@TableName("gl_purchase_offer_grant_item")
public class PurchaseOfferGrantItem {

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private Long offerId;

    private String grantType;

    private String currencyCode;

    private BigDecimal grantAmount;

    private String fundPropertyCode;

    private String wageringMode;

    private BigDecimal wageringRequiredAmount;

    private BigDecimal wageringMultiplier;

    private String gameScopeType;

    private String gameScopeValue;

    private Integer wageringExpireDays;

    private Integer sortOrder;

    private String remark;
}
