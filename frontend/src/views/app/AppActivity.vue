<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { Campaign, ClaimResponse, DailyTask } from '../../api/contracts'

const campaigns = ref<Campaign[]>([])
const tasks = ref<DailyTask[]>([])
const error = ref('')
const message = ref('')
const loading = ref(true)
const coupon = ref('WELCOME500')
const processing = ref('')

onMounted(loadActivity)

async function loadActivity() {
  loading.value = true
  error.value = ''
  try {
    const [campaignList, taskList] = await Promise.all([
      apiGet<Campaign[]>('/campaigns'),
      apiGet<DailyTask[]>('/tasks/daily'),
    ])
    campaigns.value = campaignList
    tasks.value = taskList
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Activity request failed.'
  } finally {
    loading.value = false
  }
}

async function claimCampaign(code: string) {
  await claim(code, () => apiPost<ClaimResponse>(`/campaigns/${code}/claim`, undefined, `activity-${code}-${Date.now()}`))
}

async function claimTask(code: string) {
  await claim(code, async () => {
    await apiPost<DailyTask>(`/tasks/${code}/progress`, { progress: 1 })
    return apiPost<ClaimResponse>(`/tasks/${code}/claim`, undefined, `activity-task-${code}-${Date.now()}`)
  })
}

async function claimCoupon() {
  await claim('coupon', () => apiPost<ClaimResponse>('/coupon/claim', { code: coupon.value }, `activity-coupon-${Date.now()}`))
}

async function claim(key: string, action: () => Promise<ClaimResponse>) {
  processing.value = key
  error.value = ''
  message.value = ''
  try {
    const result = await action()
    message.value = `${result.campaignCode} issued ${result.rewards.map((item) => `${Number(item.amount).toLocaleString()} ${item.currency}`).join(' + ')}`
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Claim failed.'
  } finally {
    processing.value = ''
  }
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">Activity center</p>
        <h1>Claimable rewards</h1>
      </div>
      <RouterLink class="plain-link" to="/app">Home</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">Loading activity...</section>
    <p v-if="message" class="notice success">{{ message }}</p>
    <p v-if="error" class="notice danger">{{ error }}</p>

    <template v-if="!loading">
      <section class="section-block">
        <div class="section-title">
          <h2>Campaigns</h2>
          <span>{{ campaigns.length }} records</span>
        </div>
        <article v-for="campaign in campaigns" :key="campaign.campaignCode" class="reward-row">
          <div>
            <strong>{{ campaign.campaignCode }}</strong>
            <span>{{ campaign.campaignType }} · {{ campaign.status }}</span>
          </div>
          <button :disabled="processing === campaign.campaignCode || campaign.status !== 'active'" @click="claimCampaign(campaign.campaignCode)">
            {{ processing === campaign.campaignCode ? 'Claiming' : 'Claim' }}
          </button>
        </article>
        <p v-if="!campaigns.length" class="empty-state">No campaign available.</p>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>Daily tasks</h2>
          <span>Cooldown: daily</span>
        </div>
        <article v-for="task in tasks" :key="task.taskCode" class="reward-row">
          <div>
            <strong>{{ task.taskCode }}</strong>
            <span>Target {{ task.target }} · {{ task.status }}</span>
          </div>
          <button :disabled="processing === task.taskCode" @click="claimTask(task.taskCode)">
            {{ processing === task.taskCode ? 'Claiming' : 'Check in' }}
          </button>
        </article>
        <p v-if="!tasks.length" class="empty-state">No daily task available.</p>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>Coupon</h2>
          <span>One use per account</span>
        </div>
        <div class="coupon-row">
          <input v-model="coupon" aria-label="Coupon code" />
          <button :disabled="processing === 'coupon' || !coupon" @click="claimCoupon">
            {{ processing === 'coupon' ? 'Applying' : 'Apply' }}
          </button>
        </div>
      </section>
    </template>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app">Home</RouterLink>
      <RouterLink to="/app/store">Store</RouterLink>
      <RouterLink to="/app/kyc">KYC</RouterLink>
      <RouterLink to="/app/redemption">Redeem</RouterLink>
      <RouterLink to="/app/wallet">Wallet</RouterLink>
    </nav>
  </main>
</template>
