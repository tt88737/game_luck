package com.gameluck.wallet.client.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class ClientWalletCurrencyVo {
    private String currencyCode;
    private String currencyName;
    private Integer decimalScale;
    private Boolean depositEnabled;
    private Boolean withdrawEnabled;
    private Boolean exchangeEnabled;
    private Boolean playEnabled;

    @JsonIgnore
    private Boolean visibleForPolicy;

    public void clearVisibleForPolicy() {
        this.visibleForPolicy = null;
    }
}
