package com.gameluck.promotion.client.domain.vo;

import com.gameluck.promotion.domain.bo.PromotionRewardItemBo;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ClientDailyLoginRewardVo {

    private Long promotionId;

    private String promotionNo;

    private String promotionName;

    private String promotionType;

    private LocalDate claimDate;

    private List<PromotionRewardItemBo> rewardItems;

    private Boolean canClaim;

    private String claimStatus;

    private String claimNo;

    private String walletTransactionNo;
}
