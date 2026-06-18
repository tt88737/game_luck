<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { KycStatus, P1Operations } from '../../api/contracts'

const loading = ref(true)
const approving = ref<number | null>(null)
const error = ref('')
const message = ref('')
const data = ref<P1Operations | null>(null)

const pendingKyc = computed(() => data.value?.kycApplications.filter((row) => row.status === 'reviewing') ?? [])
const pendingRedemptions = computed(() => data.value?.redemptionRequests.filter((row) => row.status === 'reviewing') ?? [])

onMounted(loadOperations)

async function loadOperations() {
  loading.value = true
  error.value = ''
  try {
    data.value = await apiGet<P1Operations>('/admin/p1/operations')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function approveKyc(userId: number) {
  approving.value = userId
  error.value = ''
  message.value = ''
  try {
    const result = await apiPost<KycStatus>(`/admin/kyc/${userId}/approve`)
    message.value = `KYC approved for user ${result.userId}.`
    await loadOperations()
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    approving.value = null
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'P1 operations request failed.'
}

function amount(value: string | number, digits = 2) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}
</script>

<template>
  <main class="admin-shell">
    <aside class="admin-nav">
      <strong>Tang Luck Ops</strong>
      <RouterLink to="/admin">Dashboard</RouterLink>
      <RouterLink to="/admin/campaigns">Campaigns</RouterLink>
      <RouterLink to="/admin/p1">P1 Ops</RouterLink>
      <RouterLink to="/admin/audit-logs">Audit logs</RouterLink>
    </aside>

    <section class="admin-content">
      <header class="admin-header">
        <div>
          <p class="eyebrow">P1 sandbox</p>
          <h1>Purchase, KYC, redemption</h1>
        </div>
        <button @click="loadOperations">Refresh</button>
      </header>

      <section v-if="loading" class="status-panel">Loading P1 operations...</section>
      <section v-else-if="error && !data" class="status-panel danger">{{ error }}</section>

      <template v-else>
        <p v-if="message" class="notice success">{{ message }}</p>
        <p v-if="error" class="notice danger">{{ error }}</p>

        <section class="admin-metrics">
          <div><span>Purchase orders</span><strong>{{ data?.purchaseOrders.length ?? 0 }}</strong></div>
          <div><span>KYC reviewing</span><strong>{{ pendingKyc.length }}</strong></div>
          <div><span>Redemptions reviewing</span><strong>{{ pendingRedemptions.length }}</strong></div>
          <div><span>Mode</span><strong>Sandbox</strong></div>
        </section>

        <section class="section-block">
          <div class="section-title">
            <h2>KYC applications</h2>
            <span>Manual approval</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Name</th>
                  <th>Status</th>
                  <th>Updated</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in data?.kycApplications" :key="row.userId">
                  <td>{{ row.userId }}</td>
                  <td>{{ row.legalName ?? '-' }}</td>
                  <td><span class="status-tag" :class="{ active: row.status === 'approved', pending: row.status === 'reviewing' }">{{ row.status }}</span></td>
                  <td>{{ row.updatedAt ? new Date(row.updatedAt).toLocaleString() : '-' }}</td>
                  <td>
                    <button :disabled="row.status !== 'reviewing' || approving === row.userId" @click="approveKyc(row.userId)">
                      {{ approving === row.userId ? 'Approving' : 'Approve' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="!data?.kycApplications.length"><td colspan="5">No KYC applications.</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="section-block">
          <div class="section-title">
            <h2>Redemption requests</h2>
            <span>Review queue</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Request</th>
                  <th>User</th>
                  <th>SC</th>
                  <th>Method</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in data?.redemptionRequests" :key="row.redemptionId">
                  <td><code>{{ row.redemptionId }}</code></td>
                  <td>{{ row.userId }}</td>
                  <td>{{ amount(row.scAmount) }}</td>
                  <td>{{ row.method }}</td>
                  <td><span class="status-tag pending">{{ row.status }}</span></td>
                </tr>
                <tr v-if="!data?.redemptionRequests.length"><td colspan="5">No redemption requests.</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="section-block">
          <div class="section-title">
            <h2>Purchase orders</h2>
            <span>GC only</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Order</th>
                  <th>User</th>
                  <th>Package</th>
                  <th>Paid</th>
                  <th>Granted</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in data?.purchaseOrders" :key="row.orderId">
                  <td><code>{{ row.orderId }}</code><span>{{ row.status }} · {{ row.provider }}</span></td>
                  <td>{{ row.userId }}</td>
                  <td>{{ row.packageCode }}</td>
                  <td>{{ amount(row.priceAmount) }} {{ row.priceCurrency }}</td>
                  <td>{{ amount(row.amountGranted, 0) }} {{ row.currencyGranted }}</td>
                </tr>
                <tr v-if="!data?.purchaseOrders.length"><td colspan="5">No purchase orders.</td></tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </section>
  </main>
</template>
