import type { ApiErrorBody } from './contracts'

const API_BASE = '/api/v1'

export class ApiError extends Error {
  code: string
  traceId: string
  details: Record<string, unknown>

  constructor(body: ApiErrorBody) {
    super(body.message || 'Request failed.')
    this.name = 'ApiError'
    this.code = body.code || 'REQUEST_FAILED'
    this.traceId = body.trace_id || ''
    this.details = body.details || {}
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'GET' })
}

export async function apiPost<T>(path: string, body?: unknown, idempotencyKey?: string): Promise<T> {
  const headers: Record<string, string> = {}
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey
  return request<T>(path, {
    method: 'POST',
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export async function apiPatch<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'PATCH',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  const token = localStorage.getItem('tangluck_token')
  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...(init.body ? { 'Content-Type': 'application/json' } : {}),
    ...(init.headers as Record<string, string> | undefined),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
  const userId = localStorage.getItem('tangluck_user_id')
  if (userId) headers['X-User-Id'] = userId

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  const payload = await response.json().catch(() => undefined)
  if (!response.ok) {
    throw new ApiError(normalizeErrorBody(payload, response.status))
  }
  return payload as T
}

function normalizeErrorBody(payload: unknown, status: number): ApiErrorBody {
  if (payload && typeof payload === 'object') {
    const body = payload as Partial<ApiErrorBody>
    if (typeof body.message === 'string') {
      return {
        code: typeof body.code === 'string' ? body.code : `HTTP_${status}`,
        message: body.message,
        trace_id: typeof body.trace_id === 'string' ? body.trace_id : '',
        details: body.details && typeof body.details === 'object' ? body.details : {},
      }
    }
  }
  return {
    code: `HTTP_${status}`,
    message: `HTTP ${status} request failed.`,
    trace_id: '',
    details: {},
  }
}
