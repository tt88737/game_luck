<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { AdminUser } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const rows = ref<AdminUser[]>([])
const loading = ref(true)
const error = ref('')

onMounted(loadUsers)

async function loadUsers() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<AdminUser[]>('/admin/users')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'User request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Account operations</p>
        <h1>Users</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">Loading users...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>User</th>
              <th>Region</th>
              <th>Status</th>
              <th>Risk</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.userId">
              <td data-label="User"><strong>{{ row.email }}</strong><span>#{{ row.userId }}</span></td>
              <td data-label="Region">{{ row.countryCode }}-{{ row.stateCode }}</td>
              <td data-label="Status"><span class="status-tag active">{{ row.status }}</span></td>
              <td data-label="Risk"><span class="status-tag" :class="{ pending: row.riskLevel !== 'low' }">{{ row.riskLevel }}</span></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="4">No users.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
