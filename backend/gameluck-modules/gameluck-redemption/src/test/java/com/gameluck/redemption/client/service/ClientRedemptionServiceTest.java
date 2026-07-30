package com.gameluck.redemption.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.member.compliance.MemberComplianceAction;
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.compliance.MemberComplianceReason;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.member.service.IMemberComplianceGateService;
import com.gameluck.redemption.client.domain.bo.ClientRedemptionRequestBo;
import com.gameluck.redemption.client.domain.vo.ClientRedemptionVo;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.domain.bo.RedemptionOrderBo;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.redemption.service.IRedemptionOrderService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientRedemptionServiceTest {

    @Test
    @Tag("local")
    void listReturnsCurrentMemberOrdersOnly() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        IMemberComplianceGateService complianceGateService = mock(IMemberComplianceGateService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, memberMapper, complianceGateService, tokenService);
        when(mapper.selectClientOrders("000000", 1001L, 0, 20)).thenReturn(List.of(order()));

        List<ClientRedemptionVo> result = service.redemptions("Bearer " + tokenService.issue(1001L));

        assertEquals(1, result.size());
        assertEquals("RD1001", result.get(0).getOrderNo());
        assertEquals("PENDING", result.get(0).getStatus());
    }

    @Test
    @Tag("local")
    void requestCreatesScRedemptionForCurrentMember() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        IMemberComplianceGateService complianceGateService = mock(IMemberComplianceGateService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, memberMapper, complianceGateService, tokenService);
        MemberProfile member = eligibleMember();
        when(memberMapper.selectClientMember("000000", 1001L)).thenReturn(member);
        when(complianceGateService.evaluate(any(MemberComplianceContext.class))).thenReturn(allowDecision());
        when(orderService.insertByBo(any(RedemptionOrderBo.class))).thenReturn(true);
        ClientRedemptionRequestBo bo = redemptionRequest("SC");

        ClientRedemptionVo result = service.request("Bearer " + tokenService.issue(1001L), bo);

        assertEquals("SC", result.getCurrencyCode());
        assertEquals("1.00", result.getAmount());
        assertEquals("PENDING", result.getStatus());
        verify(orderService).insertByBo(any(RedemptionOrderBo.class));
        ArgumentCaptor<MemberComplianceContext> contextCaptor = ArgumentCaptor.forClass(MemberComplianceContext.class);
        verify(complianceGateService).evaluate(contextCaptor.capture());
        MemberComplianceContext context = contextCaptor.getValue();
        assertEquals("000000", context.getTenantId());
        assertEquals("SC", context.getCurrencyCode());
        assertEquals("h5", context.getChannel());
        assertEquals(member.getId(), context.getMember().getId());
        assertEquals(MemberComplianceAction.REDEMPTION_REQUEST, context.getAction());
    }

    @Test
    @Tag("local")
    void requestRejectsUnsupportedCurrency() {
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        when(memberMapper.selectClientMember("000000", 1001L)).thenReturn(eligibleMember());
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        IMemberComplianceGateService complianceGateService = mock(IMemberComplianceGateService.class);
        when(complianceGateService.evaluate(any(MemberComplianceContext.class))).thenReturn(allowDecision());
        ClientRedemptionService service = new ClientRedemptionService(
            mock(RedemptionOrderMapper.class), orderService, memberMapper, complianceGateService, new ClientTokenService());
        ClientRedemptionRequestBo bo = redemptionRequest("GC");

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.request("Bearer " + new ClientTokenService().issue(1001L), bo));

        assertEquals("client.redemption.currency.unsupported", exception.getMessage());
        verify(orderService, never()).insertByBo(any(RedemptionOrderBo.class));
    }

    @Test
    @Tag("local")
    void requestRejectsMissingMember() {
        ServiceException exception = assertGateFailure(null, "client.redemption.member.not.exists",
            MemberComplianceReason.MEMBER_NOT_EXISTS);

        assertEquals("client.redemption.member.not.exists", exception.getMessage());
    }

    @Test
    @Tag("local")
    void requestRejectsInactiveMember() {
        ServiceException exception = assertGateFailure(eligibleMember(), "client.redemption.member.inactive",
            MemberComplianceReason.MEMBER_INACTIVE);

        assertEquals("client.redemption.member.inactive", exception.getMessage());
    }

    @Test
    @Tag("local")
    void requestRejectsHighRiskMember() {
        ServiceException exception = assertGateFailure(eligibleMember(), "client.redemption.risk.blocked",
            MemberComplianceReason.RISK_BLOCKED);

        assertEquals("client.redemption.risk.blocked", exception.getMessage());
    }

    @Test
    @Tag("local")
    void requestRejectsMissingAgeConfirmation() {
        ServiceException exception = assertGateFailure(eligibleMember(), "client.redemption.age.required",
            MemberComplianceReason.AGE_REQUIRED);

        assertEquals("client.redemption.age.required", exception.getMessage());
    }

    @Test
    @Tag("local")
    void requestRejectsMissingAgreements() {
        ServiceException exception = assertGateFailure(eligibleMember(), "client.redemption.agreements.required",
            MemberComplianceReason.AGREEMENTS_REQUIRED);

        assertEquals("client.redemption.agreements.required", exception.getMessage());
    }

    @Test
    @Tag("local")
    void requestRejectsNotStartedKycMember() {
        ServiceException exception = assertGateFailure(eligibleMember(), "client.redemption.kyc.required",
            MemberComplianceReason.KYC_REQUIRED);

        assertEquals("client.redemption.kyc.required", exception.getMessage());
    }

    @Test
    @Tag("local")
    void requestRejectsPendingKycMember() {
        ServiceException exception = assertGateFailure(eligibleMember(), "client.redemption.kyc.required",
            MemberComplianceReason.KYC_REQUIRED);

        assertEquals("client.redemption.kyc.required", exception.getMessage());
    }

    @Test
    @Tag("local")
    void requestRejectsDeniedRegion() {
        ServiceException exception = assertGateFailure(eligibleMember(), "client.redemption.region.blocked",
            MemberComplianceReason.REGION_BLOCKED);

        assertEquals("client.redemption.region.blocked", exception.getMessage());
    }

    @Test
    @Tag("local")
    void requestAllowsRegionWhenHigherPriorityPolicyAllowsIt() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        IMemberComplianceGateService complianceGateService = mock(IMemberComplianceGateService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, memberMapper, complianceGateService, tokenService);
        MemberProfile member = eligibleMember();
        member.setCountryCode("US");
        member.setStateCode("WA");
        when(memberMapper.selectClientMember("000000", 1001L)).thenReturn(member);
        when(complianceGateService.evaluate(any(MemberComplianceContext.class))).thenReturn(allowDecision());
        ClientRedemptionRequestBo bo = redemptionRequest("SC");

        ClientRedemptionVo result = service.request("Bearer " + tokenService.issue(1001L), bo);

        assertEquals("PENDING", result.getStatus());
        verify(orderService).insertByBo(any(RedemptionOrderBo.class));
    }

    private ServiceException assertGateFailure(MemberProfile member, String messageKey, MemberComplianceReason reason) {
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        IMemberComplianceGateService complianceGateService = mock(IMemberComplianceGateService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientRedemptionService service = new ClientRedemptionService(
            mock(RedemptionOrderMapper.class), orderService, memberMapper, complianceGateService, tokenService);
        when(memberMapper.selectClientMember("000000", 1001L)).thenReturn(member);
        when(complianceGateService.evaluate(any(MemberComplianceContext.class))).thenReturn(denyDecision(messageKey, reason));
        ClientRedemptionRequestBo bo = redemptionRequest("SC");

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.request("Bearer " + tokenService.issue(1001L), bo));

        verify(orderService, never()).insertByBo(any(RedemptionOrderBo.class));
        return exception;
    }

    private MemberComplianceDecision allowDecision() {
        return MemberComplianceDecision.builder()
            .allowed(true)
            .reasonCode(MemberComplianceReason.ALLOWED.name())
            .build();
    }

    private MemberComplianceDecision denyDecision(String messageKey, MemberComplianceReason reason) {
        return MemberComplianceDecision.builder()
            .allowed(false)
            .reasonCode(reason.name())
            .messageKey(messageKey)
            .build();
    }

    private ClientRedemptionRequestBo redemptionRequest(String currencyCode) {
        ClientRedemptionRequestBo bo = new ClientRedemptionRequestBo();
        bo.setCurrencyCode(currencyCode);
        bo.setAmount(new BigDecimal("1.00"));
        return bo;
    }

    private MemberProfile eligibleMember() {
        MemberProfile member = new MemberProfile();
        member.setId(1001L);
        member.setTenantId("000000");
        member.setStatus("ACTIVE");
        member.setRiskLevel("NORMAL");
        member.setKycStatus("APPROVED");
        member.setCountryCode("US");
        member.setStateCode("CA");
        member.setAgeConfirmed(true);
        member.setTermsAccepted(true);
        member.setPrivacyAccepted(true);
        member.setSweepstakesRulesAccepted(true);
        return member;
    }

    private RedemptionOrder order() {
        RedemptionOrder order = new RedemptionOrder();
        order.setId(1L);
        order.setRedemptionOrderNo("RD1001");
        order.setCurrencyCode("SC");
        order.setAmount(new BigDecimal("1.00"));
        order.setStatus("PENDING");
        order.setFreezeNo("WF1001");
        order.setCreateTime(new Date());
        return order;
    }
}
