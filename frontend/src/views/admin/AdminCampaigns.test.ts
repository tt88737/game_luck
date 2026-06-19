import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AdminCampaigns from './AdminCampaigns.vue'
import AdminDashboard from './AdminDashboard.vue'
import AdminAuditLogs from './AdminAuditLogs.vue'

function json(data: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }))
}

const stubs = {
  RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
}

describe('admin pages', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('renders campaign table with filters, status tags, SC strategy, budget, and legal approval', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({ campaignCode: 'OPS_SC_BONUS', status: 'draft' }))

    const wrapper = mount(AdminCampaigns, { global: { stubs } })
    await flushPromises()

    expect(fetchMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Status')
    expect(wrapper.text()).toContain('OPS_SC_BONUS')
    expect(wrapper.text()).toContain('default_small_sc')
    expect(wrapper.text()).toContain('10,000 GC + 0.50 SC')
    expect(wrapper.text()).toContain('LEGAL-2026-0618-SC')
  })

  it('creates campaign draft only when the operator clicks create draft', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({ campaignCode: 'OPS_SC_BONUS', status: 'draft' }))

    const wrapper = mount(AdminCampaigns, { global: { stubs } })
    await flushPromises()
    await wrapper.get('[data-test="create-campaign"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/campaigns', expect.objectContaining({ method: 'POST' }))
  })

  it('shows publish blocking reason from backend', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/admin/campaigns')) return json({ campaignCode: 'OPS_SC_BONUS', status: 'draft' })
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

    const wrapper = mount(AdminCampaigns, { global: { stubs } })
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

    const dashboard = mount(AdminDashboard, { global: { stubs } })
    await flushPromises()
    expect(dashboard.text()).toContain('12')
    expect(dashboard.text()).toContain('3.50')
    expect(dashboard.text()).toContain('Risk events')

    const audit = mount(AdminAuditLogs, { global: { stubs } })
    await flushPromises()
    expect(audit.text()).toContain('ops_admin')
    expect(audit.text()).toContain('campaign_publish')
    expect(audit.text()).toContain('OPS_SC_BONUS')
    expect(audit.text()).toContain('127.0.0.1')
  })
})
