export interface TransactionVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  transactionNo: string;
  idempotencyKey: string;
  memberId: string | number;
  currencyCode: string;
  operation: string;
  sourceType: string;
  businessNo: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  frozenBefore: number;
  frozenAfter: number;
  releaseMode: string;
  requiredTurnover: number;
  requestHash: string;
  status: string;
  failCode: string;
  failReason: string;
  operatorId: string | number;
  remark: string;
}

export interface TransactionQuery extends PageQuery {
  transactionNo?: string;
  memberId?: string | number;
  currencyCode?: string;
  operation?: string;
  sourceType?: string;
  businessNo?: string;
  status?: string;
}
