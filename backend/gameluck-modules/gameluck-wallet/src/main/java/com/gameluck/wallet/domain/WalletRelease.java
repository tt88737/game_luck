package com.gameluck.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet release record gl_wallet_release.
 */
@Data
@TableName("gl_wallet_release")
public class WalletRelease implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String releaseNo;

    private Long memberId;

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

    @Version
    private Integer version;
}
