package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.DepositOrder;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Deposit order view object.
 */
@Data
@AutoMapper(target = DepositOrder.class)
public class DepositOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String depositOrderNo;

    private Long memberId;

    private String memberNo;

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

    private Date createTime;

    private Date updateTime;
}
