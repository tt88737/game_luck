import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppRegister from './AppRegister.vue'

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

describe('AppRegister', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('loads legal documents and registers a CA user', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/compliance/documents')) {
        return json([
          { documentType: 'terms', version: 'terms-v1', title: 'Terms of Use', contentUrl: '/legal/terms-v1' },
          { documentType: 'sweepstakes_rules', version: 'rules-v1', title: 'Sweepstakes Rules', contentUrl: '/legal/rules-v1' },
          { documentType: 'privacy', version: 'privacy-v1', title: 'Privacy Policy', contentUrl: '/legal/privacy-v1' },
        ])
      }
      if (url.endsWith('/auth/register') && init?.method === 'POST') {
        return json({
          user: { userId: 7, email: 'player@example.com', countryCode: 'US', stateCode: 'CA', riskLevel: 'normal', status: 'active' },
          wallet: { gcBalance: '0', scBalance: '0', scFrozen: '0' },
          token: 'jwt-token',
        })
      }
      return json({})
    })

    const wrapper = mount(AppRegister)
    await flushPromises()
    await wrapper.get('input[type="email"]').setValue('player@example.com')
    await wrapper.get('input[type="password"]').setValue('Password123!')
    await wrapper.get('input[type="date"]').setValue('1990-01-01')
    await wrapper.get('select').setValue('CA')
    for (const checkbox of wrapper.findAll('input[type="checkbox"]')) {
      await checkbox.setValue(true)
    }
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/register', expect.objectContaining({ method: 'POST' }))
    expect(localStorage.getItem('tangluck_token')).toBe('jwt-token')
    expect(localStorage.getItem('tangluck_user_id')).toBe('7')
    expect(push).toHaveBeenCalledWith('/app')
  })

  it('shows backend region restriction errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/compliance/documents')) {
        return json([{ documentType: 'terms', version: 'terms-v1', title: 'Terms of Use', contentUrl: '/legal/terms-v1' }])
      }
      return json({
        code: 'REGION_BLOCKED',
        message: 'Registration is not available in WA.',
        trace_id: 'trace-wa',
        details: { state_code: 'WA' },
      }, 403)
    })

    const wrapper = mount(AppRegister)
    await flushPromises()
    await wrapper.get('input[type="email"]').setValue('wa@example.com')
    await wrapper.get('input[type="password"]').setValue('Password123!')
    await wrapper.get('input[type="date"]').setValue('1990-01-01')
    await wrapper.get('select').setValue('WA')
    await wrapper.get('input[type="checkbox"]').setValue(true)
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Registration is not available in WA.')
  })
})
