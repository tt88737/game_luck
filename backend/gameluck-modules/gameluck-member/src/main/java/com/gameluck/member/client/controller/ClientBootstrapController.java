package com.gameluck.member.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.member.client.domain.vo.ClientBootstrapVo;
import com.gameluck.member.client.service.ClientBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/api/client")
public class ClientBootstrapController {

    private final ClientBootstrapService clientBootstrapService;

    @GetMapping("/bootstrap")
    public R<ClientBootstrapVo> bootstrap(@RequestHeader(value = "X-Brand-Code", required = false) String brandCode,
                                          @RequestHeader(value = "X-Channel-Code", required = false) String channelCode) {
        return R.ok(clientBootstrapService.getBootstrap(brandCode, channelCode));
    }
}
