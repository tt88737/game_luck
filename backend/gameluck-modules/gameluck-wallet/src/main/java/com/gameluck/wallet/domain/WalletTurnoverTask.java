package com.gameluck.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet turnover task snapshot gl_wallet_turnover_task.
 */
@Data
@TableName("gl_wallet_turnover_task")
public class WalletTurnoverTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String turnoverTaskNo;

    private Long memberId;

    private String currencyCode;

    private String fundPropertyCode;

    private String sourceType;

    private String sourceId;

    private String businessNo;

    private String walletTransactionNo;

    private BigDecimal rewardAmount;

    private BigDecimal requiredTurnover;

    private BigDecimal completedTurnover;

    private String gameScopeType;

    private String gameScopeValue;

    private String ruleSnapshot;

    private String status;

    private Date expireTime;

    private Date completeTime;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
