package com.gameluck.game.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.game.client.domain.bo.ClientGameLaunchBo;
import com.gameluck.game.client.domain.vo.ClientGameLaunchVo;
import com.gameluck.game.client.domain.vo.ClientGameVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientGameService {

    private final ClientTokenService clientTokenService;

    public List<ClientGameVo> games(String currencyCode) {
        if (currencyCode != null && !currencyCode.isBlank() && !"GC".equals(currencyCode) && !"SC".equals(currencyCode)) {
            return List.of();
        }
        ClientGameVo game = new ClientGameVo();
        game.setProviderCode("mock");
        game.setGameCode("mock-slot-001");
        game.setGameName(MessageUtils.message("client.game.mock.slot"));
        game.setStatus("enabled");
        game.setSupportedCurrencies(List.of("GC", "SC"));
        game.setThumbnailUrl("");
        game.setMaintenance(false);
        return List.of(game);
    }

    public ClientGameLaunchVo launch(String authorization, ClientGameLaunchBo bo) {
        clientTokenService.requireMemberId(authorization);
        ClientGameLaunchVo vo = new ClientGameLaunchVo();
        vo.setSessionNo("GS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        vo.setLaunchMode("stub");
        vo.setLaunchUrl("");
        vo.setMessage(MessageUtils.message("client.game.launch.not.live"));
        return vo;
    }
}
