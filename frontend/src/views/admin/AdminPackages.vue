<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPatch } from '../../api/http'
import type { ProductPackage } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const rows = ref<ProductPackage[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')

onMounted(loadPackages)

async function loadPackages() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<ProductPackage[]>('/admin/product-packages')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function setStatus(row: ProductPackage, status: string) {
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPatch<ProductPackage>(`/admin/product-packages/${row.packageCode}`, { ...row, status })
    rows.value = rows.value.map((item) => item.packageCode === updated.packageCode ? updated : item)
    notice.value = `${updated.packageCode} updated. Audit log created.`
  } catch (err) {
    error.value = messageFrom(err)
  }
}

function money(value: string | number, digits = 2) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Product package request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Commerce configuration</p>
        <h1>Packages</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">Loading product packages...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Package</th>
              <th>Status</th>
              <th>Price</th>
              <th>GC</th>
              <th>Provider</th>
              <th>Legal</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.packageCode">
              <td data-label="Package"><strong>{{ row.name }}</strong><span>{{ row.packageCode }}</span></td>
              <td data-label="Status"><span class="status-tag" :class="{ active: row.status === 'active', paused: row.status !== 'active' }">{{ row.status }}</span></td>
              <td data-label="Price">{{ money(row.priceAmount) }} {{ row.priceCurrency }}</td>
              <td data-label="GC">{{ money(row.gcAmount, 0) }}</td>
              <td data-label="Provider">{{ row.provider }}</td>
              <td data-label="Legal">{{ row.legalApprovalId || 'Missing' }}</td>
              <td data-label="Actions">
                <div class="action-group">
                  <button :data-test="`activate-package-${row.packageCode}`" :disabled="row.status === 'active'" @click="setStatus(row, 'active')">Activate</button>
                  <button :data-test="`pause-package-${row.packageCode}`" :disabled="row.status === 'paused'" @click="setStatus(row, 'paused')">Pause</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="7">No product packages.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
