export type ReconciliationBatchStatus = 'UPLOADED' | 'VALIDATED' | 'RECONCILING' | 'COMPLETED' | 'FAILED';
export type ReconciliationLineStatus = 'INVALID' | 'MATCHED' | 'ISSUE';
export type ReconciliationIssueStatus = 'OPEN' | 'RESOLVED' | 'IGNORED';
export type ReconciliationIssueType =
  | 'PLATFORM_RECORD_MISSING'
  | 'PROVIDER_RECORD_MISSING'
  | 'ORDER_IDENTITY_MISMATCH'
  | 'AMOUNT_MISMATCH'
  | 'CURRENCY_MISMATCH'
  | 'EVENT_MISSING'
  | 'STATUS_MISMATCH'
  | 'DUPLICATE_PROVIDER_RECORD'
  | 'UNSUPPORTED_RECORD';
export type ReconciliationResolutionType = 'PLATFORM_CONFIRMED' | 'PROVIDER_CONFIRMED' | 'EXPECTED_DIFFERENCE' | 'DUPLICATE_CONFIRMED' | 'OTHER';
export type ProviderEventType = 'PAYMENT_SUCCEEDED' | 'PAYMENT_FAILED' | 'PAYMENT_CANCELLED' | 'REFUND_SUCCEEDED' | 'CHARGEBACK_CREATED';
export interface ReconciliationBatchQuery extends PageQuery {
  providerCode?: string;
  statementDate?: string;
  status?: ReconciliationBatchStatus | '';
  originalFileName?: string;
}
export interface ReconciliationBatchVO {
  id: string;
  tenantId: string;
  providerCode: string;
  statementDate: string;
  originalFileName: string;
  fileDigest: string;
  totalCount: number;
  validCount: number;
  invalidCount: number;
  matchedCount: number;
  discrepancyCount: number;
  status: ReconciliationBatchStatus;
  failureReason: string;
  creatorId: string;
  creatorName: string;
  createTime: string;
  updateTime: string;
}
export type ReconciliationBatchDetailVO = ReconciliationBatchVO;
export interface ReconciliationLineQuery extends PageQuery {
  lineStatus?: ReconciliationLineStatus | '';
}
export interface ReconciliationLineVO {
  id: string;
  batchId: string;
  sourceRowNumber: number;
  providerRecordId: string;
  eventType: ProviderEventType;
  providerSessionNo: string;
  purchaseOrderNo: string;
  currencyCode: string;
  amount: string;
  occurredTime: string;
  status: ReconciliationLineStatus;
  parseError: string;
  rawFieldsJson: string;
  createTime: string;
}
export interface ReconciliationIssueQuery extends PageQuery {
  issueType?: ReconciliationIssueType | '';
  status?: ReconciliationIssueStatus | '';
  purchaseOrderNo?: string;
  sessionNo?: string;
  providerRecordId?: string;
}
export interface ReconciliationIssueVO {
  id: string;
  batchId: string;
  lineId: string | null;
  issueType: ReconciliationIssueType;
  status: ReconciliationIssueStatus;
  paymentSessionId: string | null;
  sessionNo: string;
  purchaseOrderId: string | null;
  purchaseOrderNo: string;
  webhookEventId: string | null;
  reversalId: string | null;
  providerEventType: string;
  platformEventType: string;
  providerCurrencyCode: string;
  platformCurrencyCode: string;
  providerAmount: string;
  platformAmount: string;
  providerStatus: string;
  platformStatus: string;
  diagnosticSnapshotJson: string;
  resolutionType: ReconciliationResolutionType | '';
  resolutionRemark: string;
  resolvedBy: string | null;
  resolvedTime: string;
  version: number;
  createTime: string;
  updateTime: string;
}
export interface ReconciliationActionLogVO {
  id: string;
  batchId: string;
  issueId: string;
  actionType: string;
  beforeStatus: ReconciliationIssueStatus;
  afterStatus: ReconciliationIssueStatus;
  operatorId: string;
  operatorName: string;
  remark: string;
  createTime: string;
}
export interface ReconciliationIssueDetailVO extends ReconciliationIssueVO {
  actionLogs: ReconciliationActionLogVO[];
  sourceRowNumber: number | null;
  sourceLine: ReconciliationLineVO | null;
  canonicalOriginalFields: string | null;
  platformOnly: boolean;
}
export interface ReconciliationResolutionCommand {
  resolutionType: ReconciliationResolutionType;
  remark: string;
  expectedVersion: number;
}
