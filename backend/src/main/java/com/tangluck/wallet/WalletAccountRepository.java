package com.tangluck.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {
    List<WalletAccount> findByUserId(Long userId);

    Optional<WalletAccount> findByUserIdAndCurrency(Long userId, String currency);
}
