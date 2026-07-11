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

export interface ManualAdjustResult {
  transactionNo?: string;
  status?: string;
  id?: string | number;
}

export interface ApiDataResponse<T> {
  code?: number;
  msg?: string;
  data: T;
}
