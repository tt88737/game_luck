package com.gameluck.wallet.client.domain.vo;

import lombok.Data;

@Data
public class ClientWalletLedgerVo {
    private Long ledgerId;
    private String currencyCode;
    private String direction;
    private String amount;
    private String afterAvailable;
    private String bizType;
    private String createdAt;
}
