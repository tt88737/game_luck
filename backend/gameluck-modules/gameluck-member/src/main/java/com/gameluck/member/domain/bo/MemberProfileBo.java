package com.gameluck.member.domain.bo;

import com.gameluck.member.domain.MemberProfile;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;

/**
 * Member profile query and form object.
 */
@Data
@AutoMapper(target = MemberProfile.class, reverseConvertGenerate = false)
public class MemberProfileBo {

    private Long id;

    private String tenantId;

    private String memberNo;

    @NotBlank(message = "{member.username.required}")
    private String username;

    private String nickname;

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

    private Date lastLoginTime;

    private String remark;

    private Date beginTime;

    private Date endTime;
}
