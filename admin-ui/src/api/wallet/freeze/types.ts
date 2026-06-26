export interface FreezeVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  freezeNo: string;
  memberId: string | number;
  currencyCode: string;
  amount: number;
  sourceType: string;
  businessNo: string;
  status: string;
  operatorId: string | number;
  remark: string;
}

export interface FreezeQuery extends PageQuery {
  freezeNo?: string;
  memberId?: string | number;
  currencyCode?: string;
  sourceType?: string;
  businessNo?: string;
  status?: string;
}
