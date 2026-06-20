<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { RedemptionRequest } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const rows = ref<RedemptionRequest[]>([])
const loading = ref(true)
const error = ref('')

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

function money(value: string | number, digits = 2) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Redemption request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Prize operations</p>
        <h1>Redemptions</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">Loading redemptions...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Redemption</th>
              <th>User</th>
              <th>SC</th>
              <th>Method</th>
              <th>Status</th>
              <th>Scope</th>
              <th>Created</th>
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
              <td data-label="Created">{{ new Date(row.createdAt).toLocaleString() }}</td>
            </tr>
            <tr v-if="!rows.length"><td colspan="7">No redemptions.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
