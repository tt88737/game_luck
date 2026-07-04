export interface ReportOverviewSummaryVO {
  memberCount: number;
  walletAccountCount: number;
  walletAvailableAmount: string | number;
  walletFrozenAmount: string | number;
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
  approvedRedemptionCount: number;
  rejectedRedemptionCount: number;
  approvedRedemptionAmount: string | number;
}

