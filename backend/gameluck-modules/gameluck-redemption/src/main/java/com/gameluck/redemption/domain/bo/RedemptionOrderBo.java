package com.gameluck.redemption.domain.bo;

import com.gameluck.redemption.domain.RedemptionOrder;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Redemption order query and form object.
 */
@Data
@AutoMapper(target = RedemptionOrder.class, reverseConvertGenerate = false)
public class RedemptionOrderBo {

    private Long id;

    private String tenantId;

    private String redemptionOrderNo;

    @NotNull(message = "{redemption.member.id.required}")
    private Long memberId;

    private String memberNo;

    private String currencyCode;

    @NotNull(message = "{redemption.amount.required}")
    @DecimalMin(value = "0.000001", message = "{redemption.amount.positive}")
    private BigDecimal amount;

    private String redemptionMethod;

    private String accountRef;

    private String status;

    private String auditReason;

    private String remark;

    private Date beginTime;

    private Date endTime;
}
