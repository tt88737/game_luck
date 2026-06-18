import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppHome from './AppHome.vue'

function json(data: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }))
}

describe('AppHome', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('shows balances, rewards, tasks, coupon, and legal links', async () => {
    localStorage.setItem('tangluck_user_id', '1')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/wallet/summary')) {
        return json({
          wallet: { gcBalance: '10000.0000', scBalance: '0.5000', scFrozen: '0.0000', scRedeemable: '0.5000' },
          scSourceSummary: [{ source: 'register_bonus', amount: '0.5000' }],
          notices: ['SC is promotional and not sold in P0-A.'],
        })
      }
      if (url.endsWith('/campaigns')) {
        return json([{ campaignCode: 'WELCOME_BONUS', campaignType: 'register_bonus', status: 'active' }])
      }
      if (url.endsWith('/tasks/daily')) {
        return json([{ taskId: 'DAILY_LOGIN', taskCode: 'DAILY_LOGIN', target: 1, status: 'in_progress' }])
      }
      if (url.endsWith('/compliance/documents')) {
        return json([
          { documentType: 'terms', version: 'terms-v1', title: 'Terms of Use', contentUrl: '/legal/terms-v1' },
          { documentType: 'sweepstakes_rules', version: 'rules-v1', title: 'Sweepstakes Rules', contentUrl: '/legal/rules-v1' },
          { documentType: 'amoe', version: 'amoe-v1', title: 'AMOE / No Purchase Necessary', contentUrl: '/legal/amoe-v1' },
        ])
      }
      return json({})
    })

    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).toContain('10,000')
    expect(wrapper.text()).toContain('0.50')
    expect(wrapper.text()).toContain('WELCOME_BONUS')
    expect(wrapper.text()).toContain('DAILY_LOGIN')
    expect(wrapper.text()).toContain('WELCOME500')
    expect(wrapper.text()).toContain('Terms of Use')
    expect(wrapper.text()).toContain('AMOE / No Purchase Necessary')
  })

  it('displays region restriction from backend errors', async () => {
    localStorage.setItem('tangluck_user_id', '1')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      if (String(input).endsWith('/wallet/summary')) {
        return json({
          code: 'REGION_BLOCKED',
          message: 'This feature is not available in your region.',
          trace_id: 'trace-1',
          details: { state_code: 'WA' },
        }, 403)
      }
      return json([])
    })

    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).toContain('This feature is not available in your region.')
    expect(wrapper.text()).toContain('Region restricted')
  })

  it('shows duplicate claim errors from campaign claim', async () => {
    localStorage.setItem('tangluck_user_id', '1')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/wallet/summary')) {
        return json({
          wallet: { gcBalance: '0', scBalance: '0', scFrozen: '0', scRedeemable: '0' },
          scSourceSummary: [],
          notices: [],
        })
      }
      if (url.endsWith('/campaigns')) return json([{ campaignCode: 'WELCOME_BONUS', campaignType: 'register_bonus', status: 'active' }])
      if (url.endsWith('/tasks/daily')) return json([])
      if (url.endsWith('/compliance/documents')) return json([])
      if (url.endsWith('/campaigns/WELCOME_BONUS/claim') && init?.method === 'POST') {
        return json({
          code: 'CLAIM_DUPLICATED',
          message: 'Reward already claimed for this period.',
          trace_id: 'trace-2',
          details: {},
        }, 409)
      }
      return json({})
    })

    const wrapper = mountHome()
    await flushPromises()
    await wrapper.get('[data-test="claim-welcome"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Reward already claimed for this period.')
  })
})

function mountHome() {
  const pinia = createPinia()
  return mount(AppHome, {
    global: {
      plugins: [pinia],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="to"><slot /></a>',
        },
      },
    },
  })
}
