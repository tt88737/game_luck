import type {
  ApiResponse,
  ClientBootstrap,
  ClientGame,
  ClientGameLaunch,
  ClientDailyLoginReward,
  ClientExchangeOption,
  ClientExchangeOrder,
  ClientLoginResponse,
  ClientMember,
  ClientPage,
  ClientPromotion,
  ClientPurchaseOffer,
  ClientPurchaseOrder,
  ClientPaymentSession,
  PaymentWebhookAck,
  SimulatedCheckout,
  SimulatedPaymentAction,
  ClientRegisterRequest,
  ClientRedemption,
  WalletAccount,
  WalletLedger,
} from '../types/client'

const API_BASE = import.meta.env.VITE_API_BASE || ''
const TOKEN_KEY = 'gameluck.client.token'

export function getClientToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setClientToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearClientToken() {
  localStorage.removeItem(TOKEN_KEY)
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getClientToken()
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('X-Channel-Code', 'h5')
  headers.set('X-Brand-Code', 'demo')
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (response.status === 401) {
    clearClientToken()
    throw new Error('请先登录')
  }
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  const payload = (await response.json()) as ApiResponse<T>
  if (payload.code !== 200 && payload.code !== 0) {
    throw new Error(payload.msg || payload.message || '请求失败')
  }
  return payload.data
}

async function directRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  return response.json() as Promise<T>
}

export const clientApi = {
  bootstrap: () => request<ClientBootstrap>('/api/client/bootstrap'),
  login: (username: string, password: string) =>
    request<ClientLoginResponse>('/api/client/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  register: (payload: ClientRegisterRequest) =>
    request<ClientLoginResponse>('/api/client/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  me: () => request<ClientMember>('/api/client/member/me'),
  walletAccounts: () => request<WalletAccount[]>('/api/client/wallet/accounts'),
  walletLedgers: (currencyCode = 'GC') =>
    request<ClientPage<WalletLedger>>(`/api/client/wallet/ledgers?currencyCode=${encodeURIComponent(currencyCode)}&pageNum=1&pageSize=20`),
  walletExchangeOptions: () => request<ClientExchangeOption[]>('/api/client/wallet/exchange/options'),
  submitWalletExchange: (exchangeRuleId: number, fromAmount: string, idempotencyKey: string) =>
    request<ClientExchangeOrder>('/api/client/wallet/exchange/orders', {
      method: 'POST',
      body: JSON.stringify({ exchangeRuleId, fromAmount, idempotencyKey }),
    }),
  games: (currencyCode = 'GC') => request<ClientGame[]>(`/api/client/games?currencyCode=${encodeURIComponent(currencyCode)}`),
  launchGame: (providerCode: string, gameCode: string, currencyCode: string) =>
    request<ClientGameLaunch>('/api/client/games/launch', {
      method: 'POST',
      body: JSON.stringify({ providerCode, gameCode, currencyCode }),
    }),
  promotions: () => request<ClientPromotion[]>('/api/client/promotions'),
  dailyLoginReward: () => request<ClientDailyLoginReward>('/api/client/promotions/daily-login'),
  claimDailyLoginReward: () =>
    request<ClientDailyLoginReward>('/api/client/promotions/daily-login/claim', {
      method: 'POST',
    }),
  claimPromotion: (promotionId: number) =>
    request<ClientPromotion>('/api/client/promotions/claim', {
      method: 'POST',
      body: JSON.stringify({ promotionId }),
    }),
  redemptions: () => request<ClientRedemption[]>('/api/client/redemptions'),
  requestRedemption: (currencyCode: string, amount: string) =>
    request<ClientRedemption>('/api/client/redemptions/request', {
      method: 'POST',
      body: JSON.stringify({ currencyCode, amount }),
    }),
  purchaseOffers: () => request<ClientPurchaseOffer[]>('/api/client/purchase/offers'),
  payPurchaseOffer: (offerId: number, idempotencyKey: string) =>
    request<ClientPurchaseOrder>('/api/client/purchase/orders/pay', {
      method: 'POST',
      body: JSON.stringify({ offerId, idempotencyKey }),
    }),
  createPaymentSession: (orderNo: string, requestKey: string, providerCode = 'SIMULATED') =>
    request<ClientPaymentSession>(`/api/client/purchase/orders/${encodeURIComponent(orderNo)}/payment-sessions`, {
      method: 'POST',
      body: JSON.stringify({ requestKey, providerCode }),
    }),
  paymentSession: (sessionNo: string) =>
    request<ClientPaymentSession>(`/api/client/purchase/payment-sessions/${encodeURIComponent(sessionNo)}`),
  simulatedCheckout: (providerSessionNo: string) =>
    directRequest<SimulatedCheckout>(`/payment/simulated/checkout/${encodeURIComponent(providerSessionNo)}`),
  executeSimulatedPaymentAction: (providerSessionNo: string, action: SimulatedPaymentAction) =>
    directRequest<PaymentWebhookAck>(`/payment/simulated/checkout/${encodeURIComponent(providerSessionNo)}/actions`, {
      method: 'POST',
      body: JSON.stringify({ action }),
    }),
  replaySimulatedPayment: (providerSessionNo: string) =>
    directRequest<PaymentWebhookAck>(`/payment/simulated/checkout/${encodeURIComponent(providerSessionNo)}/replay`, {
      method: 'POST',
    }),
}
