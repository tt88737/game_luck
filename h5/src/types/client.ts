export interface ApiResponse<T> {
  code: number
  msg?: string
  message?: string
  data: T
}

export interface ClientCurrency {
  currencyCode: string
  currencyName: string
  decimalScale: number
  playable: boolean
  rechargeable: boolean
  withdrawable: boolean
}

export interface ClientBootstrap {
  tenantId: string
  brandCode: string
  channelCode: string
  brandName: string
  theme: {
    logoText: string
    primaryColor: string
  }
  features: {
    walletEnabled: boolean
    gameEnabled: boolean
    promotionEnabled: boolean
    redemptionEnabled: boolean
    paymentEnabled: boolean
    kycEnabled: boolean
  }
  currencies: ClientCurrency[]
}

export interface ClientMember {
  memberId: number
  memberNo: string
  username: string
  nickname: string
  status: string
  kycStatus: string
}

export interface ClientLoginResponse {
  accessToken: string
  expiresIn: number
  member: ClientMember
}

export interface ClientRegisterRequest {
  username: string
  password: string
  nickname?: string
  countryCode: string
  stateCode?: string
  ageConfirmed: boolean
  termsAccepted: boolean
  privacyAccepted: boolean
  sweepstakesRulesAccepted: boolean
}

export interface WalletAccount {
  currencyCode: string
  currencyName: string
  availableBalance: string
  frozenBalance: string
  decimalScale: number
  playable: boolean
  withdrawable: boolean
}

export interface WalletLedger {
  ledgerId: number
  currencyCode: string
  direction: string
  amount: string
  afterAvailable: string
  bizType: string
  createdAt: string
}

export interface ClientPage<T> {
  records: T[]
  total: number
}

export interface ClientGame {
  providerCode: string
  gameCode: string
  gameName: string
  status: string
  supportedCurrencies: string[]
  thumbnailUrl: string
  maintenance: boolean
}

export interface ClientGameLaunch {
  sessionNo: string
  launchMode: string
  launchUrl: string
  message: string
}

export interface ClientPromotion {
  promotionId: number
  promotionNo: string
  promotionName: string
  currencyCode: string
  rewardAmount: string
  status: string
  claimStatus: string
  claimNo: string
  walletTransactionNo: string
  canClaim: boolean
}

export interface ClientRewardItem {
  currencyCode: string
  rewardAmount: string | number
}

export interface ClientDailyLoginReward {
  promotionId?: number
  promotionNo?: string
  promotionName?: string
  promotionType?: string
  claimDate: string
  canClaim: boolean
  claimStatus: string
  rewardItems: ClientRewardItem[]
  claimNo?: string
  walletTransactionNo?: string
}

export interface ClientRedemption {
  orderId: number
  orderNo: string
  currencyCode: string
  amount: string
  status: string
  walletFreezeNo: string
  reviewRemark: string
  createdAt: string
}

export interface ClientPurchaseGrantItem {
  grantType: string
  currencyCode: string
  grantAmount: string
  wageringMode: string
  requiredTurnover: string
  wageringMultiplier: string
  gameScopeType: string
  gameScopeValue?: string
}

export interface ClientPurchaseOffer {
  offerId: number
  offerNo: string
  offerName: string
  offerType: string
  payCurrencyCode: string
  payAmount: string
  grantItems: ClientPurchaseGrantItem[]
  limitText: string
  wageringText: string
}

export interface ClientPurchaseOrder {
  orderId: number
  orderNo: string
  offerId: number
  offerNo: string
  offerName: string
  payCurrencyCode: string
  payAmount: string
  status: string
  grantItems: ClientPurchaseGrantItem[]
  creditedAt?: string
}
