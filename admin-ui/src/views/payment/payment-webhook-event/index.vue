<template>
  <div class="payment-ops-page p-2">
    <el-alert v-if="!canList" :title="t('paymentWebhookEvent.messages.permissionDenied')" type="warning" show-icon :closable="false" />
    <template v-else>
    <div v-show="showSearch" class="mb-[10px]"><el-card shadow="never">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="ops-filter">
        <el-form-item :label="t('paymentWebhookEvent.fields.providerEventId')" prop="providerEventId"><el-input v-model="queryParams.providerEventId" clearable /></el-form-item>
        <el-form-item :label="t('paymentWebhookEvent.fields.purchaseOrderNo')" prop="purchaseOrderNo"><el-input v-model="queryParams.purchaseOrderNo" clearable /></el-form-item>
        <el-form-item :label="t('paymentWebhookEvent.fields.sessionNo')" prop="sessionNo"><el-input v-model="queryParams.sessionNo" clearable /></el-form-item>
        <el-form-item :label="t('paymentWebhookEvent.fields.providerSessionNo')" prop="providerSessionNo"><el-input v-model="queryParams.providerSessionNo" clearable /></el-form-item>
        <el-form-item :label="t('paymentWebhookEvent.fields.eventType')" prop="eventType"><el-select v-model="queryParams.eventType" clearable class="!w-190px"><el-option v-for="item in eventTypeOptions" :key="item.value" v-bind="item" /></el-select></el-form-item>
        <el-form-item :label="t('common.status')" prop="status"><el-select v-model="queryParams.status" clearable class="!w-150px"><el-option v-for="item in statusOptions" :key="item.value" v-bind="item" /></el-select></el-form-item>
        <el-form-item :label="t('paymentWebhookEvent.fields.providerCode')" prop="providerCode"><el-input v-model="queryParams.providerCode" clearable /></el-form-item>
        <el-form-item :label="t('paymentWebhookEvent.fields.receivedRange')"><el-date-picker v-model="dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" :range-separator="t('paymentWebhookEvent.range.to')" /></el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">{{ t('common.search') }}</el-button><el-button icon="Refresh" @click="resetQuery">{{ t('common.reset') }}</el-button></el-form-item>
      </el-form>
    </el-card></div>
    <el-alert v-if="loadError" :title="t('paymentWebhookEvent.messages.loadFailed')" type="error" show-icon :closable="false" class="mb-[10px]"><el-button link type="primary" @click="getList">{{ t('paymentWebhookEvent.actions.retryLoad') }}</el-button></el-alert>
    <el-card shadow="never">
      <template #header><div class="ops-header"><span>{{ t('paymentWebhookEvent.title') }}</span><right-toolbar v-model:show-search="showSearch" @query-table="getList" /></div></template>
      <div class="table-scroll"><el-table v-loading="loading" border :data="eventList" :empty-text="t('paymentWebhookEvent.empty')">
        <el-table-column :label="t('paymentWebhookEvent.fields.providerEventId')" prop="providerEventId" min-width="220" show-overflow-tooltip />
        <el-table-column :label="t('paymentWebhookEvent.fields.eventType')" min-width="160"><template #default="scope">{{ businessLabel('paymentWebhookEventType', scope.row.eventType, tt) }}</template></el-table-column>
        <el-table-column :label="t('common.status')" width="120" align="center"><template #default="scope"><el-tag :type="paymentWebhookStatusType(scope.row.status)">{{ businessLabel('paymentWebhookStatus', scope.row.status, tt) }}</el-tag></template></el-table-column>
        <el-table-column :label="t('paymentWebhookEvent.fields.purchaseOrderNo')" min-width="180"><template #default="scope"><el-button link type="primary" @click="goPurchase(scope.row.purchaseOrderNo)">{{ scope.row.purchaseOrderNo || '-' }}</el-button></template></el-table-column>
        <el-table-column :label="t('paymentWebhookEvent.fields.sessionNo')" min-width="190"><template #default="scope"><el-button link type="primary" @click="goSession(scope.row.sessionNo)">{{ scope.row.sessionNo || '-' }}</el-button></template></el-table-column>
        <el-table-column :label="t('paymentWebhookEvent.fields.attempts')" prop="processingCount" width="90" align="center" />
        <el-table-column :label="t('paymentWebhookEvent.fields.failureReason')" prop="failureReason" min-width="220" show-overflow-tooltip />
        <el-table-column :label="t('paymentWebhookEvent.fields.receivedTime')" prop="receivedTime" width="170" />
        <el-table-column :label="t('common.operation')" width="106" fixed="right" align="center"><template #default="scope">
          <el-tooltip :content="t('common.detail')"><el-button v-hasPermi="['payment:webhookEvent:query']" link type="primary" icon="View" @click="openDetail(scope.row)" /></el-tooltip>
          <el-tooltip v-if="scope.row.status === 'FAILED'" :content="t('paymentWebhookEvent.actions.retry')"><el-button v-hasPermi="['payment:webhookEvent:retry']" link type="danger" icon="RefreshRight" :loading="retryingId === scope.row.id" :disabled="retryingId !== null" @click="confirmRetry(scope.row)" /></el-tooltip>
        </template></el-table-column>
      </el-table></div>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-drawer v-model="detailOpen" :title="t('paymentWebhookEvent.detail.title')" :size="drawerSize" append-to-body>
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <el-alert v-else-if="detailError" :title="t('paymentWebhookEvent.messages.detailFailed')" type="error" show-icon :closable="false" />
      <template v-else>
        <el-descriptions :column="detailColumns" border>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.providerEventId')" class-name="break-value">{{ detail.providerEventId }}</el-descriptions-item>
          <el-descriptions-item :label="t('common.status')"><el-tag :type="paymentWebhookStatusType(detail.status)">{{ businessLabel('paymentWebhookStatus', detail.status, tt) }}</el-tag></el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.eventType')">{{ businessLabel('paymentWebhookEventType', detail.eventType, tt) }}</el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.providerCode')">{{ detail.providerCode }}</el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.purchaseOrderNo')"><el-button link type="primary" @click="goPurchase(detail.purchaseOrderNo)">{{ detail.purchaseOrderNo || '-' }}</el-button></el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.sessionNo')"><el-button link type="primary" @click="goSession(detail.sessionNo)">{{ detail.sessionNo || '-' }}</el-button></el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.providerSessionNo')" class-name="break-value">{{ detail.providerSessionNo }}</el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.attempts')">{{ detail.processingCount }}</el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.receivedTime')">{{ detail.receivedTime }}</el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.lastProcessingTime')">{{ detail.lastProcessingTime || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('paymentWebhookEvent.fields.failureReason')" :span="detailColumns" class-name="break-value">{{ detail.failureReason || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="isReversalEvent" :label="t('paymentWebhookEvent.fields.linkedReversal')" :span="detailColumns"><el-button link type="primary" icon="Right" @click="goReversal(detail.purchaseOrderNo)">{{ t('paymentWebhookEvent.actions.openReversal') }}</el-button></el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">{{ t('paymentWebhookEvent.detail.signatureDigest') }}</el-divider>
        <div class="readonly-digest">{{ detail.signatureDigest || '-' }}</div>
        <el-divider content-position="left">{{ t('paymentWebhookEvent.detail.rawPayload') }}</el-divider>
        <pre class="raw-payload" tabindex="0">{{ formattedRawBody }}</pre>
        <el-alert v-if="rawFormatFallback" :title="t('paymentWebhookEvent.messages.rawFormatFallback')" type="warning" show-icon :closable="false" class="mt-2" />
        <div v-if="detail.status === 'FAILED'" class="drawer-actions"><el-button v-hasPermi="['payment:webhookEvent:retry']" type="danger" icon="RefreshRight" :loading="retryingId === detail.id" :disabled="retryingId !== null" @click="confirmRetry(detail as PaymentWebhookEventVO)">{{ t('paymentWebhookEvent.actions.retry') }}</el-button></div>
      </template>
    </el-drawer>
    </template>
  </div>
</template>

<script setup name="PaymentWebhookEvent" lang="ts">
import { getPaymentWebhookEvent, listPaymentWebhookEvent, retryPaymentWebhookEvent } from '@/api/payment/paymentWebhookEvent';
import type { PaymentWebhookEventDetailVO, PaymentWebhookEventQuery, PaymentWebhookEventVO } from '@/api/payment/paymentWebhookEvent/types';
import { businessLabel, businessOptions, paymentWebhookStatusType } from '@/utils/businessLabels';
import { tt } from '@/utils/i18nText';
import auth from '@/plugins/auth';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const router = useRouter();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { width } = useWindowSize();
const queryFormRef = ref<ElFormInstance>();
const showSearch = ref(true);
const loading = ref(false);
const loadError = ref(false);
const eventList = ref<PaymentWebhookEventVO[]>([]);
const total = ref(0);
const dateRange = ref<[string, string] | []>([]);
const detailOpen = ref(false);
const detailLoading = ref(false);
const detailError = ref(false);
const detail = ref<Partial<PaymentWebhookEventDetailVO>>({});
const retryingId = ref<string | number | null>(null);
const detailColumns = computed(() => (width.value < 768 ? 1 : 2));
const drawerSize = computed(() => (width.value < 768 ? '100%' : '760px'));
const eventTypeOptions = businessOptions('paymentWebhookEventType', tt);
const statusOptions = businessOptions('paymentWebhookStatus', tt);
const canList = auth.hasPermi('payment:webhookEvent:list');
const queryParams = ref<PaymentWebhookEventQuery>({ pageNum: 1, pageSize: 10, providerEventId: '', purchaseOrderNo: '', sessionNo: '', providerSessionNo: '', eventType: '', status: '', providerCode: '' });
const rawFormatFallback = ref(false);
const formattedRawBody = computed(() => {
  rawFormatFallback.value = false;
  if (!detail.value.rawBody) return '-';
  try { return JSON.stringify(JSON.parse(detail.value.rawBody), null, 2); } catch { rawFormatFallback.value = true; return detail.value.rawBody; }
});
const isReversalEvent = computed(() => detail.value.eventType === 'REFUND_SUCCEEDED' || detail.value.eventType === 'CHARGEBACK_CREATED');

const getList = async () => {
  loading.value = true; loadError.value = false;
  try { const res = await listPaymentWebhookEvent({ ...queryParams.value, beginTime: dateRange.value[0], endTime: dateRange.value[1] }); eventList.value = res.rows; total.value = res.total; }
  catch { loadError.value = true; eventList.value = []; total.value = 0; }
  finally { loading.value = false; }
};
const handleQuery = () => { queryParams.value.pageNum = 1; getList(); };
const resetQuery = () => { queryFormRef.value?.resetFields(); dateRange.value = []; handleQuery(); };
const goPurchase = (purchaseOrderNo?: string) => { if (purchaseOrderNo) router.push({ path: '/payment/purchase-order', query: { purchaseOrderNo } }); };
const goSession = (sessionNo?: string) => { if (sessionNo) router.push({ path: '/payment/payment-session', query: { sessionNo } }); };
const goReversal = (purchaseOrderNo?: string) => { if (purchaseOrderNo) router.push({ path: '/payment/purchase-reversal-review', query: { purchaseOrderNo } }); };
const openDetail = async (row: PaymentWebhookEventVO) => {
  detailOpen.value = true; detailLoading.value = true; detailError.value = false;
  try { detail.value = (await getPaymentWebhookEvent(row.id)).data; } catch { detailError.value = true; } finally { detailLoading.value = false; }
};
const confirmRetry = async (row: PaymentWebhookEventVO) => {
  if (row.status !== 'FAILED' || retryingId.value !== null) return;
  try { await proxy?.$modal.confirm(t('paymentWebhookEvent.retry.confirm', { eventId: row.providerEventId })); } catch { return; }
  retryingId.value = row.id;
  try {
    const result = (await retryPaymentWebhookEvent(row.id)).data;
    Object.assign(row, result);
    if (detail.value.id === row.id) Object.assign(detail.value, result);
    proxy?.$modal.msgSuccess(t('paymentWebhookEvent.messages.retrySuccess'));
  } catch { proxy?.$modal.msgError(t('paymentWebhookEvent.messages.retryFailed')); }
  finally { retryingId.value = null; }
};
if (canList) getList();
</script>

<style scoped>
.payment-ops-page { max-width: 100%; overflow-x: hidden; }
.ops-header { display: flex; align-items: center; justify-content: space-between; min-height: 28px; }
.table-scroll { max-width: 100%; overflow-x: auto; }
:deep(.break-value), .readonly-digest, .raw-payload { overflow-wrap: anywhere; word-break: break-word; }
.readonly-digest { padding: 10px 12px; border: 1px solid var(--el-border-color); background: var(--el-fill-color-light); font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
.raw-payload { max-height: 360px; margin: 0; overflow: auto; padding: 12px; border: 1px solid var(--el-border-color); background: var(--el-fill-color-lighter); color: var(--el-text-color-primary); white-space: pre-wrap; font: 12px/1.55 ui-monospace, SFMono-Regular, Consolas, monospace; }
.drawer-actions { display: flex; justify-content: flex-end; margin-top: 16px; }
@media (max-width: 767px) { .ops-filter :deep(.el-form-item) { margin-right: 0; width: 100%; } .ops-filter :deep(.el-form-item__content), .ops-filter :deep(.el-input), .ops-filter :deep(.el-select), .ops-filter :deep(.el-date-editor) { width: 100% !important; min-width: 0; } }
</style>
