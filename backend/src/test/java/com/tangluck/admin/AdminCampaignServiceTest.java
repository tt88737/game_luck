package com.tangluck.admin;

import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.promotion.PromotionCampaignRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AdminCampaignServiceTest {
    @Autowired
    private AdminCampaignService adminCampaignService;

    @Autowired
    private PromotionCampaignRepository campaignRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void createsDraftCampaign() {
        var response = adminCampaignService.createCampaign(validRequest("ops_daily_test"));

        assertThat(response.status()).isEqualTo("draft");
        assertThat(campaignRepository.findByCampaignCode("ops_daily_test")).isPresent();
    }

    @Test
    void publishBlocksScCampaignWithoutLegalApproval() {
        adminCampaignService.createCampaign(validRequest("ops_missing_legal").withLegalApprovalId(null));

        assertThatThrownBy(() -> adminCampaignService.publish("ops_missing_legal", 1L, "ops_admin", "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.LEGAL_APPROVAL_REQUIRED));
    }

    @Test
    void publishActivatesCampaignAndWritesAudit() {
        adminCampaignService.createCampaign(validRequest("ops_publish"));

        var response = adminCampaignService.publish("ops_publish", 1L, "ops_admin", "127.0.0.1");

        assertThat(response.status()).isEqualTo("active");
        assertThat(auditLogRepository.findByTargetTypeAndTargetId("promotion_campaign", "ops_publish"))
                .anySatisfy(log -> assertThat(log.getAction()).isEqualTo("campaign_publish"));
    }

    @Test
    void pauseCampaignWritesAudit() {
        adminCampaignService.createCampaign(validRequest("ops_pause"));
        adminCampaignService.publish("ops_pause", 1L, "ops_admin", "127.0.0.1");

        var response = adminCampaignService.pause("ops_pause", 1L, "ops_admin", "127.0.0.1");

        assertThat(response.status()).isEqualTo("paused");
        assertThat(auditLogRepository.findByTargetTypeAndTargetId("promotion_campaign", "ops_pause"))
                .anySatisfy(log -> assertThat(log.getAction()).isEqualTo("campaign_pause"));
    }

    @Test
    void dashboardSummaryReturnsOperationalMetrics() {
        var summary = adminCampaignService.dashboardSummary();

        assertThat(summary.registrations()).isGreaterThanOrEqualTo(0);
        assertThat(summary.claims()).isGreaterThanOrEqualTo(0);
        assertThat(summary.scGranted()).isNotNull();
        assertThat(summary.riskEvents()).isGreaterThanOrEqualTo(0);
    }

    private AdminCampaignRequest validRequest(String code) {
        return new AdminCampaignRequest(
                code,
                "Ops Daily",
                "daily_login",
                List.of("CA", "TX"),
                List.of("WA"),
                List.of(new AdminRewardRequest("GC", "1000"), new AdminRewardRequest("SC", "0.05")),
                "default_small_sc",
                "rules-v1",
                "LEGAL-2026-0617-CA",
                "gc_only"
        );
    }
}
