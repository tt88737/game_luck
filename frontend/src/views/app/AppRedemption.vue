<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { KycStatus, RedemptionRequest, WalletSummary } from '../../api/contracts'
import { i18n } from '../../i18n'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const wallet = ref<WalletSummary | null>(null)
const kyc = ref<KycStatus | null>(null)
const redemption = ref<RedemptionRequest | null>(null)
const form = reactive({
  scAmount: '0.50',
  method: 'gift_card',
})

const redeemable = computed(() => Number(wallet.value?.wallet.scRedeemable ?? 0))
const kycReady = computed(() => kyc.value?.status === 'approved')
const canSubmit = computed(() => kycReady.value && redeemable.value >= Number(form.scAmount) && !submitting.value)

onMounted(loadState)

async function loadState() {
  if (!session.userId) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [walletState, kycState] = await Promise.all([
      apiGet<WalletSummary>('/wallet/summary'),
      apiGet<KycStatus>('/kyc/status'),
    ])
    wallet.value = walletState
    kyc.value = kycState
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function submitRedemption() {
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    redemption.value = await apiPost<RedemptionRequest>(
      '/redemptions',
      { scAmount: form.scAmount, method: form.method },
      `web-redemption-${Date.now()}`,
    )
    success.value = i18n.t('redemption.success', { redemptionId: redemption.value.redemptionId })
    await loadState()
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    submitting.value = false
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('redemption.requestFailed')
}

function amount(value: string | number | undefined, digits = 2) {
  return Number(value ?? 0).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">{{ $t('nav.redeem') }}</p>
        <h1>{{ $t('redemption.heading') }}</h1>
      </div>
      <RouterLink class="plain-link" to="/app/kyc">{{ $t('nav.kyc') }}</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('redemption.loading') }}</section>
    <section v-else-if="!session.userId" class="status-panel">
      <strong>{{ $t('register.heading') }}</strong>
      <span>Create an account before requesting redemption.</span>
      <RouterLink class="plain-link" to="/app/register">{{ $t('register.submit') }}</RouterLink>
      <RouterLink class="plain-link" to="/app/login">{{ $t('login.submit') }}</RouterLink>
    </section>
    <section v-else>
      <section class="wallet-band">
        <div><span>SC balance</span><strong>{{ amount(wallet?.wallet.scBalance) }}</strong></div>
        <div><span>{{ $t('common.frozen') }}</span><strong>{{ amount(wallet?.wallet.scFrozen) }}</strong></div>
        <div><span>{{ $t('common.redeemable') }}</span><strong>{{ amount(wallet?.wallet.scRedeemable) }}</strong></div>
        <div><span>{{ $t('common.kyc') }}</span><strong>{{ kyc?.status ?? 'not_started' }}</strong></div>
      </section>

      <p v-if="!kycReady" class="notice danger">{{ $t('redemption.kycRequired') }}</p>
      <p v-else-if="redeemable < Number(form.scAmount)" class="notice danger">{{ $t('redemption.insufficient') }}</p>
      <p v-else class="notice">{{ $t('redemption.notice') }}</p>
      <p v-if="success" class="notice success">{{ success }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('common.newRequest') }}</h2>
          <span>{{ $t('admin.manualApproval') }}</span>
        </div>
        <form class="form-stack" @submit.prevent="submitRedemption">
          <label>
            {{ $t('common.scAmount') }}
            <input v-model="form.scAmount" inputmode="decimal" required />
          </label>
          <label>
            {{ $t('common.method') }}
            <select v-model="form.method">
              <option value="gift_card">Gift card</option>
              <option value="manual_review">{{ $t('admin.manualApproval') }}</option>
            </select>
          </label>
          <button data-test="submit-redemption" :disabled="!canSubmit">{{ submitting ? $t('common.submitting') : $t('common.submitRequest') }}</button>
        </form>
      </section>

      <section v-if="redemption" class="section-block">
        <div class="section-title">
          <h2>{{ $t('common.latestRequest') }}</h2>
          <span class="status-tag pending">{{ redemption.status }}</span>
        </div>
        <div class="source-list">
          <span>{{ redemption.redemptionId }}</span>
          <span>{{ amount(redemption.scAmount) }} SC · {{ redemption.method }}</span>
        </div>
      </section>
    </section>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app">{{ $t('nav.home') }}</RouterLink>
      <RouterLink to="/app/store">{{ $t('nav.store') }}</RouterLink>
      <RouterLink to="/app/kyc">{{ $t('nav.kyc') }}</RouterLink>
      <RouterLink to="/app/redemption">{{ $t('nav.redeem') }}</RouterLink>
      <RouterLink to="/app/wallet">{{ $t('common.wallet') }}</RouterLink>
    </nav>
  </main>
</template>
