<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { KycStatus } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

const rows = ref<KycStatus[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')

onMounted(loadKyc)

async function loadKyc() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<KycStatus[]>('/admin/kyc-applications')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function reviewKyc(row: KycStatus, action: 'approve' | 'reject') {
  error.value = ''
  notice.value = ''
  try {
    const payload = action === 'reject' ? { reason: i18n.t('admin.kycRejectedDefaultReason') } : {}
    const updated = await apiPost<KycStatus>(`/admin/kyc/${row.userId}/${action}`, payload)
    rows.value = rows.value.map((item) => item.userId === updated.userId ? updated : item)
    notice.value = i18n.t('admin.kycStatusUpdated', { userId: row.userId, status: updated.status })
  } catch (err) {
    error.value = messageFrom(err)
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('admin.kycReviewRequestFailed')
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">{{ $t('common.identityReview') }}</p>
        <h1>{{ $t('admin.kycApplications') }}</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('admin.loadingKyc') }}</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('admin.user') }}</th>
              <th>{{ $t('common.legalName') }}</th>
              <th>{{ $t('common.status') }}</th>
              <th>{{ $t('common.reason') }}</th>
              <th>{{ $t('common.updated') }}</th>
              <th>{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.userId">
              <td :data-label="$t('admin.user')"><strong>#{{ row.userId }}</strong></td>
              <td :data-label="$t('common.legalName')">{{ row.legalName || $t('common.missing') }}</td>
              <td :data-label="$t('common.status')"><span class="status-tag" :class="{ active: row.status === 'approved', pending: row.status !== 'approved' }">{{ row.status }}</span></td>
              <td :data-label="$t('common.reason')">{{ row.reviewReason || $t('admin.reviewReasonDefault') }}</td>
              <td :data-label="$t('common.updated')">{{ row.updatedAt ? new Date(row.updatedAt).toLocaleString() : '-' }}</td>
              <td :data-label="$t('common.actions')">
                <div class="action-group">
                  <button :data-test="`approve-kyc-${row.userId}`" :disabled="row.status === 'approved'" @click="reviewKyc(row, 'approve')">{{ $t('common.approve') }}</button>
                  <button :data-test="`reject-kyc-${row.userId}`" :disabled="row.status === 'rejected'" @click="reviewKyc(row, 'reject')">{{ $t('common.reject') }}</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="6">{{ $t('admin.noKycApplications') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
