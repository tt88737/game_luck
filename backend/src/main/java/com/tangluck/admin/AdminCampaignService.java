package com.tangluck.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tangluck.auth.UserRepository;
import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.promotion.PromotionCampaign;
import com.tangluck.promotion.PromotionCampaignRepository;
import com.tangluck.promotion.PromotionClaimRepository;
import com.tangluck.wallet.WalletLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
@Service
public class AdminCampaignService {
    private final PromotionCampaignRepository campaignRepository;
    private final AdminAuditService adminAuditService;
    private final UserRepository userRepository;
    private final PromotionClaimRepository claimRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final ObjectMapper objectMapper;

    public AdminCampaignService(PromotionCampaignRepository campaignRepository, AdminAuditService adminAuditService, UserRepository userRepository, PromotionClaimRepository claimRepository, WalletLedgerRepository ledgerRepository, ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.adminAuditService = adminAuditService;
        this.userRepository = userRepository;
        this.claimRepository = claimRepository;
        this.ledgerRepository = ledgerRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AdminCampaignResponse createCampaign(AdminCampaignRequest request) {
        var campaign = campaignRepository.save(new PromotionCampaign(
                request.campaignCode(),
                request.name(),
                request.campaignType(),
                "draft",
                writeJson(request.eligibleRegions()),
                writeJson(request.blockedRegions()),
                writeJson(request.rewardPolicy()),
                request.scStrategy(),
                "daily",
                request.rulesVersion(),
                request.legalApprovalId(),
                request.riskAction()
        ));
        return new AdminCampaignResponse(campaign.getCampaignCode(), campaign.getStatus());
    }

    @Transactional
    public AdminCampaignResponse publish(String campaignCode, Long operatorId, String operatorRole, String ip) {
        return publish(campaignCode, new AdminOperatorContext(operatorId, operatorRole, java.util.Set.of("*"), ip));
    }

    @Transactional
    public AdminCampaignResponse publish(String campaignCode, AdminOperatorContext operator) {
        var campaign = campaignRepository.findByCampaignCode(campaignCode).orElseThrow();
        validatePublish(campaign);
        var before = statusJson(campaign.getStatus());
        campaign.publish();
        campaignRepository.save(campaign);
        writeAudit(operator, "campaign_publish", campaignCode, before, statusJson(campaign.getStatus()));
        return new AdminCampaignResponse(campaign.getCampaignCode(), campaign.getStatus());
    }

    @Transactional
    public AdminCampaignResponse pause(String campaignCode, Long operatorId, String operatorRole, String ip) {
        return pause(campaignCode, new AdminOperatorContext(operatorId, operatorRole, java.util.Set.of("*"), ip));
    }

    @Transactional
    public AdminCampaignResponse pause(String campaignCode, AdminOperatorContext operator) {
        var campaign = campaignRepository.findByCampaignCode(campaignCode).orElseThrow();
        var before = statusJson(campaign.getStatus());
        campaign.pause();
        campaignRepository.save(campaign);
        writeAudit(operator, "campaign_pause", campaignCode, before, statusJson(campaign.getStatus()));
        return new AdminCampaignResponse(campaign.getCampaignCode(), campaign.getStatus());
    }

    @Transactional(readOnly = true)
    public DashboardSummary dashboardSummary() {
        var scGranted = ledgerRepository.findAll().stream()
                .filter(ledger -> "SC".equals(ledger.getCurrency()))
                .map(ledger -> ledger.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummary(userRepository.count(), claimRepository.count(), scGranted, 0);
    }

    private void validatePublish(PromotionCampaign campaign) {
        if (!"gc_only".equals(campaign.getScStrategy()) && (campaign.getLegalApprovalId() == null || campaign.getLegalApprovalId().isBlank())) {
            throw new BusinessException(ErrorCode.LEGAL_APPROVAL_REQUIRED, "SC campaign requires legal approval.");
        }
        if (campaign.getRulesVersion() == null || campaign.getRulesVersion().isBlank()) {
            throw new BusinessException(ErrorCode.LEGAL_APPROVAL_REQUIRED, "Rules version is required.");
        }
    }

    private void writeAudit(AdminOperatorContext operator, String action, String targetId, String beforeJson, String afterJson) {
        adminAuditService.write(operator, action, "promotion_campaign", targetId, beforeJson, afterJson, null);
    }

    private String statusJson(String status) {
        return "{\"status\":\"" + status + "\"}";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not serialize campaign config", exception);
        }
    }
}
