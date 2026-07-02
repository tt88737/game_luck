export interface GameBetOrderVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  betOrderNo: string;
  memberId: string | number;
  currencyCode: string;
  gameCode: string;
  roundNo: string;
  betAmount: number;
  payoutAmount: number;
  netAmount: number;
  status: string;
  betWalletTransactionNo: string;
  settleWalletTransactionNo: string;
  refundWalletTransactionNo: string;
  refundIdempotencyKey: string;
  cancelTime: string;
  betIdempotencyKey: string;
  settleIdempotencyKey: string;
  betTime: string;
  settleTime: string;
  failReason: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface GameBetOrderForm {
  id?: string | number;
  memberId?: string | number;
  currencyCode?: string;
  gameCode?: string;
  roundNo?: string;
  betAmount?: number;
  payoutAmount?: number;
  remark?: string;
}

export interface GameBetOrderQuery extends PageQuery {
  betOrderNo?: string;
  memberId?: string | number;
  currencyCode?: string;
  gameCode?: string;
  roundNo?: string;
  status?: string;
  beginTime?: string;
  endTime?: string;
}
