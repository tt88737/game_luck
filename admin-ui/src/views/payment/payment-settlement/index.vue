<template>
  <div class="settlement-page p-2">
    <el-alert v-if="!canList" :title="t('paymentSettlement.permissionDenied')" type="warning" show-icon :closable="false" />
    <template v-else>
      <el-form :model="query" inline class="filter-band">
        <el-form-item :label="t('paymentSettlement.number')"><el-input v-model="query.settlementNo" clearable /></el-form-item>
        <el-form-item :label="t('paymentSettlement.provider')"><el-input v-model="query.providerCode" clearable /></el-form-item>
        <el-form-item :label="t('paymentSettlement.currency')"><el-input v-model="query.currencyCode" maxlength="3" clearable /></el-form-item>
        <el-form-item :label="t('common.status')"
          ><el-select v-model="query.status" clearable><el-option v-for="o in statusOptions" :key="o" :label="statusLabel(o)" :value="o" /></el-select
        ></el-form-item>
        <el-form-item
          ><el-button type="primary" icon="Search" @click="search">{{ t('common.search') }}</el-button
          ><el-button icon="Refresh" @click="reset">{{ t('common.reset') }}</el-button></el-form-item
        >
      </el-form>
      <el-alert v-if="loadError" :title="t('paymentSettlement.loadFailed')" type="error" show-icon :closable="false"
        ><el-button link type="primary" @click="load">{{ t('paymentSettlement.retry') }}</el-button></el-alert
      >
      <div class="toolbar">
        <strong>{{ t('paymentSettlement.title') }}</strong
        ><el-button v-hasPermi="['payment:settlement:create']" type="primary" icon="Plus" @click="createOpen = true">{{
          t('paymentSettlement.create')
        }}</el-button>
      </div>
      <div class="table-scroll">
        <el-table
          v-loading="loading"
          :data="rows"
          border
          :empty-text="filtered ? t('paymentSettlement.filteredEmpty') : t('paymentSettlement.empty')"
        >
          <el-table-column prop="settlementNo" :label="t('paymentSettlement.number')" min-width="190" show-overflow-tooltip />
          <el-table-column prop="providerCode" :label="t('paymentSettlement.provider')" width="120" />
          <el-table-column prop="currencyCode" :label="t('paymentSettlement.currency')" width="90"
            ><template #default="s"
              ><span class="currency-swatch">{{ s.row.currencyCode }}</span></template
            ></el-table-column
          >
          <el-table-column :label="t('common.status')" width="125"
            ><template #default="s"
              ><el-tag :type="statusType(s.row.status)">{{ statusLabel(s.row.status) }}</el-tag></template
            ></el-table-column
          >
          <el-table-column :label="t('paymentSettlement.window')" min-width="290"
            ><template #default="s">{{ s.row.periodStart }} - {{ s.row.periodEnd }}</template></el-table-column
          >
          <el-table-column prop="grossPayment" :label="t('paymentSettlement.gross')" width="130" align="right" />
          <el-table-column prop="totalFee" :label="t('paymentSettlement.fees')" width="120" align="right" />
          <el-table-column :label="t('paymentSettlement.net')" width="145" align="right"
            ><template #default="s"
              ><strong>{{ s.row.netSettlement }}</strong
              ><small class="net-direction">{{
                Number(s.row.netSettlement) < 0 ? t('paymentSettlement.payable') : t('paymentSettlement.receivable')
              }}</small></template
            ></el-table-column
          >
          <el-table-column :label="t('common.operation')" width="125" fixed="right"
            ><template #default="s">
              <el-tooltip :content="t('common.detail')"
                ><el-button v-hasPermi="['payment:settlement:query']" link icon="View" type="primary" @click="openDetail(s.row.id)"
              /></el-tooltip>
              <el-tooltip v-if="s.row.status === 'CREATED'" :content="t('paymentSettlement.calculate')"
                ><el-button v-hasPermi="['payment:settlement:calculate']" link icon="DataAnalysis" type="warning" @click="calculate(s.row)"
              /></el-tooltip>
              <el-tooltip v-if="s.row.status === 'CALCULATED'" :content="t('paymentSettlement.close')"
                ><el-button v-hasPermi="['payment:settlement:close']" link icon="CircleCheck" type="success" @click="openClose(s.row)"
              /></el-tooltip> </template
          ></el-table-column>
        </el-table>
      </div>
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load" />
    </template>

    <el-dialog v-model="createOpen" :title="t('paymentSettlement.create')" width="min(620px, 94vw)" @closed="resetCreate">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="150px">
        <el-form-item :label="t('paymentSettlement.provider')" prop="providerCode"><el-input v-model="createForm.providerCode" /></el-form-item>
        <el-form-item :label="t('paymentSettlement.currency')" prop="currencyCode"
          ><el-input v-model="createForm.currencyCode" maxlength="3"
        /></el-form-item>
        <el-form-item :label="t('paymentSettlement.window')" prop="window"
          ><el-date-picker v-model="createForm.window" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss[Z]" :disabled-date="futureDate"
        /></el-form-item>
        <el-form-item :label="t('paymentSettlement.feePercent')" prop="paymentFeeRate"
          ><el-input-number v-model="createForm.paymentFeeRate" :min="0" :max="100" :precision="6" /><span class="suffix">%</span></el-form-item
        >
        <el-form-item :label="t('paymentSettlement.fixedFee')" prop="paymentFixedFee"
          ><el-input-number v-model="createForm.paymentFixedFee" :min="0" :precision="6"
        /></el-form-item>
        <el-form-item :label="t('paymentSettlement.chargebackFee')" prop="chargebackFixedFee"
          ><el-input-number v-model="createForm.chargebackFixedFee" :min="0" :precision="6"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="createOpen = false">{{ t('common.cancel') }}</el-button
        ><el-button type="primary" :loading="creating" @click="submitCreate">{{ t('common.confirm') }}</el-button></template
      >
    </el-dialog>

    <el-drawer v-model="detailOpen" :title="t('paymentSettlement.detail')" :size="drawerSize" append-to-body>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <el-alert v-else-if="detailError" :title="t('paymentSettlement.detailFailed')" type="error" show-icon :closable="false"
        ><el-button link @click="reloadDetail">{{ t('paymentSettlement.retry') }}</el-button></el-alert
      >
      <template v-else>
        <div class="summary-band">
          <span>{{ detail.settlementNo }}</span
          ><el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag
          ><strong>{{ detail.currencyCode }} {{ detail.netSettlement }}</strong
          ><span>{{ detail.eventCount }} {{ t('paymentSettlement.events') }}</span>
        </div>
        <el-alert v-if="detail.status === 'CALCULATING'" :title="t('paymentSettlement.processing')" type="info" show-icon />
        <el-alert v-if="detail.status === 'FAILED'" :title="detail.failureReason || t('paymentSettlement.failed')" type="error" show-icon />
        <el-descriptions :column="columns" border class="summary-details">
          <el-descriptions-item :label="t('paymentSettlement.gross')">{{ detail.grossPayment }}</el-descriptions-item
          ><el-descriptions-item :label="t('paymentSettlement.refunds')">{{ detail.refundAmount }}</el-descriptions-item>
          <el-descriptions-item :label="t('paymentSettlement.chargebacks')">{{ detail.chargebackAmount }}</el-descriptions-item
          ><el-descriptions-item :label="t('paymentSettlement.fees')">{{ detail.totalFee }}</el-descriptions-item>
        </el-descriptions>
        <el-tabs v-model="tab" @tab-change="loadItems">
          <el-tab-pane :label="t('paymentSettlement.items')" name="items"
            ><div class="item-filter">
              <el-select v-model="itemQuery.eventType" clearable @change="searchItems"
                ><el-option v-for="o in eventOptions" :key="o" :label="o" :value="o"
              /></el-select>
            </div>
            <div class="table-scroll">
              <el-table v-loading="itemsLoading" :data="items" border :empty-text="t('paymentSettlement.emptyItems')"
                ><el-table-column prop="eventType" :label="t('paymentSettlement.eventType')" min-width="190" /><el-table-column
                  prop="purchaseOrderNo"
                  :label="t('paymentSettlement.orderNo')"
                  min-width="180" /><el-table-column prop="sourceAmount" :label="t('paymentSettlement.amount')" width="120" /><el-table-column
                  prop="feeAmount"
                  :label="t('paymentSettlement.fees')"
                  width="120" /><el-table-column prop="netContribution" :label="t('paymentSettlement.net')" width="140"
              /></el-table>
            </div>
            <pagination
              v-if="itemTotal > 0"
              v-model:page="itemQuery.pageNum"
              v-model:limit="itemQuery.pageSize"
              :total="itemTotal"
              @pagination="loadItems"
          /></el-tab-pane>
          <el-tab-pane :label="t('paymentSettlement.evidence')" name="evidence">
            <pre class="readonly">{{ pretty(detail.evidenceSnapshotJson) }}</pre>
            <el-button link type="primary" @click="goReconciliation">{{ t('paymentSettlement.openReconciliation') }}</el-button></el-tab-pane
          >
          <el-tab-pane :label="t('paymentSettlement.history')" name="history"
            ><el-empty v-if="!detail.actionLogs?.length" :description="t('paymentSettlement.emptyHistory')" /><el-timeline v-else
              ><el-timeline-item v-for="log in detail.actionLogs" :key="log.id" :timestamp="log.createTime"
                >{{ log.operatorName }} | {{ log.actionType }} | {{ log.remark || '-' }}</el-timeline-item
              ></el-timeline
            ></el-tab-pane
          >
        </el-tabs>
        <div class="drawer-actions">
          <el-button
            v-if="detail.status === 'CREATED'"
            v-hasPermi="['payment:settlement:calculate']"
            type="warning"
            icon="DataAnalysis"
            @click="calculate(detail)"
            >{{ t('paymentSettlement.calculate') }}</el-button
          ><el-button
            v-if="detail.status === 'CALCULATED'"
            v-hasPermi="['payment:settlement:close']"
            type="success"
            icon="CircleCheck"
            @click="openClose(detail)"
            >{{ t('paymentSettlement.close') }}</el-button
          >
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="closeOpen" :title="t('paymentSettlement.close')" width="min(540px, 94vw)"
      ><el-alert :title="t('paymentSettlement.noMutation')" type="warning" show-icon :closable="false" /><el-form
        ref="closeRef"
        :model="closeForm"
        :rules="closeRules"
        label-width="110px"
        ><el-form-item :label="t('paymentSettlement.remark')" prop="remark"
          ><el-input v-model="closeForm.remark" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item></el-form
      ><template #footer
        ><el-button @click="closeOpen = false">{{ t('common.cancel') }}</el-button
        ><el-button type="danger" :loading="closing" @click="submitClose">{{ t('common.confirm') }}</el-button></template
      ></el-dialog
    >
  </div>
</template>

<script setup name="PaymentSettlement" lang="ts">
import auth from '@/plugins/auth';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useWindowSize } from '@vueuse/core';
import type { FormInstance, FormRules } from 'element-plus';
import {
  calculateSettlementBatch,
  closeSettlementBatch,
  createSettlementBatch,
  getSettlementBatch,
  listSettlementBatches,
  listSettlementItems
} from '@/api/payment/paymentSettlement';
import type {
  SettlementBatchQuery,
  SettlementBatchStatus,
  SettlementBatchVO,
  SettlementCreateCommand,
  SettlementDetailVO,
  SettlementEventType,
  SettlementItemQuery,
  SettlementItemVO
} from '@/api/payment/paymentSettlement/types';

const { t } = useI18n(),
  router = useRouter(),
  { width } = useWindowSize();
const canList = auth.hasPermi('payment:settlement:list');
const drawerSize = computed(() => (width.value < 768 ? '100%' : '920px')),
  columns = computed(() => (width.value < 600 ? 1 : 2));
const statusOptions: SettlementBatchStatus[] = ['CREATED', 'CALCULATING', 'CALCULATED', 'CLOSED', 'FAILED'];
const eventOptions: SettlementEventType[] = ['PAYMENT_SUCCEEDED', 'REFUND_SUCCEEDED', 'CHARGEBACK_CREATED'];
const statusLabel = (s: SettlementBatchStatus) => t(`paymentSettlement.status.${s}`);
const statusType = (s: SettlementBatchStatus) =>
  ({ CREATED: 'info', CALCULATING: 'warning', CALCULATED: 'primary', CLOSED: 'success', FAILED: 'danger' })[s] as any;
const query = reactive<SettlementBatchQuery>({ pageNum: 1, pageSize: 10, settlementNo: '', providerCode: '', currencyCode: '', status: '' });
const rows = ref<SettlementBatchVO[]>([]),
  total = ref(0),
  loading = ref(false),
  loadError = ref(false);
const filtered = computed(() => !!(query.settlementNo || query.providerCode || query.currencyCode || query.status));
const load = async () => {
  loading.value = true;
  loadError.value = false;
  try {
    const r = await listSettlementBatches({ ...query });
    rows.value = r.rows;
    total.value = r.total;
  } catch {
    loadError.value = true;
    rows.value = [];
  } finally {
    loading.value = false;
  }
};
const search = () => {
  query.pageNum = 1;
  load();
};
const reset = () => {
  Object.assign(query, { pageNum: 1, settlementNo: '', providerCode: '', currencyCode: '', status: '' });
  load();
};

type CreateForm = {
  providerCode: string;
  currencyCode: string;
  window: string[];
  paymentFeeRate: number;
  paymentFixedFee: number;
  chargebackFixedFee: number;
};
const createOpen = ref(false),
  creating = ref(false),
  createRef = ref<FormInstance>();
const newCreate = (): CreateForm => ({
  providerCode: 'SIMULATED',
  currencyCode: 'USD',
  window: [],
  paymentFeeRate: 2.9,
  paymentFixedFee: 0.3,
  chargebackFixedFee: 15
});
const createForm = reactive<CreateForm>(newCreate());
const windowValidator = (_: unknown, value: string[], done: (error?: Error) => void) => {
  if (!value?.[0] || !value?.[1]) return done(new Error(t('paymentSettlement.windowRequired')));
  const span = Date.parse(value[1]) - Date.parse(value[0]);
  return span <= 0 || span > 31 * 24 * 60 * 60 * 1000 ? done(new Error(t('paymentSettlement.windowInvalid'))) : done();
};
const createRules: FormRules<CreateForm> = {
  providerCode: [{ required: true, message: () => t('paymentSettlement.providerRequired') }],
  currencyCode: [{ required: true, pattern: /^[A-Za-z]{3}$/, message: () => t('paymentSettlement.currencyRequired') }],
  window: [{ validator: windowValidator, trigger: 'change' }],
  paymentFeeRate: [{ required: true, type: 'number', min: 0, max: 100 }],
  paymentFixedFee: [{ required: true, type: 'number', min: 0 }],
  chargebackFixedFee: [{ required: true, type: 'number', min: 0 }]
};
const futureDate = (date: Date) => date.getTime() > Date.now();
const resetCreate = () => {
  Object.assign(createForm, newCreate());
  createRef.value?.clearValidate();
};
const submitCreate = async () => {
  await createRef.value?.validate();
  creating.value = true;
  try {
    const data: SettlementCreateCommand = {
      providerCode: createForm.providerCode.trim(),
      currencyCode: createForm.currencyCode.trim().toUpperCase(),
      periodStart: createForm.window[0],
      periodEnd: createForm.window[1],
      paymentFeeRate: (createForm.paymentFeeRate / 100).toFixed(8),
      paymentFixedFee: createForm.paymentFixedFee.toFixed(6),
      chargebackFixedFee: createForm.chargebackFixedFee.toFixed(6)
    };
    const r = await createSettlementBatch(data);
    createOpen.value = false;
    await load();
    await openDetail(r.data.id);
  } finally {
    creating.value = false;
  }
};

const detailOpen = ref(false),
  detailLoading = ref(false),
  detailError = ref(false),
  detail = ref<Partial<SettlementDetailVO>>({}),
  tab = ref('items');
const openDetail = async (id: string) => {
  detailOpen.value = true;
  detail.value = { id };
  await reloadDetail();
};
const reloadDetail = async () => {
  if (!detail.value.id) return;
  detailLoading.value = true;
  detailError.value = false;
  try {
    detail.value = (await getSettlementBatch(detail.value.id)).data;
    if (tab.value === 'items') await loadItems();
  } catch {
    detailError.value = true;
  } finally {
    detailLoading.value = false;
  }
};
const itemQuery = reactive<SettlementItemQuery>({ pageNum: 1, pageSize: 10, eventType: '' }),
  items = ref<SettlementItemVO[]>([]),
  itemTotal = ref(0),
  itemsLoading = ref(false);
const loadItems = async () => {
  if (tab.value !== 'items' || !detail.value.id) return;
  itemsLoading.value = true;
  try {
    const r = await listSettlementItems(detail.value.id, { ...itemQuery });
    items.value = r.rows;
    itemTotal.value = r.total;
  } finally {
    itemsLoading.value = false;
  }
};
const searchItems = () => {
  itemQuery.pageNum = 1;
  loadItems();
};
const calculate = async (row: Partial<SettlementBatchVO>) => {
  if (!row.id || row.status !== 'CREATED') return;
  await ElMessageBox.confirm(t('paymentSettlement.calculateConfirm'), t('paymentSettlement.calculate'), { type: 'warning' });
  await calculateSettlementBatch(row.id);
  ElMessage.success(t('paymentSettlement.calculateAccepted'));
  await load();
  if (detailOpen.value) await reloadDetail();
};
const closeOpen = ref(false),
  closing = ref(false),
  closeRef = ref<FormInstance>(),
  closeTarget = ref<Partial<SettlementBatchVO>>({});
const closeForm = reactive({ remark: '' });
const closeRules = { remark: [{ required: true, whitespace: true, message: () => t('paymentSettlement.remarkRequired'), trigger: 'blur' }] };
const openClose = (row: Partial<SettlementBatchVO>) => {
  if (row.status !== 'CALCULATED') return;
  closeTarget.value = row;
  closeForm.remark = '';
  closeOpen.value = true;
};
const submitClose = async () => {
  await closeRef.value?.validate();
  if (!closeTarget.value.id || closeTarget.value.version == null) return;
  closing.value = true;
  try {
    await closeSettlementBatch(closeTarget.value.id, { version: closeTarget.value.version, remark: closeForm.remark.trim() });
    closeOpen.value = false;
    ElMessage.success(t('paymentSettlement.closed'));
    await load();
    if (detailOpen.value) await reloadDetail();
  } finally {
    closing.value = false;
  }
};
const pretty = (value?: string) => {
  if (!value) return t('paymentSettlement.noEvidence');
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};
const goReconciliation = () => router.push({ path: '/payment/payment-reconciliation', query: { providerCode: detail.value.providerCode } });
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
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.table-scroll {
  width: 100%;
  overflow-x: auto;
}
.currency-swatch {
  display: inline-block;
  min-width: 44px;
  padding: 2px 8px;
  border-left: 4px solid var(--el-color-primary);
  background: var(--el-fill-color-light);
}
.net-direction {
  display: block;
  color: var(--el-text-color-secondary);
}
.suffix {
  margin-left: 8px;
}
.summary-band {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color);
}
.summary-details {
  margin: 16px 0;
}
.item-filter {
  width: 220px;
  margin: 8px 0 12px;
}
.readonly {
  max-height: 360px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: var(--el-fill-color-light);
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
}
.drawer-actions {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-light);
}
@media (max-width: 600px) {
  .settlement-page {
    padding: 4px;
  }
  .filter-band :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }
  .filter-band :deep(.el-form-item__content),
  .filter-band :deep(.el-input),
  .filter-band :deep(.el-select) {
    width: 100%;
  }
  .summary-band {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }
  .item-filter {
    width: 100%;
  }
}
</style>
