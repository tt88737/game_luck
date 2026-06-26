package com.gameluck.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Member wallet account gl_wallet_account.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_wallet_account")
public class WalletAccount extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private Long memberId;

    private String currencyCode;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private String status;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
