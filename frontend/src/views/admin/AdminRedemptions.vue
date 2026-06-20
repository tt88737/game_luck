<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { RedemptionRequest } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

const rows = ref<RedemptionRequest[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')

onMounted(loadRedemptions)

async function loadRedemptions() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<RedemptionRequest[]>('/admin/redemptions')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function reviewRedemption(row: RedemptionRequest, action: 'approve' | 'reject' | 'mark-paid') {
  error.value = ''
  notice.value = ''
  try {
    const payload = action === 'approve'
      ? { reason: i18n.t('admin.redemptionApprovedDefaultReason') }
      : action === 'reject'
        ? { reason: i18n.t('admin.redemptionRejectedDefaultReason') }
        : { providerReference: `manual-${row.redemptionId}` }
    const updated = await apiPost<RedemptionRequest>(`/admin/redemptions/${row.redemptionId}/${action}`, payload)
    rows.value = rows.value.map((item) => item.redemptionId === updated.redemptionId ? updated : item)
    notice.value = `${updated.redemptionId} ${updated.status}.`
  } catch (err) {
    error.value = messageFrom(err)
  }
}

function money(value: string | number, digits = 2) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('admin.redemptionRequestFailed')
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">{{ $t('admin.prizeOperations') }}</p>
        <h1>{{ $t('admin.redemptionRequests') }}</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('admin.loadingRedemptions') }}</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('common.request') }}</th>
              <th>{{ $t('admin.user') }}</th>
              <th>SC</th>
              <th>{{ $t('common.method') }}</th>
              <th>{{ $t('common.status') }}</th>
              <th>{{ $t('admin.scope') }}</th>
              <th>{{ $t('common.reason') }}</th>
              <th>{{ $t('admin.providerRef') }}</th>
              <th>{{ $t('admin.created') }}</th>
              <th>{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.redemptionId">
              <td data-label="Redemption"><strong>{{ row.redemptionId }}</strong></td>
              <td data-label="User">{{ row.userId }}</td>
              <td data-label="SC">{{ money(row.scAmount) }}</td>
              <td data-label="Method">{{ row.method }}</td>
              <td data-label="Status"><span class="status-tag pending">{{ row.status }}</span></td>
              <td data-label="Scope">{{ row.sandboxOnly ? 'sandbox' : 'production' }}</td>
              <td data-label="Reason">{{ row.reviewReason || $t('admin.reviewReasonDefault') }}</td>
              <td data-label="Provider Ref">{{ row.providerReference || '-' }}</td>
              <td data-label="Created">{{ new Date(row.createdAt).toLocaleString() }}</td>
              <td data-label="Actions">
                <div class="action-group">
                  <button :data-test="`approve-redemption-${row.redemptionId}`" :disabled="row.status !== 'reviewing'" @click="reviewRedemption(row, 'approve')">{{ $t('common.approve') }}</button>
                  <button :data-test="`reject-redemption-${row.redemptionId}`" :disabled="row.status !== 'reviewing'" @click="reviewRedemption(row, 'reject')">{{ $t('common.reject') }}</button>
                  <button :data-test="`mark-paid-redemption-${row.redemptionId}`" :disabled="row.status !== 'payout_pending'" @click="reviewRedemption(row, 'mark-paid')">{{ $t('admin.markPaid') }}</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="10">{{ $t('admin.noRedemptionRequests') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
