<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet } from '../../api/http'
import type { LedgerPage, WalletSummary } from '../../api/contracts'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const loading = ref(true)
const error = ref('')
const currency = ref<'GC' | 'SC'>('SC')
const summary = ref<WalletSummary | null>(null)
const ledger = ref<LedgerPage | null>(null)

const gcBalance = computed(() => formatAmount(summary.value?.wallet.gcBalance ?? 0, 0))
const scBalance = computed(() => formatAmount(summary.value?.wallet.scBalance ?? 0, 2))
const scFrozen = computed(() => formatAmount(summary.value?.wallet.scFrozen ?? 0, 2))
const scRedeemable = computed(() => formatAmount(summary.value?.wallet.scRedeemable ?? 0, 2))

onMounted(loadWallet)

async function loadWallet() {
  if (!session.userId) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [wallet, rows] = await Promise.all([
      apiGet<WalletSummary>('/wallet/summary'),
      apiGet<LedgerPage>(`/wallet/ledger?currency=${currency.value}`),
    ])
    summary.value = wallet
    ledger.value = rows
  } catch (err) {
    error.value = err instanceof ApiError || err instanceof Error ? err.message : 'Wallet request failed.'
  } finally {
    loading.value = false
  }
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
        <p class="eyebrow">Wallet</p>
        <h1>Balances and ledger</h1>
      </div>
      <RouterLink class="plain-link" to="/app">Home</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">Loading ledger...</section>
    <section v-else-if="!session.userId" class="status-panel">
      <strong>Create your account</strong>
      <span>Create an account before viewing wallet balances and ledger.</span>
      <RouterLink class="plain-link" to="/app/register">Register and continue</RouterLink>
    </section>
    <section v-else-if="error" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <section class="wallet-band">
        <div><span>GC balance</span><strong>{{ gcBalance }}</strong></div>
        <div><span>SC balance</span><strong>{{ scBalance }}</strong></div>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>SC status</h2>
          <span>Wallet controls</span>
        </div>
        <div class="metric-grid">
          <div><span>Frozen SC</span><strong>{{ scFrozen }}</strong></div>
          <div><span>Redeemable SC</span><strong>{{ scRedeemable }}</strong></div>
        </div>
        <p class="notice">SC is granted through promotions or AMOE paths in P0-A. It is not sold and redemption payout is not connected.</p>
        <div class="source-list">
          <span v-for="source in summary?.scSourceSummary" :key="source.source">
            {{ source.source }}: {{ formatAmount(source.amount, 2) }} SC
          </span>
          <span v-if="!summary?.scSourceSummary.length">No SC source yet.</span>
        </div>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>Ledger</h2>
          <select v-model="currency" @change="loadWallet">
            <option value="SC">SC</option>
            <option value="GC">GC</option>
          </select>
        </div>
        <div class="ledger-list">
          <article v-for="row in ledger?.items" :key="row.ledgerId" class="ledger-row">
            <div>
              <strong>{{ row.businessType }}</strong>
              <span>{{ row.status }} · {{ new Date(row.createdAt).toLocaleString() }}</span>
            </div>
            <b>{{ formatAmount(row.amount, row.currency === 'GC' ? 0 : 2) }} {{ row.currency }}</b>
          </article>
          <p v-if="!ledger?.items.length" class="empty-state">No ledger rows for {{ currency }}.</p>
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
