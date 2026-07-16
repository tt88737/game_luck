package com.gameluck.payment.domain.bo;

import com.gameluck.payment.domain.PurchaseOffer;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Purchase offer query and form object.
 */
@Data
@AutoMapper(target = PurchaseOffer.class, reverseConvertGenerate = false)
public class PurchaseOfferBo {

    private Long id;

    private String tenantId;

    private String offerNo;

    @NotBlank(message = "{purchase.offer.name.required}")
    private String offerName;

    private String offerType;

    private String payCurrencyCode;

    @NotNull(message = "{purchase.offer.pay.amount.required}")
    @DecimalMin(value = "0.000001", message = "{purchase.offer.pay.amount.positive}")
    private BigDecimal payAmount;

    private String userScopeType;

    private String userScopeValue;

    private String regionScopeType;

    private String regionScopeValue;

    private String purchaseLimitType;

    private String stackable;

    private String status;

    private Integer sortOrder;

    private Date startTime;

    private Date endTime;

    private String remark;

    private Date beginTime;

    private Date endQueryTime;

    @Valid
    private List<PurchaseOfferGrantItemBo> grantItems;

    public static PurchaseOfferGrantItemBo grantItem(String grantType, String currencyCode, BigDecimal grantAmount, String wageringMode, BigDecimal wageringMultiplier) {
        PurchaseOfferGrantItemBo item = new PurchaseOfferGrantItemBo();
        item.setGrantType(grantType);
        item.setCurrencyCode(currencyCode);
        item.setGrantAmount(grantAmount);
        item.setWageringMode(wageringMode);
        item.setWageringMultiplier(wageringMultiplier);
        item.setGameScopeType("ALL");
        return item;
    }
}
