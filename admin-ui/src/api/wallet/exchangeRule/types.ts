export interface ExchangeRuleVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  ruleName: string;
  fromCurrencyCode: string;
  toCurrencyCode: string;
  rateType: string;
  rateValue: number;
  minFromAmount: number;
  maxFromAmount: number;
  dailyFromLimit: number;
  feeType: string;
  feeValue: number;
  turnoverRequired: string;
  turnoverMultiplier: number;
  gameScopeType: string;
  gameScopeValue?: string;
  countryCode?: string;
  stateCode?: string;
  memberTag?: string;
  channel?: string;
  status: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface ExchangeRuleForm {
  id?: string | number;
  ruleName?: string;
  fromCurrencyCode?: string;
  toCurrencyCode?: string;
  rateType?: string;
  rateValue?: number;
  minFromAmount?: number;
  maxFromAmount?: number;
  dailyFromLimit?: number;
  feeType?: string;
  feeValue?: number;
  turnoverRequired?: string;
  turnoverMultiplier?: number;
  gameScopeType?: string;
  gameScopeValue?: string;
  countryCode?: string;
  stateCode?: string;
  memberTag?: string;
  channel?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface ExchangeRuleQuery extends PageQuery {
  ruleName?: string;
  fromCurrencyCode?: string;
  toCurrencyCode?: string;
  rateType?: string;
  feeType?: string;
  channel?: string;
  status?: string;
}
