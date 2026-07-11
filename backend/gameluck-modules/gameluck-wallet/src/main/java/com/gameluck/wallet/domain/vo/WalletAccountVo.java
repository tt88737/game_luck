package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletAccount;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Wallet account view object.
 */
@Data
@AutoMapper(target = WalletAccount.class)
public class WalletAccountVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private Long memberId;

    private String memberNo;

    private String currencyCode;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private String status;

    private Date createTime;

    private Date updateTime;

    private Integer version;
}
