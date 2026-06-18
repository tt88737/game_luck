package com.tangluck.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {
    Optional<WalletLedger> findByIdempotencyKey(String idempotencyKey);

    List<WalletLedger> findByUserId(Long userId);

    Page<WalletLedger> findByUserIdAndCurrencyOrderByCreatedAtDesc(Long userId, String currency, Pageable pageable);

    Page<WalletLedger> findByUserIdAndCurrencyAndBusinessTypeOrderByCreatedAtDesc(Long userId, String currency, String businessType, Pageable pageable);
}
