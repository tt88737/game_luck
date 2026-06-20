<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { AdminCampaign, AdminCampaignResponse } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

interface CampaignRow extends AdminCampaign {
  budgetLabel: string
  eligibleRegions: string[]
  blockedRegions: string[]
  rewardPolicy: Array<{ currency: string; amount: string }>
}

const loading = ref(false)
const error = ref('')
const notice = ref('')
const statusFilter = ref('all')
const rows = ref<CampaignRow[]>([])

onMounted(loadCampaigns)

async function loadCampaigns() {
  loading.value = true
  error.value = ''
  try {
    const items = await apiGet<AdminCampaign[]>('/admin/campaigns')
    rows.value = items.map(toRow)
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Campaign list failed.'
  } finally {
    loading.value = false
  }
}

async function createDraft() {
  loading.value = true
  error.value = ''
  notice.value = ''
  try {
    const response = await apiPost<AdminCampaignResponse>('/admin/campaigns', {
      campaignCode: `ops_campaign_${Date.now()}`,
      name: 'Ops Campaign',
      campaignType: 'daily_login',
      eligibleRegions: ['CA'],
      blockedRegions: ['WA'],
      rewardPolicy: [{ currency: 'GC', amount: '1000' }],
      scStrategy: 'gc_only',
      rulesVersion: 'rules-v1',
      legalApprovalId: '',
      riskAction: 'pass',
    })
    notice.value = `${response.campaignCode} draft created.`
    await loadCampaigns()
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Campaign setup failed.'
  } finally {
    loading.value = false
  }
}

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

function toRow(item: AdminCampaign): CampaignRow {
  const rewards = parseRewards(item.rewardPolicyJson)
  return {
    ...item,
    eligibleRegions: parseStrings(item.eligibleRegionsJson),
    blockedRegions: parseStrings(item.blockedRegionsJson),
    rewardPolicy: rewards,
    budgetLabel: rewards.map((reward) => `${reward.amount} ${reward.currency}`).join(' + '),
  }
}

function parseRewards(value: string) {
  try {
    const parsed = JSON.parse(value) as Array<{ currency: string; amount: string | number }>
    return parsed.map((reward) => ({ currency: reward.currency, amount: String(reward.amount) }))
  } catch {
    return []
  }
}

function parseStrings(value: string | null) {
  if (!value) return []
  try {
    return JSON.parse(value) as string[]
  } catch {
    return []
  }
}
</script>

<template>
  <AdminLayout>
      <header class="admin-header">
        <div>
          <p class="eyebrow">Promotion operations</p>
          <h1>Campaigns</h1>
        </div>
        <button data-test="create-campaign" :disabled="loading" @click="createDraft">Create draft</button>
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
  </AdminLayout>
</template>
