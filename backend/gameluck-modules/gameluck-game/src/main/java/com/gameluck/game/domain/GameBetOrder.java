package com.gameluck.game.domain;

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

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_game_bet_order")
public class GameBetOrder extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
