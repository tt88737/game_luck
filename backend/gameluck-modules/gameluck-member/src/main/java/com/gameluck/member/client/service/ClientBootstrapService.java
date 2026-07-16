package com.gameluck.member.client.service;

import cn.hutool.core.util.StrUtil;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.member.client.domain.vo.ClientBootstrapVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientBootstrapService {

    public ClientBootstrapVo getBootstrap(String brandCode, String channelCode) {
        ClientBootstrapVo vo = new ClientBootstrapVo();
        vo.setTenantId("000000");
        vo.setBrandCode(StrUtil.blankToDefault(brandCode, "demo"));
        vo.setChannelCode(StrUtil.blankToDefault(channelCode, "h5"));
        vo.setBrandName("GameLuck");
        vo.setTheme(theme());
        vo.setFeatures(features());
        vo.setCurrencies(List.of(
            currency("GC", MessageUtils.message("client.currency.gc")),
            currency("SC", MessageUtils.message("client.currency.sc"))));
        return vo;
    }

    private ClientBootstrapVo.Theme theme() {
        ClientBootstrapVo.Theme theme = new ClientBootstrapVo.Theme();
        theme.setLogoText("GameLuck");
        theme.setPrimaryColor("#1f7a4d");
        return theme;
    }

    private ClientBootstrapVo.Features features() {
        ClientBootstrapVo.Features features = new ClientBootstrapVo.Features();
        features.setWalletEnabled(true);
        features.setGameEnabled(true);
        features.setPromotionEnabled(true);
        features.setRedemptionEnabled(false);
        features.setPaymentEnabled(true);
        features.setKycEnabled(false);
        return features;
    }

    private ClientBootstrapVo.Currency currency(String code, String name) {
        ClientBootstrapVo.Currency currency = new ClientBootstrapVo.Currency();
        currency.setCurrencyCode(code);
        currency.setCurrencyName(name);
        currency.setDecimalScale(2);
        currency.setPlayable(true);
        currency.setRechargeable(false);
        currency.setWithdrawable(false);
        return currency;
    }
}
