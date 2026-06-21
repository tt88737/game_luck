package com.tangluck.notifications;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record InboxItemDto(
            Long id,
            Long userId,
            String title,
            String message,
            String rewardCurrency,
            BigDecimal rewardAmount,
            String status,
            String sourceType,
            String sourceId,
            Long ledgerId,
            Instant createdAt,
            Instant expiresAt,
            Instant claimedAt
    ) {
    }

    public record ManualGrantRequest(
            @NotNull Long userId,
            @NotBlank String title,
            @NotBlank String message,
            @NotBlank String rewardCurrency,
            @DecimalMin("0.0001") BigDecimal rewardAmount,
            @NotBlank String sourceType,
            @NotBlank String sourceId
    ) {
    }
}
