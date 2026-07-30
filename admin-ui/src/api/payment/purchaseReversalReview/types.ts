import type { MemberProfileVO } from '@/api/member/profile/types';
import type {
  PurchaseOrderGrantSnapshotVO,
  PurchaseOrderVO,
  PurchasePaymentEventVO,
  PurchaseReversalItemVO
} from '@/api/payment/purchaseOrder/types';

export interface PurchaseReversalReviewQuery extends PageQuery {
  reversalNo?: string;
  purchaseOrderNo?: string;
  memberId?: string | number;
  memberNo?: string;
  reversalType?: string;
  dispositionStatus?: string;
  beginTime?: string;
  endTime?: string;
}

export interface PurchaseReversalReviewVO {
  id: string | number;
  reversalNo: string;
  purchaseOrderNo: string;
  memberId: string | number;
  memberNo: string;
  reversalType: string;
  status: string;
  dispositionStatus: string;
  reason: string;
  reviewReason: string;
  riskLevel: string;
  retryCount: number;
  lastRetryTime: string;
  resolvedTime: string;
  createTime: string;
  items: PurchaseReversalItemVO[];
}

export interface PurchaseReversalReviewLogVO {
  operationNo: string;
  requestKey: string;
  operationType: string;
  beforeStatus: string;
  afterStatus: string;
  operatorId: string | number;
  operatorName: string;
  reviewNote: string;
  snapshotJson: string;
  createTime: string;
}

export interface PurchaseReversalReviewDetailVO extends PurchaseReversalReviewVO {
  purchaseOrder: PurchaseOrderVO;
  member: MemberProfileVO;
  grantSnapshots: PurchaseOrderGrantSnapshotVO[];
  paymentEvents: PurchasePaymentEventVO[];
  reviewLogs: PurchaseReversalReviewLogVO[];
}

export interface PurchaseReversalReviewActionForm {
  requestKey: string;
  reviewNote?: string;
}

export interface PurchaseReversalReviewActionResultVO {
  operationType: string;
  dispositionStatus: string;
  completed: boolean;
  detail: PurchaseReversalReviewDetailVO;
}
