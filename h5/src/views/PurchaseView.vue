<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { ClientPurchaseOffer, ClientPurchaseOrder, WalletAccount } from '../types/client'

const offers = ref<ClientPurchaseOffer[]>([])
const accounts = ref<WalletAccount[]>([])
const lastOrder = ref<ClientPurchaseOrder | null>(null)
const loading = ref(false)
const walletLoading = ref(false)
const payingId = ref<number | null>(null)
const error = ref('')
const success = ref('')

const paymentEnabled = computed(() => Boolean(sessionState.bootstrap?.features.paymentEnabled))
const gcBalance = computed(() => accounts.value.find((item) => item.currencyCode === 'GC')?.availableBalance || '0')
const scBalance = computed(() => accounts.value.find((item) => item.currencyCode === 'SC')?.availableBalance || '0')

async function loadOffers() {
  if (!paymentEnabled.value) {
    offers.value = []
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    offers.value = await clientApi.purchaseOffers()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '购买产品加载失败'
  } finally {
    loading.value = false
  }
}

async function loadWallet() {
  if (!sessionState.member) {
    accounts.value = []
    return
  }
  walletLoading.value = true
  try {
    accounts.value = await clientApi.walletAccounts()
  } finally {
    walletLoading.value = false
  }
}

async function load() {
  await loadWallet()
}

function grantText(offer: ClientPurchaseOffer) {
  return offer.grantItems.map((item) => `${item.grantAmount} ${item.currencyCode}`).join(' + ')
}

function turnoverText(item: ClientPurchaseOffer['grantItems'][number]) {
  if (!item.requiredTurnover || Number(item.requiredTurnover) <= 0) {
    return `${item.currencyCode} 无流水要求`
  }
  const scope = item.gameScopeType === 'ALL' ? '全部游戏' : item.gameScopeValue || '指定游戏'
  return `${item.currencyCode} 需完成 ${item.requiredTurnover} 流水，${scope}可核销`
}

function idempotencyKey(offerId: number) {
  return `h5-${offerId}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function sessionRequestKey(orderNo: string) {
  const storageKey = `payment-session:${orderNo}`
  const existing = sessionStorage.getItem(storageKey)
  if (existing) return existing
  const value = globalThis.crypto?.randomUUID?.() || `h5-session-${orderNo}-${Date.now()}`
  sessionStorage.setItem(storageKey, value)
  return value
}

async function pay(offer: ClientPurchaseOffer) {
  if (!sessionState.member || payingId.value) {
    return
  }
  payingId.value = offer.offerId
  error.value = ''
  success.value = ''
  try {
    const order = await clientApi.payPurchaseOffer(offer.offerId, idempotencyKey(offer.offerId))
    lastOrder.value = order
    const paymentSession = await clientApi.createPaymentSession(order.orderNo, sessionRequestKey(order.orderNo))
    success.value = `订单 ${order.orderNo} 已创建，正在前往支付`
    window.location.assign(paymentSession.checkoutUrl)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '购买失败'
  } finally {
    payingId.value = null
  }
}

onMounted(load)
watch(() => sessionState.member?.memberId, () => loadWallet())
watch(paymentEnabled, (enabled) => {
  if (enabled) {
    loadOffers()
  } else {
    offers.value = []
    error.value = ''
  }
}, { immediate: true })
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">购买</p>
    <h1>购买 GC 套餐</h1>
    <p class="muted">支付成功后自动入账，SC 奖励按产品规则生成对应流水要求。</p>
  </section>

  <section class="purchase-summary">
    <div>
      <span>GC 余额</span>
      <strong>{{ walletLoading ? '...' : gcBalance }}</strong>
    </div>
    <div>
      <span>SC 余额</span>
      <strong>{{ walletLoading ? '...' : scBalance }}</strong>
    </div>
    <RouterLink class="btn compact" to="/wallet">查看钱包</RouterLink>
  </section>

  <section v-if="!paymentEnabled" class="empty-state">
    <strong>购买暂未开放</strong>
    <p class="muted">当前品牌未开启购买功能。</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>登录后购买</strong>
    <p class="muted">登录后可购买 GC 套餐，SC 奖励会按规则入账。</p>
    <RouterLink class="btn primary" to="/login">登录</RouterLink>
  </section>

  <p v-if="error" class="error-text">{{ error }}</p>
  <p v-if="success" class="success-text">{{ success }}</p>
  <p v-if="loading" class="muted">正在加载购买产品...</p>

  <section v-if="!loading && paymentEnabled && offers.length" class="purchase-grid">
    <article v-for="offer in offers" :key="offer.offerId" class="purchase-card">
      <div class="coin-stack" aria-hidden="true">
        <span>GC</span>
        <span>SC</span>
      </div>
      <div class="purchase-main">
        <small>{{ offer.offerType }}</small>
        <h2>{{ offer.offerName }}</h2>
        <p class="pay-line">{{ offer.payAmount }} {{ offer.payCurrencyCode }}</p>
        <p class="grant-line">到账 {{ grantText(offer) }}</p>
      </div>
      <ul class="rule-list">
        <li v-for="item in offer.grantItems" :key="`${offer.offerId}-${item.currencyCode}-${item.grantType}`">
          {{ turnoverText(item) }}
        </li>
      </ul>
      <button
        class="btn primary"
        :disabled="!sessionState.member || payingId === offer.offerId"
        @click="pay(offer)"
      >
        {{ payingId === offer.offerId ? '购买中...' : '立即购买' }}
      </button>
    </article>
  </section>

  <section v-else-if="!loading && paymentEnabled" class="empty-state">
    <strong>暂无可购买产品</strong>
    <p class="muted">运营开启购买产品后会展示在这里。</p>
  </section>

  <section v-if="lastOrder" class="order-result">
    <div>
      <p class="eyebrow">最近订单</p>
      <h2>{{ lastOrder.orderNo }}</h2>
      <p class="muted">{{ lastOrder.status }} · {{ lastOrder.payAmount }} {{ lastOrder.payCurrencyCode }}</p>
    </div>
    <div class="reward-pills">
      <span v-for="item in lastOrder.grantItems" :key="`${lastOrder.orderNo}-${item.currencyCode}-${item.grantType}`">
        {{ item.grantAmount }} {{ item.currencyCode }}
      </span>
    </div>
  </section>
</template>

<style scoped>
.purchase-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  gap: 12px;
  align-items: stretch;
  margin-bottom: 18px;
}

.purchase-summary div,
.purchase-card,
.order-result {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.purchase-summary div {
  display: grid;
  gap: 6px;
  min-height: 88px;
  padding: 16px;
}

.purchase-summary span,
.purchase-card small {
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}

.purchase-summary strong {
  color: var(--surface-strong);
  font-size: 24px;
}

.purchase-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}

.purchase-card {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.coin-stack {
  display: flex;
  align-items: center;
  min-height: 54px;
}

.coin-stack span {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border: 2px solid #d5a447;
  border-radius: 50%;
  background: #fff4d8;
  color: #6c4810;
  font-size: 14px;
  font-weight: 900;
}

.coin-stack span + span {
  margin-left: -12px;
  border-color: #3aa987;
  background: #e9fff6;
  color: var(--brand-strong);
}

.purchase-main {
  display: grid;
  gap: 8px;
}

.pay-line {
  color: var(--surface-strong);
  font-size: 26px;
  font-weight: 850;
}

.grant-line {
  color: var(--brand-strong);
  font-weight: 800;
}

.rule-list {
  display: grid;
  gap: 8px;
  min-height: 68px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.rule-list li {
  padding: 8px 10px;
  border: 1px solid #eee5d7;
  border-radius: 7px;
  color: var(--muted);
  line-height: 1.45;
}

.order-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  padding: 18px;
}

@media (max-width: 760px) {
  .page-heading h1,
  .page-heading .muted {
    max-width: 100%;
    overflow-wrap: anywhere;
  }

  .purchase-summary {
    grid-template-columns: 1fr 1fr;
  }

  .purchase-summary .btn {
    grid-column: 1 / -1;
  }

  .order-result {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
