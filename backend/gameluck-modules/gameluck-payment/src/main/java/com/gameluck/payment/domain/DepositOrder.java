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
 * Deposit order gl_payment_deposit_order.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_payment_deposit_order")
public class DepositOrder extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String depositOrderNo;

    private Long memberId;

    private String currencyCode;

    private BigDecimal amount;

    private String payMethod;

    private String payChannel;

    private String status;

    private String walletTransactionNo;

    private String walletIdempotencyKey;

    private Date payTime;

    private String failReason;

    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
