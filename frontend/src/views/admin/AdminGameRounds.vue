<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet } from '../../api/http'
import type { SlotRound, SlotRoundPage } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'

const rows = ref<SlotRound[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')

onMounted(loadRounds)

async function loadRounds() {
  loading.value = true
  error.value = ''
  try {
    const page = await apiGet<SlotRoundPage>('/admin/game-rounds')
    rows.value = page.items
    total.value = page.total
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

function money(value: string | number, digits = 0) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Game round request failed.'
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">Round ledger audit</p>
        <h1>Game Rounds</h1>
      </div>
      <button :disabled="loading" @click="loadRounds">Refresh</button>
    </header>

    <div class="filter-bar">
      <span>{{ total }} records</span>
      <span>Wallet debit and payout ledger IDs are reconciled per round.</span>
    </div>

    <section v-if="loading" class="status-panel">Loading game rounds...</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Round</th>
              <th>User</th>
              <th>Game</th>
              <th>Bet</th>
              <th>Payout</th>
              <th>Multiplier</th>
              <th>Ledgers</th>
              <th>Status</th>
              <th>Time</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.roundId">
              <td data-label="Round"><strong>{{ row.roundId }}</strong></td>
              <td data-label="User">{{ row.userId }}</td>
              <td data-label="Game">{{ row.gameCode }}</td>
              <td data-label="Bet">{{ money(row.betAmount) }} {{ row.currency }}</td>
              <td data-label="Payout">{{ money(row.payoutAmount) }} {{ row.currency }}</td>
              <td data-label="Multiplier">x{{ Number(row.multiplier).toFixed(2) }}</td>
              <td data-label="Ledgers">ledger {{ row.debitLedgerId || '-' }} / ledger {{ row.creditLedgerId || '-' }}</td>
              <td data-label="Status"><span class="status-tag active">{{ row.status }}</span></td>
              <td data-label="Time">{{ row.createdAt }}</td>
            </tr>
            <tr v-if="!rows.length"><td colspan="9">No game rounds.</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
