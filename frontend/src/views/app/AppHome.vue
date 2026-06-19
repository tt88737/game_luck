<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { Campaign, ClaimResponse, ComplianceDocument, DailyTask, WalletSummary } from '../../api/contracts'
import { useSessionStore } from '../../stores/session'
import { i18n } from '../../i18n'

const session = useSessionStore()
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

onMounted(() => {
  if (!session.userId) {
    loading.value = false
    return
  }
  void loadHome()
})

watch(() => session.userId, (userId) => {
  if (userId && !summary.value) void loadHome()
})

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
  return i18n.t('home.networkFailed')
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
        <p class="eyebrow">{{ $t('home.eyebrow') }}</p>
        <h1>{{ $t('home.heading') }}</h1>
      </div>
      <RouterLink class="plain-link" to="/app/wallet">{{ $t('nav.ledger') }}</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('home.loading') }}</section>

    <section v-else-if="!session.userId" class="status-panel">
      <strong>{{ $t('home.registerTitle') }}</strong>
      <span>{{ $t('home.registerBody') }}</span>
      <RouterLink class="plain-link" to="/app/register">{{ $t('home.registerCta') }}</RouterLink>
      <RouterLink class="plain-link" to="/app/login">{{ $t('login.submit') }}</RouterLink>
    </section>

    <section v-else-if="error && !summary" class="status-panel danger">
      <strong>{{ isRegionRestricted ? $t('home.regionRestricted') : $t('home.unableToLoad') }}</strong>
      <span>{{ error }}</span>
    </section>

    <template v-else>
      <section class="wallet-band" aria-label="Wallet balance">
        <div>
          <span>{{ $t('home.goldCoins') }}</span>
          <strong>{{ gcBalance }}</strong>
        </div>
        <div>
          <span>{{ $t('home.sweepsCoins') }}</span>
          <strong>{{ scBalance }}</strong>
        </div>
      </section>

      <section class="lobby-hero">
        <p class="eyebrow">{{ $t('home.availableRewards') }}</p>
        <h2>{{ $t('home.heading') }}</h2>
        <p>{{ $t('home.welcomeBonus') }}</p>
      </section>

      <nav class="quick-actions" aria-label="Quick actions">
        <RouterLink to="/app/store">
          {{ $t('nav.store') }}
          <span>{{ $t('store.gcPackages') }}</span>
        </RouterLink>
        <RouterLink to="/app/activity">
          {{ $t('nav.activity') }}
          <span>{{ $t('home.allActivity') }}</span>
        </RouterLink>
        <RouterLink to="/app/redemption">
          {{ $t('nav.redeem') }}
          <span>{{ $t('common.scAmount') }}</span>
        </RouterLink>
      </nav>

      <p v-if="summary?.notices.length" class="notice">{{ summary.notices[0] }}</p>
      <p v-if="success" class="notice success">{{ success }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('home.availableRewards') }}</h2>
          <RouterLink to="/app/activity">{{ $t('home.allActivity') }}</RouterLink>
        </div>

        <article v-if="welcome" class="reward-row">
          <div>
            <strong>{{ welcome.campaignCode }}</strong>
            <span>{{ $t('home.welcomeBonus') }}</span>
          </div>
          <button data-test="claim-welcome" :disabled="claiming === welcome.campaignCode" @click="claimCampaign(welcome.campaignCode)">
            {{ claiming === welcome.campaignCode ? $t('home.claiming') : $t('home.claim') }}
          </button>
        </article>

        <article v-if="dailyLogin" class="reward-row">
          <div>
            <strong>{{ dailyLogin.taskCode }}</strong>
            <span>{{ $t('home.dailyLogin') }}</span>
          </div>
          <button :disabled="claiming === dailyLogin.taskCode" @click="claimTask(dailyLogin.taskCode)">
            {{ claiming === dailyLogin.taskCode ? $t('home.claiming') : $t('home.checkIn') }}
          </button>
        </article>

        <article class="reward-row">
          <div>
            <strong>WELCOME500</strong>
            <span>{{ $t('home.couponCopy') }}</span>
          </div>
          <button :disabled="claiming === 'WELCOME500'" @click="claimCoupon">
            {{ claiming === 'WELCOME500' ? $t('home.applying') : $t('home.apply') }}
          </button>
        </article>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('home.rules') }}</h2>
          <span>{{ $t('home.requiredLinks') }}</span>
        </div>
        <div class="legal-list">
          <a v-for="doc in documents" :key="doc.documentType" :href="doc.contentUrl">{{ doc.title }}</a>
          <a v-if="!documents.some((doc) => doc.documentType === 'amoe')" href="/legal/amoe-v1">AMOE / No Purchase Necessary</a>
        </div>
      </section>
    </template>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app/register">{{ $t('nav.register') }}</RouterLink>
      <RouterLink to="/app/login">{{ $t('login.submit') }}</RouterLink>
      <RouterLink to="/app">{{ $t('nav.home') }}</RouterLink>
      <RouterLink to="/app/store">{{ $t('nav.store') }}</RouterLink>
      <RouterLink to="/app/redemption">{{ $t('nav.redeem') }}</RouterLink>
      <RouterLink to="/app/wallet">{{ $t('common.wallet') }}</RouterLink>
    </nav>
  </main>
</template>
