export interface AccountVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  memberId: string | number;
  memberNo: string;
  currencyCode: string;
  availableBalance: number;
  frozenBalance: number;
  status: string;
  version: number;
}

export interface AccountQuery extends PageQuery {
  memberId?: string | number;
  memberNo?: string;
  currencyCode?: string;
  status?: string;
}
