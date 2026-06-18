export interface ApiErrorBody {
  code: string
  message: string
  trace_id: string
  details: Record<string, unknown>
}

export interface WalletSummary {
  wallet: {
    gcBalance: string | number
    scBalance: string | number
    scFrozen: string | number
    scRedeemable: string | number
  }
  scSourceSummary: Array<{ source: string; amount: string | number }>
  notices: string[]
}

export interface LedgerItem {
  ledgerId: number
  currency: string
  amount: string | number
  direction: string
  businessType: string
  businessId: string
  status: string
  createdAt: string
}

export interface LedgerPage {
  items: LedgerItem[]
  page: number
  pageSize: number
  total: number
}

export interface Campaign {
  campaignCode: string
  campaignType: string
  status: string
}

export interface DailyTask {
  taskId: string
  taskCode: string
  target: number
  status: string
}

export interface Reward {
  currency: string
  amount: string | number
}

export interface ClaimResponse {
  claimId: number
  campaignCode: string
  status: string
  riskAction: string
  rewards: Reward[]
  ledgerIds: number[]
}

export interface ComplianceDocument {
  documentType: string
  version: string
  title: string
  contentUrl: string
}
