package com.tangluck.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminCampaignService adminCampaignService;
    private final AuditLogRepository auditLogRepository;

    public AdminController(AdminCampaignService adminCampaignService, AuditLogRepository auditLogRepository) {
        this.adminCampaignService = adminCampaignService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummary dashboardSummary() {
        return adminCampaignService.dashboardSummary();
    }

    @PostMapping("/campaigns")
    public AdminCampaignResponse createCampaign(@RequestBody AdminCampaignRequest request, HttpServletRequest servletRequest) {
        AdminOperatorContext.from(servletRequest).require("campaign.write");
        return adminCampaignService.createCampaign(request);
    }

    @PostMapping("/campaigns/{campaignCode}/publish")
    public AdminCampaignResponse publish(@PathVariable String campaignCode, HttpServletRequest servletRequest) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("campaign.publish");
        return adminCampaignService.publish(campaignCode, operator);
    }

    @PostMapping("/campaigns/{campaignCode}/pause")
    public AdminCampaignResponse pause(@PathVariable String campaignCode, HttpServletRequest servletRequest) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("campaign.publish");
        return adminCampaignService.pause(campaignCode, operator);
    }

    @GetMapping("/audit-logs")
    public List<AuditLogDto> auditLogs(
            @RequestParam(name = "target_type") String targetType,
            @RequestParam(name = "target_id") String targetId
    ) {
        return auditLogRepository.findByTargetTypeAndTargetId(targetType, targetId).stream()
                .map(log -> new AuditLogDto(
                        log.getId(),
                        log.getOperatorId(),
                        log.getOperatorRole(),
                        log.getAction(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getBeforeJson(),
                        log.getAfterJson(),
                        log.getIp(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
