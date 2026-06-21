package com.tangluck.activity;

import java.math.BigDecimal;
import java.util.List;

public final class ActivityDtos {
    private ActivityDtos() {
    }

    public record ActivityTaskDto(
            String taskCode,
            String name,
            String targetType,
            BigDecimal progress,
            BigDecimal target,
            String status,
            String rewardCurrency,
            BigDecimal rewardAmount
    ) {
    }

    public record ActivitySummaryDto(
            List<ActivityTaskDto> tasks,
            long claimableCount
    ) {
    }

    public record TaskClaimDto(
            String taskCode,
            String status,
            String rewardCurrency,
            BigDecimal rewardAmount,
            Long ledgerId
    ) {
    }

    public record AdminTaskMetricDto(
            String taskCode,
            String name,
            String targetType,
            long completedCount,
            BigDecimal rewardAmount
    ) {
    }

    public record AdminActivityDashboardDto(
            long totalParticipants,
            long completedTasks,
            BigDecimal gcGranted,
            List<AdminTaskMetricDto> tasks
    ) {
    }
}
