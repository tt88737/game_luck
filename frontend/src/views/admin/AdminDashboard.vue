<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { DashboardSummary } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

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
  <AdminLayout>
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
  </AdminLayout>
</template>
