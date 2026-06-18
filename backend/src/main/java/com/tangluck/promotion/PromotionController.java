package com.tangluck.promotion;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PromotionController {
    private final PromotionService promotionService;
    private final CouponService couponService;
    private final PromotionCampaignRepository campaignRepository;

    public PromotionController(PromotionService promotionService, CouponService couponService, PromotionCampaignRepository campaignRepository) {
        this.promotionService = promotionService;
        this.couponService = couponService;
        this.campaignRepository = campaignRepository;
    }

    @GetMapping("/campaigns")
    public List<CampaignDto> campaigns() {
        return campaignRepository.findAll().stream()
                .filter(campaign -> !"daily_task".equals(campaign.getCampaignType()))
                .map(campaign -> new CampaignDto(campaign.getCampaignCode(), campaign.getCampaignType(), campaign.getStatus()))
                .toList();
    }

    @PostMapping("/campaigns/{campaignCode}/claim")
    public ClaimResponse claimCampaign(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable String campaignCode
    ) {
        return promotionService.claimCampaign(userId, campaignCode, idempotencyKey);
    }

    @GetMapping("/tasks/daily")
    public List<TaskDto> dailyTasks() {
        return campaignRepository.findAll().stream()
                .filter(campaign -> "daily_task".equals(campaign.getCampaignType()))
                .map(campaign -> new TaskDto(campaign.getCampaignCode(), campaign.getCampaignCode(), 1, "in_progress"))
                .toList();
    }

    @PostMapping("/tasks/{taskCode}/progress")
    public TaskDto progressTask(@PathVariable String taskCode, @RequestBody TaskProgressRequest request) {
        var status = request.progress() >= 1 ? "completed" : "in_progress";
        return new TaskDto(taskCode, taskCode, 1, status);
    }

    @PostMapping("/tasks/{taskCode}/claim")
    public ClaimResponse claimTask(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable String taskCode
    ) {
        return promotionService.claimCampaign(userId, taskCode, idempotencyKey);
    }

    @PostMapping("/coupon/claim")
    public ClaimResponse claimCoupon(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CouponClaimRequest request
    ) {
        return couponService.claim(userId, request.code(), idempotencyKey);
    }

    public record CampaignDto(String campaignCode, String campaignType, String status) {
    }

    public record TaskDto(String taskId, String taskCode, int target, String status) {
    }

    public record TaskProgressRequest(int progress) {
    }

    public record CouponClaimRequest(String code) {
    }
}
