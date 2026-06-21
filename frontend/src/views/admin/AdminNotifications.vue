<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { RewardInboxItem } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const rows = ref<RewardInboxItem[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
const form = reactive({
  userId: 2,
  title: 'Manual bonus',
  message: 'Ops grant.',
  rewardCurrency: 'GC',
  rewardAmount: '500.0000',
  sourceType: 'manual_grant',
  sourceId: 'manual-web',
})

onMounted(loadRows)

async function loadRows() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<RewardInboxItem[]>('/admin/notifications')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function manualGrant() {
  error.value = ''
  notice.value = ''
  try {
    const created = await apiPost<RewardInboxItem>('/admin/notifications/manual-grant', form)
    rows.value = [created, ...rows.value]
    notice.value = `${created.title} issued to user ${created.userId}.`
  } catch (err) {
    error.value = messageFrom(err)
  }
}

async function expire(row: RewardInboxItem) {
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPost<RewardInboxItem>(`/admin/notifications/${row.id}/expire`)
    rows.value = rows.value.map((item) => item.id === updated.id ? updated : item)
    notice.value = `${updated.id} expired.`
  } catch (err) {
    error.value = messageFrom(err)
  }
}

function money(value: string | number, digits = 0) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Notification request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">CRM rewards</p>
        <h1>Notifications</h1>
      </div>
    </header>

    <form class="filter-bar" data-test="manual-grant" @submit.prevent="manualGrant">
      <label>
        User ID
        <input v-model.number="form.userId" type="number" min="1" />
      </label>
      <label>
        Title
        <input v-model="form.title" />
      </label>
      <label>
        Amount
        <input v-model="form.rewardAmount" />
      </label>
      <button type="submit">Manual grant</button>
    </form>

    <section v-if="loading" class="status-panel">Loading notifications...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Reward</th>
              <th>User</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Source</th>
              <th>Ledger</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td data-label="Reward"><strong>{{ row.title }}</strong><span>{{ row.message }}</span></td>
              <td data-label="User">{{ row.userId }}</td>
              <td data-label="Amount">{{ money(row.rewardAmount) }} {{ row.rewardCurrency }}</td>
              <td data-label="Status"><span class="status-tag" :class="{ active: row.status === 'claimable', paused: row.status !== 'claimable' }">{{ row.status }}</span></td>
              <td data-label="Source">{{ row.sourceType }} / {{ row.sourceId }}</td>
              <td data-label="Ledger">ledger {{ row.ledgerId || '-' }}</td>
              <td data-label="Actions">
                <button :disabled="row.status !== 'claimable'" @click="expire(row)">Expire</button>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="7">No notifications.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
