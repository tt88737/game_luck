package com.gameluck.game.client.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ClientGameVo {
    private String providerCode;
    private String gameCode;
    private String gameName;
    private String status;
    private List<String> supportedCurrencies;
    private String thumbnailUrl;
    private Boolean maintenance;
}
