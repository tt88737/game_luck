<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clientApi } from '../api/client'
import type { PaymentWebhookAck, SimulatedCheckout, SimulatedPaymentAction } from '../types/client'

const route = useRoute()
const router = useRouter()
const checkout = ref<SimulatedCheckout | null>(null)
const acknowledgement = ref<PaymentWebhookAck | null>(null)
const loading = ref(true)
const acting = ref<SimulatedPaymentAction | 'REPLAY' | null>(null)
const error = ref('')

const providerSessionNo = computed(() => String(route.params.providerSessionNo || ''))
const actionText: Record<SimulatedPaymentAction, string> = {
  PAYMENT_SUCCEEDED: '模拟支付成功',
  PAYMENT_FAILED: '模拟支付失败',
  PAYMENT_CANCELLED: '取消支付',
  REFUND_SUCCEEDED: '模拟退款成功',
  CHARGEBACK_CREATED: '模拟拒付',
}

async function loadCheckout() {
  loading.value = true
  error.value = ''
  try {
    checkout.value = await clientApi.simulatedCheckout(providerSessionNo.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '支付会话加载失败'
  } finally {
    loading.value = false
  }
}

async function runAction(action: SimulatedPaymentAction) {
  acting.value = action
  error.value = ''
  try {
    acknowledgement.value = await clientApi.executeSimulatedPaymentAction(providerSessionNo.value, action)
    await router.replace(`/purchase-result/${encodeURIComponent(checkout.value!.sessionNo)}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '支付操作失败'
  } finally {
    acting.value = null
  }
}

async function replay() {
  acting.value = 'REPLAY'
  error.value = ''
  try {
    acknowledgement.value = await clientApi.replaySimulatedPayment(providerSessionNo.value)
    await loadCheckout()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '回放失败'
  } finally {
    acting.value = null
  }
}

onMounted(loadCheckout)
</script>

<template>
  <section class="page-heading checkout-heading">
    <p class="eyebrow">SIMULATED PROVIDER</p>
    <h1>托管支付结算</h1>
    <p class="muted">此页面模拟外部支付方，仅执行服务端允许的测试结果。</p>
  </section>

  <p v-if="loading" class="muted">正在加载支付会话...</p>
  <p v-if="error" class="error-text">{{ error }}</p>

  <section v-if="checkout" class="checkout-panel">
    <header>
      <div><span>应付金额</span><strong>{{ checkout.payAmount }} {{ checkout.payCurrencyCode }}</strong></div>
      <span class="status-chip">{{ checkout.status }}</span>
    </header>
    <dl class="checkout-facts">
      <div><dt>订单号</dt><dd>{{ checkout.orderNo }}</dd></div>
      <div><dt>平台会话</dt><dd>{{ checkout.sessionNo }}</dd></div>
      <div><dt>支付方会话</dt><dd>{{ checkout.providerSessionNo }}</dd></div>
      <div><dt>有效期至</dt><dd>{{ checkout.expireTime }}</dd></div>
    </dl>
    <div class="checkout-actions">
      <button
        v-for="action in checkout.allowedActions"
        :key="action"
        class="btn"
        :class="action === 'PAYMENT_SUCCEEDED' ? 'primary' : ''"
        :disabled="acting !== null"
        @click="runAction(action)"
      >{{ acting === action ? '处理中...' : actionText[action] }}</button>
    </div>
    <button v-if="checkout.latestProviderEventId" class="text-action" :disabled="acting !== null" @click="replay">
      {{ acting === 'REPLAY' ? '回放中...' : '回放最近一次回调' }}
    </button>
    <p v-if="acknowledgement" class="success-text">回调 {{ acknowledgement.providerEventId }}：{{ acknowledgement.status }}</p>
  </section>
</template>

<style scoped>
.checkout-heading { max-width: 720px; }
.checkout-panel { max-width: 720px; border: 1px solid var(--line); border-radius: 8px; background: var(--surface); padding: 22px; }
.checkout-panel header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding-bottom: 18px; border-bottom: 1px solid var(--line); }
.checkout-panel header div { display: grid; gap: 6px; }
.checkout-panel header span, dt { color: var(--muted); font-size: 13px; font-weight: 700; }
.checkout-panel header strong { font-size: 28px; color: var(--surface-strong); }
.status-chip { padding: 5px 8px; border: 1px solid var(--line); border-radius: 6px; }
.checkout-facts { display: grid; gap: 0; margin: 18px 0; }
.checkout-facts div { display: grid; grid-template-columns: 120px minmax(0, 1fr); gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--line); }
.checkout-facts dd { margin: 0; overflow-wrap: anywhere; }
.checkout-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.text-action { margin-top: 18px; border: 0; background: transparent; color: var(--brand-strong); cursor: pointer; }
@media (max-width: 760px) {
  .checkout-panel { padding: 16px; }
  .checkout-panel header { flex-direction: column; }
  .checkout-facts div { grid-template-columns: 1fr; gap: 4px; }
  .checkout-actions .btn { width: 100%; }
}
</style>
