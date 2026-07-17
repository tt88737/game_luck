export interface FundPropertyVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  propertyCode: string;
  propertyName: string;
  defaultSourceType: string;
  defaultTurnoverMode: string;
  defaultTurnoverRequiredAmount: number;
  defaultTurnoverMultiplier: number;
  defaultGameScopeType: string;
  defaultGameScopeValue?: string;
  status: string;
  sortOrder: number;
  remark?: string;
}

export interface FundPropertyForm {
  id?: string | number;
  propertyCode?: string;
  propertyName?: string;
  defaultSourceType?: string;
  defaultTurnoverMode?: string;
  defaultTurnoverRequiredAmount?: number;
  defaultTurnoverMultiplier?: number;
  defaultGameScopeType?: string;
  defaultGameScopeValue?: string;
  status?: string;
  sortOrder?: number;
  remark?: string;
}

export interface FundPropertyQuery extends PageQuery {
  propertyCode?: string;
  propertyName?: string;
  defaultSourceType?: string;
  defaultTurnoverMode?: string;
  status?: string;
}
