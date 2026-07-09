<script setup lang="ts">
import { sessionState } from '../stores/session'
</script>

<template>
  <section class="hero-panel">
    <div class="hero-copy">
      <p class="eyebrow">Player account</p>
      <h1>Wallet, games, rewards, and redemption in one entry.</h1>
      <p class="hero-text">
        {{ sessionState.bootstrap?.brandName || 'GameLuck' }} is running with backend client API data.
      </p>
      <div class="hero-actions">
        <RouterLink class="btn primary" :to="sessionState.member ? '/wallet' : '/login'">
          {{ sessionState.member ? 'Open wallet' : 'Login' }}
        </RouterLink>
        <RouterLink class="btn secondary" to="/games">Games</RouterLink>
      </div>
    </div>
    <div class="balance-board" aria-label="Client bootstrap summary">
      <div class="state-row">
        <span>Account</span>
        <strong>{{ sessionState.member ? 'Logged in' : 'Logged out' }}</strong>
      </div>
      <div v-for="currency in sessionState.bootstrap?.currencies || []" :key="currency.currencyCode" class="balance-row">
        <span>{{ currency.currencyCode }}</span>
        <strong>{{ currency.playable ? 'Playable' : 'Disabled' }}</strong>
      </div>
    </div>
  </section>

  <section class="quick-grid">
    <RouterLink class="feature-tile" to="/wallet">
      <span class="tile-icon">W</span>
      <strong>Wallet</strong>
      <small>{{ sessionState.bootstrap?.features.walletEnabled ? 'Balances available' : 'Wallet disabled' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/games">
      <span class="tile-icon">G</span>
      <strong>Games</strong>
      <small>{{ sessionState.bootstrap?.features.gameEnabled ? 'Mock lobby available' : 'Games disabled' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/promotions">
      <span class="tile-icon">R</span>
      <strong>Rewards</strong>
      <small>{{ sessionState.bootstrap?.features.promotionEnabled ? 'Reward entry visible' : 'Rewards disabled' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/redemptions">
      <span class="tile-icon">D</span>
      <strong>Redeem</strong>
      <small>{{ sessionState.bootstrap?.features.redemptionEnabled ? 'Redeem enabled' : 'Redeem not live' }}</small>
    </RouterLink>
  </section>
</template>
