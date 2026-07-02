export interface DepositOrderVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  depositOrderNo: string;
  memberId: string | number;
  currencyCode: string;
  amount: number;
  payMethod: string;
  payChannel: string;
  status: string;
  walletTransactionNo: string;
  walletIdempotencyKey: string;
  payTime: string;
  failReason: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface DepositOrderForm {
  id?: string | number;
  memberId?: string | number;
  currencyCode?: string;
  amount?: number;
  payMethod?: string;
  payChannel?: string;
  remark?: string;
}

export interface DepositOrderQuery extends PageQuery {
  depositOrderNo?: string;
  memberId?: string | number;
  currencyCode?: string;
  status?: string;
  beginTime?: string;
  endTime?: string;
}
