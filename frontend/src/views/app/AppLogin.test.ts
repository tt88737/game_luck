import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppLogin from './AppLogin.vue'
import { i18n } from '../../i18n'

function json(data: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }))
}

const push = vi.fn()

vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
  useRouter: () => ({ push }),
}))

describe('AppLogin', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('logs in and stores the returned session', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/auth/login') && init?.method === 'POST') {
        return json({
          user: { userId: 9, email: 'player@example.com', countryCode: 'US', stateCode: 'CA', riskLevel: 'low', status: 'active' },
          wallet: { gcBalance: '0', scBalance: '0', scFrozen: '0' },
          token: 'login-token',
        })
      }
      return json({})
    })

    const wrapper = mount(AppLogin, { global: { mocks: { $t: i18n.t } } })
    await wrapper.get('input[type="email"]').setValue('player@example.com')
    await wrapper.get('input[type="password"]').setValue('StrongPass123!')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/login', expect.objectContaining({ method: 'POST' }))
    expect(localStorage.getItem('tangluck_token')).toBe('login-token')
    expect(localStorage.getItem('tangluck_user_id')).toBe('9')
    expect(push).toHaveBeenCalledWith('/lobby')
  })

  it('shows invalid credential errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({
      code: 'AUTH_INVALID_CREDENTIALS',
      message: 'Invalid email or password.',
      trace_id: 'trace-login',
      details: {},
    }, 401))

    const wrapper = mount(AppLogin, { global: { mocks: { $t: i18n.t } } })
    await wrapper.get('input[type="email"]').setValue('player@example.com')
    await wrapper.get('input[type="password"]').setValue('bad-password')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Invalid email or password.')
  })
})
