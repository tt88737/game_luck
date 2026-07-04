<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="report-toolbar">
          <div>
            <div class="report-title">Report Overview</div>
            <div class="report-subtitle">Real-time MVP metrics from member, wallet, payment, game, promotion, and redemption modules.</div>
          </div>
          <el-button v-hasPermi="['report:overview:query']" type="primary" icon="Refresh" :loading="loading" @click="getSummary">Refresh</el-button>
        </div>
      </template>

      <el-empty v-if="!loading && !summary" description="No report data returned" />

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
          <template #header>Wallet And Payment</template>
          <el-table v-loading="loading" border :data="walletPaymentRows">
            <el-table-column label="Metric" prop="metric" min-width="180" />
            <el-table-column label="Value" prop="value" align="right" width="180" />
            <el-table-column label="State" prop="state" align="center" width="140">
              <template #default="scope">
                <el-tag :type="scope.row.type">{{ scope.row.state }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="mb-[10px]">
          <template #header>Game And Promotion</template>
          <el-table v-loading="loading" border :data="gamePromotionRows">
            <el-table-column label="Metric" prop="metric" min-width="180" />
            <el-table-column label="Value" prop="value" align="right" width="180" />
            <el-table-column label="State" prop="state" align="center" width="140">
              <template #default="scope">
                <el-tag :type="scope.row.type">{{ scope.row.state }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <template #header>Redemption Review</template>
      <el-table v-loading="loading" border :data="redemptionRows">
        <el-table-column label="Metric" prop="metric" min-width="180" />
        <el-table-column label="Value" prop="value" align="right" width="180" />
        <el-table-column label="Operational Meaning" prop="meaning" min-width="260" show-overflow-tooltip />
        <el-table-column label="State" prop="state" align="center" width="140">
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

interface MetricRow {
  metric: string;
  value: string;
  state: string;
  type: 'success' | 'warning' | 'danger' | 'info' | 'primary';
  meaning?: string;
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

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
  { label: 'Members', value: formatCount(data.value.memberCount), hint: 'Registered profiles' },
  { label: 'Wallet Accounts', value: formatCount(data.value.walletAccountCount), hint: 'Currency accounts' },
  { label: 'Deposit Amount', value: formatAmount(data.value.successfulDepositAmount), hint: 'Successful deposits' },
  { label: 'Game Net', value: formatAmount(data.value.netGameAmount), hint: 'Payout minus bet' },
  { label: 'Rewards', value: formatAmount(data.value.successfulRewardAmount), hint: 'Successful claims' },
  { label: 'Pending Redeem', value: formatCount(data.value.pendingRedemptionCount), hint: 'Needs review' }
]);

const walletPaymentRows = computed<MetricRow[]>(() => [
  {
    metric: 'Wallet available total',
    value: formatAmount(data.value.walletAvailableAmount),
    state: 'Available',
    type: 'success'
  },
  {
    metric: 'Wallet frozen total',
    value: formatAmount(data.value.walletFrozenAmount),
    state: Number(data.value.walletFrozenAmount || 0) > 0 ? 'Frozen' : 'Clear',
    type: Number(data.value.walletFrozenAmount || 0) > 0 ? 'warning' : 'info'
  },
  {
    metric: 'Deposit orders',
    value: formatCount(data.value.depositOrderCount),
    state: 'Orders',
    type: 'primary'
  },
  {
    metric: 'Successful deposit amount',
    value: formatAmount(data.value.successfulDepositAmount),
    state: 'Credited',
    type: 'success'
  }
]);

const gamePromotionRows = computed<MetricRow[]>(() => [
  {
    metric: 'Game orders',
    value: formatCount(data.value.gameOrderCount),
    state: 'Orders',
    type: 'primary'
  },
  {
    metric: 'Total bet amount',
    value: formatAmount(data.value.totalBetAmount),
    state: 'Debit',
    type: 'warning'
  },
  {
    metric: 'Total payout amount',
    value: formatAmount(data.value.totalPayoutAmount),
    state: 'Credit',
    type: 'success'
  },
  {
    metric: 'Promotion claims',
    value: formatCount(data.value.promotionClaimCount),
    state: 'Claims',
    type: 'primary'
  },
  {
    metric: 'Successful reward amount',
    value: formatAmount(data.value.successfulRewardAmount),
    state: 'Credited',
    type: 'success'
  }
]);

const redemptionRows = computed<MetricRow[]>(() => [
  {
    metric: 'Redemption orders',
    value: formatCount(data.value.redemptionOrderCount),
    meaning: 'All submitted redemption requests.',
    state: 'Orders',
    type: 'primary'
  },
  {
    metric: 'Pending review',
    value: formatCount(data.value.pendingRedemptionCount),
    meaning: 'Frozen funds that still need an operator decision.',
    state: Number(data.value.pendingRedemptionCount || 0) > 0 ? 'Action' : 'Clear',
    type: Number(data.value.pendingRedemptionCount || 0) > 0 ? 'warning' : 'info'
  },
  {
    metric: 'Approved',
    value: formatCount(data.value.approvedRedemptionCount),
    meaning: 'Requests settled from frozen wallet balance.',
    state: 'Settled',
    type: 'success'
  },
  {
    metric: 'Rejected',
    value: formatCount(data.value.rejectedRedemptionCount),
    meaning: 'Requests rejected and released back to available balance.',
    state: 'Released',
    type: 'info'
  },
  {
    metric: 'Approved amount',
    value: formatAmount(data.value.approvedRedemptionAmount),
    meaning: 'Total amount approved for redemption.',
    state: 'Amount',
    type: 'success'
  }
]);

const getSummary = async () => {
  loading.value = true;
  try {
    const res = await getReportOverviewSummary();
    summary.value = res.data;
  } catch (error) {
    proxy?.$modal.msgError('Failed to load report overview');
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

