package com.gameluck.payment.domain.bo;

import com.gameluck.payment.domain.DepositOrder;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Deposit order query and form object.
 */
@Data
@AutoMapper(target = DepositOrder.class, reverseConvertGenerate = false)
public class DepositOrderBo {

    private Long id;

    private String tenantId;

    private String depositOrderNo;

    @NotNull(message = "{member.id.required}")
    private Long memberId;

    private String currencyCode;

    @NotNull(message = "{payment.deposit.amount.required}")
    @DecimalMin(value = "0.000001", message = "{payment.deposit.amount.positive}")
    private BigDecimal amount;

    private String payMethod;

    private String payChannel;

    private String status;

    private String remark;

    private Date beginTime;

    private Date endTime;
}
