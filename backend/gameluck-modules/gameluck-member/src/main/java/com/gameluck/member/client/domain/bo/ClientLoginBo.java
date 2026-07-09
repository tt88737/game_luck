package com.gameluck.member.client.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientLoginBo {

    @NotBlank(message = "{client.auth.username.required}")
    private String username;

    @NotBlank(message = "{client.auth.password.required}")
    private String password;
}
