package com.gameluck.member.client.domain.vo;

import lombok.Data;

@Data
public class ClientLoginVo {
    private String accessToken;
    private Long expiresIn;
    private ClientMemberVo member;
}
