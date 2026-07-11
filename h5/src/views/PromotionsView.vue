<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { clientApi } from '../api/client'
import { t } from '../i18n'
import { sessionState } from '../stores/session'
import type { ClientDailyLoginReward, ClientPromotion } from '../types/client'

const promotions = ref<ClientPromotion[]>([])
const dailyReward = ref<ClientDailyLoginReward | null>(null)
const loading = ref(false)
const claimingId = ref<number | null>(null)
const claimingDaily = ref(false)
const error = ref('')
const success = ref('')

async function loadPromotions() {
  if (!sessionState.member) {
    dailyReward.value = null
    promotions.value = []
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [daily, rows] = await Promise.all([
      clientApi.dailyLoginReward(),
      clientApi.promotions(),
    ])
    dailyReward.value = daily
    promotions.value = rows
  } catch (err) {
    error.value = err instanceof Error ? err.message : '奖励加载失败'
  } finally {
    loading.value = false
  }
}

async function claimDailyReward() {
  claimingDaily.value = true
  error.value = ''
  success.value = ''
  try {
    dailyReward.value = await clientApi.claimDailyLoginReward()
    success.value = t('dailyRewardClaimSuccess')
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('dailyRewardClaimFailed')
  } finally {
    claimingDaily.value = false
  }
}

async function claim(promotion: ClientPromotion) {
  claimingId.value = promotion.promotionId
  error.value = ''
  success.value = ''
  try {
    const result = await clientApi.claimPromotion(promotion.promotionId)
    success.value = `${result.promotionName} 已领取`
    await loadPromotions()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '领取失败'
  } finally {
    claimingId.value = null
  }
}

onMounted(loadPromotions)
watch(() => sessionState.member?.memberId, () => loadPromotions())
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">奖励</p>
    <h1>活动奖励领取</h1>
    <p class="muted">奖励数据来自后端玩家端活动 API。</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>请先登录</strong>
    <RouterLink class="btn primary" to="/login">登录</RouterLink>
  </section>

  <template v-else>
    <p v-if="loading" class="muted">正在加载奖励...</p>
    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="success" class="success-text">{{ success }}</p>

    <section v-if="dailyReward" class="daily-reward-panel">
      <div>
        <p class="eyebrow">{{ t('dailyRewardTitle') }}</p>
        <h2>{{ dailyReward.promotionName || t('dailyRewardTitle') }}</h2>
        <p class="muted">
          {{ dailyReward.claimStatus === 'NOT_CONFIGURED' ? t('dailyRewardUnavailable') : t('dailyRewardSubtitle') }}
        </p>
        <div v-if="dailyReward.rewardItems.length" class="reward-pills">
          <span v-for="item in dailyReward.rewardItems" :key="item.currencyCode">
            {{ item.rewardAmount }} {{ item.currencyCode }}
          </span>
        </div>
      </div>
      <button
        class="btn primary compact"
        :disabled="!dailyReward.canClaim || claimingDaily"
        @click="claimDailyReward"
      >
        {{ claimingDaily ? t('dailyRewardClaiming') : dailyReward.canClaim ? t('dailyRewardClaim') : t('dailyRewardClaimed') }}
      </button>
    </section>

    <section v-if="promotions.length" class="item-list">
      <article v-for="promotion in promotions" :key="promotion.promotionId" class="list-card">
        <div>
          <small>{{ promotion.rewardAmount }} {{ promotion.currencyCode }}</small>
          <h2>{{ promotion.promotionName }}</h2>
          <p>{{ promotion.canClaim ? '可领取' : '已领取' }}</p>
        </div>
        <button
          class="btn compact"
          :disabled="!promotion.canClaim || claimingId === promotion.promotionId"
          @click="claim(promotion)"
        >
          {{ claimingId === promotion.promotionId ? '领取中...' : promotion.canClaim ? '领取' : '已领取' }}
        </button>
      </article>
    </section>

    <section v-else-if="!loading" class="empty-state">
      <strong>暂无可领取奖励</strong>
    </section>
  </template>
</template>
