<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { ProductPackage, PurchaseOrder } from '../../api/contracts'
import { i18n } from '../../i18n'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const loading = ref(true)
const buying = ref('')
const error = ref('')
const success = ref('')
const packages = ref<ProductPackage[]>([])
const lastOrder = ref<PurchaseOrder | null>(null)

onMounted(loadPackages)

async function loadPackages() {
  loading.value = true
  error.value = ''
  try {
    packages.value = await apiGet<ProductPackage[]>('/purchase/packages')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function buyPackage(item: ProductPackage) {
  if (session.isGuest) {
    openBindAccount()
    return
  }
  buying.value = item.packageCode
  error.value = ''
  success.value = ''
  try {
    const order = await apiPost<PurchaseOrder>(
      '/purchase/orders',
      { packageCode: item.packageCode },
      `web-purchase-${item.packageCode}-${Date.now()}`,
    )
    lastOrder.value = order
    success.value = order.status === 'paid'
      ? i18n.t('store.purchaseSuccess', { amount: format(order.amountGranted, 0) })
      : 'Order is waiting for payment confirmation.'
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    buying.value = ''
  }
}

function openBindAccount() {
  window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: { mode: 'register' } }))
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('store.purchaseFailed')
}

function format(value: string | number, digits: number) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">{{ $t('store.gcPackages') }}</p>
        <h1>{{ $t('store.heading') }}</h1>
      </div>
      <RouterLink class="plain-link" to="/me/wallet">{{ $t('nav.ledger') }}</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('store.loading') }}</section>
    <section v-else-if="error && !packages.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p class="notice">{{ $t('store.notice') }}</p>
      <section v-if="session.isGuest" class="guest-gate">
        <div>
          <strong>{{ $t('guestGate.storeTitle') }}</strong>
          <span>{{ $t('guestGate.storeBody') }}</span>
        </div>
        <button type="button" class="small-action" @click="openBindAccount">{{ $t('auth.bindAccount') }}</button>
      </section>
      <p v-if="success" class="notice success">{{ success }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>{{ $t('common.packages') }}</h2>
          <span>{{ $t('store.activeCount', { count: packages.length }) }}</span>
        </div>
        <div class="game-card-grid">
          <article v-for="item in packages" :key="item.packageCode" class="section-block store-card">
            <div class="reward-row">
              <div>
                <strong>{{ item.name }}</strong>
                <span>{{ format(item.gcAmount, 0) }} GC · {{ format(item.priceAmount, 2) }} {{ item.priceCurrency }}</span>
              </div>
              <button :data-test="`buy-${item.packageCode}`" :disabled="session.isGuest || buying === item.packageCode" @click="buyPackage(item)">
                {{ buying === item.packageCode ? $t('store.buying') : $t('common.buy') }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section v-if="lastOrder" class="section-block">
        <div class="section-title">
          <h2>{{ $t('common.latestOrder') }}</h2>
          <span class="status-tag" :class="{ active: lastOrder.status === 'paid', pending: lastOrder.status !== 'paid' }">{{ lastOrder.status }}</span>
        </div>
        <div class="source-list">
          <span>{{ $t('common.order') }}: {{ lastOrder.orderId }}</span>
          <span>{{ $t('common.provider') }}: {{ lastOrder.provider }}</span>
          <span>{{ $t('common.credited') }}: {{ format(lastOrder.amountGranted, 0) }} {{ lastOrder.currencyGranted }}</span>
          <span v-if="lastOrder.status !== 'paid'">Payment confirmation required before GC is credited.</span>
        </div>
      </section>
    </template>
  </main>
</template>
