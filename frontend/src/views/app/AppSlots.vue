<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { ActivitySummary, SlotGame, SlotRound, SlotRoundPage, WalletSummary } from '../../api/contracts'
import { useSessionStore } from '../../stores/session'

const route = useRoute()
const session = useSessionStore()
const gameCode = computed(() => String(route?.params?.gameCode || 'lucky_slots'))
const loading = ref(true)
const spinning = ref(false)
const error = ref('')
const result = ref<SlotRound | null>(null)
const game = ref<SlotGame | null>(null)
const wallet = ref<WalletSummary | null>(null)
const rounds = ref<SlotRound[]>([])
const activity = ref<ActivitySummary | null>(null)
const betAmount = ref('10.0000')

const gcBalance = computed(() => Number(wallet.value?.wallet.gcBalance ?? 0))
const canSpin = computed(() => !!session.userId && !!game.value && !spinning.value && gcBalance.value >= Number(betAmount.value))
const visibleReels = computed(() => result.value?.reels?.length ? result.value.reels : rounds.value[0]?.reels?.length ? rounds.value[0].reels : defaultReels())

onMounted(loadSlots)

async function loadSlots() {
  loading.value = true
  error.value = ''
  try {
    const [games, summary, history] = await Promise.all([
      apiGet<SlotGame[]>('/slots/games'),
      apiGet<WalletSummary>('/wallet/summary'),
      apiGet<SlotRoundPage>('/slots/rounds'),
    ])
    game.value = games.find((item) => item.gameCode === gameCode.value) ?? games[0] ?? null
    wallet.value = summary
    rounds.value = history.items
    activity.value = await apiGet<ActivitySummary>('/player/activity-summary').catch(() => null)
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function spin() {
  if (!canSpin.value || !game.value) return
  spinning.value = true
  error.value = ''
  try {
    const round = await apiPost<SlotRound>(`/slots/${game.value.gameCode}/spin`, {
      currency: 'GC',
      betAmount: betAmount.value,
    }, `web-slot-${game.value.gameCode}-${Date.now()}`)
    result.value = round
    rounds.value = [round, ...rounds.value.filter((item) => item.roundId !== round.roundId)].slice(0, 10)
    await Promise.all([refreshWallet(), refreshActivity()])
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    spinning.value = false
  }
}

async function refreshWallet() {
  wallet.value = await apiGet<WalletSummary>('/wallet/summary')
}

async function refreshActivity() {
  activity.value = await apiGet<ActivitySummary>('/player/activity-summary').catch(() => activity.value)
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return 'Slots request failed.'
}

function money(value: string | number, digits = 0) {
  return Number(value).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

function symbolLabel(value: string) {
  const map: Record<string, string> = { coin: 'C', seven: '7', bar: 'B', cherry: 'Ch', lemon: 'L' }
  return map[value] ?? value.slice(0, 2).toUpperCase()
}

function defaultReels() {
  return [
    ['coin', 'seven', 'coin'],
    ['bar', 'coin', 'lemon'],
    ['cherry', 'seven', 'coin'],
    ['coin', 'bar', 'lemon'],
    ['seven', 'coin', 'bar'],
  ]
}
</script>

<template>
  <main class="app-screen slots-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">Slots</p>
        <h1>{{ game?.name || 'Lucky Slots' }}</h1>
      </div>
      <RouterLink class="plain-link" to="/app">Lobby</RouterLink>
    </header>

    <section v-if="loading" class="status-panel">Loading Slots...</section>

    <template v-else>
      <section class="wallet-band">
        <div>
          <span>GC balance</span>
          <strong>{{ money(gcBalance) }}</strong>
        </div>
        <div>
          <span>Bet range</span>
          <strong>{{ money(game?.minBet || 0) }}-{{ money(game?.maxBet || 0) }}</strong>
        </div>
      </section>

      <section class="slot-machine" aria-label="Slot reels">
        <div class="slot-reel" v-for="(reel, reelIndex) in visibleReels" :key="reelIndex">
          <span v-for="(symbol, symbolIndex) in reel" :key="`${symbol}-${symbolIndex}`" class="slot-symbol" :class="`symbol-${symbol}`">
            {{ symbolLabel(symbol) }}
          </span>
        </div>
      </section>

      <section class="slot-controls">
        <label>
          Bet
          <select v-model="betAmount">
            <option value="1.0000">1 GC</option>
            <option value="5.0000">5 GC</option>
            <option value="10.0000">10 GC</option>
            <option value="25.0000">25 GC</option>
          </select>
        </label>
        <button data-test="spin-slots" :disabled="!canSpin" @click="spin">
          {{ spinning ? 'Spinning' : 'Spin' }}
        </button>
      </section>

      <p v-if="!canSpin && !spinning" class="notice">Insufficient GC balance or game unavailable.</p>
      <p v-if="error" class="notice danger">{{ error }}</p>

      <section v-if="result" class="section-block">
        <div class="section-title">
          <h2>Result</h2>
          <span>{{ result.status }}</span>
        </div>
        <article class="reward-row">
          <div>
            <strong>{{ result.roundId }}</strong>
            <span>{{ money(result.betAmount) }} GC bet · x{{ Number(result.multiplier).toFixed(2) }}</span>
          </div>
          <strong>{{ money(result.payoutAmount) }} GC</strong>
        </article>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>Task progress</h2>
          <RouterLink to="/app/activity">Activity</RouterLink>
        </div>
        <article v-for="task in activity?.tasks || []" :key="task.taskCode" class="reward-row">
          <div>
            <strong>{{ task.taskCode }}</strong>
            <span>{{ task.name }} · {{ money(task.progress) }}/{{ money(task.target) }} · {{ task.status }}</span>
          </div>
          <strong>{{ money(task.rewardAmount) }} {{ task.rewardCurrency }}</strong>
        </article>
        <p v-if="!activity?.tasks?.length" class="empty-state">No task progress yet.</p>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h2>Recent rounds</h2>
          <RouterLink to="/app/wallet">Ledger</RouterLink>
        </div>
        <article v-for="round in rounds" :key="round.roundId" class="reward-row">
          <div>
            <strong>{{ round.roundId }}</strong>
            <span>{{ round.status }} · debit {{ round.debitLedgerId || '-' }} · credit {{ round.creditLedgerId || '-' }}</span>
          </div>
          <strong>{{ money(round.payoutAmount) }} GC</strong>
        </article>
        <p v-if="!rounds.length" class="empty-state">No rounds yet.</p>
      </section>
    </template>
  </main>
</template>
