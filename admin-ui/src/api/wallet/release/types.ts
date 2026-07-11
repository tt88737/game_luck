export interface ReleaseVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  releaseNo: string;
  memberId: string | number;
  memberNo: string;
  currencyCode: string;
  sourceType: string;
  businessNo: string;
  amount: number;
  releasedAmount: number;
  consumedAmount: number;
  requiredTurnover: number;
  completedTurnover: number;
  releaseMode: string;
  releaseStatus: string;
  metadata: string;
  operatorId: string | number;
  remark: string;
  version: number;
}

export interface ReleaseQuery extends PageQuery {
  releaseNo?: string;
  memberId?: string | number;
  memberNo?: string;
  currencyCode?: string;
  sourceType?: string;
  businessNo?: string;
  releaseMode?: string;
  releaseStatus?: string;
}
