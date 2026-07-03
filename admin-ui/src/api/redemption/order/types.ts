export interface RedemptionOrderVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  redemptionOrderNo: string;
  memberId: string | number;
  currencyCode: string;
  amount: number;
  redemptionMethod: string;
  accountRef: string;
  status: string;
  freezeNo: string;
  freezeWalletTransactionNo: string;
  settleWalletTransactionNo: string;
  releaseWalletTransactionNo: string;
  freezeIdempotencyKey: string;
  settleIdempotencyKey: string;
  releaseIdempotencyKey: string;
  auditBy: string | number;
  auditTime: string;
  auditReason: string;
  failReason: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface RedemptionOrderForm {
  id?: string | number;
  memberId?: string | number;
  currencyCode?: string;
  amount?: number;
  redemptionMethod?: string;
  accountRef?: string;
  auditReason?: string;
  remark?: string;
}

export interface RedemptionOrderQuery extends PageQuery {
  redemptionOrderNo?: string;
  memberId?: string | number;
  currencyCode?: string;
  status?: string;
  beginTime?: string;
  endTime?: string;
}
