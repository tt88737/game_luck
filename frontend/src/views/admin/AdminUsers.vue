<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { AdminUser } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

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
  return i18n.t('admin.userRequestFailed')
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">{{ $t('admin.accountOperations') }}</p>
        <h1>{{ $t('admin.users') }}</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('admin.loadingUsers') }}</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('admin.user') }}</th>
              <th>{{ $t('common.region') }}</th>
              <th>{{ $t('common.status') }}</th>
              <th>{{ $t('admin.risk') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.userId">
              <td :data-label="$t('admin.user')"><strong>{{ row.email }}</strong><span>#{{ row.userId }}</span></td>
              <td :data-label="$t('common.region')">{{ row.countryCode }}-{{ row.stateCode }}</td>
              <td :data-label="$t('common.status')"><span class="status-tag active">{{ row.status }}</span></td>
              <td :data-label="$t('admin.risk')"><span class="status-tag" :class="{ pending: row.riskLevel !== 'low' }">{{ row.riskLevel }}</span></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="4">{{ $t('admin.noUsers') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
