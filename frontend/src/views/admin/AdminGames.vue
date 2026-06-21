<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPatch } from '../../api/http'
import type { SlotGame } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const rows = ref<SlotGame[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')

onMounted(loadGames)

async function loadGames() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<SlotGame[]>('/admin/games')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function setStatus(row: SlotGame, status: string) {
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPatch<SlotGame>(`/admin/games/${row.gameCode}`, {
      name: row.name,
      status,
      minBet: row.minBet,
      maxBet: row.maxBet,
      sortOrder: row.sortOrder,
      legalApprovalId: row.legalApprovalId,
    })
    rows.value = rows.value.map((item) => item.gameCode === updated.gameCode ? updated : item)
    notice.value = `${updated.gameCode} updated. Audit log created.`
  } catch (err) {
    error.value = messageFrom(err)
  }
}

function money(value: string | number, digits = 0) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Game request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Game configuration</p>
        <h1>Games</h1>
      </div>
    </header>

    <section v-if="loading" class="status-panel">Loading games...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Game</th>
              <th>Status</th>
              <th>Currency</th>
              <th>Bet limits</th>
              <th>Reels</th>
              <th>Legal approval</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.gameCode">
              <td data-label="Game"><strong>{{ row.name }}</strong><span>{{ row.gameCode }}</span></td>
              <td data-label="Status"><span class="status-tag" :class="{ active: row.status === 'active', paused: row.status !== 'active' }">{{ row.status }}</span></td>
              <td data-label="Currency">{{ row.currency }}</td>
              <td data-label="Bet limits">{{ money(row.minBet) }}-{{ money(row.maxBet) }}</td>
              <td data-label="Reels">{{ row.reelCount }} x {{ row.rowCount }}</td>
              <td data-label="Legal">{{ row.legalApprovalId || 'Missing' }}</td>
              <td data-label="Actions">
                <div class="action-group">
                  <button :data-test="`activate-game-${row.gameCode}`" :disabled="row.status === 'active'" @click="setStatus(row, 'active')">Activate</button>
                  <button :data-test="`pause-game-${row.gameCode}`" :disabled="row.status === 'paused'" @click="setStatus(row, 'paused')">Pause</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="7">No games configured.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
