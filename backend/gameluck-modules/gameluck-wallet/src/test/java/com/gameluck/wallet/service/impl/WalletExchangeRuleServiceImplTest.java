package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.wallet.domain.WalletExchangeRule;
import com.gameluck.wallet.domain.bo.WalletExchangeRuleBo;
import com.gameluck.wallet.mapper.WalletExchangeRuleMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletExchangeRuleServiceImplTest {

    @Test
    @Tag("local")
    void insertRejectsSameSourceAndTargetCurrency() {
        WalletExchangeRuleServiceImpl service = new WalletExchangeRuleServiceImpl(mock(WalletExchangeRuleMapper.class));
        WalletExchangeRuleBo bo = validBo();
        bo.setToCurrencyCode("GC");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("wallet.exchange.rule.currency.same", exception.getMessage());
    }

    @Test
    @Tag("local")
    void insertRejectsInvalidPercentFee() {
        WalletExchangeRuleServiceImpl service = new WalletExchangeRuleServiceImpl(mock(WalletExchangeRuleMapper.class));
        WalletExchangeRuleBo bo = validBo();
        bo.setFeeType("PERCENT");
        bo.setFeeValue(new BigDecimal("101"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("wallet.exchange.rule.fee.percent.invalid", exception.getMessage());
    }

    @Test
    @Tag("local")
    void insertStoresDisabledRuleWithValidatedCoreFields() {
        WalletExchangeRuleMapper mapper = mock(WalletExchangeRuleMapper.class);
        when(mapper.insert(any(WalletExchangeRule.class))).thenReturn(1);
        WalletExchangeRuleServiceImpl service = new WalletExchangeRuleServiceImpl(mapper);
        WalletExchangeRuleBo bo = validBo();

        int rows = service.insertByBo(bo);

        assertEquals(1, rows);
        ArgumentCaptor<WalletExchangeRule> captor = ArgumentCaptor.forClass(WalletExchangeRule.class);
        verify(mapper).insert(captor.capture());
        WalletExchangeRule rule = captor.getValue();
        assertEquals("GC", rule.getFromCurrencyCode());
        assertEquals("SC", rule.getToCurrencyCode());
        assertEquals(new BigDecimal("100.00000000"), rule.getRateValue());
        assertEquals("1", rule.getStatus());
        assertEquals("ALL", rule.getGameScopeType());
    }

    private WalletExchangeRuleBo validBo() {
        WalletExchangeRuleBo bo = new WalletExchangeRuleBo();
        bo.setRuleName("GC to SC disabled draft");
        bo.setFromCurrencyCode("GC");
        bo.setToCurrencyCode("SC");
        bo.setRateType("FIXED");
        bo.setRateValue(new BigDecimal("100"));
        bo.setFeeType("NONE");
        bo.setFeeValue(BigDecimal.ZERO);
        bo.setTurnoverRequired("1");
        bo.setTurnoverMultiplier(BigDecimal.ZERO);
        return bo;
    }
}
