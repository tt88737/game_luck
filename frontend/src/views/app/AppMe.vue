<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()

const accountTitle = computed(() => {
  if (session.isGuest) return 'Guest mode'
  if (session.email) return session.email
  if (session.userId) return `Account ${session.userId}`
  return 'Guest mode'
})

function openAuth(mode: 'register' | 'login') {
  window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: { mode } }))
}
</script>

<template>
  <main class="app-screen me-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">{{ $t('me.eyebrow') }}</p>
        <h1>{{ $t('nav.me') }}</h1>
      </div>
    </header>

    <section class="section-block me-account-panel">
      <div>
        <span class="status-tag" :class="{ active: !session.isGuest }">{{ accountTitle }}</span>
        <p>{{ session.isGuest ? $t('me.guestBody') : $t('me.formalBody') }}</p>
      </div>
      <div v-if="session.isGuest" class="me-action-row">
        <button type="button" @click="openAuth('register')">{{ $t('auth.bindAccount') }}</button>
        <button type="button" class="secondary" @click="openAuth('login')">{{ $t('auth.signIn') }}</button>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">
        <h2>{{ $t('me.walletTitle') }}</h2>
        <span>{{ $t('me.walletHint') }}</span>
      </div>
      <div class="me-link-grid">
        <RouterLink to="/me/wallet">
          <strong>{{ $t('common.wallet') }}</strong>
          <span>{{ $t('me.walletBody') }}</span>
        </RouterLink>
        <RouterLink to="/me/redeem">
          <strong>{{ $t('nav.redeem') }}</strong>
          <span>{{ $t('me.redeemBody') }}</span>
        </RouterLink>
        <RouterLink to="/me/kyc">
          <strong>{{ $t('common.kyc') }}</strong>
          <span>{{ $t('me.kycBody') }}</span>
        </RouterLink>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">
        <h2>{{ $t('me.legalTitle') }}</h2>
        <span>{{ $t('me.legalHint') }}</span>
      </div>
      <div class="me-link-grid">
        <RouterLink to="/promo/amoe">
          <strong>AMOE</strong>
          <span>{{ $t('home.amoeNoPurchase') }}</span>
        </RouterLink>
        <a href="/legal/rules-v1">
          <strong>{{ $t('home.rules') }}</strong>
          <span>{{ $t('home.requiredLinks') }}</span>
        </a>
        <a href="mailto:support@tangluck.local">
          <strong>{{ $t('me.supportTitle') }}</strong>
          <span>{{ $t('me.supportBody') }}</span>
        </a>
      </div>
    </section>
  </main>
</template>
