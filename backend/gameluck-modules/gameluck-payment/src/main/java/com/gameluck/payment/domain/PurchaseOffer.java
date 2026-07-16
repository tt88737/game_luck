package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Purchase offer gl_purchase_offer.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_purchase_offer")
public class PurchaseOffer extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
