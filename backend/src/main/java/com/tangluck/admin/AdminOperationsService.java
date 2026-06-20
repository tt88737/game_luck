package com.tangluck.admin;

import com.tangluck.auth.User;
import com.tangluck.auth.UserRepository;
import com.tangluck.wallet.WalletLedger;
import com.tangluck.wallet.WalletLedgerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tangluck.admin.AdminOperationsDtos.AdminUserDto;
import static com.tangluck.admin.AdminOperationsDtos.AdminWalletLedgerDto;

@Service
public class AdminOperationsService {
    private final UserRepository userRepository;
    private final WalletLedgerRepository walletLedgerRepository;

    public AdminOperationsService(UserRepository userRepository, WalletLedgerRepository walletLedgerRepository) {
        this.userRepository = userRepository;
        this.walletLedgerRepository = walletLedgerRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> users() {
        return userRepository.findAll(PageRequest.of(0, 50)).stream()
                .map(this::toUserDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminWalletLedgerDto> walletLedger() {
        return walletLedgerRepository.findAll(PageRequest.of(0, 50)).stream()
                .map(this::toLedgerDto)
                .toList();
    }

    private AdminUserDto toUserDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getCountryCode(),
                user.getStateCode(),
                user.getStatus(),
                user.getRiskLevel()
        );
    }

    private AdminWalletLedgerDto toLedgerDto(WalletLedger ledger) {
        return new AdminWalletLedgerDto(
                ledger.getId(),
                ledger.getUserId(),
                ledger.getCurrency(),
                ledger.getDirection(),
                ledger.getAmount(),
                ledger.getBalanceAfter(),
                ledger.getFrozenAfter(),
                ledger.getBusinessType(),
                ledger.getBusinessId(),
                ledger.getStatus(),
                ledger.getCreatedAt()
        );
    }
}
