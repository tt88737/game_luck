import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiGet, apiPost, ApiError } from './http'

describe('http api client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('attaches bearer token and parses JSON success responses', async () => {
    localStorage.setItem('tangluck_token', 'jwt_user_token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    const result = await apiGet<{ ok: boolean }>('/me')

    expect(result.ok).toBe(true)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/me', expect.objectContaining({
      headers: expect.objectContaining({
        Authorization: 'Bearer jwt_user_token',
      }),
    }))
  })

  it('surfaces backend error code and message', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({
        code: 'REGION_BLOCKED',
        message: 'This feature is not available in your region.',
        trace_id: 'trc_20260618_0001',
        details: { state_code: 'WA' },
      }), {
        status: 403,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    await expect(apiPost('/campaigns/cmp/claim', {})).rejects.toMatchObject({
      code: 'REGION_BLOCKED',
      message: 'This feature is not available in your region.',
    } satisfies Partial<ApiError>)
  })
})
