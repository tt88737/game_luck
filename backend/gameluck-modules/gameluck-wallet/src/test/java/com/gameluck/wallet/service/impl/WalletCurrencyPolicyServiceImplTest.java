package com.gameluck.wallet.service.impl;

import com.gameluck.wallet.domain.WalletCurrency;
import com.gameluck.wallet.domain.WalletCurrencyPolicy;
import com.gameluck.wallet.client.domain.vo.ClientWalletCurrencyVo;
import com.gameluck.wallet.mapper.WalletCurrencyMapper;
import com.gameluck.wallet.mapper.WalletCurrencyPolicyMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WalletCurrencyPolicyServiceImplTest {

    @Test
    @Tag("local")
    void listClientCurrenciesHidesCurrencyDeniedByCountryPolicy() {
        WalletCurrencyMapper currencyMapper = mock(WalletCurrencyMapper.class);
        WalletCurrencyPolicyMapper policyMapper = mock(WalletCurrencyPolicyMapper.class);
        WalletCurrencyPolicyServiceImpl service = service(currencyMapper, policyMapper, "US", "WA");
        when(currencyMapper.selectList(any())).thenReturn(List.of(currency("GC"), currency("SC")));
        when(policyMapper.selectList(any())).thenReturn(List.of(policy("SC", "US", null, "H5", "1", "1", "1", "1", "1")));

        List<ClientWalletCurrencyVo> result = service.listClientCurrencies("000000", 1001L, "H5");

        assertEquals(1, result.size());
        assertEquals("GC", result.get(0).getCurrencyCode());
    }

    @Test
    @Tag("local")
    void listClientCurrenciesCombinesCurrencyAndPolicyActionFlags() {
        WalletCurrencyMapper currencyMapper = mock(WalletCurrencyMapper.class);
        WalletCurrencyPolicyMapper policyMapper = mock(WalletCurrencyPolicyMapper.class);
        WalletCurrencyPolicyServiceImpl service = service(currencyMapper, policyMapper, "US", "WA");
        WalletCurrency sc = currency("SC");
        sc.setDepositEnabled("1");
        sc.setWithdrawEnabled("0");
        sc.setExchangeEnabled("0");
        sc.setPlayEnabled("0");
        when(currencyMapper.selectList(any())).thenReturn(List.of(sc));
        when(policyMapper.selectList(any())).thenReturn(List.of(policy("SC", "US", "WA", "H5", "0", "1", "0", "1", "0")));

        List<ClientWalletCurrencyVo> result = service.listClientCurrencies("000000", 1001L, "H5");

        assertEquals(1, result.size());
        ClientWalletCurrencyVo currency = result.get(0);
        assertFalse(currency.getDepositEnabled());
        assertTrue(currency.getWithdrawEnabled());
        assertFalse(currency.getExchangeEnabled());
        assertTrue(currency.getPlayEnabled());
    }

    private static WalletCurrencyPolicyServiceImpl service(WalletCurrencyMapper currencyMapper,
                                                           WalletCurrencyPolicyMapper policyMapper,
                                                           String countryCode,
                                                           String stateCode) {
        return new WalletCurrencyPolicyServiceImpl(currencyMapper, policyMapper, mock(JdbcTemplate.class)) {
            @Override
            protected MemberCurrencyContext loadMemberContext(String tenantId, Long memberId) {
                return new MemberCurrencyContext(countryCode, stateCode);
            }
        };
    }

    private static WalletCurrency currency(String currencyCode) {
        WalletCurrency currency = new WalletCurrency();
        currency.setCurrencyCode(currencyCode);
        currency.setCurrencyName(currencyCode + " Name");
        currency.setScaleNum(6);
        currency.setEnabled("0");
        currency.setDepositEnabled("0");
        currency.setWithdrawEnabled("0");
        currency.setExchangeEnabled("0");
        currency.setPlayEnabled("0");
        currency.setSortOrder(1);
        return currency;
    }

    private static WalletCurrencyPolicy policy(String currencyCode, String countryCode, String stateCode, String channel,
                                               String visibleEnabled, String depositEnabled, String withdrawEnabled,
                                               String exchangeEnabled, String playEnabled) {
        WalletCurrencyPolicy policy = new WalletCurrencyPolicy();
        policy.setCurrencyCode(currencyCode);
        policy.setCountryCode(countryCode);
        policy.setStateCode(stateCode);
        policy.setChannel(channel);
        policy.setVisibleEnabled(visibleEnabled);
        policy.setDepositEnabled(depositEnabled);
        policy.setWithdrawEnabled(withdrawEnabled);
        policy.setExchangeEnabled(exchangeEnabled);
        policy.setPlayEnabled(playEnabled);
        policy.setStatus("0");
        policy.setPriority(100);
        return policy;
    }
}
