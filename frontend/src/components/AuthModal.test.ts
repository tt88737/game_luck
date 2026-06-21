import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AuthModal from './AuthModal.vue'
import { i18n } from '../i18n'

function json(data: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }))
}

describe('AuthModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('renders register and login inside the auth modal', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/compliance/documents')) {
        return json([
          { documentType: 'terms', version: 'terms-v1', title: 'Terms of Use', contentUrl: '/legal/terms-v1' },
          { documentType: 'sweepstakes_rules', version: 'rules-v1', title: 'Sweepstakes Rules', contentUrl: '/legal/rules-v1' },
          { documentType: 'privacy', version: 'privacy-v1', title: 'Privacy Policy', contentUrl: '/legal/privacy-v1' },
        ])
      }
      return json({})
    })

    const wrapper = mount(AuthModal, {
      props: { modelValue: true, initialMode: 'register' },
      global: { mocks: { $t: i18n.t }, stubs: { teleport: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Bind account')
    expect(wrapper.text()).toContain('Sign in')
    expect(wrapper.find('[data-test="auth-bind-submit"]').exists()).toBe(true)

    await wrapper.get('[data-test="auth-login-tab"]').trigger('click')

    expect(wrapper.find('[data-test="auth-login-submit"]').exists()).toBe(true)
  })

  it('submits bind email through the guest session', async () => {
    localStorage.setItem('tangluck_user_id', '77')
    localStorage.setItem('tangluck_token', 'guest-token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/compliance/documents')) {
        return json([{ documentType: 'terms', version: 'terms-v1', title: 'Terms of Use', contentUrl: '/legal/terms-v1' }])
      }
      if (url.endsWith('/auth/bind-email') && init?.method === 'POST') {
        return json({
          user: { userId: 77, email: 'player@example.com', countryCode: 'US', stateCode: 'CA', riskLevel: 'normal', status: 'active' },
          wallet: { gcBalance: '10000', scBalance: '0', scFrozen: '0' },
          token: 'formal-token',
          accountType: 'formal',
        })
      }
      return json({})
    })

    const wrapper = mount(AuthModal, {
      props: { modelValue: true, initialMode: 'register' },
      global: { mocks: { $t: i18n.t }, stubs: { teleport: true } },
    })
    await flushPromises()
    await wrapper.get('input[type="email"]').setValue('player@example.com')
    await wrapper.get('input[type="password"]').setValue('Password123!')
    await wrapper.get('input[type="date"]').setValue('1990-01-01')
    await wrapper.get('input[type="checkbox"]').setValue(true)
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/bind-email', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.emitted('authenticated')).toBeTruthy()
  })
})
