package com.gameluck.wallet.domain.bo;

import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.wallet.domain.WalletCurrencyPolicy;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WalletCurrencyPolicy.class, reverseConvertGenerate = false)
public class WalletCurrencyPolicyBo extends BaseEntity {

    private Long id;
    private String tenantId;
    @NotBlank(message = "{wallet.currency.policy.name.required}")
    private String policyName;
    @NotBlank(message = "{wallet.currency.required}")
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
}
