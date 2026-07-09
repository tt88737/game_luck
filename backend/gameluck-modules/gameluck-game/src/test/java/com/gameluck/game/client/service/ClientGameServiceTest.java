package com.gameluck.game.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.game.client.domain.bo.ClientGameLaunchBo;
import com.gameluck.game.client.domain.vo.ClientGameLaunchVo;
import com.gameluck.game.client.domain.vo.ClientGameVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientGameServiceTest {

    @Test
    @Tag("local")
    void gamesReturnsMockGameForPlayableCurrency() {
        ClientGameService service = new ClientGameService(new ClientTokenService());

        List<ClientGameVo> result = service.games("GC");

        assertEquals(1, result.size());
        assertEquals("mock", result.get(0).getProviderCode());
        assertTrue(result.get(0).getSupportedCurrencies().contains("GC"));
    }

    @Test
    @Tag("local")
    void launchReturnsStubWithoutWalletDebit() {
        ClientTokenService tokenService = new ClientTokenService();
        ClientGameService service = new ClientGameService(tokenService);
        ClientGameLaunchBo bo = new ClientGameLaunchBo();
        bo.setProviderCode("mock");
        bo.setGameCode("mock-slot-001");
        bo.setCurrencyCode("GC");

        ClientGameLaunchVo result = service.launch("Bearer " + tokenService.issue(1001L), bo);

        assertEquals("stub", result.getLaunchMode());
        assertEquals("", result.getLaunchUrl());
        assertTrue(result.getSessionNo().startsWith("GS"));
    }
}
