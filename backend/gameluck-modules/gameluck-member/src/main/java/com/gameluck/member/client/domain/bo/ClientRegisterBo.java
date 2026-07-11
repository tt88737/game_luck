package com.gameluck.member.client.domain.bo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientRegisterBo {

    @NotBlank(message = "{client.auth.username.required}")
    @Size(max = 64, message = "{client.auth.username.max}")
    private String username;

    @NotBlank(message = "{client.auth.password.required}")
    @Size(min = 8, max = 64, message = "{client.auth.password.length}")
    private String password;

    @Size(max = 128, message = "{client.auth.nickname.max}")
    private String nickname;

    @NotBlank(message = "{client.register.country.required}")
    @Size(max = 16, message = "{client.register.country.max}")
    private String countryCode;

    @Size(max = 32, message = "{client.register.state.max}")
    private String stateCode;

    @AssertTrue(message = "{client.register.age.required}")
    @NotNull(message = "{client.register.age.required}")
    private Boolean ageConfirmed;

    @AssertTrue(message = "{client.register.terms.required}")
    @NotNull(message = "{client.register.terms.required}")
    private Boolean termsAccepted;

    @AssertTrue(message = "{client.register.privacy.required}")
    @NotNull(message = "{client.register.privacy.required}")
    private Boolean privacyAccepted;

    @AssertTrue(message = "{client.register.rules.required}")
    @NotNull(message = "{client.register.rules.required}")
    private Boolean sweepstakesRulesAccepted;
}
