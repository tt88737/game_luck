<script setup lang="ts">
import { promotions } from '../data/demo'
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">奖励</p>
    <h1>活动奖励领取</h1>
    <p class="muted">每个活动奖励只能领取一次，KYC 未完成或账号不可用时无法领取。</p>
  </section>

  <section class="item-list">
    <article v-for="promotion in promotions" :key="promotion.name" class="list-card">
      <div>
        <small>{{ promotion.amount }}</small>
        <h2>{{ promotion.name }}</h2>
        <p>{{ promotion.state }}</p>
      </div>
      <button class="btn compact" :disabled="promotion.state !== '可领取'">
        {{ promotion.state === '可领取' ? '领取' : '不可领取' }}
      </button>
    </article>
  </section>
</template>
