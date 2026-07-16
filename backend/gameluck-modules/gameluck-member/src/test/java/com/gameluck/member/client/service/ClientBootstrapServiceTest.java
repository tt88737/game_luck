package com.gameluck.member.client.service;

import com.gameluck.member.client.domain.vo.ClientBootstrapVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientBootstrapServiceTest {

    @Test
    @Tag("local")
    void defaultBootstrapReturnsDemoBrandCurrenciesAndFeatureFlags() {
        ClientBootstrapService service = new ClientBootstrapService();

        ClientBootstrapVo result = service.getBootstrap(null, null);

        assertEquals("000000", result.getTenantId());
        assertEquals("demo", result.getBrandCode());
        assertEquals("h5", result.getChannelCode());
        assertEquals("GameLuck", result.getBrandName());
        assertTrue(result.getFeatures().getWalletEnabled());
        assertTrue(result.getFeatures().getGameEnabled());
        assertTrue(result.getFeatures().getPaymentEnabled());
        assertEquals(2, result.getCurrencies().size());
        assertEquals("GC", result.getCurrencies().get(0).getCurrencyCode());
        assertEquals("SC", result.getCurrencies().get(1).getCurrencyCode());
    }
}
