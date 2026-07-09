package com.gameluck.member.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.member.client.domain.bo.ClientLoginBo;
import com.gameluck.member.client.domain.vo.ClientLoginVo;
import com.gameluck.member.client.domain.vo.ClientMemberVo;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ClientAuthService {

    private static final String TENANT_ID = "000000";
    private static final String DEMO_PASSWORD = "Demo123456";

    private final MemberProfileMapper memberProfileMapper;
    private final ClientTokenService clientTokenService;

    public ClientLoginVo login(ClientLoginBo bo) {
        MemberProfile member = memberProfileMapper.selectByUsername(TENANT_ID, bo.getUsername());
        if (member == null || !DEMO_PASSWORD.equals(bo.getPassword())) {
            throw new ServiceException(MessageUtils.message("client.auth.invalid.credentials"));
        }
        ClientLoginVo vo = new ClientLoginVo();
        vo.setAccessToken(clientTokenService.issue(member.getId()));
        vo.setExpiresIn(clientTokenService.expiresIn());
        vo.setMember(toClientMember(member));
        return vo;
    }

    public ClientMemberVo currentMember(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        MemberProfile member = memberProfileMapper.selectClientMember(TENANT_ID, memberId);
        if (member == null) {
            throw new ServiceException(MessageUtils.message("client.auth.member.not.exists"));
        }
        return toClientMember(member);
    }

    private ClientMemberVo toClientMember(MemberProfile member) {
        ClientMemberVo vo = new ClientMemberVo();
        vo.setMemberId(member.getId());
        vo.setMemberNo(member.getMemberNo());
        vo.setUsername(member.getUsername());
        vo.setNickname(member.getNickname());
        vo.setStatus(member.getStatus());
        vo.setKycStatus("not_required");
        return vo;
    }
}
