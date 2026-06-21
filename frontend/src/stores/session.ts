import { defineStore } from 'pinia'
import { apiGet, apiPost } from '../api/http'
import type { BindEmailRequest, GuestRequest, RegisterResponse } from '../api/contracts'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('tangluck_token') ?? '',
    userId: localStorage.getItem('tangluck_user_id') ?? '',
    email: localStorage.getItem('tangluck_user_email') ?? '',
    countryCode: localStorage.getItem('tangluck_country_code') ?? '',
    stateCode: localStorage.getItem('tangluck_state_code') ?? '',
    riskLevel: localStorage.getItem('tangluck_risk_level') ?? '',
    status: localStorage.getItem('tangluck_user_status') ?? '',
    accountType: localStorage.getItem('tangluck_account_type') ?? '',
    hydrated: false,
  }),
  getters: {
    isGuest: (state) => state.accountType === 'guest' || state.status === 'guest',
  },
  actions: {
    setSession(token: string, userId: string) {
      this.token = token
      this.userId = userId
      localStorage.setItem('tangluck_token', token)
      localStorage.setItem('tangluck_user_id', userId)
    },
    applyAuthResponse(response: RegisterResponse) {
      this.setSession(response.token, String(response.user.userId))
      this.email = response.user.email
      this.countryCode = response.user.countryCode
      this.stateCode = response.user.stateCode
      this.riskLevel = response.user.riskLevel
      this.status = response.user.status
      this.accountType = response.accountType ?? (response.user.status === 'guest' ? 'guest' : 'formal')
      localStorage.setItem('tangluck_user_email', this.email)
      localStorage.setItem('tangluck_country_code', this.countryCode)
      localStorage.setItem('tangluck_state_code', this.stateCode)
      localStorage.setItem('tangluck_risk_level', this.riskLevel)
      localStorage.setItem('tangluck_user_status', this.status)
      localStorage.setItem('tangluck_account_type', this.accountType)
    },
    async ensureGuestSession() {
      if (this.userId && this.token) return
      const payload: GuestRequest = {
        deviceId: getDeviceId(),
        countryCode: 'US',
        stateCode: 'CA',
        utmSource: 'web',
      }
      const response = await apiPost<RegisterResponse>('/auth/guest', payload)
      this.applyAuthResponse(response)
    },
    async bindEmail(payload: BindEmailRequest) {
      const response = await apiPost<RegisterResponse>('/auth/bind-email', payload)
      this.applyAuthResponse(response)
      return response
    },
    async hydrate() {
      if (!this.token) {
        this.hydrated = true
        return
      }
      try {
        const response = await apiGet<RegisterResponse>('/me')
        this.applyAuthResponse(response)
      } catch {
        this.clearSession()
      } finally {
        this.hydrated = true
      }
    },
    clearSession() {
      this.token = ''
      this.userId = ''
      this.email = ''
      this.countryCode = ''
      this.stateCode = ''
      this.riskLevel = ''
      this.status = ''
      this.accountType = ''
      localStorage.removeItem('tangluck_token')
      localStorage.removeItem('tangluck_user_id')
      localStorage.removeItem('tangluck_user_email')
      localStorage.removeItem('tangluck_country_code')
      localStorage.removeItem('tangluck_state_code')
      localStorage.removeItem('tangluck_risk_level')
      localStorage.removeItem('tangluck_user_status')
      localStorage.removeItem('tangluck_account_type')
    },
  },
})

function getDeviceId() {
  const existing = localStorage.getItem('tangluck_device_id')
  if (existing) return existing
  const id = `web-${crypto.randomUUID?.() ?? Math.random().toString(36).slice(2)}`
  localStorage.setItem('tangluck_device_id', id)
  return id
}
