<script setup lang="ts">
import { balances, playerState, redemptions } from '../data/demo'
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">兑换</p>
    <h1>提交审核后释放余额</h1>
    <p class="muted">兑换需要先完成 KYC。提交兑换后，可用 SC 会被冻结，直到审核完成。</p>
  </section>

  <section class="redeem-panel">
    <div>
      <span>可用 SC</span>
      <strong>{{ balances[0].available }}</strong>
      <small>KYC 状态：{{ playerState.kycStatus }}</small>
    </div>
    <button class="btn primary" type="button" disabled>需要 KYC</button>
  </section>

  <section class="table-panel">
    <h2>最近兑换</h2>
    <div class="table-list">
      <div v-for="item in redemptions" :key="item.no" class="table-row">
        <span>{{ item.no }}</span>
        <strong>{{ item.amount }}</strong>
        <em>{{ item.status }}</em>
      </div>
    </div>
  </section>
</template>
