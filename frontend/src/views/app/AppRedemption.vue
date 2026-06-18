<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { KycStatus, RedemptionRequest, WalletSummary } from '../../api/contracts'

const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const wallet = ref<WalletSummary | null>(null)
const kyc = ref<KycStatus | null>(null)
const redemption = ref<RedemptionRequest | null>(null)
const form = reactive({
  scAmount: '0.50',
  method: 'sandbox_gift_card',
})

const redeemable = computed(() => Number(wallet.value?.wallet.scRedeemable ?? 0))
const kycReady = computed(() => kyc.value?.status === 'approved')
const canSubmit = computed(() => kycReady.value && redeemable.value >= Number(form.scAmount) && !submitting.value)

onMounted(loadState)

async function loadState() {
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
    success.value = `Request ${redemption.value.redemptionId} is waiting for manual review.`
    await loadState()
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    submitting.value = false
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Redemption request failed.'
}

function amount(value: string | number | undefined, digits = 2) {
  return Number(value ?? 0).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">P1 Redemption</p>
        <h1>Request review</h1>
      </div>
      <RouterLink class="plain-link" to="/app/kyc">KYC</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">Loading redemption state...</section>
    <section v-else>
      <section class="wallet-band">
        <div><span>SC balance</span><strong>{{ amount(wallet?.wallet.scBalance) }}</strong></div>
        <div><span>Frozen</span><strong>{{ amount(wallet?.wallet.scFrozen) }}</strong></div>
        <div><span>Redeemable</span><strong>{{ amount(wallet?.wallet.scRedeemable) }}</strong></div>
        <div><span>KYC</span><strong>{{ kyc?.status ?? 'not_started' }}</strong></div>
      </section>

      <p v-if="!kycReady" class="notice danger">KYC approval is required before creating a redemption request.</p>
      <p v-else-if="redeemable < Number(form.scAmount)" class="notice danger">Insufficient redeemable SC for this request.</p>
      <p v-else class="notice">Sandbox redemption freezes SC and creates a manual review record. No real payout is connected.</p>
      <p v-if="success" class="notice success">{{ success }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>New request</h2>
          <span>Manual review</span>
        </div>
        <form class="form-stack" @submit.prevent="submitRedemption">
          <label>
            SC amount
            <input v-model="form.scAmount" inputmode="decimal" required />
          </label>
          <label>
            Method
            <select v-model="form.method">
              <option value="sandbox_gift_card">Sandbox gift card</option>
              <option value="manual_review">Manual review</option>
            </select>
          </label>
          <button data-test="submit-redemption" :disabled="!canSubmit">{{ submitting ? 'Submitting' : 'Submit request' }}</button>
        </form>
      </section>

      <section v-if="redemption" class="section-block">
        <div class="section-title">
          <h2>Latest request</h2>
          <span class="status-tag pending">{{ redemption.status }}</span>
        </div>
        <div class="source-list">
          <span>{{ redemption.redemptionId }}</span>
          <span>{{ amount(redemption.scAmount) }} SC · {{ redemption.method }}</span>
        </div>
      </section>
    </section>

    <nav class="bottom-nav" aria-label="App navigation">
      <RouterLink to="/app">Home</RouterLink>
      <RouterLink to="/app/store">Store</RouterLink>
      <RouterLink to="/app/kyc">KYC</RouterLink>
      <RouterLink to="/app/redemption">Redeem</RouterLink>
      <RouterLink to="/app/wallet">Wallet</RouterLink>
    </nav>
  </main>
</template>
