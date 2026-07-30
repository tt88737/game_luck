<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { clientApi } from '../api/client'
import type { ClientPaymentSession } from '../types/client'

const route = useRoute()
const paymentSession = ref<ClientPaymentSession | null>(null)
const error = ref('')
const attempts = ref(0)
const walletRefreshed = ref(false)
let pollTimer: ReturnType<typeof setTimeout> | undefined
const terminal = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED'])
const sessionNo = computed(() => String(route.params.sessionNo || ''))
const effectiveStatus = computed(() => {
  const session = paymentSession.value
  if (session && ['CREATED', 'PENDING'].includes(session.status)
    && session.expireTime && Date.parse(session.expireTime) <= Date.now()) return 'EXPIRED'
  return session?.status || ''
})

const resultTitles: Record<string, string> = {
  SUCCEEDED: '支付成功',
  FAILED: '支付失败',
  CANCELLED: '支付已取消',
  EXPIRED: '支付会话已过期',
}
const title = computed(() => resultTitles[effectiveStatus.value] || '正在确认支付结果')

async function poll() {
  error.value = ''
  try {
    paymentSession.value = await clientApi.paymentSession(sessionNo.value)
    if (paymentSession.value.status === 'SUCCEEDED' && !walletRefreshed.value) {
      await clientApi.walletAccounts()
      walletRefreshed.value = true
    }
    if (!terminal.has(effectiveStatus.value) && attempts.value < 40) {
      attempts.value += 1
      pollTimer = setTimeout(poll, 1500)
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '支付状态查询失败'
  }
}

onMounted(poll)
onUnmounted(() => { if (pollTimer) clearTimeout(pollTimer) })
</script>

<template>
  <section class="result-panel" :class="effectiveStatus.toLowerCase()">
    <p class="eyebrow">支付结果</p>
    <h1>{{ title }}</h1>
    <p v-if="!paymentSession" class="muted">正在从平台确认支付状态...</p>
    <dl v-else>
      <div><dt>平台会话</dt><dd>{{ paymentSession.sessionNo }}</dd></div>
      <div><dt>订单号</dt><dd>{{ paymentSession.orderNo }}</dd></div>
      <div><dt>金额</dt><dd>{{ paymentSession.payAmount }} {{ paymentSession.payCurrencyCode }}</dd></div>
      <div><dt>状态</dt><dd>{{ effectiveStatus }}</dd></div>
    </dl>
    <p v-if="error" class="error-text">{{ error }}</p>
    <div class="result-actions">
      <button v-if="error" class="btn" @click="poll">重新查询</button>
      <RouterLink class="btn primary" to="/purchase">返回购买</RouterLink>
      <RouterLink v-if="paymentSession?.status === 'SUCCEEDED'" class="btn" to="/wallet">查看钱包</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.result-panel { max-width: 680px; border: 1px solid var(--line); border-top: 4px solid #8a929c; border-radius: 8px; background: var(--surface); padding: 24px; }
.result-panel.succeeded { border-top-color: var(--brand-strong); }
.result-panel.failed, .result-panel.expired { border-top-color: #c44949; }
.result-panel h1 { margin: 6px 0 20px; font-size: 28px; }
.result-panel dl { display: grid; margin: 0 0 20px; }
.result-panel dl div { display: grid; grid-template-columns: 120px minmax(0, 1fr); gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--line); }
.result-panel dt { color: var(--muted); font-weight: 700; }
.result-panel dd { margin: 0; overflow-wrap: anywhere; }
.result-actions { display: flex; flex-wrap: wrap; gap: 10px; }
@media (max-width: 760px) {
  .result-panel { padding: 18px; }
  .result-panel dl div { grid-template-columns: 1fr; gap: 4px; }
  .result-actions .btn { flex: 1 1 140px; }
}
</style>
