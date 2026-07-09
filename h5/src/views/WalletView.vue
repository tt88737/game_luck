<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { WalletAccount, WalletLedger } from '../types/client'

const accounts = ref<WalletAccount[]>([])
const ledgers = ref<WalletLedger[]>([])
const loading = ref(false)
const error = ref('')

async function loadWallet() {
  if (!sessionState.member) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    accounts.value = await clientApi.walletAccounts()
    const currency = accounts.value[0]?.currencyCode || 'GC'
    ledgers.value = (await clientApi.walletLedgers(currency)).records
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Wallet load failed'
  } finally {
    loading.value = false
  }
}

onMounted(loadWallet)
watch(() => sessionState.member?.memberId, () => loadWallet())
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">Wallet</p>
    <h1>Balances and recent ledger</h1>
    <p class="muted">Balances are loaded from the backend client wallet API.</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>Login required</strong>
    <RouterLink class="btn primary" to="/login">Login</RouterLink>
  </section>

  <template v-else>
    <p v-if="loading" class="muted">Loading wallet...</p>
    <p v-if="error" class="error-text">{{ error }}</p>

    <section class="data-grid">
      <article v-for="account in accounts" :key="account.currencyCode" class="metric-card">
        <span>{{ account.currencyName }}</span>
        <strong>{{ account.availableBalance }}</strong>
        <small>Locked: {{ account.frozenBalance }} | {{ account.playable ? 'Playable' : 'Disabled' }}</small>
      </article>
    </section>

    <section class="table-panel">
      <h2>Recent ledger</h2>
      <div v-if="ledgers.length" class="table-list">
        <div v-for="row in ledgers" :key="row.ledgerId" class="table-row">
          <span>{{ row.createdAt }}</span>
          <strong>{{ row.direction }}</strong>
          <span>{{ row.amount }}</span>
          <em>{{ row.bizType }}</em>
        </div>
      </div>
      <p v-else class="muted">No ledger records.</p>
    </section>
  </template>
</template>
