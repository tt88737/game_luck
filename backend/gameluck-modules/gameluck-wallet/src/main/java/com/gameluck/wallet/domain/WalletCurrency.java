package com.gameluck.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Wallet currency config gl_wallet_currency.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_wallet_currency")
public class WalletCurrency extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String currencyCode;

    private String currencyName;

    private Integer scaleNum;

    private String enabled;

    private String creditEnabled;

    private String debitEnabled;

    private String freezeEnabled;

    private String withdrawEnabled;

    private String exchangeEnabled;

    private String negativeAllowed;

    private Integer sortOrder;

    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
