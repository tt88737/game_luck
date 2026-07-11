package com.gameluck.member.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.member.client.domain.bo.ClientLoginBo;
import com.gameluck.member.client.domain.bo.ClientRegisterBo;
import com.gameluck.member.client.domain.vo.ClientLoginVo;
import com.gameluck.member.client.domain.vo.ClientMemberVo;
import com.gameluck.member.client.service.ClientAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/api/client")
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    @PostMapping("/auth/login")
    public R<ClientLoginVo> login(@Valid @RequestBody ClientLoginBo bo) {
        return R.ok(clientAuthService.login(bo));
    }

    @PostMapping("/auth/register")
    public R<ClientLoginVo> register(@Valid @RequestBody ClientRegisterBo bo) {
        return R.ok(clientAuthService.register(bo));
    }

    @GetMapping("/member/me")
    public R<ClientMemberVo> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientAuthService.currentMember(authorization));
    }
}
