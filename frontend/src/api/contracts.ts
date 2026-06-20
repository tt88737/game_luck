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

export interface AcceptedDocument {
  documentType: string
  version: string
}

export interface RegisterRequest {
  email: string
  password: string
  birthDate: string
  countryCode: string
  stateCode: string
  acceptedDocuments: AcceptedDocument[]
  utmSource: string
  deviceId: string
}

export interface RegisterResponse {
  user: {
    userId: number
    email: string
    countryCode: string
    stateCode: string
    riskLevel: string
    status: string
  }
  wallet: {
    gcBalance: string | number
    scBalance: string | number
    scFrozen: string | number
  }
  token: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface DashboardSummary {
  registrations: number
  claims: number
  scGranted: string | number
  riskEvents: number
}

export interface AdminRewardRequest {
  currency: string
  amount: string
}

export interface AdminCampaignRequest {
  campaignCode: string
  name: string
  campaignType: string
  eligibleRegions: string[]
  blockedRegions: string[]
  rewardPolicy: AdminRewardRequest[]
  scStrategy: string
  rulesVersion: string
  legalApprovalId: string
  riskAction: string
}

export interface AdminCampaignResponse {
  campaignCode: string
  status: string
}

export interface AuditLog {
  id: number
  operatorId: number
  operatorRole: string
  action: string
  targetType: string
  targetId: string
  beforeJson: string
  afterJson: string
  ip: string
  createdAt: string
}

export interface ProductPackage {
  packageCode: string
  name: string
  priceAmount: string | number
  priceCurrency: string
  gcAmount: string | number
  sandboxOnly: boolean
}

export interface PurchaseOrder {
  orderId: string
  userId: number
  packageCode: string
  priceAmount: string | number
  priceCurrency: string
  status: string
  provider: string
  currencyGranted: string
  amountGranted: string | number
  createdAt: string
}

export interface KycStatus {
  userId: number
  status: 'not_started' | 'reviewing' | 'approved' | 'rejected'
  legalName: string | null
  reviewReason: string | null
  updatedAt: string | null
}

export interface RedemptionRequest {
  redemptionId: string
  userId: number
  scAmount: string | number
  method: string
  status: string
  sandboxOnly: boolean
  createdAt: string
}

export interface P1Operations {
  purchaseOrders: PurchaseOrder[]
  kycApplications: KycStatus[]
  redemptionRequests: RedemptionRequest[]
}

export interface AdminRegion {
  countryCode: string
  stateCode: string
  registrationAllowed: boolean
  gameAllowed: boolean
  purchaseAllowed: boolean
  scGrantAllowed: boolean
  redemptionAllowed: boolean
  amoeAllowed: boolean
  requiresLegalReview: boolean
  status: string
  legalApprovalId: string | null
}

export interface AdminLegalDocument {
  documentType: string
  version: string
  title: string
  contentUrl: string
  status: string
  legalApprovalId: string | null
}
