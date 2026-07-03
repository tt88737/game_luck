<script setup lang="ts">
import { balances, ledgerRows } from '../data/demo'
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">Wallet</p>
    <h1>Balances and recent ledger</h1>
    <p class="muted">Locked balance is shown separately so redemption freezes are visible to the player.</p>
  </section>

  <section class="data-grid">
    <article v-for="balance in balances" :key="balance.code" class="metric-card">
      <span>{{ balance.name }}</span>
      <strong>{{ balance.available }}</strong>
      <small>Locked: {{ balance.locked }} | {{ balance.status }}</small>
    </article>
  </section>

  <section class="table-panel">
    <h2>Recent ledger</h2>
    <div class="table-list">
      <div v-for="row in ledgerRows" :key="row.time + row.type" class="table-row">
        <span>{{ row.time }}</span>
        <strong>{{ row.type }}</strong>
        <span>{{ row.amount }}</span>
        <em>{{ row.status }}</em>
      </div>
    </div>
  </section>
</template>
