package com.gameluck.report.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReportDailyTrendVo {
    private LocalDate reportDate;
    private Long memberCount;
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
    private BigDecimal approvedRedemptionAmount;
}
