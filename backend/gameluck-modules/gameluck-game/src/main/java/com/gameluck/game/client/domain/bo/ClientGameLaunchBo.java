package com.gameluck.game.client.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientGameLaunchBo {
    @NotBlank(message = "{game.provider.required}")
    private String providerCode;

    @NotBlank(message = "{game.code.required}")
    private String gameCode;

    @NotBlank(message = "{wallet.currency.required}")
    private String currencyCode;
}
