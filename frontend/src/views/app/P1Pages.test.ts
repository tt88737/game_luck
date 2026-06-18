import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AppStore from './AppStore.vue'
import AppKyc from './AppKyc.vue'
import AppRedemption from './AppRedemption.vue'
import AdminP1Operations from '../admin/AdminP1Operations.vue'

function json(data: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }))
}

const stubs = {
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
}

describe('P1 sandbox pages', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('creates a sandbox GC purchase order from the store', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/purchase/packages')) {
        return json([{ packageCode: 'gc_499', name: 'GC 5,000 Sandbox Pack', priceAmount: '4.9900', priceCurrency: 'USD', gcAmount: '5000.0000', sandboxOnly: true }])
      }
      if (url.endsWith('/purchase/orders') && init?.method === 'POST') {
        return json({ orderId: 'ord_demo', userId: 1, packageCode: 'gc_499', priceAmount: '4.9900', priceCurrency: 'USD', status: 'paid', provider: 'sandbox', currencyGranted: 'GC', amountGranted: '5000.0000', createdAt: '2026-06-19T00:00:00Z' })
      }
      return json({})
    })

    const wrapper = mount(AppStore, { global: { stubs } })
    await flushPromises()
    await wrapper.get('[data-test="buy-gc_499"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Sandbox order paid')
    expect(wrapper.text()).toContain('ord_demo')
    expect(wrapper.text()).toContain('5,000 GC')
  })

  it('submits sandbox KYC for manual review', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/kyc/status')) return json({ userId: 1, status: 'not_started', legalName: null, reviewReason: null, updatedAt: null })
      if (url.endsWith('/kyc/applications') && init?.method === 'POST') {
        return json({ userId: 1, status: 'reviewing', legalName: 'P1 Demo User', reviewReason: null, updatedAt: '2026-06-19T00:00:00Z' })
      }
      return json({})
    })

    const wrapper = mount(AppKyc, { global: { stubs } })
    await flushPromises()
    await wrapper.get('[data-test="submit-kyc"]').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('reviewing')
  })

  it('shows KYC blocking state on redemption page', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/wallet/summary')) {
        return json({ wallet: { gcBalance: '0', scBalance: '0.5000', scFrozen: '0', scRedeemable: '0.5000' }, scSourceSummary: [], notices: [] })
      }
      if (url.endsWith('/kyc/status')) return json({ userId: 1, status: 'reviewing', legalName: 'P1 Demo User', reviewReason: null, updatedAt: '2026-06-19T00:00:00Z' })
      return json({})
    })

    const wrapper = mount(AppRedemption, { global: { stubs } })
    await flushPromises()

    expect(wrapper.text()).toContain('KYC approval is required')
    expect(wrapper.get('[data-test="submit-redemption"]').attributes('disabled')).toBeDefined()
  })

  it('lets admin approve KYC from the P1 operations table', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/p1/operations')) {
        return json({
          purchaseOrders: [],
          kycApplications: [{ userId: 1, status: 'reviewing', legalName: 'P1 Demo User', reviewReason: null, updatedAt: '2026-06-19T00:00:00Z' }],
          redemptionRequests: [],
        })
      }
      if (url.endsWith('/admin/kyc/1/approve') && init?.method === 'POST') {
        return json({ userId: 1, status: 'approved', legalName: 'P1 Demo User', reviewReason: 'sandbox approved by ops', updatedAt: '2026-06-19T00:00:01Z' })
      }
      return json({})
    })

    const wrapper = mount(AdminP1Operations, { global: { stubs } })
    await flushPromises()
    await wrapper.get('button:not([disabled])').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('P1 Demo User')
  })
})
