<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { AdminCampaign, AdminCampaignResponse } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

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
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('admin.campaignListFailed')
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
    notice.value = i18n.t('admin.legalDocumentDraftCreated', { version: response.campaignCode })
    await loadCampaigns()
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('admin.campaignSetupFailed')
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
    notice.value = i18n.t('admin.campaignPublished', { code: row.campaignCode })
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('admin.publishFailed')
  }
}

async function pause(row: CampaignRow) {
  error.value = ''
  notice.value = ''
  try {
    const response = await apiPost<AdminCampaignResponse>(`/admin/campaigns/${row.campaignCode}/pause`)
    row.status = response.status
    notice.value = i18n.t('admin.campaignPaused', { code: row.campaignCode })
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('admin.pauseFailed')
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
          <p class="eyebrow">{{ $t('admin.contentOperations') }}</p>
          <h1>{{ $t('admin.campaigns') }}</h1>
        </div>
        <button data-test="create-campaign" :disabled="loading" @click="createDraft">{{ $t('admin.createDraft') }}</button>
      </header>

      <div class="filter-bar">
        <label>
          {{ $t('admin.campaignStatusFilter') }}
          <select v-model="statusFilter">
            <option value="all">{{ $t('admin.filterAll') }}</option>
            <option value="draft">draft</option>
            <option value="active">active</option>
            <option value="paused">paused</option>
          </select>
        </label>
        <span>{{ $t('admin.campaignPublishHint') }}</span>
      </div>

      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('common.campaign') }}</th>
              <th>{{ $t('common.status') }}</th>
              <th>SC strategy</th>
              <th>Budget</th>
              <th>{{ $t('admin.legalApproval') }} ID</th>
              <th>{{ $t('admin.regions') }}</th>
              <th>{{ $t('common.actions') }}</th>
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
              <td>{{ row.legalApprovalId || $t('common.missing') }}</td>
              <td>{{ row.eligibleRegions.join(', ') }}</td>
              <td>
                <div class="action-group">
                  <button data-test="publish-campaign" :disabled="loading || row.status === 'active'" @click="publish(row)">{{ $t('admin.publish') }}</button>
                  <button :disabled="loading || row.status !== 'active'" @click="pause(row)">{{ $t('common.pause') }}</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
  </AdminLayout>
</template>
