package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Purchase offer grant item view object.
 */
@Data
@AutoMapper(target = PurchaseOfferGrantItem.class)
public class PurchaseOfferGrantItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

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
