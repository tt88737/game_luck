<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { ClientGame } from '../types/client'

const games = ref<ClientGame[]>([])
const launchMessage = ref('')
const error = ref('')

async function loadGames() {
  try {
    games.value = await clientApi.games('GC')
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Game lobby load failed'
  }
}

async function launch(game: ClientGame) {
  if (!sessionState.member) {
    error.value = 'Login required'
    return
  }
  const result = await clientApi.launchGame(game.providerCode, game.gameCode, 'GC')
  launchMessage.value = `${result.sessionNo}: ${result.message}`
}

onMounted(loadGames)
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">Games</p>
    <h1>Simulated game lobby</h1>
    <p class="muted">The lobby is loaded from the backend client game API.</p>
  </section>

  <p v-if="error" class="error-text">{{ error }}</p>
  <p v-if="launchMessage" class="success-text">{{ launchMessage }}</p>

  <section class="item-list">
    <article v-for="game in games" :key="game.gameCode" class="list-card">
      <div>
        <small>{{ game.providerCode }} | {{ game.supportedCurrencies.join(', ') }}</small>
        <h2>{{ game.gameName }}</h2>
        <p>{{ game.maintenance ? 'Maintenance' : 'Available' }}</p>
      </div>
      <button class="btn compact" :disabled="game.maintenance" @click="launch(game)">Launch</button>
    </article>
  </section>
</template>
