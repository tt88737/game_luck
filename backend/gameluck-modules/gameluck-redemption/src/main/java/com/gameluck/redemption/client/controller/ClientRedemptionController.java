package com.gameluck.redemption.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.redemption.client.domain.bo.ClientRedemptionRequestBo;
import com.gameluck.redemption.client.domain.vo.ClientRedemptionVo;
import com.gameluck.redemption.client.service.ClientRedemptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/api/client/redemptions")
public class ClientRedemptionController {

    private final ClientRedemptionService clientRedemptionService;

    @GetMapping
    public R<List<ClientRedemptionVo>> redemptions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientRedemptionService.redemptions(authorization));
    }

    @PostMapping("/request")
    public R<ClientRedemptionVo> request(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @Valid @RequestBody ClientRedemptionRequestBo bo) {
        return R.ok(clientRedemptionService.request(authorization, bo));
    }
}
