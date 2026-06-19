import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useSessionStore } from './session'

function json(data: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }))
}

describe('session store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('hydrates the current user from the session token', async () => {
    localStorage.setItem('tangluck_token', 'local-user-42')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({
      user: {
        userId: 42,
        email: 'player@example.com',
        countryCode: 'US',
        stateCode: 'CA',
        riskLevel: 'normal',
        status: 'active',
      },
      wallet: { gcBalance: '0', scBalance: '0', scFrozen: '0' },
      token: 'local-user-42',
    }))

    const session = useSessionStore()
    await session.hydrate()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/me', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer local-user-42' }),
    }))
    expect(session.userId).toBe('42')
    expect(session.email).toBe('player@example.com')
    expect(localStorage.getItem('tangluck_user_id')).toBe('42')
  })

  it('clears stale storage when the token is rejected', async () => {
    localStorage.setItem('tangluck_token', 'local-user-404')
    localStorage.setItem('tangluck_user_id', '404')
    vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({
      code: 'AUTH_INVALID_CREDENTIALS',
      message: 'Invalid email or password.',
      trace_id: 'trace-session',
      details: {},
    }, 401))

    const session = useSessionStore()
    await session.hydrate()

    expect(session.token).toBe('')
    expect(session.userId).toBe('')
    expect(localStorage.getItem('tangluck_token')).toBeNull()
  })
})
