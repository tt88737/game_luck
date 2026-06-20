import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AdminCampaigns from './AdminCampaigns.vue'
import AdminDashboard from './AdminDashboard.vue'
import AdminAuditLogs from './AdminAuditLogs.vue'
import AdminRegions from './AdminRegions.vue'
import AdminLegalDocuments from './AdminLegalDocuments.vue'
import AdminLobby from './AdminLobby.vue'
import { i18n } from '../../i18n'

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

describe('admin pages', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
    i18n.setLocale('en')
  })

  it('renders campaign table with filters, status tags, SC strategy, budget, and legal approval', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      if (String(input).endsWith('/admin/campaigns')) {
        return json([{
          campaignCode: 'register_bonus_v1',
          name: 'Welcome Bonus',
          campaignType: 'register',
          status: 'active',
          scStrategy: 'default_small_sc',
          rulesVersion: 'rules-v1',
          legalApprovalId: 'LEGAL-2026-0617-CA',
          riskAction: 'gc_only',
          eligibleRegionsJson: '["CA","TX"]',
          blockedRegionsJson: '["WA"]',
          rewardPolicyJson: '[{"currency":"GC","amount":10000},{"currency":"SC","amount":"0.50"}]',
        }])
      }
      return json({})
    })

    const wrapper = mount(AdminCampaigns, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Status')
    expect(wrapper.text()).toContain('register_bonus_v1')
    expect(wrapper.text()).not.toContain('OPS_SC_BONUS')
    expect(wrapper.text()).toContain('default_small_sc')
    expect(wrapper.text()).toContain('GC')
    expect(wrapper.text()).toContain('LEGAL-2026-0617-CA')
  })

  it('uses the formal admin navigation across operations modules', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/admin/dashboard/summary')) {
        return json({ registrations: 12, claims: 8, scGranted: '3.5000', riskEvents: 1 })
      }
      return json({})
    })

    const wrapper = mount(AdminDashboard, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Regions')
    expect(wrapper.text()).toContain('Legal Docs')
    expect(wrapper.text()).toContain('Lobby')
    expect(wrapper.text()).toContain('Packages')
    expect(wrapper.text()).toContain('Orders')
    expect(wrapper.text()).toContain('KYC applications')
    expect(wrapper.text()).toContain('Redemption requests')
    expect(wrapper.text()).toContain('Wallet Ledger')
    expect(wrapper.text()).toContain('AMOE')
    expect(wrapper.text()).toContain('Support')
  })

  it('localizes admin shell and dashboard copy when Chinese is selected', async () => {
    i18n.setLocale('zh-CN')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      if (String(input).endsWith('/admin/dashboard/summary')) {
        return json({ registrations: 12, claims: 8, scGranted: '3.5000', riskEvents: 1 })
      }
      return json({})
    })

    const wrapper = mount(AdminDashboard, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('运营后台')
    expect(wrapper.text()).toContain('地区配置')
    expect(wrapper.text()).toContain('法务文档')
    expect(wrapper.text()).toContain('看板')
    expect(wrapper.text()).toContain('风险事件')
    expect(wrapper.text()).not.toContain('Dashboard')
    expect(wrapper.text()).not.toContain('Legal Docs')
  })

  it('creates campaign draft only when the operator clicks create draft', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({ campaignCode: 'OPS_SC_BONUS', status: 'draft' }))

    const wrapper = mount(AdminCampaigns, { global })
    await flushPromises()
    await wrapper.get('[data-test="create-campaign"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/campaigns', expect.objectContaining({ method: 'POST' }))
  })

  it('shows publish blocking reason from backend', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/admin/campaigns')) {
        return json([{
          campaignCode: 'OPS_SC_BONUS',
          name: 'Ops SC Bonus',
          campaignType: 'register_bonus',
          status: 'draft',
          scStrategy: 'default_small_sc',
          rulesVersion: 'rules-v1',
          legalApprovalId: '',
          riskAction: 'gc_only',
          eligibleRegionsJson: '["CA"]',
          blockedRegionsJson: '[]',
          rewardPolicyJson: '[{"currency":"SC","amount":"0.50"}]',
        }])
      }
      if (url.endsWith('/admin/campaigns/OPS_SC_BONUS/publish')) {
        return json({
          code: 'LEGAL_APPROVAL_REQUIRED',
          message: 'SC campaign requires legal approval.',
          trace_id: 'trace-admin',
          details: {},
        }, 400)
      }
      return json({})
    })

    const wrapper = mount(AdminCampaigns, { global })
    await flushPromises()
    await wrapper.get('[data-test="publish-campaign"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('SC campaign requires legal approval.')
  })

  it('renders dashboard metrics and audit log table', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/admin/dashboard/summary')) {
        return json({ registrations: 12, claims: 8, scGranted: '3.5000', riskEvents: 1 })
      }
      if (url.includes('/admin/audit-logs')) {
        return json([{
          id: 1,
          operatorId: 1,
          operatorRole: 'ops_admin',
          action: 'campaign_publish',
          targetType: 'promotion_campaign',
          targetId: 'OPS_SC_BONUS',
          beforeJson: '{"status":"draft"}',
          afterJson: '{"status":"active"}',
          ip: '127.0.0.1',
          createdAt: '2026-06-18T00:00:00Z',
        }])
      }
      return json({})
    })

    const dashboard = mount(AdminDashboard, { global })
    await flushPromises()
    expect(dashboard.text()).toContain('12')
    expect(dashboard.text()).toContain('3.50')
    expect(dashboard.text()).toContain('Risk events')

    const audit = mount(AdminAuditLogs, { global })
    await flushPromises()
    expect(audit.text()).toContain('ops_admin')
    expect(audit.text()).toContain('campaign_publish')
    expect(audit.text()).toContain('OPS_SC_BONUS')
    expect(audit.text()).toContain('127.0.0.1')
  })

  it('lets compliance admins update region feature switches', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/regions') && init?.method !== 'PATCH') {
        return json([{ countryCode: 'US', stateCode: 'CA', registrationAllowed: true, gameAllowed: true, purchaseAllowed: true, scGrantAllowed: true, redemptionAllowed: true, amoeAllowed: true, requiresLegalReview: false, status: 'active', legalApprovalId: 'LEGAL-CA' }])
      }
      if (url.endsWith('/admin/regions/US/CA') && init?.method === 'PATCH') {
        return json({ countryCode: 'US', stateCode: 'CA', registrationAllowed: true, gameAllowed: true, purchaseAllowed: false, scGrantAllowed: true, redemptionAllowed: true, amoeAllowed: true, requiresLegalReview: false, status: 'active', legalApprovalId: 'LEGAL-CA-OFF' })
      }
      return json({})
    })

    const wrapper = mount(AdminRegions, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('US-CA')
    expect(wrapper.text()).toContain('Purchase')
    await wrapper.get('[data-test="toggle-purchase-US-CA"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/regions/US/CA', expect.objectContaining({ method: 'PATCH' }))
    expect(wrapper.text()).toContain('LEGAL-CA-OFF')
  })

  it('lets legal admins create and publish document versions', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/legal-documents') && init?.method !== 'POST') {
        return json([{ documentType: 'privacy', version: 'privacy-v1', title: 'Privacy Policy', contentUrl: '/legal/privacy-v1', status: 'active', legalApprovalId: 'LEGAL-V1' }])
      }
      if (url.endsWith('/admin/legal-documents') && init?.method === 'POST') {
        return json({ documentType: 'privacy', version: 'privacy-v2', title: 'Privacy Policy v2', contentUrl: '/legal/privacy-v2', status: 'draft', legalApprovalId: 'LEGAL-V2' })
      }
      if (url.endsWith('/admin/legal-documents/privacy/privacy-v2/publish') && init?.method === 'POST') {
        return json({ documentType: 'privacy', version: 'privacy-v2', title: 'Privacy Policy v2', contentUrl: '/legal/privacy-v2', status: 'active', legalApprovalId: 'LEGAL-V2' })
      }
      return json({})
    })

    const wrapper = mount(AdminLegalDocuments, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Privacy Policy')
    await wrapper.get('[data-test="create-legal-document"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-test="publish-privacy-v2"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/legal-documents', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/legal-documents/privacy/privacy-v2/publish', expect.objectContaining({ method: 'POST' }))
    expect(wrapper.text()).toContain('privacy-v2')
  })

  it('lets content admins pause lobby cards', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)
      if (url.endsWith('/admin/lobby-cards') && init?.method !== 'PATCH') {
        return json([{ cardCode: 'slots_main', title: 'Lucky Slots', subtitle: 'GC play', imageUrl: '/assets/lobby/slots.png', targetUrl: '/app/activity', status: 'active', sortOrder: 10 }])
      }
      if (url.endsWith('/admin/lobby-cards/slots_main') && init?.method === 'PATCH') {
        return json({ cardCode: 'slots_main', title: 'Lucky Slots', subtitle: 'GC play', imageUrl: '/assets/lobby/slots.png', targetUrl: '/app/activity', status: 'paused', sortOrder: 10 })
      }
      return json({})
    })

    const wrapper = mount(AdminLobby, { global })
    await flushPromises()

    expect(wrapper.text()).toContain('Lucky Slots')
    await wrapper.get('[data-test="pause-card-slots_main"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/lobby-cards/slots_main', expect.objectContaining({ method: 'PATCH' }))
    expect(wrapper.text()).toContain('paused')
  })
})
