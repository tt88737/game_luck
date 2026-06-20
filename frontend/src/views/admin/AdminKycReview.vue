<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { KycStatus } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

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
    const payload = action === 'reject' ? { reason: 'Document image is unreadable.' } : {}
    const updated = await apiPost<KycStatus>(`/admin/kyc/${row.userId}/${action}`, payload)
    rows.value = rows.value.map((item) => item.userId === updated.userId ? updated : item)
    notice.value = `KYC ${row.userId} ${updated.status}.`
  } catch (err) {
    error.value = messageFrom(err)
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'KYC review request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Identity review</p>
        <h1>KYC Review</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">Loading KYC applications...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>User</th>
              <th>Legal name</th>
              <th>Status</th>
              <th>Reason</th>
              <th>Updated</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.userId">
              <td data-label="User"><strong>#{{ row.userId }}</strong></td>
              <td data-label="Legal name">{{ row.legalName || 'Missing' }}</td>
              <td data-label="Status"><span class="status-tag" :class="{ active: row.status === 'approved', pending: row.status !== 'approved' }">{{ row.status }}</span></td>
              <td data-label="Reason">{{ row.reviewReason || 'Manual review required' }}</td>
              <td data-label="Updated">{{ row.updatedAt ? new Date(row.updatedAt).toLocaleString() : '-' }}</td>
              <td data-label="Actions">
                <div class="action-group">
                  <button :data-test="`approve-kyc-${row.userId}`" :disabled="row.status === 'approved'" @click="reviewKyc(row, 'approve')">Approve</button>
                  <button :data-test="`reject-kyc-${row.userId}`" :disabled="row.status === 'rejected'" @click="reviewKyc(row, 'reject')">Reject</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="6">No KYC applications.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
