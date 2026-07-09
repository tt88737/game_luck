package com.gameluck.game.client.domain.vo;

import lombok.Data;

@Data
public class ClientGameLaunchVo {
    private String sessionNo;
    private String launchMode;
    private String launchUrl;
    private String message;
}
