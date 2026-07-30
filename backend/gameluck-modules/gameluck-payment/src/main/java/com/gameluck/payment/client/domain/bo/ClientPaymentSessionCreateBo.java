package com.gameluck.payment.client.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ClientPaymentSessionCreateBo {

    @NotBlank(message = "{payment.session.request.key.required}")
    @Size(max = 128, message = "{payment.session.request.key.too.long}")
    private String requestKey;

    @Pattern(regexp = "(?i)^\\s*SIMULATED\\s*$", message = "{payment.session.provider.unsupported}")
    private String providerCode = "SIMULATED";
}
