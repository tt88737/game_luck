<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { DashboardSummary } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

const loading = ref(true)
const error = ref('')
const summary = ref<DashboardSummary | null>(null)

onMounted(async () => {
  try {
    summary.value = await apiGet<DashboardSummary>('/admin/dashboard/summary')
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('admin.dashboardRequestFailed')
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
          <p class="eyebrow">{{ $t('admin.operations') }}</p>
          <h1>{{ $t('admin.dashboard') }}</h1>
        </div>
        <span class="status-tag active">{{ $t('admin.production') }}</span>
      </header>

      <section v-if="loading" class="status-panel">{{ $t('admin.loadingDashboard') }}</section>
      <section v-else-if="error" class="status-panel danger">{{ error }}</section>
      <section v-else class="admin-metrics">
        <div><span>{{ $t('admin.metricsRegistrations') }}</span><strong>{{ summary?.registrations }}</strong></div>
        <div><span>{{ $t('admin.metricsClaims') }}</span><strong>{{ summary?.claims }}</strong></div>
        <div><span>{{ $t('admin.metricsScGranted') }}</span><strong>{{ amount(summary?.scGranted) }}</strong></div>
        <div><span>{{ $t('admin.metricsRiskEvents') }}</span><strong>{{ summary?.riskEvents }}</strong></div>
      </section>
  </AdminLayout>
</template>
