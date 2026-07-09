package com.gameluck.member.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.member.client.domain.bo.ClientLoginBo;
import com.gameluck.member.client.domain.vo.ClientLoginVo;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAuthServiceTest {

    @Test
    @Tag("local")
    void loginReturnsTokenForDemoMember() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientAuthService service = new ClientAuthService(mapper, tokenService);
        MemberProfile member = member();
        when(mapper.selectByUsername("000000", "demo_player")).thenReturn(member);

        ClientLoginBo bo = new ClientLoginBo();
        bo.setUsername("demo_player");
        bo.setPassword("Demo123456");
        ClientLoginVo result = service.login(bo);

        assertNotNull(result.getAccessToken());
        assertEquals(7200L, result.getExpiresIn());
        assertEquals(1001L, result.getMember().getMemberId());
        assertEquals("demo_player", result.getMember().getUsername());
    }

    @Test
    @Tag("local")
    void loginRejectsWrongPassword() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        ClientAuthService service = new ClientAuthService(mapper, new ClientTokenService());
        when(mapper.selectByUsername("000000", "demo_player")).thenReturn(member());
        ClientLoginBo bo = new ClientLoginBo();
        bo.setUsername("demo_player");
        bo.setPassword("bad");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.login(bo));

        assertEquals("client.auth.invalid.credentials", exception.getMessage());
    }

    @Test
    @Tag("local")
    void tokenCanResolveCurrentMemberId() {
        ClientTokenService tokenService = new ClientTokenService();
        String token = tokenService.issue(1001L);

        assertEquals(1001L, tokenService.requireMemberId("Bearer " + token));
    }

    private MemberProfile member() {
        MemberProfile member = new MemberProfile();
        member.setId(1001L);
        member.setTenantId("000000");
        member.setMemberNo("M1001");
        member.setUsername("demo_player");
        member.setNickname("Demo Player");
        member.setStatus("ACTIVE");
        return member;
    }
}
