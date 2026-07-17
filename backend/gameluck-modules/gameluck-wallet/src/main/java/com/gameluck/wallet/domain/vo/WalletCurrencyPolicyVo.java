package com.gameluck.wallet.domain.vo;

import com.gameluck.wallet.domain.WalletCurrencyPolicy;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Wallet currency policy view object.
 */
@Data
@AutoMapper(target = WalletCurrencyPolicy.class)
public class WalletCurrencyPolicyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private Date createTime;

    private Date updateTime;

    private Integer version;
}
