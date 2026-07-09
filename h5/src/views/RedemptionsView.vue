<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { ClientRedemption, WalletAccount } from '../types/client'

const accounts = ref<WalletAccount[]>([])
const redemptions = ref<ClientRedemption[]>([])
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const form = reactive({
  amount: '1.00',
})

const scAccount = computed(() => accounts.value.find((account) => account.currencyCode === 'SC'))
const canSubmit = computed(() => Number(form.amount) > 0 && !submitting.value)

async function loadRedemptions() {
  if (!sessionState.member) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [walletAccounts, rows] = await Promise.all([
      clientApi.walletAccounts(),
      clientApi.redemptions(),
    ])
    accounts.value = walletAccounts
    redemptions.value = rows
  } catch (err) {
    error.value = err instanceof Error ? err.message : '兑换加载失败'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!canSubmit.value) {
    return
  }
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    await clientApi.requestRedemption('SC', form.amount)
    success.value = '兑换申请已提交'
    await loadRedemptions()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '兑换申请失败'
  } finally {
    submitting.value = false
  }
}

onMounted(loadRedemptions)
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">兑换</p>
    <h1>提交审核后冻结余额</h1>
    <p class="muted">当前演示流程仅支持提交 SC 兑换申请。</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>请先登录</strong>
    <RouterLink class="btn primary" to="/login">登录</RouterLink>
  </section>

  <template v-else>
    <p v-if="loading" class="muted">正在加载兑换数据...</p>
    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="success" class="success-text">{{ success }}</p>

    <section class="redeem-panel">
      <div>
        <span>可用 SC</span>
        <strong>{{ scAccount?.availableBalance || '0.00' }}</strong>
        <small>冻结：{{ scAccount?.frozenBalance || '0.00' }}</small>
      </div>
      <form class="inline-form" @submit.prevent="submit">
        <input v-model="form.amount" inputmode="decimal" aria-label="兑换金额" />
        <button class="btn primary" type="submit" :disabled="!canSubmit">
          {{ submitting ? '提交中' : '提交兑换' }}
        </button>
      </form>
    </section>

    <section class="table-panel">
      <h2>最近兑换</h2>
      <div v-if="redemptions.length" class="table-list">
        <div v-for="item in redemptions" :key="item.orderNo || item.orderId" class="table-row">
          <span>{{ item.createdAt || item.orderNo }}</span>
          <strong>{{ item.amount }} {{ item.currencyCode }}</strong>
          <em>{{ item.status }}</em>
        </div>
      </div>
      <p v-else class="muted">暂无兑换记录。</p>
    </section>
  </template>
</template>
