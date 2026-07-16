export interface PurchaseOfferGrantItem {
  id?: string | number;
  grantType: 'PURCHASE_GRANT' | 'PURCHASE_BONUS' | 'DEPOSIT_PRINCIPAL' | 'DEPOSIT_BONUS';
  currencyCode: string;
  grantAmount: number;
  wageringMode: 'NONE' | 'FIXED' | 'MULTIPLIER' | 'COMBINED_MULTIPLIER';
  wageringRequiredAmount?: number;
  wageringMultiplier?: number;
  gameScopeType?: 'ALL' | 'CATEGORY' | 'PROVIDER' | 'GAME';
  gameScopeValue?: string;
  wageringExpireDays?: number;
  sortOrder?: number;
  remark?: string;
}

export interface PurchaseOfferVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  offerNo: string;
  offerName: string;
  offerType: string;
  payCurrencyCode: string;
  payAmount: number;
  userScopeType: string;
  userScopeValue?: string;
  regionScopeType: string;
  regionScopeValue?: string;
  purchaseLimitType: string;
  stackable: string;
  status: string;
  sortOrder: number;
  startTime?: string;
  endTime?: string;
  remark?: string;
  grantItems?: PurchaseOfferGrantItem[];
}

export interface PurchaseOfferForm {
  id?: string | number;
  offerNo?: string;
  offerName?: string;
  offerType?: string;
  payCurrencyCode?: string;
  payAmount?: number;
  userScopeType?: string;
  userScopeValue?: string;
  regionScopeType?: string;
  regionScopeValue?: string;
  purchaseLimitType?: string;
  stackable?: string;
  status?: string;
  sortOrder?: number;
  startTime?: string;
  endTime?: string;
  remark?: string;
  grantItems?: PurchaseOfferGrantItem[];
}

export interface PurchaseOfferQuery extends PageQuery {
  offerNo?: string;
  offerName?: string;
  offerType?: string;
  payCurrencyCode?: string;
  status?: string;
  beginTime?: string;
  endQueryTime?: string;
}
