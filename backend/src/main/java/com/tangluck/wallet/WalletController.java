package com.tangluck.wallet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.tangluck.wallet.WalletDtos.LedgerPageResponse;
import static com.tangluck.wallet.WalletDtos.WalletSummaryResponse;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/summary")
    public WalletSummaryResponse summary(@RequestHeader("X-User-Id") Long userId) {
        return walletService.summary(userId);
    }

    @GetMapping("/ledger")
    public LedgerPageResponse ledger(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String currency,
            @RequestParam(required = false, name = "business_type") String businessType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20", name = "page_size") int pageSize
    ) {
        return walletService.ledger(userId, currency, businessType, page, pageSize);
    }
}
