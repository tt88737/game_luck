<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet } from '../../api/http'
import type { DashboardSummary } from '../../api/contracts'

const loading = ref(true)
const error = ref('')
const summary = ref<DashboardSummary | null>(null)

onMounted(async () => {
  try {
    summary.value = await apiGet<DashboardSummary>('/admin/dashboard/summary')
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Dashboard request failed.'
  } finally {
    loading.value = false
  }
})

function amount(value: string | number | undefined) {
  return Number(value ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
          <p class="eyebrow">Operations</p>
          <h1>Dashboard</h1>
        </div>
        <span class="status-tag active">Production</span>
      </header>

      <section v-if="loading" class="status-panel">Loading dashboard...</section>
      <section v-else-if="error" class="status-panel danger">{{ error }}</section>
      <section v-else class="admin-metrics">
        <div><span>Registrations</span><strong>{{ summary?.registrations }}</strong></div>
        <div><span>Claims</span><strong>{{ summary?.claims }}</strong></div>
        <div><span>SC granted</span><strong>{{ amount(summary?.scGranted) }}</strong></div>
        <div><span>Risk events</span><strong>{{ summary?.riskEvents }}</strong></div>
      </section>
    </section>
  </main>
</template>
