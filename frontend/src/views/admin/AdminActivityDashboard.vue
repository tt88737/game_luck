<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { AdminActivityDashboard } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const dashboard = ref<AdminActivityDashboard | null>(null)
const loading = ref(true)
const error = ref('')

onMounted(loadDashboard)

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await apiGet<AdminActivityDashboard>('/admin/activity-dashboard')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

function money(value: string | number, digits = 0) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Activity dashboard request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Activity operations</p>
        <h1>Activity Dashboard</h1>
      </div>
      <button :disabled="loading" @click="loadDashboard">Refresh</button>
    </header>

    <section v-if="loading" class="status-panel">Loading activity dashboard...</section>
    <section v-else-if="error && !dashboard" class="status-panel danger">{{ error }}</section>

    <template v-else-if="dashboard">
      <p v-if="error" class="notice danger">{{ error }}</p>
      <section class="admin-metrics">
        <div>
          <span>Participants</span>
          <strong>{{ dashboard.totalParticipants }}</strong>
        </div>
        <div>
          <span>Completed tasks</span>
          <strong>{{ dashboard.completedTasks }}</strong>
        </div>
        <div>
          <span>GC granted</span>
          <strong>{{ money(dashboard.gcGranted) }}</strong>
        </div>
        <div>
          <span>Task configs</span>
          <strong>{{ dashboard.tasks.length }}</strong>
        </div>
      </section>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Task</th>
              <th>Target type</th>
              <th>Completed</th>
              <th>Reward</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in dashboard.tasks" :key="task.taskCode">
              <td data-label="Task"><strong>{{ task.taskCode }}</strong><span>{{ task.name }}</span></td>
              <td data-label="Target">{{ task.targetType }}</td>
              <td data-label="Completed">{{ task.completedCount }}</td>
              <td data-label="Reward">{{ money(task.rewardAmount) }} GC</td>
            </tr>
            <tr v-if="!dashboard.tasks.length"><td colspan="4">No activity tasks.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
