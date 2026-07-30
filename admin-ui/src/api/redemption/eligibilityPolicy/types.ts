export interface RedemptionEligibilityPolicyVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  policyName: string;
  currencyCode: string;
  countryCode?: string;
  stateCode?: string;
  channel?: string;
  effect: string;
  priority: number;
  status: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface RedemptionEligibilityPolicyForm {
  id?: string | number;
  policyName?: string;
  currencyCode?: string;
  countryCode?: string;
  stateCode?: string;
  channel?: string;
  effect?: string;
  priority?: number;
  status?: string;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface RedemptionEligibilityPolicyQuery extends PageQuery {
  policyName?: string;
  currencyCode?: string;
  countryCode?: string;
  stateCode?: string;
  channel?: string;
  effect?: string;
  status?: string;
}
