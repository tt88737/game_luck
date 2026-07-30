package com.gameluck.redemption.domain.bo;

import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.redemption.domain.RedemptionEligibilityPolicy;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = RedemptionEligibilityPolicy.class, reverseConvertGenerate = false)
public class RedemptionEligibilityPolicyBo extends BaseEntity {

    private Long id;
    private String tenantId;
    @NotBlank(message = "{redemption.eligibility.policy.name.required}")
    private String policyName;
    @NotBlank(message = "{redemption.eligibility.currency.required}")
    private String currencyCode;
    private String countryCode;
    private String stateCode;
    private String channel;
    private String effect;
    private Integer priority;
    private String status;
    private Date startTime;
    private Date endTime;
    private String remark;
}
