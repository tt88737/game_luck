package com.gameluck.member.domain.vo;

import com.gameluck.member.domain.MemberProfile;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Member profile view object.
 */
@Data
@AutoMapper(target = MemberProfile.class)
public class MemberProfileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantId;

    private String memberNo;

    private String username;

    private String nickname;

    private String status;

    private String riskLevel;

    private String registerChannel;

    private String countryCode;

    private String stateCode;

    private Boolean ageConfirmed;

    private Boolean termsAccepted;

    private Boolean privacyAccepted;

    private Boolean sweepstakesRulesAccepted;

    private Date lastLoginTime;

    private String remark;

    private Date createTime;

    private Date updateTime;
}
