<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPatch } from '../../api/http'
import type { AdminRegion } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

const rows = ref<AdminRegion[]>([])
const loading = ref(true)
const saving = ref('')
const error = ref('')
const notice = ref('')

onMounted(loadRegions)

async function loadRegions() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<AdminRegion[]>('/admin/regions')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function togglePurchase(row: AdminRegion) {
  saving.value = key(row)
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPatch<AdminRegion>(`/admin/regions/${row.countryCode}/${row.stateCode}`, {
      ...row,
      purchaseAllowed: !row.purchaseAllowed,
      legalApprovalId: row.purchaseAllowed ? `${row.legalApprovalId ?? 'LEGAL'}-OFF` : row.legalApprovalId,
    })
    rows.value = rows.value.map((item) => key(item) === key(updated) ? updated : item)
    notice.value = i18n.t('admin.regionUpdated', { region: key(updated) })
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    saving.value = ''
  }
}

function key(row: AdminRegion) {
  return `${row.countryCode}-${row.stateCode}`
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('admin.regionRequestFailed')
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">{{ $t('admin.complianceConfiguration') }}</p>
        <h1>{{ $t('admin.regions') }}</h1>
      </div>
      <button :disabled="loading" @click="loadRegions">{{ $t('common.refresh') }}</button>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('admin.loadingRegions') }}</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('common.region') }}</th>
              <th>{{ $t('common.status') }}</th>
              <th>{{ $t('common.register') }}</th>
              <th>{{ $t('common.buy') }}</th>
              <th>SC grant</th>
              <th>{{ $t('nav.redeem') }}</th>
              <th>AMOE</th>
              <th>{{ $t('admin.legalApproval') }}</th>
              <th>{{ $t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="key(row)">
              <td><strong>{{ key(row) }}</strong></td>
              <td><span class="status-tag" :class="{ active: row.status === 'active', paused: row.status !== 'active' }">{{ row.status }}</span></td>
              <td>{{ row.registrationAllowed ? $t('common.allowed') : $t('common.blocked') }}</td>
              <td>{{ row.purchaseAllowed ? $t('common.allowed') : $t('common.blocked') }}</td>
              <td>{{ row.scGrantAllowed ? $t('common.allowed') : $t('common.blocked') }}</td>
              <td>{{ row.redemptionAllowed ? $t('common.allowed') : $t('common.blocked') }}</td>
              <td>{{ row.amoeAllowed ? $t('common.allowed') : $t('common.blocked') }}</td>
              <td>{{ row.legalApprovalId ?? '-' }}</td>
              <td>
                <button :data-test="`toggle-purchase-${key(row)}`" :disabled="saving === key(row)" @click="togglePurchase(row)">
                  {{ row.purchaseAllowed ? $t('common.blockPurchase') : $t('common.allowPurchase') }}
                </button>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="9">{{ $t('admin.noRegionConfiguration') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
