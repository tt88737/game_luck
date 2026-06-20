package com.tangluck.admin;

import java.math.BigDecimal;
import java.time.Instant;

public final class AdminOperationsDtos {
    private AdminOperationsDtos() {
    }

    public record AdminUserDto(
            Long userId,
            String email,
            String countryCode,
            String stateCode,
            String status,
            String riskLevel
    ) {
    }

    public record AdminWalletLedgerDto(
            Long ledgerId,
            Long userId,
            String currency,
            String direction,
            BigDecimal amount,
            BigDecimal balanceAfter,
            BigDecimal frozenAfter,
            String businessType,
            String businessId,
            String status,
            Instant createdAt
    ) {
    }
}
