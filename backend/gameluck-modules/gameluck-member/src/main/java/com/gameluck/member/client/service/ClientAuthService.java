package com.gameluck.member.client.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.member.client.domain.bo.ClientLoginBo;
import com.gameluck.member.client.domain.bo.ClientRegisterBo;
import com.gameluck.member.client.domain.vo.ClientLoginVo;
import com.gameluck.member.client.domain.vo.ClientMemberVo;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@RequiredArgsConstructor
@Service
public class ClientAuthService {

    private static final String TENANT_ID = "000000";
    private static final String DEMO_PASSWORD = "Demo123456";
    private static final String REGISTER_CHANNEL = "h5";
    private static final String REGISTER_BONUS_SOURCE = "REGISTER_BONUS";
    private static final BigDecimal REGISTER_GC_BONUS = new BigDecimal("1000.000000");
    private static final BigDecimal REGISTER_SC_BONUS = new BigDecimal("25.000000");

    private final MemberProfileMapper memberProfileMapper;
    private final ClientTokenService clientTokenService;
    private final IWalletCoreService walletCoreService;

    public ClientLoginVo login(ClientLoginBo bo) {
        String username = normalizeUsername(bo.getUsername());
        MemberProfile member = memberProfileMapper.selectByUsername(TENANT_ID, username);
        if (member == null || !passwordMatches(member, bo.getPassword())) {
            throw new ServiceException(MessageUtils.message("client.auth.invalid.credentials"));
        }
        member.setLastLoginTime(new Date());
        memberProfileMapper.updateById(member);
        return loginVo(member);
    }

    @Transactional(rollbackFor = Exception.class)
    public ClientLoginVo register(ClientRegisterBo bo) {
        String username = normalizeUsername(bo.getUsername());
        if (memberProfileMapper.selectByUsername(TENANT_ID, username) != null) {
            throw new ServiceException(MessageUtils.message("member.username.exists"));
        }
        Date now = new Date();
        MemberProfile member = new MemberProfile();
        member.setId(IdUtil.getSnowflakeNextId());
        member.setTenantId(TENANT_ID);
        member.setMemberNo("MB" + IdUtil.getSnowflakeNextIdStr());
        member.setUsername(username);
        member.setNickname(StringUtils.blankToDefault(StringUtils.trim(bo.getNickname()), username));
        member.setPasswordHash(BCrypt.hashpw(bo.getPassword()));
        member.setStatus("ACTIVE");
        member.setRiskLevel("NORMAL");
        member.setRegisterChannel(REGISTER_CHANNEL);
        member.setCountryCode(StringUtils.trim(bo.getCountryCode()));
        member.setStateCode(StringUtils.trim(bo.getStateCode()));
        member.setAgeConfirmed(Boolean.TRUE.equals(bo.getAgeConfirmed()));
        member.setTermsAccepted(Boolean.TRUE.equals(bo.getTermsAccepted()));
        member.setPrivacyAccepted(Boolean.TRUE.equals(bo.getPrivacyAccepted()));
        member.setSweepstakesRulesAccepted(Boolean.TRUE.equals(bo.getSweepstakesRulesAccepted()));
        member.setLastLoginTime(now);
        member.setVersion(0);
        member.setDelFlag(SystemConstants.NORMAL);
        member.setCreateTime(now);
        member.setUpdateTime(now);
        memberProfileMapper.insert(member);
        creditRegisterBonus(member, "GC", REGISTER_GC_BONUS);
        creditRegisterBonus(member, "SC", REGISTER_SC_BONUS);
        return loginVo(member);
    }

    private ClientLoginVo loginVo(MemberProfile member) {
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
        vo.setKycStatus("NOT_STARTED");
        return vo;
    }

    private void creditRegisterBonus(MemberProfile member, String currencyCode, BigDecimal amount) {
        WalletCreditBo bo = new WalletCreditBo();
        bo.setIdempotencyKey("register:bonus:" + TENANT_ID + ":" + member.getUsername() + ":" + currencyCode);
        bo.setMemberId(member.getId());
        bo.setCurrencyCode(currencyCode);
        bo.setSourceType(REGISTER_BONUS_SOURCE);
        bo.setBusinessNo(member.getMemberNo());
        bo.setAmount(amount);
        bo.setRemark(MessageUtils.message("client.register.wallet.remark"));
        WalletTransaction transaction = walletCoreService.credit(bo);
        if (!WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            throw new ServiceException(MessageUtils.message("client.register.wallet.credit.fail"));
        }
    }

    private boolean passwordMatches(MemberProfile member, String rawPassword) {
        if (StringUtils.isBlank(member.getPasswordHash())) {
            return DEMO_PASSWORD.equals(rawPassword);
        }
        return BCrypt.checkpw(rawPassword, member.getPasswordHash());
    }

    private String normalizeUsername(String username) {
        String normalized = StringUtils.trim(username);
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException(MessageUtils.message("client.auth.username.required"));
        }
        return normalized;
    }
}
