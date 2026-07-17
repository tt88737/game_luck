package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletTurnoverTask;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet turnover task view object.
 */
@Data
@AutoMapper(target = WalletTurnoverTask.class)
public class WalletTurnoverTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private Integer version;
}
