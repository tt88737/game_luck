package com.gameluck.redemption.domain;

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
 * Redemption eligibility policy.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_redemption_eligibility_policy")
public class RedemptionEligibilityPolicy extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
