package com.gameluck.promotion.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.promotion.client.domain.bo.ClientPromotionClaimBo;
import com.gameluck.promotion.client.domain.vo.ClientPromotionVo;
import com.gameluck.promotion.client.service.ClientPromotionService;
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
@RequestMapping("/api/client/promotions")
public class ClientPromotionController {

    private final ClientPromotionService clientPromotionService;

    @GetMapping
    public R<List<ClientPromotionVo>> promotions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientPromotionService.promotions(authorization));
    }

    @PostMapping("/claim")
    public R<ClientPromotionVo> claim(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @Valid @RequestBody ClientPromotionClaimBo bo) {
        return R.ok(clientPromotionService.claim(authorization, bo));
    }
}
