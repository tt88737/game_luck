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
    error.value = err instanceof Error ? err.message : '游戏大厅加载失败'
  }
}

async function launch(game: ClientGame) {
  if (!sessionState.member) {
    error.value = '请先登录'
    return
  }
  const result = await clientApi.launchGame(game.providerCode, game.gameCode, 'GC')
  launchMessage.value = `${result.sessionNo}: ${result.message}`
}

onMounted(loadGames)
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">游戏</p>
    <h1>模拟游戏大厅</h1>
    <p class="muted">大厅数据来自后端玩家端游戏 API。</p>
  </section>

  <p v-if="error" class="error-text">{{ error }}</p>
  <p v-if="launchMessage" class="success-text">{{ launchMessage }}</p>

  <section class="item-list">
    <article v-for="game in games" :key="game.gameCode" class="list-card">
      <div>
        <small>{{ game.providerCode }} | {{ game.supportedCurrencies.join(', ') }}</small>
        <h2>{{ game.gameName }}</h2>
        <p>{{ game.maintenance ? '维护中' : '可进入' }}</p>
      </div>
      <button class="btn compact" :disabled="game.maintenance" @click="launch(game)">启动</button>
    </article>
  </section>
</template>
