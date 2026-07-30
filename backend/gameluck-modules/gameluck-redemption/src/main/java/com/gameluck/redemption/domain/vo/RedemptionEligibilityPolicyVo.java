package com.gameluck.redemption.domain.vo;

import com.gameluck.redemption.domain.RedemptionEligibilityPolicy;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = RedemptionEligibilityPolicy.class)
public class RedemptionEligibilityPolicyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tenantId;
    private String policyName;
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
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
