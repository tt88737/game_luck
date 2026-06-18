<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { ProductPackage, PurchaseOrder } from '../../api/contracts'

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
    success.value = `Sandbox order paid. ${format(order.amountGranted, 0)} GC credited.`
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    buying.value = ''
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Purchase request failed.'
}

function format(value: string | number, digits: number) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}
</script>

<template>
  <main class="app-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">P1 Store</p>
        <h1>GC sandbox packs</h1>
      </div>
      <RouterLink class="plain-link" to="/app/wallet">Ledger</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">Loading GC packages...</section>
    <section v-else-if="error && !packages.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p class="notice">Sandbox mode: orders are marked paid by the demo backend. GC is credited only; SC is never sold here.</p>
      <p v-if="success" class="notice success">{{ success }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <section class="section-block">
        <div class="section-title">
          <h2>Packages</h2>
          <span>{{ packages.length }} active</span>
        </div>
        <article v-for="item in packages" :key="item.packageCode" class="reward-row">
          <div>
            <strong>{{ item.name }}</strong>
            <span>{{ format(item.gcAmount, 0) }} GC · {{ format(item.priceAmount, 2) }} {{ item.priceCurrency }}</span>
          </div>
          <button :data-test="`buy-${item.packageCode}`" :disabled="buying === item.packageCode" @click="buyPackage(item)">
            {{ buying === item.packageCode ? 'Paying' : 'Buy' }}
          </button>
        </article>
      </section>

      <section v-if="lastOrder" class="section-block">
        <div class="section-title">
          <h2>Latest order</h2>
          <span class="status-tag active">{{ lastOrder.status }}</span>
        </div>
        <div class="source-list">
          <span>Order: {{ lastOrder.orderId }}</span>
          <span>Provider: {{ lastOrder.provider }}</span>
          <span>Credited: {{ format(lastOrder.amountGranted, 0) }} {{ lastOrder.currencyGranted }}</span>
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
