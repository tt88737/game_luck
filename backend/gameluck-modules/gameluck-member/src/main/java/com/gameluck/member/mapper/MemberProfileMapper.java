package com.gameluck.member.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.domain.vo.MemberProfileVo;
import org.apache.ibatis.annotations.Param;

/**
 * Member profile mapper.
 */
public interface MemberProfileMapper extends BaseMapperPlus<MemberProfile, MemberProfileVo> {

    MemberProfile selectByUsername(@Param("tenantId") String tenantId, @Param("username") String username);

    MemberProfile selectClientMember(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);
}
