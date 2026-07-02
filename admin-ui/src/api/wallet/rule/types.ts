export interface RuleVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  currencyCode: string;
  sourceType: string;
  ruleName: string;
  creditEnabled: string;
  debitEnabled: string;
  withdrawEnabled: string;
  exchangeEnabled: string;
  releaseMode: string;
  turnoverRequired: string;
  defaultRequiredTurnover: number;
  status: string;
  sortOrder: number;
  remark: string;
  version: number;
}

export interface RuleForm {
  id?: string | number;
  currencyCode?: string;
  sourceType?: string;
  ruleName?: string;
  creditEnabled?: string;
  debitEnabled?: string;
  withdrawEnabled?: string;
  exchangeEnabled?: string;
  releaseMode?: string;
  turnoverRequired?: string;
  defaultRequiredTurnover?: number;
  status?: string;
  sortOrder?: number;
  remark?: string;
}

export interface RuleQuery extends PageQuery {
  currencyCode?: string;
  sourceType?: string;
  status?: string;
}
