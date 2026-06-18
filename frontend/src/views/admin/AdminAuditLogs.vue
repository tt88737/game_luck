<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet } from '../../api/http'
import type { AuditLog } from '../../api/contracts'

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
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Audit log request failed.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="admin-shell">
    <aside class="admin-nav">
      <strong>Tang Luck Ops</strong>
      <RouterLink to="/admin">Dashboard</RouterLink>
      <RouterLink to="/admin/campaigns">Campaigns</RouterLink>
      <RouterLink to="/admin/p1">P1 Ops</RouterLink>
      <RouterLink to="/admin/audit-logs">Audit logs</RouterLink>
    </aside>

    <section class="admin-content">
      <header class="admin-header">
        <div>
          <p class="eyebrow">Audit trail</p>
          <h1>Audit Logs</h1>
        </div>
      </header>

      <div class="filter-bar">
        <label>
          Target ID
          <input v-model="targetId" />
        </label>
        <button :disabled="loading" @click="loadLogs">Search</button>
      </div>

      <p v-if="error" class="notice danger">{{ error }}</p>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Operator</th>
              <th>Action</th>
              <th>Target</th>
              <th>Before</th>
              <th>After</th>
              <th>Time</th>
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
              <td colspan="7">No audit records for this target.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>
