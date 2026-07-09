package com.gameluck.game.client.controller;

import com.gameluck.common.core.domain.R;
import com.gameluck.game.client.domain.bo.ClientGameLaunchBo;
import com.gameluck.game.client.domain.vo.ClientGameLaunchVo;
import com.gameluck.game.client.domain.vo.ClientGameVo;
import com.gameluck.game.client.service.ClientGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/client/games")
public class ClientGameController {

    private final ClientGameService clientGameService;

    @GetMapping
    public R<List<ClientGameVo>> games(@RequestParam(required = false) String currencyCode) {
        return R.ok(clientGameService.games(currencyCode));
    }

    @PostMapping("/launch")
    public R<ClientGameLaunchVo> launch(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @Valid @RequestBody ClientGameLaunchBo bo) {
        return R.ok(clientGameService.launch(authorization, bo));
    }
}
