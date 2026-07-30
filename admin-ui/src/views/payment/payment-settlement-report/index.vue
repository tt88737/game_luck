<template>
  <div class="settlement-report-page p-2">
    <el-alert v-if="!canList" :title="t('paymentSettlementReport.permissionDenied')" type="warning" show-icon :closable="false" />
    <div v-else v-hasPermi="['payment:settlementReport:list']" class="report-workbench">
      <el-form :model="query" inline class="filter-band">
        <el-form-item :label="t('paymentSettlementReport.startDate')">
          <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" :disabled-date="futureDate" />
        </el-form-item>
        <el-form-item :label="t('paymentSettlementReport.endDate')">
          <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" :disabled-date="futureDate" />
        </el-form-item>
        <el-form-item :label="t('paymentSettlementReport.provider')">
          <el-input v-model="query.providerCode" clearable maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('paymentSettlementReport.currency')">
          <el-input v-model="query.currencyCode" clearable maxlength="3" />
        </el-form-item>
        <el-form-item :label="t('paymentSettlementReport.quickRange')">
          <el-radio-group v-model="quickDays" @change="applyQuickRange">
            <el-radio-button :value="7">{{ t('paymentSettlementReport.latest7') }}</el-radio-button>
            <el-radio-button :value="31">{{ t('paymentSettlementReport.latest31') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="Search" @click="search">{{ t('common.search') }}</el-button>
          <el-button icon="Refresh" @click="reset">{{ t('common.reset') }}</el-button>
          <el-button
            v-if="canExport"
            v-hasPermi="['payment:settlementReport:export']"
            type="success"
            icon="Download"
            :loading="exporting"
            @click="exportCurrent"
          >
            {{ t('paymentSettlementReport.export') }}
          </el-button>
          <el-tooltip v-else :content="t('paymentSettlementReport.exportDenied')">
            <el-button icon="Download" disabled>{{ t('paymentSettlementReport.export') }}</el-button>
          </el-tooltip>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="t('paymentSettlementReport.loadFailed')" type="error" show-icon :closable="false">
        <el-button link type="primary" @click="load">{{ t('paymentSettlementReport.retry') }}</el-button>
      </el-alert>

      <section v-if="currencyTotals.length" class="currency-summary-band" :aria-label="t('paymentSettlementReport.currencyTotals')">
        <article v-for="item in currencyTotals" :key="item.currencyCode" class="currency-summary-item">
          <div class="currency-heading">
            <span class="currency-swatch">{{ item.currencyCode }}</span>
            <span>{{ item.batchCount }} {{ t('paymentSettlementReport.batches') }}</span>
          </div>
          <strong :class="{ 'negative-value': isNegative(item.netSettlement) }">{{ item.netSettlement }}</strong>
          <span>{{ isNegative(item.netSettlement) ? t('paymentSettlementReport.negativeNet') : t('paymentSettlementReport.netSettlement') }}</span>
          <small
            >{{ t('paymentSettlementReport.gross') }} {{ item.grossPayment }} · {{ t('paymentSettlementReport.fees') }} {{ item.totalFee }}</small
          >
        </article>
      </section>

      <div class="report-heading">
        <strong>{{ t('paymentSettlementReport.title') }}</strong>
        <span v-if="generatedAt">{{ t('paymentSettlementReport.generatedAt') }} {{ generatedAt }}</span>
      </div>
      <div class="table-scroll">
        <el-table
          v-loading="loading"
          :data="rows"
          border
          :empty-text="filtered ? t('paymentSettlementReport.filteredEmpty') : t('paymentSettlementReport.empty')"
        >
          <el-table-column prop="reportDate" :label="t('paymentSettlementReport.reportDate')" width="118" />
          <el-table-column prop="providerCode" :label="t('paymentSettlementReport.provider')" width="130" show-overflow-tooltip />
          <el-table-column prop="currencyCode" :label="t('paymentSettlementReport.currency')" width="92" />
          <el-table-column :label="t('paymentSettlementReport.batchCount')" width="105" align="right">
            <template #default="scope">
              <el-button v-hasPermi="['payment:settlementReport:query']" link type="primary" @click="openBatches(scope.row)">
                {{ scope.row.batchCount }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="eventCount" :label="t('paymentSettlementReport.eventCount')" width="105" align="right" />
          <el-table-column prop="paymentEventCount" :label="t('paymentSettlementReport.payments')" width="95" align="right" />
          <el-table-column prop="refundEventCount" :label="t('paymentSettlementReport.refunds')" width="95" align="right" />
          <el-table-column prop="chargebackEventCount" :label="t('paymentSettlementReport.chargebacks')" width="95" align="right" />
          <el-table-column prop="grossPayment" :label="t('paymentSettlementReport.gross')" width="130" align="right" />
          <el-table-column prop="refundAmount" :label="t('paymentSettlementReport.refundAmount')" width="125" align="right" />
          <el-table-column prop="chargebackAmount" :label="t('paymentSettlementReport.chargebackAmount')" width="125" align="right" />
          <el-table-column prop="totalFee" :label="t('paymentSettlementReport.fees')" width="120" align="right" />
          <el-table-column :label="t('paymentSettlementReport.netSettlement')" width="150" align="right">
            <template #default="scope">
              <strong :class="{ 'negative-value': isNegative(scope.row.netSettlement) }">{{ scope.row.netSettlement }}</strong>
              <small class="net-label">{{
                isNegative(scope.row.netSettlement) ? t('paymentSettlementReport.negativeNet') : t('paymentSettlementReport.nonNegativeNet')
              }}</small>
            </template>
          </el-table-column>
          <el-table-column prop="latestCloseTime" :label="t('paymentSettlementReport.latestClose')" width="190" />
        </el-table>
      </div>
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load" />
    </div>

    <el-drawer v-model="batchDrawerOpen" :title="t('paymentSettlementReport.sourceBatches')" :size="drawerSize" append-to-body>
      <el-skeleton v-if="batchLoading" :rows="7" animated />
      <el-alert v-else-if="batchError" :title="t('paymentSettlementReport.batchLoadFailed')" type="error" show-icon :closable="false">
        <el-button link type="primary" @click="reloadBatches">{{ t('paymentSettlementReport.retry') }}</el-button>
      </el-alert>
      <div v-else class="table-scroll">
        <el-table :data="batches" border :empty-text="t('paymentSettlementReport.emptyBatches')">
          <el-table-column prop="settlementNo" :label="t('paymentSettlementReport.settlementNo')" min-width="190" show-overflow-tooltip />
          <el-table-column prop="periodStart" :label="t('paymentSettlementReport.periodStart')" min-width="190" />
          <el-table-column prop="periodEnd" :label="t('paymentSettlementReport.periodEnd')" min-width="190" />
          <el-table-column prop="eventCount" :label="t('paymentSettlementReport.eventCount')" width="105" align="right" />
          <el-table-column prop="netSettlement" :label="t('paymentSettlementReport.netSettlement')" width="145" align="right" />
          <el-table-column :label="t('common.operation')" width="92" fixed="right">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="goSettlementDetail(scope.row.id)">
                {{ t('common.detail') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="PaymentSettlementReport" lang="ts">
import auth from '@/plugins/auth';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useWindowSize } from '@vueuse/core';
import { saveAs } from 'file-saver';
import { exportSettlementReport, listSettlementReport, listSettlementReportBatches } from '@/api/payment/paymentSettlementReport';
import type {
  SettlementReportBatchVO,
  SettlementReportCurrencyTotalVO,
  SettlementReportQuery,
  SettlementReportRowVO
} from '@/api/payment/paymentSettlementReport/types';

const { t } = useI18n();
const router = useRouter();
const { width } = useWindowSize();
const canList = auth.hasPermi('payment:settlementReport:list');
const canExport = auth.hasPermi('payment:settlementReport:export');
const drawerSize = computed(() => (width.value < 768 ? '100%' : '920px'));

const utcDate = (date: Date) => date.toISOString().slice(0, 10);
const quickDays = ref(7);
const quickRangeHandlers = {
  7: () => setQuickRange(7),
  31: () => setQuickRange(31)
};
const query = reactive<SettlementReportQuery>({ pageNum: 1, pageSize: 10, startDate: '', endDate: '', providerCode: '', currencyCode: '' });
const rows = ref<SettlementReportRowVO[]>([]);
const currencyTotals = ref<SettlementReportCurrencyTotalVO[]>([]);
const total = ref(0);
const generatedAt = ref('');
const loading = ref(false);
const loadError = ref(false);
const exporting = ref(false);
const filtered = computed(() => !!(query.providerCode || query.currencyCode));

function setQuickRange(days: 7 | 31) {
  const end = new Date();
  const start = new Date(end);
  start.setUTCDate(start.getUTCDate() - days + 1);
  query.startDate = utcDate(start);
  query.endDate = utcDate(end);
  quickDays.value = days;
}
const applyQuickRange = (value: string | number | boolean | undefined) => {
  const days = value === 31 ? 31 : 7;
  quickRangeHandlers[days]();
};
const futureDate = (date: Date) => date.getTime() > Date.now();
const validateRange = () => {
  const start = Date.parse(`${query.startDate}T00:00:00Z`);
  const end = Date.parse(`${query.endDate}T00:00:00Z`);
  const today = Date.parse(`${utcDate(new Date())}T00:00:00Z`);
  if (!Number.isFinite(start) || !Number.isFinite(end) || start > end || end > today || (end - start) / 86400000 + 1 > 31) {
    ElMessage.error(t('paymentSettlementReport.dateInvalid'));
    return false;
  }
  return true;
};
const normalizedQuery = (): SettlementReportQuery => ({
  ...query,
  providerCode: query.providerCode?.trim().toUpperCase() || undefined,
  currencyCode: query.currencyCode?.trim().toUpperCase() || undefined
});
const isNegative = (value: string) => /^-/.test(value.trim());

const load = async () => {
  if (!validateRange()) return;
  loading.value = true;
  loadError.value = false;
  try {
    const result = (await listSettlementReport(normalizedQuery())).data;
    rows.value = result.rows;
    total.value = result.total;
    currencyTotals.value = result.currencyTotals;
    generatedAt.value = result.generatedAt;
  } catch {
    rows.value = [];
    total.value = 0;
    currencyTotals.value = [];
    loadError.value = true;
  } finally {
    loading.value = false;
  }
};
const search = () => {
  query.pageNum = 1;
  load();
};
const reset = () => {
  query.providerCode = '';
  query.currencyCode = '';
  query.pageNum = 1;
  setQuickRange(7);
  load();
};
const exportCurrent = async () => {
  if (!canExport || exporting.value || !validateRange()) return;
  exporting.value = true;
  try {
    const { pageNum: _pageNum, pageSize: _pageSize, ...params } = normalizedQuery();
    const blob = (await exportSettlementReport(params)) as unknown as Blob;
    saveAs(blob, `payment-settlement-report_${query.startDate}_${query.endDate}.csv`);
  } finally {
    exporting.value = false;
  }
};

const batchDrawerOpen = ref(false);
const batchLoading = ref(false);
const batchError = ref(false);
const batches = ref<SettlementReportBatchVO[]>([]);
const selectedGroup = ref<SettlementReportRowVO>();
const openBatches = async (row: SettlementReportRowVO) => {
  selectedGroup.value = row;
  batchDrawerOpen.value = true;
  await reloadBatches();
};
const reloadBatches = async () => {
  if (!selectedGroup.value) return;
  batchLoading.value = true;
  batchError.value = false;
  try {
    batches.value = (
      await listSettlementReportBatches(selectedGroup.value.reportDate, selectedGroup.value.providerCode, selectedGroup.value.currencyCode)
    ).data;
  } catch {
    batches.value = [];
    batchError.value = true;
  } finally {
    batchLoading.value = false;
  }
};
const goSettlementDetail = (batchId: string) => router.push({ path: '/payment/payment-settlement', query: { batchId } });

setQuickRange(7);
onMounted(() => {
  if (canList) load();
});
</script>

<style scoped>
.report-workbench {
  min-width: 0;
}
.filter-band {
  padding: 12px 12px 0;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-light);
}
.filter-actions {
  margin-left: auto;
}
.currency-summary-band {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  border-bottom: 1px solid var(--el-border-color-light);
}
.currency-summary-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 16px;
  padding: 14px 16px;
  border-right: 1px solid var(--el-border-color-light);
}
.currency-summary-item strong {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.currency-summary-item small {
  grid-column: 1 / -1;
  color: var(--el-text-color-secondary);
}
.currency-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}
.currency-swatch {
  border-left: 4px solid var(--el-color-primary);
  padding: 2px 8px;
  background: var(--el-fill-color-light);
  font-weight: 600;
}
.report-heading {
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--el-text-color-secondary);
}
.report-heading strong {
  color: var(--el-text-color-primary);
}
.table-scroll {
  width: 100%;
  overflow-x: auto;
}
.negative-value {
  color: var(--el-color-danger);
}
.net-label {
  display: block;
  color: var(--el-text-color-secondary);
}
@media (max-width: 600px) {
  .settlement-report-page {
    padding: 4px;
    overflow-x: hidden;
  }
  .filter-band :deep(.el-form-item),
  .filter-band :deep(.el-form-item__content),
  .filter-band :deep(.el-input),
  .filter-band :deep(.el-date-editor) {
    width: 100%;
    margin-right: 0;
  }
  .filter-actions {
    margin-left: 0;
  }
  .currency-summary-band {
    grid-template-columns: minmax(0, 1fr);
  }
  .currency-summary-item {
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-light);
  }
  .report-heading {
    align-items: flex-start;
    flex-direction: column;
    padding: 12px 0;
  }
}
</style>
