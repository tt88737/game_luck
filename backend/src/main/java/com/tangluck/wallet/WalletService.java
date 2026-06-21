package com.tangluck.wallet;

import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tangluck.wallet.WalletDtos.LedgerDto;
import static com.tangluck.wallet.WalletDtos.LedgerPageResponse;
import static com.tangluck.wallet.WalletDtos.ScSourceDto;
import static com.tangluck.wallet.WalletDtos.WalletSummaryDto;
import static com.tangluck.wallet.WalletDtos.WalletSummaryResponse;

@Service
public class WalletService {
    private final WalletAccountRepository accountRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final Clock clock;

    public WalletService(WalletAccountRepository accountRepository, WalletLedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public LedgerDto credit(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        return ledgerRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createCredit(userId, currency, amount, businessType, businessId, idempotencyKey));
    }

    @Transactional
    public LedgerDto debit(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        return ledgerRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createDebit(userId, currency, amount, businessType, businessId, idempotencyKey));
    }

    @Transactional
    public LedgerDto freeze(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        return ledgerRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createFreeze(userId, currency, amount, businessType, businessId, idempotencyKey));
    }

    @Transactional
    public LedgerDto unfreeze(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        return ledgerRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createUnfreeze(userId, currency, amount, businessType, businessId, idempotencyKey));
    }

    @Transactional
    public LedgerDto redeemFrozen(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        return ledgerRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createRedeemFrozen(userId, currency, amount, businessType, businessId, idempotencyKey));
    }

    @Transactional(readOnly = true)
    public WalletSummaryResponse summary(Long userId) {
        var gc = accountRepository.findByUserIdAndCurrency(userId, "GC").orElseThrow();
        var sc = accountRepository.findByUserIdAndCurrency(userId, "SC").orElseThrow();
        var sourceSummary = ledgerRepository.findByUserId(userId).stream()
                .filter(ledger -> "SC".equals(ledger.getCurrency()))
                .collect(Collectors.groupingBy(
                        WalletLedger::getBusinessType,
                        Collectors.reducing(BigDecimal.ZERO, WalletLedger::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> new ScSourceDto(entry.getKey(), entry.getValue()))
                .toList();

        return new WalletSummaryResponse(
                new WalletSummaryDto(gc.getBalance(), sc.getBalance(), sc.getFrozenBalance(), sc.getBalance().subtract(sc.getFrozenBalance())),
                sourceSummary,
                List.of(
                        "Sweeps Coins are not sold.",
                        "No Purchase Necessary / AMOE is available."
                )
        );
    }

    @Transactional(readOnly = true)
    public LedgerPageResponse ledger(Long userId, String currency, String businessType, int page, int pageSize) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
        var ledgerPage = businessType == null || businessType.isBlank()
                ? ledgerRepository.findByUserIdAndCurrencyOrderByCreatedAtDesc(userId, currency, pageable)
                : ledgerRepository.findByUserIdAndCurrencyAndBusinessTypeOrderByCreatedAtDesc(userId, currency, businessType, pageable);

        return new LedgerPageResponse(
                ledgerPage.getContent().stream().map(this::toDto).toList(),
                page,
                pageSize,
                ledgerPage.getTotalElements()
        );
    }

    private LedgerDto createCredit(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        var account = accountRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Wallet account does not exist.", Map.of("currency", currency)));
        account.credit(amount, clock.instant());
        accountRepository.save(account);

        var ledger = ledgerRepository.save(new WalletLedger(
                userId,
                account.getId(),
                currency,
                "credit",
                amount,
                account.getBalance(),
                account.getFrozenBalance(),
                businessType,
                businessId,
                idempotencyKey,
                clock.instant()
        ));

        return toDto(ledger);
    }

    private LedgerDto createDebit(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        var account = accountRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Wallet account does not exist.", Map.of("currency", currency)));
        try {
            account.debit(amount, clock.instant());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Insufficient balance.", Map.of("currency", currency));
        }
        accountRepository.save(account);

        var ledger = ledgerRepository.save(new WalletLedger(
                userId,
                account.getId(),
                currency,
                "debit",
                amount,
                account.getBalance(),
                account.getFrozenBalance(),
                businessType,
                businessId,
                idempotencyKey,
                clock.instant()
        ));

        return toDto(ledger);
    }

    private LedgerDto createFreeze(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        var account = accountRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Wallet account does not exist.", Map.of("currency", currency)));
        try {
            account.freeze(amount, clock.instant());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.REDEMPTION_NOT_ALLOWED, "Insufficient redeemable balance.", Map.of("currency", currency));
        }
        accountRepository.save(account);

        var ledger = ledgerRepository.save(new WalletLedger(
                userId,
                account.getId(),
                currency,
                "freeze",
                amount,
                account.getBalance(),
                account.getFrozenBalance(),
                businessType,
                businessId,
                idempotencyKey,
                clock.instant()
        ));

        return toDto(ledger);
    }

    private LedgerDto createUnfreeze(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        var account = accountRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Wallet account does not exist.", Map.of("currency", currency)));
        try {
            account.unfreeze(amount, clock.instant());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.REDEMPTION_NOT_ALLOWED, "Insufficient frozen balance.", Map.of("currency", currency));
        }
        accountRepository.save(account);

        var ledger = ledgerRepository.save(new WalletLedger(
                userId,
                account.getId(),
                currency,
                "unfreeze",
                amount,
                account.getBalance(),
                account.getFrozenBalance(),
                businessType,
                businessId,
                idempotencyKey,
                clock.instant()
        ));

        return toDto(ledger);
    }

    private LedgerDto createRedeemFrozen(Long userId, String currency, BigDecimal amount, String businessType, String businessId, String idempotencyKey) {
        var account = accountRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Wallet account does not exist.", Map.of("currency", currency)));
        try {
            account.redeemFrozen(amount, clock.instant());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.REDEMPTION_NOT_ALLOWED, "Insufficient frozen balance.", Map.of("currency", currency));
        }
        accountRepository.save(account);

        var ledger = ledgerRepository.save(new WalletLedger(
                userId,
                account.getId(),
                currency,
                "debit",
                amount,
                account.getBalance(),
                account.getFrozenBalance(),
                businessType,
                businessId,
                idempotencyKey,
                clock.instant()
        ));

        return toDto(ledger);
    }

    private LedgerDto toDto(WalletLedger ledger) {
        return new LedgerDto(
                ledger.getId(),
                ledger.getCurrency(),
                ledger.getAmount(),
                ledger.getDirection(),
                ledger.getBusinessType(),
                ledger.getBusinessId(),
                ledger.getStatus(),
                ledger.getCreatedAt()
        );
    }
}
