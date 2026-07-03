<script setup lang="ts">
import { balances, games, playerState, promotions, redemptions } from '../data/demo'
</script>

<template>
  <section class="hero-panel">
    <div class="hero-copy">
      <p class="eyebrow">Player account</p>
      <h1>Wallet, games, rewards, and redemption in one entry.</h1>
      <p class="hero-text">
        Demo mode is active. Login is required before real balance changes, claims, bets, or redemption requests.
      </p>
      <div class="hero-actions">
        <RouterLink class="btn primary" to="/login">Login</RouterLink>
        <RouterLink class="btn secondary" to="/register">Register</RouterLink>
      </div>
    </div>
    <div class="balance-board" aria-label="Wallet summary">
      <div class="state-row">
        <span>Account state</span>
        <strong>{{ playerState.isLoggedIn ? 'Logged in' : 'Logged out' }}</strong>
      </div>
      <div v-for="balance in balances" :key="balance.code" class="balance-row">
        <span>{{ balance.code }}</span>
        <strong>{{ balance.available }}</strong>
      </div>
    </div>
  </section>

  <section class="quick-grid">
    <RouterLink class="feature-tile" to="/wallet">
      <span class="tile-icon">W</span>
      <strong>Wallet</strong>
      <small>{{ balances.length }} currencies, locked balance visible</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/games">
      <span class="tile-icon">G</span>
      <strong>Games</strong>
      <small>{{ games.filter((item) => item.state === 'Open').length }} open game</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/promotions">
      <span class="tile-icon">R</span>
      <strong>Rewards</strong>
      <small>{{ promotions.length }} configured rewards</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/redemptions">
      <span class="tile-icon">D</span>
      <strong>Redeem</strong>
      <small>{{ redemptions[0].status }}</small>
    </RouterLink>
  </section>
</template>
