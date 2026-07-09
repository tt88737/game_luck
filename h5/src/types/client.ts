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
