package com.gameluck.game.domain.vo;

import com.gameluck.game.domain.GameBetOrder;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = GameBetOrder.class)
public class GameBetOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tenantId;
    private String betOrderNo;
    private Long memberId;
    private String currencyCode;
    private String gameCode;
    private String roundNo;
    private BigDecimal betAmount;
    private BigDecimal payoutAmount;
    private BigDecimal netAmount;
    private String status;
    private String betWalletTransactionNo;
    private String settleWalletTransactionNo;
    private String refundWalletTransactionNo;
    private String refundIdempotencyKey;
    private Date cancelTime;
    private String betIdempotencyKey;
    private String settleIdempotencyKey;
    private Date betTime;
    private Date settleTime;
    private String failReason;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
