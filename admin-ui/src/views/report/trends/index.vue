<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="trends-toolbar">
          <div>
            <div class="trends-title">{{ t('reportTrends.title') }}</div>
            <div class="trends-subtitle">{{ t('reportTrends.subtitle') }}</div>
          </div>
          <div class="trends-actions">
            <el-radio-group v-model="range" size="default" @change="getList">
              <el-radio-button :label="7">{{ t('reportTrends.range.seven') }}</el-radio-button>
              <el-radio-button :label="30">{{ t('reportTrends.range.thirty') }}</el-radio-button>
            </el-radio-group>
            <el-button v-hasPermi="['report:trends:query']" type="primary" icon="Refresh" :loading="loading" @click="getList">
              {{ t('reportTrends.refresh') }}
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!loading && !rows.length" :description="t('reportTrends.empty')" />

      <el-row v-else v-loading="loading" :gutter="10" class="metric-row">
        <el-col v-for="card in metricCards" :key="card.label" :xs="24" :sm="12" :md="8" :lg="4">
          <div class="metric-card">
            <div class="metric-label">{{ card.label }}</div>
            <div class="metric-value">{{ card.value }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" border :data="rows">
        <el-table-column :label="t('reportTrends.columns.date')" prop="reportDate" align="center" width="120" />
        <el-table-column :label="t('reportTrends.columns.members')" prop="memberCount" align="right" width="110" />
        <el-table-column :label="t('reportTrends.columns.depositOrders')" prop="depositOrderCount" align="right" width="120" />
        <el-table-column :label="t('reportTrends.columns.depositAmount')" align="right" width="150">
          <template #default="scope">{{ formatAmount(scope.row.successfulDepositAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.gameOrders')" prop="gameOrderCount" align="right" width="120" />
        <el-table-column :label="t('reportTrends.columns.betAmount')" align="right" width="140">
          <template #default="scope">{{ formatAmount(scope.row.totalBetAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.payoutAmount')" align="right" width="140">
          <template #default="scope">{{ formatAmount(scope.row.totalPayoutAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.gameNet')" align="right" width="140">
          <template #default="scope">{{ formatAmount(scope.row.netGameAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.promotionClaims')" prop="promotionClaimCount" align="right" width="120" />
        <el-table-column :label="t('reportTrends.columns.rewardAmount')" align="right" width="140">
          <template #default="scope">{{ formatAmount(scope.row.successfulRewardAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.redemptionOrders')" prop="redemptionOrderCount" align="right" width="130" />
        <el-table-column :label="t('reportTrends.columns.pendingRedeem')" prop="pendingRedemptionCount" align="right" width="110" />
        <el-table-column :label="t('reportTrends.columns.approvedRedeemAmount')" align="right" width="150">
          <template #default="scope">{{ formatAmount(scope.row.approvedRedemptionAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="ReportTrends" lang="ts">
import { listReportDailyTrends } from '@/api/report/trends';
import { ReportDailyTrendVO } from '@/api/report/trends/types';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { t } = useI18n();

const loading = ref(false);
const range = ref(7);
const rows = ref<ReportDailyTrendVO[]>([]);

const amount = (value: string | number | undefined) => Number(value || 0);
const formatAmount = (value: string | number | undefined) => amount(value).toFixed(6);
const formatCount = (value: number | undefined) => Number(value || 0).toLocaleString();

const totals = computed(() =>
  rows.value.reduce(
    (acc, row) => {
      acc.depositAmount += amount(row.successfulDepositAmount);
      acc.gameNet += amount(row.netGameAmount);
      acc.rewards += amount(row.successfulRewardAmount);
      acc.approvedRedeem += amount(row.approvedRedemptionAmount);
      acc.pendingRedeem += Number(row.pendingRedemptionCount || 0);
      return acc;
    },
    { depositAmount: 0, gameNet: 0, rewards: 0, approvedRedeem: 0, pendingRedeem: 0 }
  )
);

const metricCards = computed(() => [
  { label: t('reportTrends.cards.depositAmount'), value: formatAmount(totals.value.depositAmount) },
  { label: t('reportTrends.cards.gameNet'), value: formatAmount(totals.value.gameNet) },
  { label: t('reportTrends.cards.rewards'), value: formatAmount(totals.value.rewards) },
  { label: t('reportTrends.cards.approvedRedeem'), value: formatAmount(totals.value.approvedRedeem) },
  { label: t('reportTrends.cards.pendingRedeem'), value: formatCount(totals.value.pendingRedeem) }
]);

const getList = async () => {
  loading.value = true;
  try {
    const res = await listReportDailyTrends(range.value);
    rows.value = res.data || [];
  } catch (error) {
    proxy?.$modal.msgError(t('reportTrends.messages.loadFailed'));
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.trends-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.trends-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.trends-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.trends-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-row {
  row-gap: 10px;
}

.metric-card {
  min-height: 86px;
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

@media (max-width: 768px) {
  .trends-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .trends-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
