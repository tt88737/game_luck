package com.gameluck.member.client.domain.vo;

import lombok.Data;

@Data
public class ClientMemberVo {
    private Long memberId;
    private String memberNo;
    private String username;
    private String nickname;
    private String status;
    private String kycStatus;
}
