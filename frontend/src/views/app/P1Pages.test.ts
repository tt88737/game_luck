import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppStore from './AppStore.vue'
import AppKyc from './AppKyc.vue'
import AppRedemption from './AppRedemption.vue'
import AdminP1Operations from '../admin/AdminP1Operations.vue'
import AdminPackages from '../admin/AdminPackages.vue'
import AdminOrders from '../admin/AdminOrders.vue'
import AdminUsers from '../admin/AdminUsers.vue'
import AdminWalletLedger from '../admin/AdminWalletLedger.vue'
import AdminKycReview from '../admin/AdminKycReview.vue'
import AdminRedemptions from '../admin/AdminRedemptions.vue'
import AppActivity from './AppActivity.vue'
import AppSlots from './AppSlots.vue'
import AppInbox from './AppInbox.vue'
import AppShell from './AppShell.vue'
import AppMe from './AppMe.vue'
import { i18n } from '../../i18n'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

function json(data: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }))
}

const stubs = {
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
}

const global = {
  stubs,
  mocks: {
    $t: i18n.t,
  },
}

describe('P1 production pages', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
    i18n.setLocale('en')
  })

  it('localizes C-side activity workflow when Chinese is selected', async () => {
    i18n.setLocale('zh-CN')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/campaigns')) return json([])
      if (url.endsWith('/tasks/daily')) return json([])
      return json({})
    })

    const wrapper = mount(AppActivity, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('促销')
    expect(wrapper.text()).toContain('可领取奖励')
    expect(wrapper.text()).toContain('活动')
    expect(wrapper.text()).toContain('每日任务')
    expect(wrapper.text()).not.toContain('Activity center')
    expect(wrapper.text()).not.toContain('Claimable rewards')
  })

  it('renders the C-side shell with guest account actions and production nav', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/auth/guest') && init?.method === 'POST') {
        return json({
          user: { userId: 77, email: 'guest_77@guest.tangluck.local', countryCode: 'US', stateCode: 'CA', riskLevel: 'normal', status: 'guest' },
          wallet: { gcBalance: '10000', scBalance: '0', scFrozen: '0' },
          token: 'guest-token',
          accountType: 'guest',
        })
      }
      return json({})
    })

    const wrapper = mount(AppShell, {
      global: {
        plugins: [createRouter({
          history: createMemoryHistory(),
          routes: [{ path: '/app', component: { template: '<div />' } }],
        })],
        mocks: { $t: i18n.t },
        stubs: {
          RouterLink: { props: ['to'], template: `<a :href="typeof to === 'string' ? to : to.path"><slot /></a>` },
          RouterView: { template: '<section data-test="route-outlet">Lobby content</section>' },
          AuthModal: true,
        },
      },
    })
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/guest', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('Guest')
    expect(wrapper.text()).not.toContain('User ')
    expect(wrapper.text()).toContain('Bind account')
    expect(wrapper.text()).toContain('Sign in')
    expect(wrapper.text()).toContain('Store')
    expect(wrapper.text()).toContain('Promo')
    expect(wrapper.text()).toContain('Lobby')
    expect(wrapper.text()).toContain('Inbox')
    expect(wrapper.text()).toContain('Me')
    expect(wrapper.find('.bottom-nav').text()).not.toContain('Slots')
    expect(wrapper.find('.bottom-nav').text()).not.toContain('Activity')
    expect(wrapper.find('.bottom-nav').text()).not.toContain('Wallet')
    expect(wrapper.find('.bottom-nav').text()).not.toContain('Redeem')
    expect(wrapper.find('a[href="/lobby"]').exists()).toBe(true)
    expect(wrapper.find('.bottom-nav').text()).not.toContain('Register')
  })

  it('renders Me as account wallet compliance aggregation for guests', async () => {
    localStorage.setItem('tangluck_user_id', '77')
    localStorage.setItem('tangluck_account_type', 'guest')

    const wrapper = mount(AppMe, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Guest mode')
    expect(wrapper.text()).toContain('Bind account')
    expect(wrapper.text()).toContain('Wallet')
    expect(wrapper.text()).toContain('Redeem')
    expect(wrapper.text()).toContain('KYC')
    expect(wrapper.text()).toContain('AMOE')
    expect(wrapper.find('a[href="/me/wallet"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/me/redeem"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/me/kyc"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/promo/amoe"]').exists()).toBe(true)
  })

  it('shows retry when guest session boot fails', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({ message: 'guest failed' }, 500))

    const wrapper = mount(AppShell, {
      global: {
        plugins: [createRouter({
          history: createMemoryHistory(),
          routes: [{ path: '/app', component: { template: '<div />' } }],
        })],
        mocks: { $t: i18n.t },
        stubs: {
          RouterLink: { props: ['to'], template: `<a :href="typeof to === 'string' ? to : to.path"><slot /></a>` },
          RouterView: { template: '<section data-test="route-outlet">Lobby content</section>' },
          AuthModal: true,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Session could not start')
    expect(wrapper.text()).toContain('Retry')
    expect(wrapper.text()).not.toContain('User ')
  })

  it('creates a GC purchase order from the store', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/purchase/packages')) {
        return json([{ packageCode: 'gc_499', name: 'GC 5,000 Pack', priceAmount: '4.9900', priceCurrency: 'USD', gcAmount: '5000.0000', sandboxOnly: false }])
      }
      if (url.endsWith('/purchase/orders') && init?.method === 'POST') {
        return json({ orderId: 'ord_1001', userId: 1, packageCode: 'gc_499', priceAmount: '4.9900', priceCurrency: 'USD', status: 'payment_pending', provider: 'manual', currencyGranted: 'GC', amountGranted: '0', createdAt: '2026-06-19T00:00:00Z' })
      }
      return json({})
    })

    const wrapper = mount(AppStore, { global })
    await flushPromises()
    await wrapper.get('[data-test="buy-gc_499"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('payment_pending')
    expect(wrapper.text()).toContain('ord_1001')
    expect(wrapper.text()).toContain('0 GC')
  })

  it('asks guest users to bind before purchasing GC packages', async () => {
    localStorage.setItem('tangluck_user_id', '77')
    localStorage.setItem('tangluck_account_type', 'guest')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/purchase/packages')) {
        return json([{ packageCode: 'gc_499', name: 'GC 5,000 Pack', priceAmount: '4.9900', priceCurrency: 'USD', gcAmount: '5000.0000', sandboxOnly: false }])
      }
      return json({})
    })

    const wrapper = mount(AppStore, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Bind account before purchase')
    expect(wrapper.text()).toContain('Guest play is available')
    expect(wrapper.find('[data-test="buy-gc_499"]').attributes('disabled')).toBeDefined()
  })

  it('spins Lucky Slots and renders settled round history', async () => {
    localStorage.setItem('tangluck_user_id', '1')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/slots/games')) {
        return json([{ gameCode: 'lucky_slots', name: 'Lucky Slots', status: 'active', reelCount: 5, rowCount: 3, minBet: '1.0000', maxBet: '100.0000', currency: 'GC', sortOrder: 10, legalApprovalId: 'LEGAL-SLOTS-001' }])
      }
      if (url.endsWith('/wallet/summary')) {
        return json({ wallet: { gcBalance: '100.0000', scBalance: '0', scFrozen: '0', scRedeemable: '0' }, scSourceSummary: [], notices: [] })
      }
      if (url.endsWith('/slots/rounds')) {
        return json({ items: [{ roundId: 'round_1001', userId: 1, gameCode: 'lucky_slots', currency: 'GC', betAmount: '10.0000', payoutAmount: '20.0000', multiplier: '2.0000', reels: [['coin', 'seven', 'coin'], ['coin', 'seven', 'coin'], ['coin', 'seven', 'coin'], ['coin', 'seven', 'coin'], ['coin', 'seven', 'coin']], status: 'settled', debitLedgerId: 10, creditLedgerId: 11, createdAt: '2026-06-21T00:00:00Z' }], page: 1, pageSize: 20, total: 1 })
      }
      if (url.endsWith('/player/activity-summary')) {
        return json({ tasks: [{ taskCode: 'daily_spin_10', name: 'Spin 10 times', targetType: 'spin_count', progress: '2.0000', target: '10.0000', status: 'in_progress', rewardCurrency: 'GC', rewardAmount: '1000.0000' }], claimableCount: 0 })
      }
      if (url.endsWith('/slots/lucky_slots/spin') && init?.method === 'POST') {
        return json({ roundId: 'round_1002', userId: 1, gameCode: 'lucky_slots', currency: 'GC', betAmount: '10.0000', payoutAmount: '20.0000', multiplier: '2.0000', reels: [['coin', 'seven', 'coin'], ['coin', 'seven', 'coin'], ['coin', 'seven', 'coin'], ['coin', 'seven', 'coin'], ['coin', 'seven', 'coin']], status: 'settled', debitLedgerId: 12, creditLedgerId: 13, createdAt: '2026-06-21T00:01:00Z' })
      }
      return json({})
    })

    const wrapper = mount(AppSlots, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Lucky Slots')
    expect(wrapper.text()).toContain('round_1001')
    expect(wrapper.text()).toContain('daily_spin_10')
    await wrapper.get('[data-test="spin-slots"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/slots/lucky_slots/spin', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('20 GC')
    expect(wrapper.text()).toContain('round_1002')
  })

  it('claims a completed Slots task from the activity center', async () => {
    localStorage.setItem('tangluck_user_id', '1')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/player/activity-summary')) {
        return json({ tasks: [{ taskCode: 'daily_spin_10', name: 'Spin 10 times', targetType: 'spin_count', progress: '10.0000', target: '10.0000', status: 'claimable', rewardCurrency: 'GC', rewardAmount: '1000.0000' }], claimableCount: 1 })
      }
      if (url.endsWith('/player/tasks/daily_spin_10/claim') && init?.method === 'POST') {
        return json({ taskCode: 'daily_spin_10', status: 'completed', rewardCurrency: 'GC', rewardAmount: '1000.0000', ledgerId: 88 })
      }
      if (url.endsWith('/campaigns')) return json([])
      if (url.endsWith('/tasks/daily')) return json([])
      return json({})
    })

    const wrapper = mount(AppActivity, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Spin 10 times')
    await wrapper.get('[data-test="claim-activity-task-daily_spin_10"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/player/tasks/daily_spin_10/claim', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('1,000 GC')
  })

  it('claims a reward from the inbox', async () => {
    localStorage.setItem('tangluck_user_id', '1')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/player/notifications') && init?.method !== 'POST') {
        return json([{ id: 9, userId: 1, title: 'Retention bonus', message: 'Claim your GC reward.', rewardCurrency: 'GC', rewardAmount: '750.0000', status: 'claimable', sourceType: 'manual_grant', sourceId: 'crm-ticket-1001', ledgerId: null, createdAt: '2026-06-21T00:00:00Z', expiresAt: null, claimedAt: null }])
      }
      if (url.endsWith('/player/notifications/9/claim') && init?.method === 'POST') {
        return json({ id: 9, userId: 1, title: 'Retention bonus', message: 'Claim your GC reward.', rewardCurrency: 'GC', rewardAmount: '750.0000', status: 'claimed', sourceType: 'manual_grant', sourceId: 'crm-ticket-1001', ledgerId: 99, createdAt: '2026-06-21T00:00:00Z', expiresAt: null, claimedAt: '2026-06-21T00:01:00Z' })
      }
      return json({})
    })

    const wrapper = mount(AppInbox, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Retention bonus')
    await wrapper.get('[data-test="claim-inbox-9"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/player/notifications/9/claim', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('claimed')
    expect(wrapper.text()).toContain('ledger 99')
  })

  it('submits KYC for manual review', async () => {
    localStorage.setItem('tangluck_user_id', '1')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/kyc/status')) return json({ userId: 1, status: 'not_started', legalName: null, reviewReason: null, updatedAt: null })
      if (url.endsWith('/kyc/applications') && init?.method === 'POST') {
        return json({ userId: 1, status: 'reviewing', legalName: 'P1 User', reviewReason: null, updatedAt: '2026-06-19T00:00:00Z' })
      }
      return json({})
    })

    const wrapper = mount(AppKyc, { global })
    await flushPromises()
    await wrapper.get('[data-test="submit-kyc"]').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('reviewing')
  })

  it('asks guest users to bind before KYC submission', async () => {
    localStorage.setItem('tangluck_user_id', '77')
    localStorage.setItem('tangluck_account_type', 'guest')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({}))

    const wrapper = mount(AppKyc, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Bind account before identity review')
    expect(wrapper.text()).toContain('Identity verification is tied to a formal account')
    expect(wrapper.find('[data-test="submit-kyc"]').exists()).toBe(false)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('shows KYC blocking state on redemption page', async () => {
    localStorage.setItem('tangluck_user_id', '1')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/wallet/summary')) {
        return json({ wallet: { gcBalance: '0', scBalance: '0.5000', scFrozen: '0', scRedeemable: '0.5000' }, scSourceSummary: [], notices: [] })
      }
      if (url.endsWith('/kyc/status')) return json({ userId: 1, status: 'reviewing', legalName: 'P1 User', reviewReason: null, updatedAt: '2026-06-19T00:00:00Z' })
      return json({})
    })

    const wrapper = mount(AppRedemption, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('KYC approval is required')
    expect(wrapper.get('[data-test="submit-redemption"]').attributes('disabled')).toBeDefined()
  })

  it('asks guest users to bind before redemption requests', async () => {
    localStorage.setItem('tangluck_user_id', '77')
    localStorage.setItem('tangluck_account_type', 'guest')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({}))

    const wrapper = mount(AppRedemption, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Bind account before redemption')
    expect(wrapper.text()).toContain('Redemption requests require a formal account')
    expect(wrapper.find('[data-test="submit-redemption"]').exists()).toBe(false)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('does not call user-scoped APIs when the user is not registered', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({}))

    const kyc = mount(AppKyc, { global })
    await flushPromises()
    expect(kyc.text()).toContain('Create your account')

    const redemption = mount(AppRedemption, { global })
    await flushPromises()
    expect(redemption.text()).toContain('Create your account')

    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('lets admin approve KYC from the P1 operations table', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/p1/operations')) {
        return json({
          purchaseOrders: [],
          kycApplications: [{ userId: 1, status: 'reviewing', legalName: 'P1 User', reviewReason: null, updatedAt: '2026-06-19T00:00:00Z' }],
          redemptionRequests: [],
        })
      }
      if (url.endsWith('/admin/kyc/1/approve') && init?.method === 'POST') {
        return json({ userId: 1, status: 'approved', legalName: 'P1 User', reviewReason: 'approved by ops', updatedAt: '2026-06-19T00:00:01Z' })
      }
      return json({})
    })

    const wrapper = mount(AdminP1Operations, { global })
    await flushPromises()
    await wrapper.get('button:not([disabled])').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('P1 User')
  })

  it('lets admins pause product packages', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/product-packages') && init?.method !== 'PATCH') {
        return json([{ packageCode: 'gc_999', name: 'GC 12,000 Pack', priceAmount: '9.9900', priceCurrency: 'USD', gcAmount: '12000.0000', sandboxOnly: false, status: 'active', provider: 'manual', sortOrder: 20, legalApprovalId: 'LEGAL-PACKAGE-GC' }])
      }
      if (url.endsWith('/admin/product-packages/gc_999') && init?.method === 'PATCH') {
        return json({ packageCode: 'gc_999', name: 'GC 12,000 Pack', priceAmount: '9.9900', priceCurrency: 'USD', gcAmount: '12000.0000', sandboxOnly: false, status: 'paused', provider: 'manual', sortOrder: 20, legalApprovalId: 'LEGAL-PACKAGE-GC' })
      }
      return json({})
    })

    const wrapper = mount(AdminPackages, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('GC 12,000 Pack')
    await wrapper.get('[data-test="pause-package-gc_999"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/product-packages/gc_999', expect.objectContaining({ method: 'PATCH' }))
    expect(wrapper.text()).toContain('paused')
  })

  it('lets admins mark pending purchase orders as paid', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/p1/operations')) {
        return json({
          purchaseOrders: [{ orderId: 'ord_1001', userId: 1, packageCode: 'gc_499', priceAmount: '4.9900', priceCurrency: 'USD', status: 'payment_pending', provider: 'manual', currencyGranted: 'GC', amountGranted: '0', createdAt: '2026-06-19T00:00:00Z' }],
          kycApplications: [],
          redemptionRequests: [],
        })
      }
      if (url.endsWith('/admin/purchase-orders/ord_1001/mark-paid') && init?.method === 'POST') {
        return json({ orderId: 'ord_1001', userId: 1, packageCode: 'gc_499', priceAmount: '4.9900', priceCurrency: 'USD', status: 'paid', provider: 'manual', currencyGranted: 'GC', amountGranted: '5000.0000', createdAt: '2026-06-19T00:00:00Z' })
      }
      return json({})
    })

    const wrapper = mount(AdminOrders, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('payment_pending')
    await wrapper.get('[data-test="mark-paid-ord_1001"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/purchase-orders/ord_1001/mark-paid', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('paid')
    expect(wrapper.text()).toContain('5,000')
  })

  it('renders real admin users and wallet ledger modules', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/admin/users')) {
        return json([{ userId: 7, email: 'ops-user@example.com', countryCode: 'US', stateCode: 'CA', status: 'active', riskLevel: 'low' }])
      }
      if (url.endsWith('/admin/wallet-ledger')) {
        return json([{ ledgerId: 9, userId: 7, currency: 'SC', direction: 'credit', amount: '0.0500', balanceAfter: '0.0500', frozenAfter: '0', businessType: 'daily_login', businessId: '1', status: 'posted', createdAt: '2026-06-20T00:00:00Z' }])
      }
      return json({})
    })

    const users = mount(AdminUsers, { global })
    await flushPromises()
    expect(users.text()).toContain('ops-user@example.com')
    expect(users.text()).toContain('Risk')
    expect(users.text()).not.toContain('scheduled')

    const ledger = mount(AdminWalletLedger, { global })
    await flushPromises()
    expect(ledger.text()).toContain('daily_login')
    expect(ledger.text()).toContain('0.05')
    expect(ledger.text()).not.toContain('scheduled')
  })

  it('renders real admin KYC and redemption review queues', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/admin/kyc-applications')) {
        return json([{ userId: 8, status: 'reviewing', legalName: 'Review User', reviewReason: null, updatedAt: '2026-06-20T00:00:00Z' }])
      }
      if (url.endsWith('/admin/redemptions')) {
        return json([{ redemptionId: 'red_1001', userId: 8, scAmount: '0.0100', method: 'paypal', status: 'reviewing', sandboxOnly: true, createdAt: '2026-06-20T00:00:00Z' }])
      }
      return json({})
    })

    const kyc = mount(AdminKycReview, { global })
    await flushPromises()
    expect(kyc.text()).toContain('Review User')
    expect(kyc.text()).toContain('reviewing')
    expect(kyc.text()).not.toContain('scheduled')

    const redemptions = mount(AdminRedemptions, { global })
    await flushPromises()
    expect(redemptions.text()).toContain('red_1001')
    expect(redemptions.text()).toContain('paypal')
    expect(redemptions.text()).not.toContain('scheduled')
  })

  it('lets admins reject KYC applications with a reason', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/kyc-applications')) {
        return json([{ userId: 8, status: 'reviewing', legalName: 'Review User', reviewReason: null, updatedAt: '2026-06-20T00:00:00Z' }])
      }
      if (url.endsWith('/admin/kyc/8/reject') && init?.method === 'POST') {
        return json({ userId: 8, status: 'rejected', legalName: 'Review User', reviewReason: 'Document image is unreadable.', updatedAt: '2026-06-20T00:00:01Z' })
      }
      return json({})
    })

    const wrapper = mount(AdminKycReview, { global })
    await flushPromises()
    await wrapper.get('[data-test="reject-kyc-8"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/kyc/8/reject', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('rejected')
    expect(wrapper.text()).toContain('Document image is unreadable.')
  })

  it('lets admins approve, reject, and mark redemption payouts paid', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/redemptions')) {
        return json([
          { redemptionId: 'red_1001', userId: 8, scAmount: '0.0100', method: 'paypal', status: 'reviewing', sandboxOnly: true, createdAt: '2026-06-20T00:00:00Z', reviewReason: null, providerReference: null },
          { redemptionId: 'red_1002', userId: 9, scAmount: '0.0200', method: 'paypal', status: 'payout_pending', sandboxOnly: true, createdAt: '2026-06-20T00:00:00Z', reviewReason: 'Approved for payout.', providerReference: null },
          { redemptionId: 'red_1003', userId: 10, scAmount: '0.0300', method: 'paypal', status: 'reviewing', sandboxOnly: true, createdAt: '2026-06-20T00:00:00Z', reviewReason: null, providerReference: null },
        ])
      }
      if (url.endsWith('/admin/redemptions/red_1001/approve') && init?.method === 'POST') {
        return json({ redemptionId: 'red_1001', userId: 8, scAmount: '0.0100', method: 'paypal', status: 'payout_pending', sandboxOnly: true, createdAt: '2026-06-20T00:00:00Z', reviewReason: 'Approved for payout.', providerReference: null })
      }
      if (url.endsWith('/admin/redemptions/red_1003/reject') && init?.method === 'POST') {
        return json({ redemptionId: 'red_1003', userId: 10, scAmount: '0.0300', method: 'paypal', status: 'rejected', sandboxOnly: true, createdAt: '2026-06-20T00:00:00Z', reviewReason: 'Payment account mismatch.', providerReference: null })
      }
      if (url.endsWith('/admin/redemptions/red_1002/mark-paid') && init?.method === 'POST') {
        return json({ redemptionId: 'red_1002', userId: 9, scAmount: '0.0200', method: 'paypal', status: 'paid', sandboxOnly: true, createdAt: '2026-06-20T00:00:00Z', reviewReason: 'Approved for payout.', providerReference: 'manual-red_1002' })
      }
      return json({})
    })

    const wrapper = mount(AdminRedemptions, { global })
    await flushPromises()

    await wrapper.get('[data-test="approve-redemption-red_1001"]').trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/redemptions/red_1001/approve', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('payout_pending')

    await wrapper.get('[data-test="reject-redemption-red_1003"]').trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/redemptions/red_1003/reject', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('Payment account mismatch.')

    await wrapper.get('[data-test="mark-paid-redemption-red_1002"]').trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/redemptions/red_1002/mark-paid', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('manual-red_1002')
  })
})
