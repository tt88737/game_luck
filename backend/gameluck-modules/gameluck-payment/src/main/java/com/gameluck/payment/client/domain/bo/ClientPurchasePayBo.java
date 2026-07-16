package com.gameluck.payment.client.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * C-side simulated purchase request.
 */
@Data
public class ClientPurchasePayBo {

    @NotNull(message = "{client.purchase.offer.id.required}")
    private Long offerId;

    @NotBlank(message = "{client.purchase.idempotency.key.required}")
    private String idempotencyKey;
}
