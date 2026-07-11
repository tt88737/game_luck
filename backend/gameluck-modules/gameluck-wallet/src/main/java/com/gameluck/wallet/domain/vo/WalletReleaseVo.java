package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletRelease;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet release view object.
 */
@Data
@AutoMapper(target = WalletRelease.class)
public class WalletReleaseVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String releaseNo;

    private Long memberId;

    private String memberNo;

    private String currencyCode;

    private String sourceType;

    private String businessNo;

    private BigDecimal amount;

    private BigDecimal releasedAmount;

    private BigDecimal consumedAmount;

    private BigDecimal requiredTurnover;

    private BigDecimal completedTurnover;

    private String releaseMode;

    private String releaseStatus;

    private String metadata;

    private Long operatorId;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private Integer version;
}
