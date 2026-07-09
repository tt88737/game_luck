package com.gameluck.wallet.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientWalletServiceTest {

    @Test
    @Tag("local")
    void accountsAreReadForCurrentMemberOnly() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, tokenService);
        WalletAccount account = new WalletAccount();
        account.setCurrencyCode("GC");
        account.setAvailableBalance(new BigDecimal("1000.00"));
        account.setFrozenBalance(BigDecimal.ZERO);
        account.setStatus("NORMAL");
        when(accountMapper.selectClientAccounts("000000", 1001L)).thenReturn(List.of(account));

        List<ClientWalletAccountVo> result = service.accounts("Bearer " + tokenService.issue(1001L));

        assertEquals(1, result.size());
        assertEquals("GC", result.get(0).getCurrencyCode());
        assertEquals("1000.00", result.get(0).getAvailableBalance());
    }

    @Test
    @Tag("local")
    void ledgerPageSizeIsCappedAtFifty() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, tokenService);
        WalletTransaction tx = new WalletTransaction();
        tx.setId(9001L);
        tx.setCurrencyCode("GC");
        tx.setOperation("credit");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setBalanceAfter(new BigDecimal("1010.00"));
        tx.setSourceType("demo_seed");
        tx.setCreateTime(new Date());
        when(transactionMapper.selectClientLedgers("000000", 1001L, "GC", 0, 50)).thenReturn(List.of(tx));
        when(transactionMapper.countClientLedgers("000000", 1001L, "GC")).thenReturn(1L);

        ClientPageVo<ClientWalletLedgerVo> result = service.ledgers("Bearer " + tokenService.issue(1001L), "GC", 1, 500);

        assertEquals(1L, result.getTotal());
        assertEquals("credit", result.getRecords().get(0).getDirection());
    }
}
