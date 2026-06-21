<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet } from '../../api/http'
import type { LedgerPage, WalletSummary } from '../../api/contracts'
import { useSessionStore } from '../../stores/session'
import { i18n } from '../../i18n'

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

watch(() => session.userId, (userId) => {
  if (userId && !summary.value) void loadWallet()
})

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
    error.value = err instanceof ApiError || err instanceof Error ? err.message : i18n.t('wallet.requestFailed')
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
        <p class="eyebrow">{{ $t('common.wallet') }}</p>
        <h1>{{ $t('wallet.heading') }}</h1>
      </div>
      <RouterLink class="plain-link" to="/lobby">{{ $t('nav.lobby') }}</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('wallet.loadingLedger') }}</section>
    <section v-else-if="error" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <section class="wallet-band">
        <div><span>{{ $t('wallet.gcBalance') }}</span><strong>{{ gcBalance }}</strong></div>
        <div><span>{{ $t('wallet.scBalance') }}</span><strong>{{ scBalance }}</strong></div>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('wallet.scStatus') }}</h2>
          <span>{{ $t('wallet.controls') }}</span>
        </div>
        <div class="metric-grid">
          <div><span>{{ $t('wallet.frozenSc') }}</span><strong>{{ scFrozen }}</strong></div>
          <div><span>{{ $t('wallet.redeemableSc') }}</span><strong>{{ scRedeemable }}</strong></div>
        </div>
        <p class="notice">{{ $t('wallet.notice') }}</p>
        <div class="source-list">
          <span v-for="source in summary?.scSourceSummary" :key="source.source">
            {{ source.source }}: {{ formatAmount(source.amount, 2) }} SC
          </span>
          <span v-if="!summary?.scSourceSummary.length">{{ $t('wallet.noScSource') }}</span>
        </div>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('nav.ledger') }}</h2>
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
          <p v-if="!ledger?.items.length" class="empty-state">{{ $t('wallet.noLedgerRows', { currency }) }}</p>
        </div>
      </section>
    </template>
  </main>
</template>
