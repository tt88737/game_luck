import { reactive } from 'vue'
import { clientApi, clearClientToken, getClientToken, setClientToken } from '../api/client'
import type { ClientBootstrap, ClientMember, ClientRegisterRequest } from '../types/client'

export const sessionState = reactive({
  bootstrap: null as ClientBootstrap | null,
  member: null as ClientMember | null,
  loading: false,
  error: '',
})

export const isLoggedIn = () => Boolean(sessionState.member)

export async function loadBootstrap() {
  sessionState.bootstrap = await clientApi.bootstrap()
}

export async function restoreSession() {
  if (!getClientToken()) {
    return
  }
  try {
    sessionState.member = await clientApi.me()
  } catch (error) {
    sessionState.member = null
    clearClientToken()
  }
}

export async function login(username: string, password: string) {
  sessionState.loading = true
  sessionState.error = ''
  try {
    const result = await clientApi.login(username, password)
    setClientToken(result.accessToken)
    sessionState.member = result.member
    return result.member
  } catch (error) {
    sessionState.error = error instanceof Error ? error.message : '登录失败'
    throw error
  } finally {
    sessionState.loading = false
  }
}

export async function register(payload: ClientRegisterRequest) {
  sessionState.loading = true
  sessionState.error = ''
  try {
    const result = await clientApi.register(payload)
    setClientToken(result.accessToken)
    sessionState.member = result.member
    return result.member
  } catch (error) {
    sessionState.error = error instanceof Error ? error.message : '注册失败'
    throw error
  } finally {
    sessionState.loading = false
  }
}

export function logout() {
  clearClientToken()
  sessionState.member = null
}
