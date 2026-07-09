<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { loadBootstrap, logout, restoreSession, sessionState } from './stores/session'

onMounted(async () => {
  await loadBootstrap()
  await restoreSession()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">GL</span>
        <span>{{ sessionState.bootstrap?.brandName || 'GameLuck' }}</span>
      </RouterLink>
      <nav class="topnav" aria-label="Primary">
        <RouterLink to="/wallet">Wallet</RouterLink>
        <RouterLink to="/games">Games</RouterLink>
        <RouterLink to="/promotions">Rewards</RouterLink>
        <RouterLink to="/redemptions">Redeem</RouterLink>
      </nav>
      <div v-if="sessionState.member" class="login-link session-chip">
        <span>{{ sessionState.member.nickname || sessionState.member.username }}</span>
        <button type="button" @click="logout">Logout</button>
      </div>
      <RouterLink v-else class="login-link" to="/login">Login</RouterLink>
    </header>

    <main class="page-wrap">
      <RouterView />
    </main>

    <footer class="tabbar" aria-label="Mobile navigation">
      <RouterLink to="/">Home</RouterLink>
      <RouterLink to="/wallet">Wallet</RouterLink>
      <RouterLink to="/games">Games</RouterLink>
      <RouterLink to="/help">Help</RouterLink>
    </footer>
  </div>
</template>
