package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletFreeze;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet freeze view object.
 */
@Data
@AutoMapper(target = WalletFreeze.class)
public class WalletFreezeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String freezeNo;

    private Long memberId;

    private String currencyCode;

    private BigDecimal amount;

    private String sourceType;

    private String businessNo;

    private String status;

    private Long operatorId;

    private String remark;

    private Date createTime;

    private Date updateTime;
}
