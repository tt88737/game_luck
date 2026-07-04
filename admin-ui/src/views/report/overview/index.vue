<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="report-toolbar">
          <div>
            <div class="report-title">{{ t('reportOverview.title') }}</div>
            <div class="report-subtitle">{{ t('reportOverview.subtitle') }}</div>
          </div>
          <el-button v-hasPermi="['report:overview:query']" type="primary" icon="Refresh" :loading="loading" @click="getSummary">
            {{ t('reportOverview.refresh') }}
          </el-button>
        </div>
      </template>

      <el-empty v-if="!loading && !summary" :description="t('reportOverview.empty')" />

      <div v-else v-loading="loading">
        <el-row :gutter="10" class="metric-row">
          <el-col v-for="card in metricCards" :key="card.label" :xs="24" :sm="12" :md="8" :lg="4">
            <div class="metric-card">
              <div class="metric-label">{{ card.label }}</div>
              <div class="metric-value">{{ card.value }}</div>
              <div class="metric-hint">{{ card.hint }}</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-row :gutter="10">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="mb-[10px]">
          <template #header>{{ t('reportOverview.sections.walletPayment') }}</template>
          <el-table v-loading="loading" border :data="walletPaymentRows">
            <el-table-column :label="t('reportOverview.columns.metric')" prop="metric" min-width="180" />
            <el-table-column :label="t('reportOverview.columns.value')" prop="value" align="right" width="180" />
            <el-table-column :label="t('reportOverview.columns.state')" prop="state" align="center" width="140">
              <template #default="scope">
                <el-tag :type="scope.row.type">{{ scope.row.state }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="mb-[10px]">
          <template #header>{{ t('reportOverview.sections.gamePromotion') }}</template>
          <el-table v-loading="loading" border :data="gamePromotionRows">
            <el-table-column :label="t('reportOverview.columns.metric')" prop="metric" min-width="180" />
            <el-table-column :label="t('reportOverview.columns.value')" prop="value" align="right" width="180" />
            <el-table-column :label="t('reportOverview.columns.state')" prop="state" align="center" width="140">
              <template #default="scope">
                <el-tag :type="scope.row.type">{{ scope.row.state }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <template #header>{{ t('reportOverview.sections.redemptionReview') }}</template>
      <el-table v-loading="loading" border :data="redemptionRows">
        <el-table-column :label="t('reportOverview.columns.metric')" prop="metric" min-width="180" />
        <el-table-column :label="t('reportOverview.columns.value')" prop="value" align="right" width="180" />
        <el-table-column :label="t('reportOverview.columns.meaning')" prop="meaning" min-width="260" show-overflow-tooltip />
        <el-table-column :label="t('reportOverview.columns.state')" prop="state" align="center" width="140">
          <template #default="scope">
            <el-tag :type="scope.row.type">{{ scope.row.state }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="ReportOverview" lang="ts">
import { getReportOverviewSummary } from '@/api/report/overview';
import { ReportOverviewSummaryVO } from '@/api/report/overview/types';
import { useI18n } from 'vue-i18n';

interface MetricRow {
  metric: string;
  value: string;
  state: string;
  type: 'success' | 'warning' | 'danger' | 'info' | 'primary';
  meaning?: string;
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { t } = useI18n();

const loading = ref(false);
const summary = ref<ReportOverviewSummaryVO>();

const zeroSummary: ReportOverviewSummaryVO = {
  memberCount: 0,
  walletAccountCount: 0,
  walletAvailableAmount: 0,
  walletFrozenAmount: 0,
  depositOrderCount: 0,
  successfulDepositAmount: 0,
  gameOrderCount: 0,
  totalBetAmount: 0,
  totalPayoutAmount: 0,
  netGameAmount: 0,
  promotionClaimCount: 0,
  successfulRewardAmount: 0,
  redemptionOrderCount: 0,
  pendingRedemptionCount: 0,
  approvedRedemptionCount: 0,
  rejectedRedemptionCount: 0,
  approvedRedemptionAmount: 0
};

const data = computed(() => summary.value || zeroSummary);

const formatAmount = (value: string | number | undefined) => Number(value || 0).toFixed(6);
const formatCount = (value: number | undefined) => Number(value || 0).toLocaleString();

const metricCards = computed(() => [
  { label: t('reportOverview.cards.members'), value: formatCount(data.value.memberCount), hint: t('reportOverview.cards.registeredProfiles') },
  { label: t('reportOverview.cards.walletAccounts'), value: formatCount(data.value.walletAccountCount), hint: t('reportOverview.cards.currencyAccounts') },
  { label: t('reportOverview.cards.depositAmount'), value: formatAmount(data.value.successfulDepositAmount), hint: t('reportOverview.cards.successfulDeposits') },
  { label: t('reportOverview.cards.gameNet'), value: formatAmount(data.value.netGameAmount), hint: t('reportOverview.cards.payoutMinusBet') },
  { label: t('reportOverview.cards.rewards'), value: formatAmount(data.value.successfulRewardAmount), hint: t('reportOverview.cards.successfulClaims') },
  { label: t('reportOverview.cards.pendingRedeem'), value: formatCount(data.value.pendingRedemptionCount), hint: t('reportOverview.cards.needsReview') }
]);

const walletPaymentRows = computed<MetricRow[]>(() => [
  {
    metric: t('reportOverview.metrics.walletAvailableTotal'),
    value: formatAmount(data.value.walletAvailableAmount),
    state: t('reportOverview.states.available'),
    type: 'success'
  },
  {
    metric: t('reportOverview.metrics.walletFrozenTotal'),
    value: formatAmount(data.value.walletFrozenAmount),
    state: Number(data.value.walletFrozenAmount || 0) > 0 ? t('reportOverview.states.frozen') : t('reportOverview.states.clear'),
    type: Number(data.value.walletFrozenAmount || 0) > 0 ? 'warning' : 'info'
  },
  {
    metric: t('reportOverview.metrics.depositOrders'),
    value: formatCount(data.value.depositOrderCount),
    state: t('reportOverview.states.orders'),
    type: 'primary'
  },
  {
    metric: t('reportOverview.metrics.successfulDepositAmount'),
    value: formatAmount(data.value.successfulDepositAmount),
    state: t('reportOverview.states.credited'),
    type: 'success'
  }
]);

const gamePromotionRows = computed<MetricRow[]>(() => [
  {
    metric: t('reportOverview.metrics.gameOrders'),
    value: formatCount(data.value.gameOrderCount),
    state: t('reportOverview.states.orders'),
    type: 'primary'
  },
  {
    metric: t('reportOverview.metrics.totalBetAmount'),
    value: formatAmount(data.value.totalBetAmount),
    state: t('reportOverview.states.debit'),
    type: 'warning'
  },
  {
    metric: t('reportOverview.metrics.totalPayoutAmount'),
    value: formatAmount(data.value.totalPayoutAmount),
    state: t('reportOverview.states.credit'),
    type: 'success'
  },
  {
    metric: t('reportOverview.metrics.promotionClaims'),
    value: formatCount(data.value.promotionClaimCount),
    state: t('reportOverview.states.claims'),
    type: 'primary'
  },
  {
    metric: t('reportOverview.metrics.successfulRewardAmount'),
    value: formatAmount(data.value.successfulRewardAmount),
    state: t('reportOverview.states.credited'),
    type: 'success'
  }
]);

const redemptionRows = computed<MetricRow[]>(() => [
  {
    metric: t('reportOverview.metrics.redemptionOrders'),
    value: formatCount(data.value.redemptionOrderCount),
    meaning: t('reportOverview.meanings.redemptionOrders'),
    state: t('reportOverview.states.orders'),
    type: 'primary'
  },
  {
    metric: t('reportOverview.metrics.pendingReview'),
    value: formatCount(data.value.pendingRedemptionCount),
    meaning: t('reportOverview.meanings.pendingReview'),
    state: Number(data.value.pendingRedemptionCount || 0) > 0 ? t('reportOverview.states.action') : t('reportOverview.states.clear'),
    type: Number(data.value.pendingRedemptionCount || 0) > 0 ? 'warning' : 'info'
  },
  {
    metric: t('reportOverview.metrics.approved'),
    value: formatCount(data.value.approvedRedemptionCount),
    meaning: t('reportOverview.meanings.approved'),
    state: t('reportOverview.states.settled'),
    type: 'success'
  },
  {
    metric: t('reportOverview.metrics.rejected'),
    value: formatCount(data.value.rejectedRedemptionCount),
    meaning: t('reportOverview.meanings.rejected'),
    state: t('reportOverview.states.released'),
    type: 'info'
  },
  {
    metric: t('reportOverview.metrics.approvedAmount'),
    value: formatAmount(data.value.approvedRedemptionAmount),
    meaning: t('reportOverview.meanings.approvedAmount'),
    state: t('reportOverview.states.amount'),
    type: 'success'
  }
]);

const getSummary = async () => {
  loading.value = true;
  try {
    const res = await getReportOverviewSummary();
    summary.value = res.data;
  } catch (error) {
    proxy?.$modal.msgError(t('reportOverview.messages.loadFailed'));
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  getSummary();
});
</script>

<style scoped>
.report-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.report-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.metric-row {
  row-gap: 10px;
}

.metric-card {
  min-height: 104px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.metric-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.metric-value {
  margin-top: 10px;
  color: var(--el-text-color-primary);
  font-size: 22px;
  font-weight: 760;
}

.metric-hint {
  margin-top: 8px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

@media (max-width: 768px) {
  .report-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
