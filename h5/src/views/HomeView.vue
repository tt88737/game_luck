<script setup lang="ts">
import { sessionState } from '../stores/session'
</script>

<template>
  <section class="hero-panel">
    <div class="hero-copy">
      <p class="eyebrow">玩家账户</p>
      <h1>钱包、游戏、奖励和兑换统一入口</h1>
      <p class="hero-text">
        {{ sessionState.bootstrap?.brandName || 'GameLuck' }} 已接入后端玩家端 API 数据。
      </p>
      <div class="hero-actions">
        <RouterLink class="btn primary" :to="sessionState.member ? '/wallet' : '/login'">
          {{ sessionState.member ? '查看钱包' : '登录' }}
        </RouterLink>
        <RouterLink class="btn secondary" to="/games">进入游戏</RouterLink>
      </div>
    </div>
    <div class="balance-board" aria-label="玩家端初始化信息">
      <div class="state-row">
        <span>账户状态</span>
        <strong>{{ sessionState.member ? '已登录' : '未登录' }}</strong>
      </div>
      <div v-for="currency in sessionState.bootstrap?.currencies || []" :key="currency.currencyCode" class="balance-row">
        <span>{{ currency.currencyCode }}</span>
        <strong>{{ currency.playable ? '可用于游戏' : '已停用' }}</strong>
      </div>
    </div>
  </section>

  <section class="quick-grid">
    <RouterLink class="feature-tile" to="/wallet">
      <span class="tile-icon">钱</span>
      <strong>钱包</strong>
      <small>{{ sessionState.bootstrap?.features.walletEnabled ? '余额可查看' : '钱包未开启' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/games">
      <span class="tile-icon">游</span>
      <strong>游戏</strong>
      <small>{{ sessionState.bootstrap?.features.gameEnabled ? '模拟大厅可用' : '游戏未开启' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/promotions">
      <span class="tile-icon">奖</span>
      <strong>奖励</strong>
      <small>{{ sessionState.bootstrap?.features.promotionEnabled ? '奖励入口可见' : '奖励未开启' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/redemptions">
      <span class="tile-icon">兑</span>
      <strong>兑换</strong>
      <small>{{ sessionState.bootstrap?.features.redemptionEnabled ? '兑换已开启' : '兑换暂未开放' }}</small>
    </RouterLink>
  </section>
</template>
