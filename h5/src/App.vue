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
      <nav class="topnav" aria-label="主导航">
        <RouterLink to="/wallet">钱包</RouterLink>
        <RouterLink to="/games">游戏</RouterLink>
        <RouterLink to="/promotions">奖励</RouterLink>
        <RouterLink to="/redemptions">兑换</RouterLink>
      </nav>
      <div v-if="sessionState.member" class="login-link session-chip">
        <span>{{ sessionState.member.nickname || sessionState.member.username }}</span>
        <button type="button" @click="logout">退出</button>
      </div>
      <RouterLink v-else class="login-link" to="/login">登录</RouterLink>
    </header>

    <main class="page-wrap">
      <RouterView />
    </main>

    <footer class="tabbar" aria-label="移动端导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/wallet">钱包</RouterLink>
      <RouterLink to="/games">游戏</RouterLink>
      <RouterLink to="/help">帮助</RouterLink>
    </footer>
  </div>
</template>
