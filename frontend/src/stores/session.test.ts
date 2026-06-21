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
      accountType: 'formal',
    }))

    const session = useSessionStore()
    await session.hydrate()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/me', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer local-user-42' }),
    }))
    expect(session.userId).toBe('42')
    expect(session.email).toBe('player@example.com')
    expect(session.accountType).toBe('formal')
    expect(localStorage.getItem('tangluck_user_id')).toBe('42')
  })

  it('creates a guest session when no session exists', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/auth/guest') && init?.method === 'POST') {
        return json({
          user: {
            userId: 77,
            email: 'guest_77@guest.tangluck.local',
            countryCode: 'US',
            stateCode: 'CA',
            riskLevel: 'normal',
            status: 'guest',
          },
          wallet: { gcBalance: '10000', scBalance: '0', scFrozen: '0' },
          token: 'guest-token',
          accountType: 'guest',
        })
      }
      return json({})
    })

    const session = useSessionStore()
    await session.ensureGuestSession()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/guest', expect.objectContaining({
      method: 'POST',
    }))
    expect(session.userId).toBe('77')
    expect(session.accountType).toBe('guest')
    expect(session.isGuest).toBe(true)
    expect(localStorage.getItem('tangluck_account_type')).toBe('guest')
  })

  it('binds email and keeps the same user id', async () => {
    localStorage.setItem('tangluck_token', 'guest-token')
    localStorage.setItem('tangluck_user_id', '77')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/auth/bind-email') && init?.method === 'POST') {
        return json({
          user: {
            userId: 77,
            email: 'player@example.com',
            countryCode: 'US',
            stateCode: 'CA',
            riskLevel: 'normal',
            status: 'active',
          },
          wallet: { gcBalance: '10000', scBalance: '0', scFrozen: '0' },
          token: 'formal-token',
          accountType: 'formal',
        })
      }
      return json({})
    })

    const session = useSessionStore()
    await session.bindEmail({
      email: 'player@example.com',
      password: 'Password123!',
      birthDate: '1990-01-01',
      countryCode: 'US',
      stateCode: 'CA',
      acceptedDocuments: [{ documentType: 'terms', version: 'terms-v1' }],
    })

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/bind-email', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'X-User-Id': '77' }),
    }))
    expect(session.userId).toBe('77')
    expect(session.email).toBe('player@example.com')
    expect(session.accountType).toBe('formal')
    expect(session.isGuest).toBe(false)
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
