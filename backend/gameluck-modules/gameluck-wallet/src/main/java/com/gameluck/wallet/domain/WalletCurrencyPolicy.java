package com.gameluck.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * Wallet currency visibility and action policy.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_wallet_currency_policy")
public class WalletCurrencyPolicy extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String policyName;

    private String currencyCode;

    private String memberTag;

    private String vipLevel;

    private String countryCode;

    private String stateCode;

    private String channel;

    private String visibleEnabled;

    private String depositEnabled;

    private String withdrawEnabled;

    private String exchangeEnabled;

    private String playEnabled;

    private Integer priority;

    private String status;

    private Date startTime;

    private Date endTime;

    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
