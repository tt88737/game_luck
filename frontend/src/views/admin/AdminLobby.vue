<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPatch } from '../../api/http'
import type { LobbyCard } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

const rows = ref<LobbyCard[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')

onMounted(loadCards)

async function loadCards() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<LobbyCard[]>('/admin/lobby-cards')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function setStatus(row: LobbyCard, status: string) {
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPatch<LobbyCard>(`/admin/lobby-cards/${row.cardCode}`, { ...row, status })
    rows.value = rows.value.map((item) => item.cardCode === updated.cardCode ? updated : item)
    notice.value = i18n.t('admin.lobbyCardUpdated', { code: updated.cardCode })
  } catch (err) {
    error.value = messageFrom(err)
  }
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('admin.lobbyCardRequestFailed')
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">{{ $t('admin.lobbyConfiguration') }}</p>
        <h1>{{ $t('admin.lobby') }}</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('admin.loadingLobby') }}</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('admin.card') }}</th>
              <th>{{ $t('common.status') }}</th>
              <th>{{ $t('common.target') }}</th>
              <th>{{ $t('common.sort') }}</th>
              <th>{{ $t('common.asset') }}</th>
              <th>{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.cardCode">
              <td><strong>{{ row.title }}</strong><span>{{ row.cardCode }} · {{ row.subtitle }}</span></td>
              <td><span class="status-tag" :class="{ active: row.status === 'active', paused: row.status !== 'active' }">{{ row.status }}</span></td>
              <td><code>{{ row.targetUrl }}</code></td>
              <td>{{ row.sortOrder }}</td>
              <td><code>{{ row.imageUrl }}</code></td>
              <td>
                <div class="action-group">
                  <button :data-test="`activate-card-${row.cardCode}`" :disabled="row.status === 'active'" @click="setStatus(row, 'active')">{{ $t('common.activate') }}</button>
                  <button :data-test="`pause-card-${row.cardCode}`" :disabled="row.status === 'paused'" @click="setStatus(row, 'paused')">{{ $t('common.pause') }}</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="6">{{ $t('admin.noLobbyCards') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
