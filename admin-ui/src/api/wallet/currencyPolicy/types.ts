export interface CurrencyPolicyVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  policyName: string;
  currencyCode: string;
  memberTag?: string;
  vipLevel?: string;
  countryCode?: string;
  stateCode?: string;
  channel?: string;
  visibleEnabled: string;
  depositEnabled: string;
  withdrawEnabled: string;
  exchangeEnabled: string;
  playEnabled: string;
  priority: number;
  status: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface CurrencyPolicyForm {
  id?: string | number;
  policyName?: string;
  currencyCode?: string;
  memberTag?: string;
  vipLevel?: string;
  countryCode?: string;
  stateCode?: string;
  channel?: string;
  visibleEnabled?: string;
  depositEnabled?: string;
  withdrawEnabled?: string;
  exchangeEnabled?: string;
  playEnabled?: string;
  priority?: number;
  status?: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface CurrencyPolicyQuery extends PageQuery {
  policyName?: string;
  currencyCode?: string;
  countryCode?: string;
  stateCode?: string;
  channel?: string;
  status?: string;
}
