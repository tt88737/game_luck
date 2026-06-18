<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiPost } from '../../api/http'
import type { AdminCampaignRequest, AdminCampaignResponse } from '../../api/contracts'

interface CampaignRow extends AdminCampaignRequest {
  status: string
  budgetLabel: string
}

const loading = ref(true)
const error = ref('')
const notice = ref('')
const statusFilter = ref('all')
const rows = ref<CampaignRow[]>([
  {
    campaignCode: 'OPS_SC_BONUS',
    name: 'Ops SC Bonus',
    campaignType: 'register_bonus',
    eligibleRegions: ['US-CA', 'US-TX'],
    blockedRegions: ['US-WA'],
    rewardPolicy: [{ currency: 'GC', amount: '10000' }, { currency: 'SC', amount: '0.50' }],
    scStrategy: 'default_small_sc',
    rulesVersion: 'rules-v1',
    legalApprovalId: 'LEGAL-2026-0618-SC',
    riskAction: 'gc_only',
    status: 'draft',
    budgetLabel: '10,000 GC + 0.50 SC',
  },
])

onMounted(async () => {
  try {
    const response = await apiPost<AdminCampaignResponse>('/admin/campaigns', toRequest(rows.value[0]))
    rows.value[0].status = response.status
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Campaign setup failed.'
  } finally {
    loading.value = false
  }
})

async function publish(row: CampaignRow) {
  error.value = ''
  notice.value = ''
  try {
    const response = await apiPost<AdminCampaignResponse>(`/admin/campaigns/${row.campaignCode}/publish`)
    row.status = response.status
    notice.value = `${row.campaignCode} published. Audit log created by backend.`
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Publish failed.'
  }
}

async function pause(row: CampaignRow) {
  error.value = ''
  notice.value = ''
  try {
    const response = await apiPost<AdminCampaignResponse>(`/admin/campaigns/${row.campaignCode}/pause`)
    row.status = response.status
    notice.value = `${row.campaignCode} paused.`
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Pause failed.'
  }
}

function toRequest(row: CampaignRow): AdminCampaignRequest {
  return {
    campaignCode: row.campaignCode,
    name: row.name,
    campaignType: row.campaignType,
    eligibleRegions: row.eligibleRegions,
    blockedRegions: row.blockedRegions,
    rewardPolicy: row.rewardPolicy,
    scStrategy: row.scStrategy,
    rulesVersion: row.rulesVersion,
    legalApprovalId: row.legalApprovalId,
    riskAction: row.riskAction,
  }
}
</script>

<template>
  <main class="admin-shell">
    <aside class="admin-nav">
      <strong>Tang Luck Ops</strong>
      <RouterLink to="/admin">Dashboard</RouterLink>
      <RouterLink to="/admin/campaigns">Campaigns</RouterLink>
      <RouterLink to="/admin/audit-logs">Audit logs</RouterLink>
    </aside>

    <section class="admin-content">
      <header class="admin-header">
        <div>
          <p class="eyebrow">Promotion operations</p>
          <h1>Campaigns</h1>
        </div>
        <button :disabled="loading">Create draft</button>
      </header>

      <div class="filter-bar">
        <label>
          Status
          <select v-model="statusFilter">
            <option value="all">All</option>
            <option value="draft">Draft</option>
            <option value="active">Active</option>
            <option value="paused">Paused</option>
          </select>
        </label>
        <span>Publishing SC campaigns requires rules version and legal approval ID.</span>
      </div>

      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Campaign</th>
              <th>Status</th>
              <th>SC strategy</th>
              <th>Budget</th>
              <th>Legal approval ID</th>
              <th>Regions</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows.filter((item) => statusFilter === 'all' || item.status === statusFilter)" :key="row.campaignCode">
              <td>
                <strong>{{ row.campaignCode }}</strong>
                <span>{{ row.name }}</span>
              </td>
              <td><span class="status-tag" :class="row.status">{{ row.status }}</span></td>
              <td>{{ row.scStrategy }}</td>
              <td>{{ row.budgetLabel }}</td>
              <td>{{ row.legalApprovalId || 'Missing' }}</td>
              <td>{{ row.eligibleRegions.join(', ') }}</td>
              <td>
                <div class="action-group">
                  <button data-test="publish-campaign" :disabled="loading || row.status === 'active'" @click="publish(row)">Publish</button>
                  <button :disabled="loading || row.status !== 'active'" @click="pause(row)">Pause</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>
