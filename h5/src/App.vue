<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { currentLocale, setLocale, t, type Locale } from './i18n'
import { loadBootstrap, logout, restoreSession, sessionState } from './stores/session'

const handleLocaleChange = (event: Event) => {
  setLocale((event.target as HTMLSelectElement).value as Locale)
}

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
        <span>{{ sessionState.bootstrap?.brandName || t('brandFallback') }}</span>
      </RouterLink>

      <nav class="topnav" :aria-label="t('navMain')">
        <RouterLink to="/wallet">{{ t('navWallet') }}</RouterLink>
        <RouterLink to="/games">{{ t('navGames') }}</RouterLink>
        <RouterLink to="/promotions">{{ t('navPromotions') }}</RouterLink>
        <RouterLink to="/redemptions">{{ t('navRedemptions') }}</RouterLink>
      </nav>

      <label class="language-switcher">
        <span>{{ t('languageLabel') }}</span>
        <select :value="currentLocale" @change="handleLocaleChange">
          <option value="zh-CN">{{ t('languageChinese') }}</option>
          <option value="en-US">{{ t('languageEnglish') }}</option>
        </select>
      </label>

      <div v-if="sessionState.member" class="login-link session-chip">
        <span>{{ sessionState.member.nickname || sessionState.member.username }}</span>
        <button type="button" @click="logout">{{ t('actionLogout') }}</button>
      </div>
      <RouterLink v-else class="login-link" to="/login">{{ t('actionLogin') }}</RouterLink>
    </header>

    <main class="page-wrap">
      <RouterView />
    </main>

    <footer class="tabbar" :aria-label="t('navMobile')">
      <RouterLink to="/">{{ t('navHome') }}</RouterLink>
      <RouterLink to="/wallet">{{ t('navWallet') }}</RouterLink>
      <RouterLink to="/games">{{ t('navGames') }}</RouterLink>
      <RouterLink to="/help">{{ t('navHelp') }}</RouterLink>
    </footer>
  </div>
</template>
