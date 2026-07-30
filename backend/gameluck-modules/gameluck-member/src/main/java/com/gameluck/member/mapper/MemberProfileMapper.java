package com.gameluck.member.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.domain.vo.MemberProfileVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * Member profile mapper.
 */
public interface MemberProfileMapper extends BaseMapperPlus<MemberProfile, MemberProfileVo> {

    MemberProfile selectByUsername(@Param("tenantId") String tenantId, @Param("username") String username);

    MemberProfile selectClientMember(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);

    MemberProfile selectByMemberNo(@Param("tenantId") String tenantId, @Param("memberNo") String memberNo);

    MemberProfile selectByIdForUpdate(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);

    int updateChargebackRisk(@Param("tenantId") String tenantId, @Param("memberId") Long memberId,
                             @Param("reason") String reason, @Param("source") String source,
                             @Param("riskUpdatedTime") Date riskUpdatedTime);
}
