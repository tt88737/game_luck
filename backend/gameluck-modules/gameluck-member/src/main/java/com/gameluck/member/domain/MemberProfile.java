package com.gameluck.member.domain;

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
 * Member profile gl_member_profile.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_member_profile")
public class MemberProfile extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String tenantId;

    private String memberNo;

    private String username;

    private String nickname;

    private String passwordHash;

    private String status;

    private String riskLevel;

    private String riskReason;

    private String riskSource;

    private Date riskUpdatedTime;

    private String kycStatus;

    private String kycReviewReason;

    private String kycReviewedBy;

    private Date kycReviewTime;

    private String registerChannel;

    private String countryCode;

    private String stateCode;

    private Boolean ageConfirmed;

    private Boolean termsAccepted;

    private Boolean privacyAccepted;

    private Boolean sweepstakesRulesAccepted;

    private Date lastLoginTime;

    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
