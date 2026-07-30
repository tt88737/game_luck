package com.gameluck.wallet.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.wallet.client.domain.bo.ClientExchangeOrderBo;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOrderVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletCurrencyVo;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import com.gameluck.wallet.service.IWalletCurrencyPolicyService;
import com.gameluck.wallet.service.IWalletExchangeOrderService;
import com.gameluck.wallet.service.IWalletExchangeRuleService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientWalletServiceTest {

    @Test
    @Tag("local")
    void accountsAreReadForCurrentMemberOnly() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        IWalletCurrencyPolicyService policyService = mock(IWalletCurrencyPolicyService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, policyService,
            mock(IWalletExchangeRuleService.class), mock(IWalletExchangeOrderService.class), tokenService);
        WalletAccount account = new WalletAccount();
        account.setCurrencyCode("GC");
        account.setAvailableBalance(new BigDecimal("1000.00"));
        account.setFrozenBalance(BigDecimal.ZERO);
        account.setStatus("NORMAL");
        when(accountMapper.selectClientAccounts("000000", 1001L)).thenReturn(List.of(account));
        when(policyService.listClientCurrencies("000000", 1001L, "H5")).thenReturn(List.of(currency("GC")));

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
        IWalletCurrencyPolicyService policyService = mock(IWalletCurrencyPolicyService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, policyService,
            mock(IWalletExchangeRuleService.class), mock(IWalletExchangeOrderService.class), tokenService);
        WalletTransaction tx = new WalletTransaction();
        tx.setId(9001L);
        tx.setCurrencyCode("GC");
        tx.setOperation("credit");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setBalanceAfter(new BigDecimal("1010.00"));
        tx.setSourceType("demo_seed");
        tx.setCreateTime(new Date());
        when(policyService.listClientCurrencies("000000", 1001L, "H5")).thenReturn(List.of(currency("GC")));
        when(transactionMapper.selectClientLedgers("000000", 1001L, "GC", List.of("GC"), 0, 50)).thenReturn(List.of(tx));
        when(transactionMapper.countClientLedgers("000000", 1001L, "GC", List.of("GC"))).thenReturn(1L);

        ClientPageVo<ClientWalletLedgerVo> result = service.ledgers("Bearer " + tokenService.issue(1001L), "GC", 1, 500);

        assertEquals(1L, result.getTotal());
        assertEquals("credit", result.getRecords().get(0).getDirection());
    }

    @Test
    @Tag("local")
    void ledgersReturnEmptyWhenRequestedCurrencyIsHiddenByPolicy() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        IWalletCurrencyPolicyService policyService = mock(IWalletCurrencyPolicyService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, policyService,
            mock(IWalletExchangeRuleService.class), mock(IWalletExchangeOrderService.class), tokenService);
        when(policyService.listClientCurrencies("000000", 1001L, "H5")).thenReturn(List.of(currency("GC")));

        ClientPageVo<ClientWalletLedgerVo> result = service.ledgers("Bearer " + tokenService.issue(1001L), "SC", 1, 20);

        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getRecords().size());
        verify(transactionMapper, never()).selectClientLedgers(eq("000000"), eq(1001L), eq("SC"), eq(List.of("GC")), anyInt(), anyInt());
        verify(transactionMapper, never()).countClientLedgers("000000", 1001L, "SC", List.of("GC"));
    }

    @Test
    @Tag("local")
    void ledgersWithoutCurrencyAreLimitedToVisibleCurrencies() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        IWalletCurrencyPolicyService policyService = mock(IWalletCurrencyPolicyService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, policyService,
            mock(IWalletExchangeRuleService.class), mock(IWalletExchangeOrderService.class), tokenService);
        WalletTransaction tx = new WalletTransaction();
        tx.setId(9002L);
        tx.setCurrencyCode("GC");
        tx.setOperation("credit");
        tx.setAmount(new BigDecimal("20.00"));
        tx.setBalanceAfter(new BigDecimal("1020.00"));
        tx.setSourceType("demo_seed");
        tx.setCreateTime(new Date());
        when(policyService.listClientCurrencies("000000", 1001L, "H5")).thenReturn(List.of(currency("GC")));
        when(transactionMapper.selectClientLedgers("000000", 1001L, null, List.of("GC"), 0, 20)).thenReturn(List.of(tx));
        when(transactionMapper.countClientLedgers("000000", 1001L, null, List.of("GC"))).thenReturn(1L);

        ClientPageVo<ClientWalletLedgerVo> result = service.ledgers("Bearer " + tokenService.issue(1001L), null, 1, 20);

        assertEquals(1L, result.getTotal());
        assertEquals("GC", result.getRecords().get(0).getCurrencyCode());
    }

    @Test
    @Tag("local")
    void exchangeOrderIsSubmittedForCurrentMember() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        IWalletCurrencyPolicyService policyService = mock(IWalletCurrencyPolicyService.class);
        IWalletExchangeOrderService exchangeOrderService = mock(IWalletExchangeOrderService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, policyService,
            mock(IWalletExchangeRuleService.class), exchangeOrderService, tokenService);
        ClientExchangeOrderBo bo = new ClientExchangeOrderBo();
        bo.setExchangeRuleId(9001L);
        bo.setFromAmount(new BigDecimal("10.000000"));
        ClientExchangeOrderVo vo = new ClientExchangeOrderVo();
        vo.setExchangeOrderNo("WE1001");
        vo.setStatus("SUCCESS");
        when(exchangeOrderService.submit(1001L, bo)).thenReturn(vo);

        ClientExchangeOrderVo result = service.exchangeOrder("Bearer " + tokenService.issue(1001L), bo);

        assertEquals("WE1001", result.getExchangeOrderNo());
        assertEquals("SUCCESS", result.getStatus());
        verify(exchangeOrderService).submit(1001L, bo);
    }

    private static ClientWalletCurrencyVo currency(String currencyCode) {
        ClientWalletCurrencyVo currency = new ClientWalletCurrencyVo();
        currency.setCurrencyCode(currencyCode);
        currency.setCurrencyName(currencyCode + " Name");
        currency.setDecimalScale(2);
        currency.setDepositEnabled(false);
        currency.setWithdrawEnabled(false);
        currency.setExchangeEnabled(false);
        currency.setPlayEnabled(true);
        return currency;
    }
}
