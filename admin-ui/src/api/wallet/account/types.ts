export interface AccountVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  memberId: string | number;
  currencyCode: string;
  availableBalance: number;
  frozenBalance: number;
  status: string;
  version: number;
}

export interface AccountQuery extends PageQuery {
  memberId?: string | number;
  currencyCode?: string;
  status?: string;
}
