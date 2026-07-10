export interface ReportDailyTrendVO {
  reportDate: string;
  memberCount: number;
  depositOrderCount: number;
  successfulDepositAmount: string | number;
  gameOrderCount: number;
  totalBetAmount: string | number;
  totalPayoutAmount: string | number;
  netGameAmount: string | number;
  promotionClaimCount: number;
  successfulRewardAmount: string | number;
  redemptionOrderCount: number;
  pendingRedemptionCount: number;
  approvedRedemptionAmount: string | number;
}
