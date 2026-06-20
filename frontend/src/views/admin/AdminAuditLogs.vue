<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { AuditLog } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

const targetId = ref('OPS_SC_BONUS')
const logs = ref<AuditLog[]>([])
const loading = ref(false)
const error = ref('')

onMounted(loadLogs)

async function loadLogs() {
  loading.value = true
  error.value = ''
  try {
    logs.value = await apiGet<AuditLog[]>(`/admin/audit-logs?target_type=promotion_campaign&target_id=${encodeURIComponent(targetId.value)}`)
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('admin.auditLogRequestFailed')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AdminLayout>
      <header class="admin-header">
        <div>
          <p class="eyebrow">{{ $t('admin.auditTrail') }}</p>
          <h1>{{ $t('admin.auditLogs') }}</h1>
        </div>
      </header>

      <div class="filter-bar">
        <label>
          {{ $t('common.target') }} ID
          <input v-model="targetId" />
        </label>
        <button :disabled="loading" @click="loadLogs">{{ $t('admin.search') }}</button>
      </div>

      <p v-if="error" class="notice danger">{{ error }}</p>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('admin.operator') }}</th>
              <th>{{ $t('common.action') }}</th>
              <th>{{ $t('common.target') }}</th>
              <th>{{ $t('admin.before') }}</th>
              <th>{{ $t('admin.after') }}</th>
              <th>{{ $t('common.time') }}</th>
              <th>IP</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>
                <strong>{{ log.operatorRole }}</strong>
                <span>#{{ log.operatorId }}</span>
              </td>
              <td>{{ log.action }}</td>
              <td>{{ log.targetType }} / {{ log.targetId }}</td>
              <td><code>{{ log.beforeJson }}</code></td>
              <td><code>{{ log.afterJson }}</code></td>
              <td>{{ new Date(log.createdAt).toLocaleString() }}</td>
              <td>{{ log.ip }}</td>
            </tr>
            <tr v-if="!logs.length && !loading">
              <td colspan="7">{{ $t('admin.noAuditRecords') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
  </AdminLayout>
</template>
