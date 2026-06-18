<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { Campaign, ClaimResponse, ComplianceDocument, DailyTask, WalletSummary } from '../../api/contracts'

const loading = ref(true)
const error = ref('')
const success = ref('')
const claiming = ref('')
const summary = ref<WalletSummary | null>(null)
const campaigns = ref<Campaign[]>([])
const tasks = ref<DailyTask[]>([])
const documents = ref<ComplianceDocument[]>([])

const gcBalance = computed(() => formatAmount(summary.value?.wallet.gcBalance ?? 0, 0))
const scBalance = computed(() => formatAmount(summary.value?.wallet.scBalance ?? 0, 2))
const welcome = computed(() => campaigns.value.find((item) => item.campaignType === 'register_bonus') ?? campaigns.value[0])
const dailyLogin = computed(() => tasks.value[0])
const isRegionRestricted = computed(() => error.value.toLowerCase().includes('region') || error.value.includes('available in your region'))

onMounted(loadHome)

async function loadHome() {
  loading.value = true
  error.value = ''
  try {
    const [wallet, campaignList, taskList, docs] = await Promise.all([
      apiGet<WalletSummary>('/wallet/summary'),
      apiGet<Campaign[]>('/campaigns'),
      apiGet<DailyTask[]>('/tasks/daily'),
      apiGet<ComplianceDocument[]>('/compliance/documents'),
    ])
    summary.value = wallet
    campaigns.value = campaignList
    tasks.value = taskList
    documents.value = docs
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function claimCampaign(code: string) {
  claiming.value = code
  error.value = ''
  success.value = ''
  try {
    const result = await apiPost<ClaimResponse>(`/campaigns/${code}/claim`, undefined, `web-${code}-${Date.now()}`)
    success.value = `Claimed ${result.rewards.map((reward) => `${formatAmount(reward.amount, reward.currency === 'GC' ? 0 : 2)} ${reward.currency}`).join(' + ')}`
    await loadHome()
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    claiming.value = ''
  }
}

async function claimTask(code: string) {
  claiming.value = code
  error.value = ''
  success.value = ''
  try {
    await apiPost<DailyTask>(`/tasks/${code}/progress`, { progress: 1 })
    const result = await apiPost<ClaimResponse>(`/tasks/${code}/claim`, undefined, `web-${code}-${Date.now()}`)
    success.value = `Daily reward issued: ${result.rewards.map((reward) => `${formatAmount(reward.amount, reward.currency === 'GC' ? 0 : 2)} ${reward.currency}`).join(' + ')}`
    await loadHome()
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    claiming.value = ''
  }
}

async function claimCoupon() {
  claiming.value = 'WELCOME500'
  error.value = ''
  success.value = ''
  try {
    const result = await apiPost<ClaimResponse>('/coupon/claim', { code: 'WELCOME500' }, `web-coupon-${Date.now()}`)
    success.value = `Coupon applied: ${result.rewards.map((reward) => `${formatAmount(reward.amount, reward.currency === 'GC' ? 0 : 2)} ${reward.currency}`).join(' + ')}`
    await loadHome()
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    claiming.value = ''
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError) return err.message
  if (err instanceof Error) return err.message
  return 'Network request failed.'
}

function formatAmount(value: string | number, digits: number) {
  return Number(value).toLocaleString('en-US', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">Tang Luck P0-A</p>
        <h1>Rewards wallet</h1>
      </div>
      <RouterLink class="plain-link" to="/app/wallet">Ledger</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">Loading wallet and rewards...</section>

    <section v-else-if="error && !summary" class="status-panel danger">
      <strong>{{ isRegionRestricted ? 'Region restricted' : 'Unable to load' }}</strong>
      <span>{{ error }}</span>
    </section>

    <template v-else>
      <section class="wallet-band" aria-label="Wallet balance">
        <div>
          <span>Gold Coins</span>
          <strong>{{ gcBalance }}</strong>
        </div>
        <div>
          <span>Sweeps Coins</span>
          <strong>{{ scBalance }}</strong>
        </div>
      </section>

      <p v-if="summary?.notices.length" class="notice">{{ summary.notices[0] }}</p>
      <p v-if="success" class="notice success">{{ success }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>Available rewards</h2>
          <RouterLink to="/app/activity">All activity</RouterLink>
        </div>

        <article v-if="welcome" class="reward-row">
          <div>
            <strong>{{ welcome.campaignCode }}</strong>
            <span>Welcome Bonus: 10,000 GC + 0.50 SC when eligible.</span>
          </div>
          <button data-test="claim-welcome" :disabled="claiming === welcome.campaignCode" @click="claimCampaign(welcome.campaignCode)">
            {{ claiming === welcome.campaignCode ? 'Claiming' : 'Claim' }}
          </button>
        </article>

        <article v-if="dailyLogin" class="reward-row">
          <div>
            <strong>{{ dailyLogin.taskCode }}</strong>
            <span>Daily login task, risk users receive GC only.</span>
          </div>
          <button :disabled="claiming === dailyLogin.taskCode" @click="claimTask(dailyLogin.taskCode)">
            {{ claiming === dailyLogin.taskCode ? 'Claiming' : 'Check in' }}
          </button>
        </article>

        <article class="reward-row">
          <div>
            <strong>WELCOME500</strong>
            <span>Coupon code for 500 GC, one redemption per account.</span>
          </div>
          <button :disabled="claiming === 'WELCOME500'" @click="claimCoupon">
            {{ claiming === 'WELCOME500' ? 'Applying' : 'Apply' }}
          </button>
        </article>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>Rules</h2>
          <span>Required links</span>
        </div>
        <div class="legal-list">
          <a v-for="doc in documents" :key="doc.documentType" :href="doc.contentUrl">{{ doc.title }}</a>
          <a v-if="!documents.some((doc) => doc.documentType === 'amoe')" href="/legal/amoe-v1">AMOE / No Purchase Necessary</a>
        </div>
      </section>
    </template>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app">Home</RouterLink>
      <RouterLink to="/app/wallet">Wallet</RouterLink>
      <RouterLink to="/app/activity">Activity</RouterLink>
    </nav>
  </main>
</template>
