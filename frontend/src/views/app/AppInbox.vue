<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { RewardInboxItem } from '../../api/contracts'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const rows = ref<RewardInboxItem[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
const processing = ref<number | null>(null)

onMounted(loadInbox)

async function loadInbox() {
  loading.value = true
  error.value = ''
  if (!session.userId) {
    loading.value = false
    return
  }
  try {
    rows.value = await apiGet<RewardInboxItem[]>('/player/notifications')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function claim(row: RewardInboxItem) {
  processing.value = row.id
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPost<RewardInboxItem>(`/player/notifications/${row.id}/claim`)
    rows.value = rows.value.map((item) => item.id === updated.id ? updated : item)
    notice.value = `${updated.title} claimed, ledger ${updated.ledgerId}.`
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    processing.value = null
  }
}

function money(value: string | number, digits = 0) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Inbox request failed.'
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">Reward inbox</p>
        <h1>Notifications</h1>
      </div>
      <RouterLink class="plain-link" to="/app">Lobby</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">Loading inbox...</section>
    <section v-else-if="!session.userId" class="status-panel">
      <strong>Create your account</strong>
      <span>Register before claiming inbox rewards.</span>
      <RouterLink class="plain-link" to="/app/register">Register</RouterLink>
    </section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>Reward inbox</h2>
          <span>{{ rows.length }} records</span>
        </div>
        <article v-for="row in rows" :key="row.id" class="reward-row">
          <div>
            <strong>{{ row.title }}</strong>
            <span>{{ row.message }}</span>
            <span>{{ row.status }} · {{ row.sourceId }} · ledger {{ row.ledgerId || '-' }}</span>
          </div>
          <button :data-test="`claim-inbox-${row.id}`" :disabled="processing === row.id || row.status !== 'claimable'" @click="claim(row)">
            {{ processing === row.id ? 'Claiming' : `${money(row.rewardAmount)} ${row.rewardCurrency}` }}
          </button>
        </article>
        <p v-if="!rows.length" class="empty-state">No inbox rewards.</p>
      </section>
    </template>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app">Home</RouterLink>
      <RouterLink to="/app/slots/lucky_slots">Slots</RouterLink>
      <RouterLink to="/app/activity">Activity</RouterLink>
      <RouterLink to="/app/inbox">Inbox</RouterLink>
      <RouterLink to="/app/wallet">Wallet</RouterLink>
    </nav>
  </main>
</template>
