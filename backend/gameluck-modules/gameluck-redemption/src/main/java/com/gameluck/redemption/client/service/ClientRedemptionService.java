package com.gameluck.redemption.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.member.compliance.MemberComplianceAction;
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.member.service.IMemberComplianceGateService;
import com.gameluck.redemption.client.domain.bo.ClientRedemptionRequestBo;
import com.gameluck.redemption.client.domain.vo.ClientRedemptionVo;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.domain.bo.RedemptionOrderBo;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.redemption.service.IRedemptionOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientRedemptionService {

    private static final String TENANT_ID = "000000";
    private static final String SUPPORTED_CURRENCY = "SC";

    private final RedemptionOrderMapper redemptionOrderMapper;
    private final IRedemptionOrderService redemptionOrderService;
    private final MemberProfileMapper memberProfileMapper;
    private final IMemberComplianceGateService complianceGateService;
    private final ClientTokenService clientTokenService;

    public List<ClientRedemptionVo> redemptions(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        return redemptionOrderMapper.selectClientOrders(TENANT_ID, memberId, 0, 20).stream()
            .map(this::toClientRedemption)
            .toList();
    }

    public ClientRedemptionVo request(String authorization, ClientRedemptionRequestBo bo) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        MemberProfile member = memberProfileMapper.selectClientMember(TENANT_ID, memberId);
        MemberComplianceDecision decision = complianceGateService.evaluate(MemberComplianceContext.builder()
            .tenantId(TENANT_ID)
            .member(member)
            .action(MemberComplianceAction.REDEMPTION_REQUEST)
            .currencyCode(SUPPORTED_CURRENCY)
            .channel("h5")
            .build());
        if (!decision.isAllowed()) {
            throw new ServiceException(MessageUtils.message(decision.getMessageKey()));
        }
        if (!SUPPORTED_CURRENCY.equals(bo.getCurrencyCode())) {
            throw new ServiceException(MessageUtils.message("client.redemption.currency.unsupported"));
        }
        RedemptionOrderBo orderBo = new RedemptionOrderBo();
        orderBo.setMemberId(memberId);
        orderBo.setCurrencyCode(SUPPORTED_CURRENCY);
        orderBo.setAmount(bo.getAmount());
        orderBo.setRedemptionMethod("SIMULATED");
        orderBo.setAccountRef("H5_DEMO");
        orderBo.setRemark(MessageUtils.message("client.redemption.request.remark"));
        redemptionOrderService.insertByBo(orderBo);

        ClientRedemptionVo vo = new ClientRedemptionVo();
        vo.setCurrencyCode(SUPPORTED_CURRENCY);
        vo.setAmount(formatAmount(bo.getAmount()));
        vo.setStatus("PENDING");
        return vo;
    }

    private ClientRedemptionVo toClientRedemption(RedemptionOrder order) {
        ClientRedemptionVo vo = new ClientRedemptionVo();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getRedemptionOrderNo());
        vo.setCurrencyCode(order.getCurrencyCode());
        vo.setAmount(formatAmount(order.getAmount()));
        vo.setStatus(order.getStatus());
        vo.setWalletFreezeNo(order.getFreezeNo());
        vo.setReviewRemark(order.getAuditReason());
        if (order.getCreateTime() != null) {
            vo.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(order.getCreateTime()));
        }
        return vo;
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2).toPlainString();
    }
}
