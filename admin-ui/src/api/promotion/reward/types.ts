export interface PromotionRewardVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  promotionNo: string;
  promotionName: string;
  currencyCode: string;
  rewardAmount: number;
  status: string;
  startTime: string;
  endTime: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface PromotionClaimVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  claimNo: string;
  promotionId: string | number;
  promotionNo: string;
  promotionName: string;
  memberId: string | number;
  currencyCode: string;
  rewardAmount: number;
  status: string;
  walletTransactionNo: string;
  idempotencyKey: string;
  failReason: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface PromotionRewardForm {
  id?: string | number;
  promotionName?: string;
  currencyCode?: string;
  rewardAmount?: number;
  status?: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface PromotionClaimForm {
  promotionId?: string | number;
  memberId?: string | number;
  remark?: string;
}

export interface PromotionRewardQuery extends PageQuery {
  promotionNo?: string;
  promotionName?: string;
  currencyCode?: string;
  status?: string;
  beginTime?: string;
  endQueryTime?: string;
}

export interface PromotionClaimQuery extends PageQuery {
  claimNo?: string;
  promotionId?: string | number;
  promotionNo?: string;
  memberId?: string | number;
  currencyCode?: string;
  status?: string;
  beginTime?: string;
  endTime?: string;
}
