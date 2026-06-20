<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { P1Operations, PurchaseOrder } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

const rows = ref<PurchaseOrder[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')

onMounted(loadOrders)

async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    const data = await apiGet<P1Operations>('/admin/p1/operations')
    rows.value = data.purchaseOrders
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function markPaid(row: PurchaseOrder) {
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPost<PurchaseOrder>(`/admin/purchase-orders/${row.orderId}/mark-paid`, { providerReference: `manual-${row.orderId}` })
    rows.value = rows.value.map((item) => item.orderId === updated.orderId ? updated : item)
    notice.value = i18n.t('admin.orderMarkedPaid', { orderId: updated.orderId })
  } catch (err) {
    error.value = messageFrom(err)
  }
}

function money(value: string | number, digits = 0) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('admin.purchaseOrderRequestFailed')
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">{{ $t('admin.commerceOperations') }}</p>
        <h1>{{ $t('admin.orders') }}</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('admin.loadingOrders') }}</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('common.order') }}</th>
              <th>{{ $t('admin.user') }}</th>
              <th>{{ $t('common.status') }}</th>
              <th>{{ $t('common.package') }}</th>
              <th>{{ $t('common.price') }}</th>
              <th>{{ $t('common.granted') }}</th>
              <th>{{ $t('common.provider') }}</th>
              <th>{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.orderId">
              <td data-label="Order"><strong>{{ row.orderId }}</strong><span>{{ row.createdAt }}</span></td>
              <td data-label="User">{{ row.userId }}</td>
              <td data-label="Status"><span class="status-tag" :class="{ active: row.status === 'paid', pending: row.status !== 'paid' }">{{ row.status }}</span></td>
              <td data-label="Package">{{ row.packageCode }}</td>
              <td data-label="Price">{{ money(row.priceAmount, 2) }} {{ row.priceCurrency }}</td>
              <td data-label="Granted">{{ money(row.amountGranted) }} {{ row.currencyGranted }}</td>
              <td data-label="Provider">{{ row.provider }}</td>
              <td data-label="Actions">
                <button :data-test="`mark-paid-${row.orderId}`" :disabled="row.status === 'paid'" @click="markPaid(row)">{{ $t('admin.markPaid') }}</button>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="8">{{ $t('admin.noPurchaseOrders') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
