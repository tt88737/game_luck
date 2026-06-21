<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { ActivitySummary, ActivityTaskClaim, Campaign, ClaimResponse, DailyTask } from '../../api/contracts'
import { i18n } from '../../i18n'

const campaigns = ref<Campaign[]>([])
const tasks = ref<DailyTask[]>([])
const activitySummary = ref<ActivitySummary | null>(null)
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
    const [campaignList, taskList, activity] = await Promise.all([
      apiGet<Campaign[]>('/campaigns'),
      apiGet<DailyTask[]>('/tasks/daily'),
      apiGet<ActivitySummary>('/player/activity-summary').catch(() => null),
    ])
    campaigns.value = campaignList
    tasks.value = taskList
    activitySummary.value = activity
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('activity.requestFailed')
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

async function claimActivityTask(code: string) {
  processing.value = code
  error.value = ''
  message.value = ''
  try {
    const result = await apiPost<ActivityTaskClaim>(`/player/tasks/${code}/claim`)
    message.value = `${result.taskCode} issued ${Number(result.rewardAmount).toLocaleString()} ${result.rewardCurrency}`
    await loadActivity()
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('activity.claimFailed')
  } finally {
    processing.value = ''
  }
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
    message.value = i18n.t('activity.rewardIssued', {
      code: result.campaignCode,
      rewards: result.rewards.map((item) => `${Number(item.amount).toLocaleString()} ${item.currency}`).join(' + '),
    })
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('activity.claimFailed')
  } finally {
    processing.value = ''
  }
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">{{ $t('nav.promo') }}</p>
        <h1>{{ $t('activity.claimableRewards') }}</h1>
      </div>
      <RouterLink class="plain-link" to="/lobby">{{ $t('nav.lobby') }}</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('activity.loading') }}</section>
    <p v-if="message" class="notice success">{{ message }}</p>
    <p v-if="error" class="notice danger">{{ error }}</p>

    <template v-if="!loading">
      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('nav.campaigns') }}</h2>
          <span>{{ $t('activity.records', { count: campaigns.length }) }}</span>
        </div>
        <article v-for="campaign in campaigns" :key="campaign.campaignCode" class="reward-row">
          <div>
            <strong>{{ campaign.campaignCode }}</strong>
            <span>{{ campaign.campaignType }} · {{ campaign.status }}</span>
          </div>
          <button :disabled="processing === campaign.campaignCode || campaign.status !== 'active'" @click="claimCampaign(campaign.campaignCode)">
            {{ processing === campaign.campaignCode ? $t('common.claiming') : $t('common.claim') }}
          </button>
        </article>
        <p v-if="!campaigns.length" class="empty-state">{{ $t('activity.noCampaign') }}</p>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>Slots tasks</h2>
          <span>{{ activitySummary?.claimableCount || 0 }} claimable</span>
        </div>
        <article v-for="task in activitySummary?.tasks || []" :key="task.taskCode" class="reward-row">
          <div>
            <strong>{{ task.name }}</strong>
            <span>{{ task.taskCode }} · {{ Number(task.progress).toLocaleString() }}/{{ Number(task.target).toLocaleString() }} · {{ task.status }}</span>
          </div>
          <button
            :data-test="`claim-activity-task-${task.taskCode}`"
            :disabled="processing === task.taskCode || task.status !== 'claimable'"
            @click="claimActivityTask(task.taskCode)"
          >
            {{ processing === task.taskCode ? $t('common.claiming') : `${Number(task.rewardAmount).toLocaleString()} ${task.rewardCurrency}` }}
          </button>
        </article>
        <p v-if="!activitySummary?.tasks?.length" class="empty-state">No Slots tasks available.</p>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('activity.dailyTasks') }}</h2>
          <span>{{ $t('activity.cooldownDaily') }}</span>
        </div>
        <article v-for="task in tasks" :key="task.taskCode" class="reward-row">
          <div>
            <strong>{{ task.taskCode }}</strong>
            <span>{{ $t('activity.targetStatus', { target: task.target, status: task.status }) }}</span>
          </div>
          <button :disabled="processing === task.taskCode" @click="claimTask(task.taskCode)">
            {{ processing === task.taskCode ? $t('common.claiming') : $t('home.checkIn') }}
          </button>
        </article>
        <p v-if="!tasks.length" class="empty-state">{{ $t('activity.noDailyTask') }}</p>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('activity.coupon') }}</h2>
          <span>{{ $t('activity.oneUsePerAccount') }}</span>
        </div>
        <div class="coupon-row">
          <input v-model="coupon" :aria-label="$t('activity.couponCode')" />
          <button :disabled="processing === 'coupon' || !coupon" @click="claimCoupon">
            {{ processing === 'coupon' ? $t('activity.applying') : $t('activity.apply') }}
          </button>
        </div>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>AMOE</h2>
          <span>{{ $t('home.amoeNoPurchase') }}</span>
        </div>
        <p class="empty-state">{{ $t('me.legalHint') }}</p>
      </section>
    </template>
  </main>
</template>
