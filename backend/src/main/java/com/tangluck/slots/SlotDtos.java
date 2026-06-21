package com.tangluck.slots;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SlotDtos {
    private SlotDtos() {
    }

    public record SlotGameDto(
            String gameCode,
            String name,
            String status,
            int reelCount,
            int rowCount,
            BigDecimal minBet,
            BigDecimal maxBet,
            String currency,
            int sortOrder,
            String legalApprovalId
    ) {
    }

    public record SpinRequest(
            @NotBlank String currency,
            @DecimalMin("0.0001") BigDecimal betAmount
    ) {
    }

    public record SlotRoundDto(
            String roundId,
            Long userId,
            String gameCode,
            String currency,
            BigDecimal betAmount,
            BigDecimal payoutAmount,
            BigDecimal multiplier,
            List<List<String>> reels,
            String status,
            Long debitLedgerId,
            Long creditLedgerId,
            Instant createdAt
    ) {
    }

    public record SlotRoundPage(
            List<SlotRoundDto> items,
            int page,
            int pageSize,
            long total
    ) {
    }

    public record UpdateSlotGameRequest(
            @NotBlank String name,
            @NotBlank String status,
            @DecimalMin("0.0001") BigDecimal minBet,
            @DecimalMin("0.0001") BigDecimal maxBet,
            int sortOrder,
            String legalApprovalId
    ) {
    }
}
