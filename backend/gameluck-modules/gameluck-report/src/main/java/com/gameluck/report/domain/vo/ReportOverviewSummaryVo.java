package com.gameluck.report.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Report overview summary metrics.
 */
@Data
public class ReportOverviewSummaryVo {

    private Long memberCount;
    private Long walletAccountCount;
    private BigDecimal walletAvailableAmount;
    private BigDecimal walletFrozenAmount;
    private Long depositOrderCount;
    private BigDecimal successfulDepositAmount;
    private Long gameOrderCount;
    private BigDecimal totalBetAmount;
    private BigDecimal totalPayoutAmount;
    private BigDecimal netGameAmount;
    private Long promotionClaimCount;
    private BigDecimal successfulRewardAmount;
    private Long redemptionOrderCount;
    private Long pendingRedemptionCount;
    private Long approvedRedemptionCount;
    private Long rejectedRedemptionCount;
    private BigDecimal approvedRedemptionAmount;
}

