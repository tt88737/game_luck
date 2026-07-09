package com.gameluck.member.client.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ClientBootstrapVo {

    private String tenantId;
    private String brandCode;
    private String channelCode;
    private String brandName;
    private Theme theme;
    private Features features;
    private List<Currency> currencies;

    @Data
    public static class Theme {
        private String logoText;
        private String primaryColor;
    }

    @Data
    public static class Features {
        private Boolean walletEnabled;
        private Boolean gameEnabled;
        private Boolean promotionEnabled;
        private Boolean redemptionEnabled;
        private Boolean paymentEnabled;
        private Boolean kycEnabled;
    }

    @Data
    public static class Currency {
        private String currencyCode;
        private String currencyName;
        private Integer decimalScale;
        private Boolean playable;
        private Boolean rechargeable;
        private Boolean withdrawable;
    }
}
