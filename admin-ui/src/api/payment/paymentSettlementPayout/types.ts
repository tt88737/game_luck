export type SettlementPayoutStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type SettlementPayoutActionType = 'CREATE' | 'EDIT' | 'SUBMIT' | 'APPROVE' | 'REJECT' | 'CANCEL';

export interface SettlementPayoutQuery extends PageQuery {
  payoutNo?: string;
  settlementNo?: string;
  status?: SettlementPayoutStatus | '';
  providerCode?: string;
  currencyCode?: string;
  createStart?: string;
  createEnd?: string;
}

export interface SettlementPayoutCreateCommand {
  settlementBatchId: string;
  payoutPurpose: string;
  payeeReference: string;
}

export interface SettlementPayoutEditCommand {
  version: number;
  payoutPurpose: string;
  payeeReference: string;
}

export interface SettlementPayoutStateCommand {
  version: number;
  reason: string;
}

export interface SettlementPayoutRowVO {
  id: string;
  payoutNo: string;
  settlementBatchId: string;
  settlementNo: string;
  providerCode: string;
  currencyCode: string;
  payoutAmount: string;
  payoutPurpose: string;
  payeeReference: string;
  status: SettlementPayoutStatus;
  makerId: string;
  makerName: string;
  submitterId: string | null;
  submitterName: string | null;
  reviewerId: string | null;
  reviewerName: string | null;
  decisionReason: string | null;
  version: number;
  submittedTime: string | null;
  reviewedTime: string | null;
  createTime: string;
  updateTime: string;
}

export interface SettlementPayoutActionLogVO {
  id: string;
  payoutId: string;
  actionType: SettlementPayoutActionType;
  beforeStatus: SettlementPayoutStatus | null;
  afterStatus: SettlementPayoutStatus;
  operatorId: string;
  operatorName: string;
  reason: string | null;
  evidenceSnapshotJson: string | null;
  expectedVersion: number | null;
  resultVersion: number;
  createTime: string;
}

export interface SettlementPayoutDetailVO extends SettlementPayoutRowVO {
  settlementEvidenceJson: string | null;
  actionLogs: SettlementPayoutActionLogVO[];
}
