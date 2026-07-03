<script setup lang="ts">
import { games } from '../data/demo'
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">Games</p>
    <h1>Simulated game lobby</h1>
    <p class="muted">Game entry checks balance, maintenance state, and betting availability before launch.</p>
  </section>

  <section class="item-list">
    <article v-for="game in games" :key="game.code" class="list-card">
      <div>
        <small>{{ game.code }} | {{ game.currency }}</small>
        <h2>{{ game.name }}</h2>
        <p>Minimum bet: {{ game.minBet }}</p>
      </div>
      <button class="btn compact" :disabled="game.state !== 'Open'">{{ game.state }}</button>
    </article>
  </section>
</template>
