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
    error.value = err instanceof Error ? err.message : '钱包加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadWallet)
watch(() => sessionState.member?.memberId, () => loadWallet())
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">钱包</p>
    <h1>余额与最近流水</h1>
    <p class="muted">余额数据来自后端玩家端钱包 API。</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>请先登录</strong>
    <RouterLink class="btn primary" to="/login">登录</RouterLink>
  </section>

  <template v-else>
    <p v-if="loading" class="muted">正在加载钱包...</p>
    <p v-if="error" class="error-text">{{ error }}</p>

    <section class="data-grid">
      <article v-for="account in accounts" :key="account.currencyCode" class="metric-card">
        <span>{{ account.currencyName }}</span>
        <strong>{{ account.availableBalance }}</strong>
        <small>冻结：{{ account.frozenBalance }} | {{ account.playable ? '可用于游戏' : '已停用' }}</small>
      </article>
    </section>

    <section class="table-panel">
      <h2>最近流水</h2>
      <div v-if="ledgers.length" class="table-list">
        <div v-for="row in ledgers" :key="row.ledgerId" class="table-row">
          <span>{{ row.createdAt }}</span>
          <strong>{{ row.direction }}</strong>
          <span>{{ row.amount }}</span>
          <em>{{ row.bizType }}</em>
        </div>
      </div>
      <p v-else class="muted">暂无流水记录。</p>
    </section>
  </template>
</template>
