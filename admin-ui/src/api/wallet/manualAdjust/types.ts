export type ManualAdjustStrategy = 'IMMEDIATE' | 'AFTER_TURNOVER' | 'MANUAL_REVIEW';

export interface ManualAdjustForm {
  adjustmentNo: string;
  memberId: string | number;
  currencyCode: string;
  amount: number | undefined;
  strategy: ManualAdjustStrategy;
  requiredTurnover?: number;
  reason: string;
}

export interface ManualAdjustResultVO {
  id?: string | number;
  tenantId?: string | number;
  transactionNo?: string;
  idempotencyKey?: string;
  requestHash?: string;
  memberId?: string | number;
  currencyCode?: string;
  operation?: string;
  sourceType?: string;
  businessNo?: string;
  amount?: number;
  balanceBefore?: number;
  balanceAfter?: number;
  frozenBefore?: number;
  frozenAfter?: number;
  releaseMode?: string;
  requiredTurnover?: number;
  status?: string;
  failCode?: string;
  failReason?: string;
  operatorId?: string | number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ApiDataResponse<T> {
  code?: number;
  msg?: string;
  data: T;
}
