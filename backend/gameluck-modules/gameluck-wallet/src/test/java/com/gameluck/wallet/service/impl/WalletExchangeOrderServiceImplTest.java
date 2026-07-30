package com.gameluck.wallet.service.impl;

import com.gameluck.wallet.client.domain.bo.ClientExchangeOrderBo;
import com.gameluck.wallet.domain.WalletExchangeOrder;
import com.gameluck.wallet.domain.WalletExchangeRule;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletDebitBo;
import com.gameluck.wallet.mapper.WalletExchangeOrderMapper;
import com.gameluck.wallet.mapper.WalletExchangeRuleMapper;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletExchangeOrderServiceImplTest {

    @Test
    @Tag("local")
    void submitDebitsSourceAndCreditsTargetWithPercentFee() {
        WalletExchangeRuleMapper ruleMapper = mock(WalletExchangeRuleMapper.class);
        WalletExchangeOrderMapper orderMapper = mock(WalletExchangeOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        when(ruleMapper.selectById(9001L)).thenReturn(enabledRule());
        when(orderMapper.insert(any(WalletExchangeOrder.class))).thenReturn(1);
        WalletTransaction debit = transaction("WT-DEBIT", "SUCCESS");
        WalletTransaction credit = transaction("WT-CREDIT", "SUCCESS");
        when(walletCoreService.debit(any(WalletDebitBo.class))).thenReturn(debit);
        when(walletCoreService.credit(any(WalletCreditBo.class))).thenReturn(credit);
        WalletExchangeOrderServiceImpl service = new WalletExchangeOrderServiceImpl(ruleMapper, orderMapper, walletCoreService);

        ClientExchangeOrderBo bo = new ClientExchangeOrderBo();
        bo.setExchangeRuleId(9001L);
        bo.setFromAmount(new BigDecimal("10.000000"));
        bo.setIdempotencyKey("exchange-smoke-001");
        var result = service.submit(1001L, bo);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("GC", result.getFromCurrencyCode());
        assertEquals(new BigDecimal("10.000000"), result.getFromAmount());
        assertEquals("SC", result.getToCurrencyCode());
        assertEquals(new BigDecimal("99.000000"), result.getToAmount());
        assertEquals(new BigDecimal("1.000000"), result.getFeeAmount());

        ArgumentCaptor<WalletDebitBo> debitCaptor = ArgumentCaptor.forClass(WalletDebitBo.class);
        verify(walletCoreService).debit(debitCaptor.capture());
        WalletDebitBo debitBo = debitCaptor.getValue();
        assertEquals(1001L, debitBo.getMemberId());
        assertEquals("GC", debitBo.getCurrencyCode());
        assertEquals(new BigDecimal("11.000000"), debitBo.getAmount());
        assertEquals("EXCHANGE", debitBo.getSourceType());

        ArgumentCaptor<WalletCreditBo> creditCaptor = ArgumentCaptor.forClass(WalletCreditBo.class);
        verify(walletCoreService).credit(creditCaptor.capture());
        WalletCreditBo creditBo = creditCaptor.getValue();
        assertEquals(1001L, creditBo.getMemberId());
        assertEquals("SC", creditBo.getCurrencyCode());
        assertEquals(new BigDecimal("99.000000"), creditBo.getAmount());
        assertEquals("EXCHANGE", creditBo.getSourceType());
        assertEquals(new BigDecimal("198.000000"), creditBo.getTurnoverRequiredAmount());

        ArgumentCaptor<WalletExchangeOrder> orderCaptor = ArgumentCaptor.forClass(WalletExchangeOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        verify(orderMapper).updateById(orderCaptor.getValue());
        WalletExchangeOrder order = orderCaptor.getValue();
        assertEquals("WT-DEBIT", order.getDebitTransactionNo());
        assertEquals("WT-CREDIT", order.getCreditTransactionNo());
        assertEquals("SUCCESS", order.getStatus());
    }

    @Test
    @Tag("local")
    void submitRejectsWhenDailyFromLimitWouldBeExceeded() {
        WalletExchangeRuleMapper ruleMapper = mock(WalletExchangeRuleMapper.class);
        WalletExchangeOrderMapper orderMapper = mock(WalletExchangeOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        WalletExchangeRule rule = enabledRule();
        rule.setDailyFromLimit(new BigDecimal("15.000000"));
        when(ruleMapper.selectById(9001L)).thenReturn(rule);
        when(orderMapper.sumSuccessFromAmountToday("000000", 1001L, 9001L)).thenReturn(new BigDecimal("10.000000"));
        WalletExchangeOrderServiceImpl service = new WalletExchangeOrderServiceImpl(ruleMapper, orderMapper, walletCoreService);

        ClientExchangeOrderBo bo = new ClientExchangeOrderBo();
        bo.setExchangeRuleId(9001L);
        bo.setFromAmount(new BigDecimal("6.000000"));
        bo.setIdempotencyKey("exchange-limit-001");

        var exception = assertThrows(RuntimeException.class, () -> service.submit(1001L, bo));

        assertEquals("wallet.exchange.daily.limit.exceeded", exception.getMessage());
        verify(walletCoreService, never()).debit(any(WalletDebitBo.class));
        verify(walletCoreService, never()).credit(any(WalletCreditBo.class));
    }

    private WalletExchangeRule enabledRule() {
        WalletExchangeRule rule = new WalletExchangeRule();
        rule.setId(9001L);
        rule.setTenantId("000000");
        rule.setRuleName("GC to SC");
        rule.setFromCurrencyCode("GC");
        rule.setToCurrencyCode("SC");
        rule.setRateType("FIXED");
        rule.setRateValue(new BigDecimal("10.00000000"));
        rule.setMinFromAmount(new BigDecimal("1.000000"));
        rule.setMaxFromAmount(new BigDecimal("100.000000"));
        rule.setDailyFromLimit(BigDecimal.ZERO);
        rule.setFeeType("PERCENT");
        rule.setFeeValue(new BigDecimal("10.000000"));
        rule.setTurnoverRequired("0");
        rule.setTurnoverMultiplier(new BigDecimal("2.0000"));
        rule.setGameScopeType("ALL");
        rule.setStatus("0");
        return rule;
    }

    private WalletTransaction transaction(String transactionNo, String status) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo(transactionNo);
        transaction.setStatus(status);
        return transaction;
    }
}
