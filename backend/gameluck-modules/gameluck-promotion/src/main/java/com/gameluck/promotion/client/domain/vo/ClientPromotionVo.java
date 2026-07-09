package com.gameluck.promotion.client.domain.vo;

import lombok.Data;

@Data
public class ClientPromotionVo {

    private Long promotionId;

    private String promotionNo;

    private String promotionName;

    private String currencyCode;

    private String rewardAmount;

    private String status;

    private String claimStatus;

    private String claimNo;

    private String walletTransactionNo;

    private Boolean canClaim;
}
