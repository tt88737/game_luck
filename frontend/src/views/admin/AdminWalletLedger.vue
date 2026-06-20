<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { AdminWalletLedger } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const rows = ref<AdminWalletLedger[]>([])
const loading = ref(true)
const error = ref('')

onMounted(loadLedger)

async function loadLedger() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<AdminWalletLedger[]>('/admin/wallet-ledger')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

function money(value: string | number, digits = 2) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Wallet ledger request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Wallet audit</p>
        <h1>Wallet Ledger</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">Loading wallet ledger...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Ledger</th>
              <th>User</th>
              <th>Currency</th>
              <th>Direction</th>
              <th>Amount</th>
              <th>Business</th>
              <th>Status</th>
              <th>Time</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.ledgerId">
              <td data-label="Ledger"><strong>#{{ row.ledgerId }}</strong><span>balance {{ money(row.balanceAfter) }}</span></td>
              <td data-label="User">{{ row.userId }}</td>
              <td data-label="Currency">{{ row.currency }}</td>
              <td data-label="Direction">{{ row.direction }}</td>
              <td data-label="Amount">{{ money(row.amount) }}</td>
              <td data-label="Business">{{ row.businessType }}<span>{{ row.businessId }}</span></td>
              <td data-label="Status"><span class="status-tag active">{{ row.status }}</span></td>
              <td data-label="Time">{{ new Date(row.createdAt).toLocaleString() }}</td>
            </tr>
            <tr v-if="!rows.length"><td colspan="8">No wallet ledger records.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
