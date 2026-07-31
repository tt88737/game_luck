<template>
  <div class="report-page p-2">
    <el-alert v-if="!canList" :title="t('paymentSettlementReport.permissionDenied')" type="warning" show-icon :closable="false" />
    <template v-else>
      <el-form :model="query" inline class="filter-band">
        <el-form-item :label="t('paymentSettlementReport.range')">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" :disabled-date="futureDate" />
        </el-form-item>
        <el-form-item>
          <el-button-group>
            <el-button :type="rangePreset === 7 ? 'primary' : ''" @click="setUtcRange(7)">{{ t('paymentSettlementReport.last7') }}</el-button>
            <el-button :type="rangePreset === 31 ? 'primary' : ''" @click="setUtcRange(31)">{{ t('paymentSettlementReport.last31') }}</el-button>
          </el-button-group>
        </el-form-item>
        <el-form-item :label="t('paymentSettlementReport.provider')"><el-input v-model="query.providerCode" clearable /></el-form-item>
        <el-form-item :label="t('paymentSettlementReport.currency')"><el-input v-model="query.currencyCode" maxlength="3" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="search">{{ t('common.search') }}</el-button>
          <el-button icon="Refresh" @click="reset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="validationError" :title="validationError" type="warning" show-icon :closable="false" />
      <el-alert v-else-if="loadFailed" :title="t('paymentSettlementReport.loadFailed')" type="error" show-icon :closable="false">
        <el-button link type="primary" @click="load">{{ t('paymentSettlementReport.retry') }}</el-button>
      </el-alert>
      <el-alert
        v-if="exportFailed"
        :title="t('paymentSettlementReport.exportFailed')"
        type="error"
        show-icon
        closable
        @close="exportFailed = false"
      />

      <div class="toolbar">
        <div>
          <strong>{{ t('paymentSettlementReport.title') }}</strong>
          <small v-if="generatedAt">{{ t('paymentSettlementReport.generatedAt') }} {{ generatedAt }}</small>
        </div>
        <el-button
          v-hasPermi="['payment:settlementReport:export']"
          icon="Download"
          :loading="exporting"
          :disabled="loading || !!validationError"
          @click="downloadCsv"
          >{{ exporting ? t('paymentSettlementReport.exporting') : t('paymentSettlementReport.export') }}</el-button
        >
      </div>

      <div v-if="currencyTotals.length" class="currency-summary" aria-live="polite">
        <section v-for="item in currencyTotals" :key="item.currencyCode" class="currency-summary__item">
          <strong>{{ item.currencyCode }}</strong>
          <span>{{ t('paymentSettlementReport.net') }} {{ item.netSettlement }}</span>
          <span>{{ t('paymentSettlementReport.gross') }} {{ item.grossPayment }}</span>
          <span>{{ item.batchCount }} {{ t('paymentSettlementReport.batches') }}</span>
        </section>
      </div>

      <div class="table-scroll">
        <el-table
          v-loading="loading"
          :data="rows"
          border
          :empty-text="filtered ? t('paymentSettlementReport.filteredEmpty') : t('paymentSettlementReport.empty')"
        >
          <el-table-column prop="settlementDate" :label="t('paymentSettlementReport.date')" width="120" />
          <el-table-column prop="providerCode" :label="t('paymentSettlementReport.provider')" min-width="140" show-overflow-tooltip />
          <el-table-column prop="currencyCode" :label="t('paymentSettlementReport.currency')" width="90" />
          <el-table-column prop="batchCount" :label="t('paymentSettlementReport.batchCount')" width="105" align="right" />
          <el-table-column prop="eventCount" :label="t('paymentSettlementReport.eventCount')" width="105" align="right" />
          <el-table-column prop="grossPayment" :label="t('paymentSettlementReport.gross')" width="135" align="right" />
          <el-table-column prop="refundAmount" :label="t('paymentSettlementReport.refunds')" width="125" align="right" />
          <el-table-column prop="chargebackAmount" :label="t('paymentSettlementReport.chargebacks')" width="125" align="right" />
          <el-table-column prop="totalFee" :label="t('paymentSettlementReport.fees')" width="115" align="right" />
          <el-table-column :label="t('paymentSettlementReport.net')" width="175" align="right">
            <template #default="scope">
              <strong :class="{ negative: isNegative(scope.row.netSettlement) }">{{ scope.row.netSettlement }}</strong>
              <small class="net-direction">{{
                isNegative(scope.row.netSettlement) ? t('paymentSettlementReport.payable') : t('paymentSettlementReport.receivable')
              }}</small>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="90" fixed="right">
            <template #default="scope">
              <el-tooltip :content="t('common.detail')">
                <el-button v-hasPermi="['payment:settlementReport:query']" link icon="View" type="primary" @click="openBatches(scope.row)" />
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load" />
    </template>

    <el-drawer v-model="drawerOpen" :title="t('paymentSettlementReport.groupBatches')" :size="drawerSize" append-to-body>
      <el-skeleton v-if="drawerLoading" :rows="6" animated />
      <el-alert v-else-if="drawerFailed" :title="t('paymentSettlementReport.detailFailed')" type="error" show-icon :closable="false">
        <el-button link type="primary" @click="reloadBatches">{{ t('paymentSettlementReport.retry') }}</el-button>
      </el-alert>
      <div v-else class="table-scroll">
        <el-table :data="batches" border :empty-text="t('paymentSettlementReport.emptyBatches')">
          <el-table-column prop="settlementNo" :label="t('paymentSettlementReport.number')" min-width="190" />
          <el-table-column prop="periodStart" :label="t('paymentSettlementReport.periodStart')" min-width="180" />
          <el-table-column prop="periodEnd" :label="t('paymentSettlementReport.periodEnd')" min-width="180" />
          <el-table-column prop="netSettlement" :label="t('paymentSettlementReport.net')" width="140" align="right" />
          <el-table-column :label="t('common.operation')" width="90" fixed="right">
            <template #default="scope">
              <el-tooltip :content="t('paymentSettlementReport.openBatch')">
                <el-button link type="primary" icon="Right" @click="openSettlement(scope.row.id)" />
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="PaymentSettlementReport" lang="ts">
import FileSaver from 'file-saver';
import auth from '@/plugins/auth';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useWindowSize } from '@vueuse/core';
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
const drawerSize = computed(() => (width.value < 768 ? '100%' : '900px'));
const utcDate = (date: Date) => date.toISOString().slice(0, 10);
const rangePreset = ref(7);
const dateRange = ref<string[]>([]);
const query = reactive<SettlementReportQuery>({ pageNum: 1, pageSize: 10, startDate: '', endDate: '', providerCode: '', currencyCode: '' });
const rows = ref<SettlementReportRowVO[]>([]);
const currencyTotals = ref<SettlementReportCurrencyTotalVO[]>([]);
const total = ref(0);
const generatedAt = ref('');
const loading = ref(false);
const loadFailed = ref(false);
const exportFailed = ref(false);
const exporting = ref(false);
const filtered = computed(() => !!(query.providerCode || query.currencyCode));
const validationError = computed(() => {
  if (dateRange.value.length !== 2) return t('paymentSettlementReport.rangeRequired');
  const days = (Date.parse(dateRange.value[1]) - Date.parse(dateRange.value[0])) / 86400000 + 1;
  if (days < 1 || days > 31) return t('paymentSettlementReport.rangeInvalid');
  if (query.currencyCode && !/^[A-Za-z]{3}$/.test(query.currencyCode.trim())) return t('paymentSettlementReport.currencyInvalid');
  return '';
});
const setUtcRange = (days: number) => {
  rangePreset.value = days;
  const end = new Date();
  const start = new Date(Date.UTC(end.getUTCFullYear(), end.getUTCMonth(), end.getUTCDate() - days + 1));
  dateRange.value = [utcDate(start), utcDate(end)];
};
const syncQuery = () => {
  query.startDate = dateRange.value[0];
  query.endDate = dateRange.value[1];
  query.providerCode = query.providerCode?.trim().toUpperCase();
  query.currencyCode = query.currencyCode?.trim().toUpperCase();
};
const load = async () => {
  if (validationError.value) return;
  syncQuery();
  loading.value = true;
  loadFailed.value = false;
  try {
    const response = (await listSettlementReport({ ...query })) as unknown as {
      rows: SettlementReportRowVO[];
      total: number;
      currencyTotals: SettlementReportCurrencyTotalVO[];
      generatedAt: string;
    };
    rows.value = response.rows;
    total.value = response.total;
    currencyTotals.value = response.currencyTotals;
    generatedAt.value = response.generatedAt;
  } catch {
    loadFailed.value = true;
    rows.value = [];
    currencyTotals.value = [];
  } finally {
    loading.value = false;
  }
};
const search = () => {
  query.pageNum = 1;
  load();
};
const reset = () => {
  Object.assign(query, { pageNum: 1, providerCode: '', currencyCode: '' });
  setUtcRange(7);
  load();
};
const futureDate = (date: Date) => date.getTime() > Date.now();
const isNegative = (value: string) => /^-/.test(value.trim());
const downloadCsv = async () => {
  if (!canExport || validationError.value) return;
  syncQuery();
  exporting.value = true;
  exportFailed.value = false;
  try {
    const blob = (await exportSettlementReport({
      startDate: query.startDate,
      endDate: query.endDate,
      providerCode: query.providerCode,
      currencyCode: query.currencyCode
    })) as unknown as Blob;
    FileSaver.saveAs(blob, `payment-settlement-report_${query.startDate}_${query.endDate}.csv`);
    ElMessage.success(t('paymentSettlementReport.exported'));
  } catch {
    exportFailed.value = true;
  } finally {
    exporting.value = false;
  }
};

const drawerOpen = ref(false);
const drawerLoading = ref(false);
const drawerFailed = ref(false);
const batches = ref<SettlementReportBatchVO[]>([]);
const selected = ref<SettlementReportRowVO>();
const openBatches = async (row: SettlementReportRowVO) => {
  selected.value = row;
  drawerOpen.value = true;
  await reloadBatches();
};
const reloadBatches = async () => {
  if (!selected.value) return;
  drawerLoading.value = true;
  drawerFailed.value = false;
  try {
    const response = await listSettlementReportBatches(selected.value.settlementDate, selected.value.providerCode, selected.value.currencyCode);
    batches.value = response.data;
  } catch {
    drawerFailed.value = true;
    batches.value = [];
  } finally {
    drawerLoading.value = false;
  }
};
const openSettlement = (batchId: string) => router.push({ path: '/payment/payment-settlement', query: { batchId } });

setUtcRange(7);
onMounted(() => {
  if (canList) load();
});
</script>

<style scoped>
.filter-band {
  padding: 12px 12px 0;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-light);
}
.toolbar {
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.toolbar > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.toolbar small {
  color: var(--el-text-color-secondary);
}
.currency-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  border: 1px solid var(--el-border-color-light);
  border-bottom: 0;
}
.currency-summary__item {
  min-width: 0;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 16px;
  border-right: 1px solid var(--el-border-color-light);
}
.currency-summary__item strong {
  grid-row: span 3;
  align-self: center;
  padding-left: 8px;
  border-left: 4px solid var(--el-color-primary);
}
.currency-summary__item span {
  overflow-wrap: anywhere;
}
.table-scroll {
  width: 100%;
  overflow-x: auto;
}
.net-direction {
  display: block;
  color: var(--el-text-color-secondary);
}
.negative {
  color: var(--el-color-danger);
}
@media (max-width: 600px) {
  .report-page {
    padding: 4px;
  }
  .filter-band :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }
  .filter-band :deep(.el-form-item__content),
  .filter-band :deep(.el-input),
  .filter-band :deep(.el-date-editor) {
    width: 100%;
  }
  .toolbar {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .toolbar .el-button {
    width: 100%;
  }
  .currency-summary {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
