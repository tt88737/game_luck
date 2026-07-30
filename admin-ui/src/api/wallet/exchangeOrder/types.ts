export interface ExchangeOrderVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  exchangeOrderNo: string;
  memberId: string | number;
  memberNo: string;
  exchangeRuleId: string | number;
  fromCurrencyCode: string;
  fromAmount: number;
  toCurrencyCode: string;
  toAmount: number;
  feeAmount: number;
  debitTransactionNo: string;
  creditTransactionNo: string;
  turnoverTaskNo: string;
  ruleSnapshot: string;
  status: string;
  failReason: string;
  createTime: string;
  updateTime: string;
}

export interface ExchangeOrderQuery extends PageQuery {
  exchangeOrderNo?: string;
  memberId?: string | number;
  memberNo?: string;
  exchangeRuleId?: string | number;
  fromCurrencyCode?: string;
  toCurrencyCode?: string;
  debitTransactionNo?: string;
  creditTransactionNo?: string;
  turnoverTaskNo?: string;
  status?: string;
  beginTime?: string;
  endTime?: string;
}
