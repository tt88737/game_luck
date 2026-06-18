package com.tangluck.wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class WalletDtos {
    private WalletDtos() {
    }

    public record LedgerDto(
            Long ledgerId,
            String currency,
            BigDecimal amount,
            String direction,
            String businessType,
            String businessId,
            String status,
            Instant createdAt
    ) {
    }

    public record WalletSummaryDto(
            BigDecimal gcBalance,
            BigDecimal scBalance,
            BigDecimal scFrozen,
            BigDecimal scRedeemable
    ) {
    }

    public record ScSourceDto(
            String source,
            BigDecimal amount
    ) {
    }

    public record WalletSummaryResponse(
            WalletSummaryDto wallet,
            List<ScSourceDto> scSourceSummary,
            List<String> notices
    ) {
    }

    public record LedgerPageResponse(
            List<LedgerDto> items,
            int page,
            int pageSize,
            long total
    ) {
    }
}
