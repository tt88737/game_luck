export interface CurrencyVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  currencyCode: string;
  currencyName: string;
  scaleNum: number;
  enabled: string;
  creditEnabled: string;
  debitEnabled: string;
  freezeEnabled: string;
  withdrawEnabled: string;
  exchangeEnabled: string;
  negativeAllowed: string;
  sortOrder: number;
  remark: string;
  version: number;
}

export interface CurrencyForm {
  id?: string | number;
  currencyCode?: string;
  currencyName?: string;
  scaleNum?: number;
  enabled?: string;
  creditEnabled?: string;
  debitEnabled?: string;
  freezeEnabled?: string;
  withdrawEnabled?: string;
  exchangeEnabled?: string;
  negativeAllowed?: string;
  sortOrder?: number;
  remark?: string;
}

export interface CurrencyQuery extends PageQuery {
  currencyCode?: string;
  currencyName?: string;
  enabled?: string;
}
