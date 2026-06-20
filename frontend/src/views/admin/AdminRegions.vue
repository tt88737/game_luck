<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPatch } from '../../api/http'
import type { AdminRegion } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const rows = ref<AdminRegion[]>([])
const loading = ref(true)
const saving = ref('')
const error = ref('')
const notice = ref('')

onMounted(loadRegions)

async function loadRegions() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<AdminRegion[]>('/admin/regions')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function togglePurchase(row: AdminRegion) {
  saving.value = key(row)
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPatch<AdminRegion>(`/admin/regions/${row.countryCode}/${row.stateCode}`, {
      ...row,
      purchaseAllowed: !row.purchaseAllowed,
      legalApprovalId: row.purchaseAllowed ? `${row.legalApprovalId ?? 'LEGAL'}-OFF` : row.legalApprovalId,
    })
    rows.value = rows.value.map((item) => key(item) === key(updated) ? updated : item)
    notice.value = `${key(updated)} updated. Audit log created.`
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    saving.value = ''
  }
}

function key(row: AdminRegion) {
  return `${row.countryCode}-${row.stateCode}`
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Region request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Compliance configuration</p>
        <h1>Regions</h1>
      </div>
      <button :disabled="loading" @click="loadRegions">Refresh</button>
    </header>

    <section v-if="loading" class="status-panel">Loading regions...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Region</th>
              <th>Status</th>
              <th>Register</th>
              <th>Purchase</th>
              <th>SC grant</th>
              <th>Redeem</th>
              <th>AMOE</th>
              <th>Legal approval</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="key(row)">
              <td><strong>{{ key(row) }}</strong></td>
              <td><span class="status-tag" :class="{ active: row.status === 'active', paused: row.status !== 'active' }">{{ row.status }}</span></td>
              <td>{{ row.registrationAllowed ? 'Allowed' : 'Blocked' }}</td>
              <td>{{ row.purchaseAllowed ? 'Allowed' : 'Blocked' }}</td>
              <td>{{ row.scGrantAllowed ? 'Allowed' : 'Blocked' }}</td>
              <td>{{ row.redemptionAllowed ? 'Allowed' : 'Blocked' }}</td>
              <td>{{ row.amoeAllowed ? 'Allowed' : 'Blocked' }}</td>
              <td>{{ row.legalApprovalId ?? '-' }}</td>
              <td>
                <button :data-test="`toggle-purchase-${key(row)}`" :disabled="saving === key(row)" @click="togglePurchase(row)">
                  {{ row.purchaseAllowed ? 'Block Purchase' : 'Allow Purchase' }}
                </button>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="9">No region configuration.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
