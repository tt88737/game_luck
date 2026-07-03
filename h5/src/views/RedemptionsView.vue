<script setup lang="ts">
import { balances, playerState, redemptions } from '../data/demo'
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">Redemption</p>
    <h1>Request review before balance release.</h1>
    <p class="muted">KYC is required. Submitting a redemption freezes available SC until review completes.</p>
  </section>

  <section class="redeem-panel">
    <div>
      <span>Available SC</span>
      <strong>{{ balances[0].available }}</strong>
      <small>KYC status: {{ playerState.kycStatus }}</small>
    </div>
    <button class="btn primary" type="button" disabled>KYC required</button>
  </section>

  <section class="table-panel">
    <h2>Recent redemption</h2>
    <div class="table-list">
      <div v-for="item in redemptions" :key="item.no" class="table-row">
        <span>{{ item.no }}</span>
        <strong>{{ item.amount }}</strong>
        <em>{{ item.status }}</em>
      </div>
    </div>
  </section>
</template>
