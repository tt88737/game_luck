package com.gameluck.redemption.domain.vo;

import com.gameluck.redemption.domain.RedemptionOrder;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Redemption order view object.
 */
@Data
@AutoMapper(target = RedemptionOrder.class)
public class RedemptionOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tenantId;
    private String redemptionOrderNo;
    private Long memberId;
    private String memberNo;
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
    private Date createTime;
    private Date updateTime;
}
