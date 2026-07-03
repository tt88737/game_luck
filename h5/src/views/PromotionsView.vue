<script setup lang="ts">
import { promotions } from '../data/demo'
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">Rewards</p>
    <h1>Promotion claims</h1>
    <p class="muted">Each promotion can be claimed once. KYC and inactive states block the action.</p>
  </section>

  <section class="item-list">
    <article v-for="promotion in promotions" :key="promotion.name" class="list-card">
      <div>
        <small>{{ promotion.amount }}</small>
        <h2>{{ promotion.name }}</h2>
        <p>{{ promotion.state }}</p>
      </div>
      <button class="btn compact" :disabled="promotion.state !== 'Claim available'">
        {{ promotion.state === 'Claim available' ? 'Claim' : 'Blocked' }}
      </button>
    </article>
  </section>
</template>
