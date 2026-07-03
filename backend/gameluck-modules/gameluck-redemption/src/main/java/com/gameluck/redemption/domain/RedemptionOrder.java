package com.gameluck.redemption.domain;

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
 * Redemption order gl_redemption_order.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_redemption_order")
public class RedemptionOrder extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String redemptionOrderNo;

    private Long memberId;

    private String currencyCode;

    private BigDecimal amount;

    private String redemptionMethod;

    private String accountRef;

    private String status;

    private String freezeNo;

    private String freezeWalletTransactionNo;

    private String settleWalletTransactionNo;

    private String releaseWalletTransactionNo;

    private String freezeIdempotencyKey;

    private String settleIdempotencyKey;

    private String releaseIdempotencyKey;

    private Long auditBy;

    private Date auditTime;

    private String auditReason;

    private String failReason;

    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
