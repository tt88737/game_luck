export type SettlementBatchStatus = 'CREATED' | 'CALCULATING' | 'CALCULATED' | 'CLOSED' | 'FAILED';
export type SettlementActionType = 'CREATE' | 'CALCULATE' | 'CALCULATION_FAILED' | 'CLOSE_REJECTED' | 'CLOSE';
export type SettlementEventType = 'PAYMENT_SUCCEEDED' | 'REFUND_SUCCEEDED' | 'CHARGEBACK_CREATED';

export interface SettlementBatchQuery extends PageQuery {
  settlementNo?: string;
  providerCode?: string;
  currencyCode?: string;
  status?: SettlementBatchStatus | '';
  periodStart?: string;
  periodEnd?: string;
}
export interface SettlementCreateCommand {
  providerCode: string;
  currencyCode: string;
  periodStart: string;
  periodEnd: string;
  paymentFeeRate: string;
  paymentFixedFee: string;
  chargebackFixedFee: string;
}
export interface SettlementCloseCommand {
  version: number;
  remark: string;
}
export interface SettlementBatchVO {
  id: string;
  settlementNo: string;
  providerCode: string;
  currencyCode: string;
  periodStart: string;
  periodEnd: string;
  status: SettlementBatchStatus;
  paymentFeeRate: string;
  paymentFixedFee: string;
  chargebackFixedFee: string;
  eventCount: number;
  paymentCount: number;
  refundCount: number;
  chargebackCount: number;
  grossPayment: string;
  refundAmount: string;
  chargebackAmount: string;
  totalFee: string;
  netSettlement: string;
  reconciliationCoverageCount: number;
  openIssueCount: number;
  failureReason: string;
  creatorId: string;
  creatorName: string;
  calculatorId: string | null;
  calculatorName: string;
  closerId: string | null;
  closerName: string;
  closeRemark: string;
  calculatedTime: string;
  closedTime: string;
  version: number;
  createTime: string;
  updateTime: string;
}
export interface SettlementActionLogVO {
  id: string;
  batchId: string;
  actionType: SettlementActionType;
  beforeStatus: SettlementBatchStatus;
  afterStatus: SettlementBatchStatus;
  operatorId: string;
  operatorName: string;
  remark: string;
  evidenceSnapshotJson: string;
  createTime: string;
}
export interface SettlementDetailVO extends SettlementBatchVO {
  evidenceSnapshotJson: string;
  actionLogs: SettlementActionLogVO[];
}
export interface SettlementItemQuery extends PageQuery {
  eventType?: SettlementEventType | '';
}
export interface SettlementItemVO {
  id: string;
  batchId: string;
  webhookEventId: string;
  providerEventId: string;
  paymentSessionId: string;
  sessionNo: string;
  providerSessionNo: string;
  purchaseOrderId: string;
  purchaseOrderNo: string;
  eventType: SettlementEventType;
  receivedTime: string;
  currencyCode: string;
  sourceAmount: string;
  grossPayment: string;
  refundAmount: string;
  chargebackAmount: string;
  feeAmount: string;
  netContribution: string;
  sourceSnapshotJson: string;
  createTime: string;
}
