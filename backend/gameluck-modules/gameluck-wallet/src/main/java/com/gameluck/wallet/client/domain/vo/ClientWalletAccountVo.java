package com.gameluck.wallet.client.domain.vo;

import lombok.Data;

@Data
public class ClientWalletAccountVo {
    private String currencyCode;
    private String currencyName;
    private String availableBalance;
    private String frozenBalance;
    private Integer decimalScale;
    private Boolean playable;
    private Boolean withdrawable;
}
