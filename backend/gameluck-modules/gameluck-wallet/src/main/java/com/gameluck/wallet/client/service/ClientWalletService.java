package com.gameluck.wallet.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientWalletService {

    private static final String TENANT_ID = "000000";

    private final WalletAccountMapper walletAccountMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final ClientTokenService clientTokenService;

    public List<ClientWalletAccountVo> accounts(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        return walletAccountMapper.selectClientAccounts(TENANT_ID, memberId).stream()
            .map(this::toAccount)
            .toList();
    }

    public ClientPageVo<ClientWalletLedgerVo> ledgers(String authorization, String currencyCode, Integer pageNum, Integer pageSize) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        int offset = (safePageNum - 1) * safePageSize;
        ClientPageVo<ClientWalletLedgerVo> page = new ClientPageVo<>();
        page.setRecords(walletTransactionMapper.selectClientLedgers(TENANT_ID, memberId, currencyCode, offset, safePageSize).stream()
            .map(this::toLedger)
            .toList());
        page.setTotal(walletTransactionMapper.countClientLedgers(TENANT_ID, memberId, currencyCode));
        return page;
    }

    private ClientWalletAccountVo toAccount(WalletAccount account) {
        ClientWalletAccountVo vo = new ClientWalletAccountVo();
        vo.setCurrencyCode(account.getCurrencyCode());
        vo.setCurrencyName("GC".equals(account.getCurrencyCode()) ? "Gold Coin" : "Sweep Coin");
        vo.setAvailableBalance(account.getAvailableBalance().setScale(2).toPlainString());
        vo.setFrozenBalance(account.getFrozenBalance().setScale(2).toPlainString());
        vo.setDecimalScale(2);
        vo.setPlayable(true);
        vo.setWithdrawable(false);
        return vo;
    }

    private ClientWalletLedgerVo toLedger(WalletTransaction transaction) {
        ClientWalletLedgerVo vo = new ClientWalletLedgerVo();
        vo.setLedgerId(transaction.getId());
        vo.setCurrencyCode(transaction.getCurrencyCode());
        vo.setDirection(transaction.getOperation());
        vo.setAmount(transaction.getAmount().setScale(2).toPlainString());
        vo.setAfterAvailable(transaction.getBalanceAfter().setScale(2).toPlainString());
        vo.setBizType(transaction.getSourceType());
        vo.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(transaction.getCreateTime()));
        return vo;
    }
}
