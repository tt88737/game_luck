package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.PurchaseOffer;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Purchase offer view object.
 */
@Data
@AutoMapper(target = PurchaseOffer.class)
public class PurchaseOfferVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String offerNo;

    private String offerName;

    private String offerType;

    private String payCurrencyCode;

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

    private Date createTime;

    private Date updateTime;

    private List<PurchaseOfferGrantItemVo> grantItems;
}
