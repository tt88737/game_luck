package com.gameluck.promotion.client.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientPromotionClaimBo {

    @NotNull(message = "{client.promotion.id.required}")
    private Long promotionId;
}
