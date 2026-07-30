export interface PurchaseOrderGrantSnapshotVO {
  id: string | number;
  tenantId: string;
  purchaseOrderId: string | number;
  purchaseOrderNo: string;
  memberId: string | number;
  grantType: string;
  currencyCode: string;
  grantAmount: number;
  fundPropertyCode: string;
  walletTransactionNo: string;
  turnoverTaskNo: string;
  wageringMode: string;
  requiredTurnover: number;
  gameScopeType: string;
  gameScopeValue: string;
  ruleSnapshot: string;
}

export interface PurchasePaymentEventVO {
  id: string | number;
  tenantId: string;
  eventKey: string;
  purchaseOrderNo: string;
  providerCode: string;
  providerOrderNo: string;
  eventType: string;
  eventStatus: string;
  requestHash: string;
  requestBody: string;
  processResult: string;
  processTime: string;
  createTime: string;
}

export interface PurchaseReversalItemVO {
  id: string | number;
  tenantId: string;
  reversalId: string | number;
  reversalNo: string;
  purchaseOrderNo: string;
  memberId: string | number;
  currencyCode: string;
  requiredAmount: number;
  availableAmount: number;
  recoveredAmount: number;
  shortfallAmount: number;
  walletTransactionNo: string;
  status: string;
  createTime: string;
  updateTime: string;
}

export interface PurchaseReversalVO {
  reversalNo: string;
  reversalType: string;
  status: string;
  reason: string;
  reviewReason: string;
  dispositionStatus: string;
  reviewedBy: string | number;
  reviewedName: string;
  reviewNote: string;
  resolvedTime: string;
  retryCount: number;
  lastRetryTime: string;
  completedTime: string;
  items: PurchaseReversalItemVO[];
}

export interface PurchaseOrderVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  purchaseOrderNo: string;
  offerId: string | number;
  offerNo: string;
  memberId: string | number;
  memberNo: string;
  payCurrencyCode: string;
  payAmount: number;
  status: string;
  idempotencyKey: string;
  providerCode: string;
  providerOrderNo: string;
  paymentSessionNo: string;
  callbackEventKey: string;
  failReason: string;
  paidTime: string;
  creditedTime: string;
  cancelTime: string;
  refundTime: string;
  chargebackTime: string;
}

export interface PurchaseOrderDetailVO extends PurchaseOrderVO {
  grantSnapshots: PurchaseOrderGrantSnapshotVO[];
  paymentEvents: PurchasePaymentEventVO[];
  reversal?: PurchaseReversalVO;
}

export interface PurchaseOrderQuery extends PageQuery {
  purchaseOrderNo?: string;
  memberId?: string | number;
  memberNo?: string;
  offerId?: string | number;
  offerNo?: string;
  status?: string;
  providerCode?: string;
  providerOrderNo?: string;
  paymentSessionNo?: string;
  idempotencyKey?: string;
  beginTime?: string;
  endTime?: string;
}

export interface PurchaseManualActionForm {
  reason: string;
}
