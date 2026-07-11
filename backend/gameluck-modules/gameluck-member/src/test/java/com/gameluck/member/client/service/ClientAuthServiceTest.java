package com.gameluck.member.client.service;

import cn.hutool.crypto.digest.BCrypt;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.member.client.domain.bo.ClientLoginBo;
import com.gameluck.member.client.domain.bo.ClientRegisterBo;
import com.gameluck.member.client.domain.vo.ClientLoginVo;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientAuthServiceTest {

    @Test
    @Tag("local")
    void loginReturnsTokenForDemoMember() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientAuthService service = new ClientAuthService(mapper, tokenService, walletCoreService);
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
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        ClientAuthService service = new ClientAuthService(mapper, new ClientTokenService(), walletCoreService);
        when(mapper.selectByUsername("000000", "demo_player")).thenReturn(member());
        ClientLoginBo bo = new ClientLoginBo();
        bo.setUsername("demo_player");
        bo.setPassword("bad");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.login(bo));

        assertEquals("client.auth.invalid.credentials", exception.getMessage());
    }

    @Test
    @Tag("local")
    void loginAcceptsStoredPasswordHash() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        ClientAuthService service = new ClientAuthService(mapper, new ClientTokenService(), walletCoreService);
        MemberProfile member = member();
        member.setPasswordHash(BCrypt.hashpw("Secret123"));
        when(mapper.selectByUsername("000000", "demo_player")).thenReturn(member);
        ClientLoginBo bo = new ClientLoginBo();
        bo.setUsername("demo_player");
        bo.setPassword("Secret123");

        ClientLoginVo result = service.login(bo);

        assertNotNull(result.getAccessToken());
        assertEquals("demo_player", result.getMember().getUsername());
    }

    @Test
    @Tag("local")
    void registerRejectsDuplicateUsername() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        ClientAuthService service = new ClientAuthService(mapper, new ClientTokenService(), walletCoreService);
        when(mapper.selectByUsername("000000", "alice")).thenReturn(member());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.register(registerBo("alice")));

        assertEquals("member.username.exists", exception.getMessage());
        verify(mapper, never()).insert(any(MemberProfile.class));
        verify(walletCoreService, never()).credit(any(WalletCreditBo.class));
    }

    @Test
    @Tag("local")
    void registerCreatesMemberCreditsGcAndScThenReturnsToken() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        ClientAuthService service = new ClientAuthService(mapper, new ClientTokenService(), walletCoreService);
        when(mapper.selectByUsername("000000", "alice")).thenReturn(null);
        when(mapper.insert(org.mockito.ArgumentMatchers.<MemberProfile>any())).thenReturn(1);
        when(walletCoreService.credit(any(WalletCreditBo.class))).thenReturn(successWalletTransaction());

        ClientLoginVo result = service.register(registerBo("alice"));

        assertNotNull(result.getAccessToken());
        assertEquals("alice", result.getMember().getUsername());
        ArgumentCaptor<MemberProfile> memberCaptor = ArgumentCaptor.forClass(MemberProfile.class);
        verify(mapper).insert(memberCaptor.capture());
        MemberProfile inserted = memberCaptor.getValue();
        assertEquals("alice", inserted.getUsername());
        assertEquals("Alice", inserted.getNickname());
        assertEquals("h5", inserted.getRegisterChannel());
        assertEquals(Boolean.TRUE, inserted.getAgeConfirmed());
        assertEquals(Boolean.TRUE, inserted.getTermsAccepted());
        assertEquals(Boolean.TRUE, inserted.getPrivacyAccepted());
        assertEquals(Boolean.TRUE, inserted.getSweepstakesRulesAccepted());
        assertNotNull(inserted.getPasswordHash());
        assertEquals(true, BCrypt.checkpw("Secret123", inserted.getPasswordHash()));
        verify(walletCoreService).credit(argThat(bo ->
            "GC".equals(bo.getCurrencyCode())
                && "REGISTER_BONUS".equals(bo.getSourceType())
                && "register:bonus:000000:alice:GC".equals(bo.getIdempotencyKey())
        ));
        verify(walletCoreService).credit(argThat(bo ->
            "SC".equals(bo.getCurrencyCode())
                && "REGISTER_BONUS".equals(bo.getSourceType())
                && "register:bonus:000000:alice:SC".equals(bo.getIdempotencyKey())
        ));
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

    private ClientRegisterBo registerBo(String username) {
        ClientRegisterBo bo = new ClientRegisterBo();
        bo.setUsername(username);
        bo.setPassword("Secret123");
        bo.setNickname("Alice");
        bo.setCountryCode("US");
        bo.setStateCode("CA");
        bo.setAgeConfirmed(true);
        bo.setTermsAccepted(true);
        bo.setPrivacyAccepted(true);
        bo.setSweepstakesRulesAccepted(true);
        return bo;
    }

    private WalletTransaction successWalletTransaction() {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT_REGISTER_BONUS");
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        return transaction;
    }
}
