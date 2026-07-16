package com.gameluck.payment.domain.bo;

import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Purchase offer grant item form object.
 */
@Data
@AutoMapper(target = PurchaseOfferGrantItem.class, reverseConvertGenerate = false)
public class PurchaseOfferGrantItemBo {

    private Long id;

    @NotBlank(message = "{purchase.grant.type.required}")
    private String grantType;

    @NotBlank(message = "{common.currency.required}")
    private String currencyCode;

    @NotNull(message = "{purchase.grant.amount.required}")
    @DecimalMin(value = "0.000001", message = "{purchase.grant.amount.positive}")
    private BigDecimal grantAmount;

    private String wageringMode;

    private BigDecimal wageringRequiredAmount;

    private BigDecimal wageringMultiplier;

    private String gameScopeType;

    private String gameScopeValue;

    private Integer wageringExpireDays;

    private Integer sortOrder;

    private String remark;
}
