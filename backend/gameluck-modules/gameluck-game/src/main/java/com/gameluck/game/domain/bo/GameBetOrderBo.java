package com.gameluck.game.domain.bo;

import com.gameluck.game.domain.GameBetOrder;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = GameBetOrder.class, reverseConvertGenerate = false)
public class GameBetOrderBo {

    private Long id;
    private String tenantId;
    private String betOrderNo;

    @NotNull(message = "{member.id.required}")
    private Long memberId;

    private String currencyCode;
    private String gameCode;
    private String roundNo;

    @NotNull(message = "{game.bet.order.bet.amount.required}")
    @DecimalMin(value = "0.000001", message = "{game.bet.order.bet.amount.positive.required}")
    private BigDecimal betAmount;

    @NotNull(message = "{game.bet.order.payout.amount.required}")
    @DecimalMin(value = "0", message = "{game.bet.order.payout.amount.non.negative.required}")
    private BigDecimal payoutAmount;

    private String status;
    private String remark;
    private Date beginTime;
    private Date endTime;
}
