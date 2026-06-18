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

export interface Campaign {
  campaignCode: string
  campaignType: string
  status: string
}
