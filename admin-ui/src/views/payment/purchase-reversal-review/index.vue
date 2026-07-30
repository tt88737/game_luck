<template>
  <div class="p-2 reversal-review-page">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="t('purchaseReversalReview.fields.reversalNo')" prop="reversalNo">
              <el-input v-model="queryParams.reversalNo" :placeholder="t('purchaseReversalReview.placeholders.reversalNo')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('purchaseReversalReview.fields.purchaseOrderNo')" prop="purchaseOrderNo">
              <el-input v-model="queryParams.purchaseOrderNo" :placeholder="t('purchaseReversalReview.placeholders.purchaseOrderNo')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('purchaseReversalReview.fields.member')" prop="memberNo">
              <el-input v-model="queryParams.memberNo" :placeholder="t('purchaseReversalReview.placeholders.member')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('purchaseReversalReview.fields.reversalType')" prop="reversalType">
              <el-select v-model="queryParams.reversalType" :placeholder="t('purchaseReversalReview.placeholders.reversalType')" clearable class="!w-150px">
                <el-option v-for="item in reversalTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">{{ t('common.search') }}</el-button>
              <el-button icon="Refresh" @click="resetQuery">{{ t('common.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <div class="review-toolbar">
          <el-segmented v-model="queryParams.dispositionStatus" :options="statusFilterOptions" @change="handleStatusFilterChange" />
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </div>
      </template>

      <el-table v-loading="loading" border :data="reviewList" :empty-text="t('purchaseReversalReview.empty')">
        <el-table-column :label="t('purchaseReversalReview.fields.reversalNo')" prop="reversalNo" min-width="190" show-overflow-tooltip />
        <el-table-column :label="t('purchaseReversalReview.fields.purchaseOrderNo')" prop="purchaseOrderNo" min-width="190" show-overflow-tooltip />
        <el-table-column :label="t('purchaseReversalReview.fields.member')" width="120">
          <template #default="scope">{{ scope.row.memberNo || scope.row.memberId }}</template>
        </el-table-column>
        <el-table-column :label="t('purchaseReversalReview.fields.reversalType')" align="center" width="120">
          <template #default="scope">{{ businessLabel('purchaseReversalType', scope.row.reversalType, tt) }}</template>
        </el-table-column>
        <el-table-column :label="t('purchaseReversalReview.fields.shortfall')" min-width="190">
          <template #default="scope">
            <div class="currency-lines">
              <el-tag v-for="item in shortfallItems(scope.row.items)" :key="item.currencyCode" type="danger" effect="plain" size="small">
                {{ item.currencyCode }} {{ item.shortfallAmount }}
              </el-tag>
              <span v-if="!shortfallItems(scope.row.items).length">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('purchaseReversalReview.fields.riskLevel')" prop="riskLevel" align="center" width="100">
          <template #default="scope"><el-tag :type="scope.row.riskLevel === 'HIGH' ? 'danger' : 'info'">{{ scope.row.riskLevel || '-' }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="t('purchaseReversalReview.fields.waiting')" align="center" width="130">
          <template #default="scope">{{ scope.row.dispositionStatus === 'PENDING_REVIEW' ? waitingDuration(scope.row.createTime) : '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('purchaseReversalReview.fields.dispositionStatus')" align="center" width="130">
          <template #default="scope">
            <el-tag :type="purchaseReversalDispositionStatusType(scope.row.dispositionStatus)">
              {{ businessLabel('purchaseReversalDispositionStatus', scope.row.dispositionStatus, tt) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('purchaseReversalReview.fields.createTime')" prop="createTime" align="center" width="170" />
        <el-table-column :label="t('common.operation')" align="center" width="80" fixed="right">
          <template #default="scope">
            <el-tooltip :content="t('purchaseReversalReview.actions.detail')" placement="top">
              <el-button v-hasPermi="['payment:reversalReview:query']" link type="primary" icon="View" @click="openDetail(scope.row.reversalNo)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-drawer v-model="detailOpen" :title="t('purchaseReversalReview.detail.title')" :size="drawerSize" append-to-body destroy-on-close>
      <div v-loading="detailLoading" class="review-detail">
        <template v-if="detail.reversalNo">
          <section class="detail-section">
            <h3>{{ t('purchaseReversalReview.detail.caseAndOrder') }}</h3>
            <el-descriptions :column="detailColumns" border>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.reversalNo')">{{ detail.reversalNo }}</el-descriptions-item>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.purchaseOrderNo')">{{ detail.purchaseOrderNo }}</el-descriptions-item>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.member')">{{ detail.memberNo || detail.memberId }}</el-descriptions-item>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.dispositionStatus')">
                <el-tag :type="purchaseReversalDispositionStatusType(detail.dispositionStatus)">{{ businessLabel('purchaseReversalDispositionStatus', detail.dispositionStatus, tt) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.orderStatus')">{{ businessLabel('purchaseOrderStatus', detail.purchaseOrder?.status, tt) }}</el-descriptions-item>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.retryCount')">{{ detail.retryCount || 0 }}</el-descriptions-item>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.reason')" :span="detailColumns">{{ detail.reason || '-' }}</el-descriptions-item>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.reviewReason')" :span="detailColumns">{{ detail.reviewReason || '-' }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-section">
            <h3>{{ t('purchaseReversalReview.detail.recoveryItems') }}</h3>
            <el-table border :data="detail.items || []">
              <el-table-column :label="t('purchaseReversalReview.fields.currency')" prop="currencyCode" width="90" fixed="left" />
              <el-table-column :label="t('purchaseReversalReview.fields.required')" prop="requiredAmount" align="right" min-width="120" />
              <el-table-column :label="t('purchaseReversalReview.fields.available')" prop="availableAmount" align="right" min-width="120" />
              <el-table-column :label="t('purchaseReversalReview.fields.recovered')" prop="recoveredAmount" align="right" min-width="120" />
              <el-table-column :label="t('purchaseReversalReview.fields.shortfall')" prop="shortfallAmount" align="right" min-width="120">
                <template #default="scope"><strong :class="{ shortfall: Number(scope.row.shortfallAmount) > 0 }">{{ scope.row.shortfallAmount }}</strong></template>
              </el-table-column>
              <el-table-column :label="t('purchaseReversalReview.fields.walletTransactionNo')" prop="walletTransactionNo" min-width="190" show-overflow-tooltip />
            </el-table>
          </section>

          <section class="detail-section">
            <h3>{{ t('purchaseReversalReview.detail.memberRisk') }}</h3>
            <el-descriptions :column="detailColumns" border>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.riskLevel')"><el-tag :type="detail.member?.riskLevel === 'HIGH' ? 'danger' : 'info'">{{ detail.member?.riskLevel || detail.riskLevel || '-' }}</el-tag></el-descriptions-item>
              <el-descriptions-item :label="t('purchaseReversalReview.fields.riskReason')">{{ detail.member?.riskReason || '-' }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-section">
            <h3>{{ t('purchaseReversalReview.detail.grants') }}</h3>
            <el-table border :data="detail.grantSnapshots || []" max-height="220">
              <el-table-column :label="t('purchaseReversalReview.fields.grantType')" prop="grantType" min-width="120" />
              <el-table-column :label="t('purchaseReversalReview.fields.currency')" prop="currencyCode" width="90" />
              <el-table-column :label="t('purchaseReversalReview.fields.amount')" prop="grantAmount" align="right" width="120" />
              <el-table-column :label="t('purchaseReversalReview.fields.fundProperty')" prop="fundPropertyCode" min-width="130" />
              <el-table-column :label="t('purchaseReversalReview.fields.turnoverTaskNo')" prop="turnoverTaskNo" min-width="180" show-overflow-tooltip />
            </el-table>
          </section>

          <section class="detail-section">
            <h3>{{ t('purchaseReversalReview.detail.paymentEvents') }}</h3>
            <el-table border :data="detail.paymentEvents || []" max-height="220">
              <el-table-column :label="t('purchaseReversalReview.fields.eventKey')" prop="eventKey" min-width="220" show-overflow-tooltip />
              <el-table-column :label="t('purchaseReversalReview.fields.eventType')" min-width="130"><template #default="scope">{{ businessLabel('purchasePaymentEventType', scope.row.eventType, tt) }}</template></el-table-column>
              <el-table-column :label="t('purchaseReversalReview.fields.eventStatus')" prop="eventStatus" width="110" />
              <el-table-column :label="t('purchaseReversalReview.fields.createTime')" prop="createTime" width="170" />
            </el-table>
          </section>

          <section class="detail-section">
            <h3>{{ t('purchaseReversalReview.detail.history') }}</h3>
            <el-table border :data="detail.reviewLogs || []" :empty-text="t('purchaseReversalReview.detail.noHistory')">
              <el-table-column :label="t('purchaseReversalReview.fields.operationType')" min-width="140"><template #default="scope"><el-tag :type="purchaseReversalReviewOperationType(scope.row.operationType)">{{ businessLabel('purchaseReversalReviewOperationType', scope.row.operationType, tt) }}</el-tag></template></el-table-column>
              <el-table-column :label="t('purchaseReversalReview.fields.operator')" prop="operatorName" min-width="120" />
              <el-table-column :label="t('purchaseReversalReview.fields.reviewNote')" prop="reviewNote" min-width="180" show-overflow-tooltip />
              <el-table-column :label="t('purchaseReversalReview.fields.createTime')" prop="createTime" width="170" />
            </el-table>
          </section>
        </template>
      </div>

      <template #footer>
        <div v-if="detail.dispositionStatus === 'PENDING_REVIEW'" class="drawer-actions">
          <el-button v-hasPermi="['payment:reversalReview:retry']" type="primary" icon="Refresh" :loading="submitting" @click="confirmRetry">
            {{ t('purchaseReversalReview.actions.retry') }}
          </el-button>
          <el-button v-hasPermi="['payment:reversalReview:acceptLoss']" type="danger" plain icon="CircleClose" :disabled="submitting" @click="openLossDialog">
            {{ t('purchaseReversalReview.actions.acceptLoss') }}
          </el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="lossDialogOpen" :title="t('purchaseReversalReview.loss.title')" width="min(560px, 92vw)" append-to-body>
      <div class="loss-lines">
        <div v-for="item in shortfallItems(detail.items)" :key="item.currencyCode"><span>{{ item.currencyCode }}</span><strong>{{ item.shortfallAmount }}</strong></div>
      </div>
      <el-form ref="lossFormRef" :model="lossForm" :rules="lossRules" label-position="top">
        <el-form-item :label="t('purchaseReversalReview.fields.reviewNote')" prop="reviewNote">
          <el-input v-model="lossForm.reviewNote" type="textarea" :rows="4" maxlength="500" show-word-limit :placeholder="t('purchaseReversalReview.loss.notePlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeLossDialog">{{ t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="submitting" @click="submitLoss">{{ t('purchaseReversalReview.loss.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PurchaseReversalReview" lang="ts">
import {
  acceptPurchaseReversalLoss,
  getPurchaseReversalReview,
  listPurchaseReversalReview,
  retryPurchaseReversalReview
} from '@/api/payment/purchaseReversalReview';
import type {
  PurchaseReversalReviewDetailVO,
  PurchaseReversalReviewQuery,
  PurchaseReversalReviewVO
} from '@/api/payment/purchaseReversalReview/types';
import type { PurchaseReversalItemVO } from '@/api/payment/purchaseOrder/types';
import {
  businessLabel,
  businessOptions,
  purchaseReversalDispositionStatusType,
  purchaseReversalReviewOperationType
} from '@/utils/businessLabels';
import { tt } from '@/utils/i18nText';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const { width: viewportWidth } = useWindowSize();
const detailColumns = computed(() => (viewportWidth.value < 768 ? 1 : 2));
const drawerSize = computed(() => (viewportWidth.value < 768 ? '100%' : 'min(1040px, 86vw)'));

const loading = ref(true);
const detailLoading = ref(false);
const submitting = ref(false);
const showSearch = ref(true);
const detailOpen = ref(false);
const lossDialogOpen = ref(false);
const total = ref(0);
const reviewList = ref<PurchaseReversalReviewVO[]>([]);
const detail = ref<Partial<PurchaseReversalReviewDetailVO>>({ items: [], grantSnapshots: [], paymentEvents: [], reviewLogs: [] });
const queryFormRef = ref<ElFormInstance>();
const lossFormRef = ref<ElFormInstance>();
const activeRequestKey = ref('');
const lossForm = reactive({ reviewNote: '' });

const queryParams = ref<PurchaseReversalReviewQuery>({
  pageNum: 1,
  pageSize: 10,
  reversalNo: '',
  purchaseOrderNo: '',
  memberNo: '',
  reversalType: '',
  dispositionStatus: 'PENDING_REVIEW'
});

const reversalTypeOptions = businessOptions('purchaseReversalType', tt);
const statusFilterOptions = computed(() => [
  { label: t('purchaseReversalReview.filters.pending'), value: 'PENDING_REVIEW' },
  { label: t('purchaseReversalReview.filters.recovered'), value: 'RECOVERY_COMPLETED' },
  { label: t('purchaseReversalReview.filters.loss'), value: 'LOSS_ACCEPTED' }
]);
const lossRules = computed(() => ({
  reviewNote: [{ required: true, whitespace: true, message: t('purchaseReversalReview.loss.noteRequired'), trigger: 'blur' }]
}));

const newRequestKey = () => globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`;
const shortfallItems = (items?: PurchaseReversalItemVO[]) => (items || []).filter((item) => Number(item.shortfallAmount) > 0);
const waitingDuration = (createTime?: string) => {
  if (!createTime) return '-';
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(createTime).getTime()) / 60000));
  if (minutes < 60) return t('purchaseReversalReview.duration.minutes', { count: minutes });
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return t('purchaseReversalReview.duration.hours', { count: hours });
  return t('purchaseReversalReview.duration.days', { count: Math.floor(hours / 24) });
};

const getList = async () => {
  loading.value = true;
  try {
    const res = await listPurchaseReversalReview(queryParams.value);
    reviewList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};
const resetQuery = () => {
  queryFormRef.value?.resetFields();
  queryParams.value.dispositionStatus = 'PENDING_REVIEW';
  handleQuery();
};
const handleStatusFilterChange = () => handleQuery();

const openDetail = async (reversalNo: string) => {
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    const res = await getPurchaseReversalReview(reversalNo);
    detail.value = res.data;
  } finally {
    detailLoading.value = false;
  }
};

const refreshAfterAction = async (nextDetail: PurchaseReversalReviewDetailVO) => {
  detail.value = nextDetail;
  await getList();
};

const confirmRetry = async () => {
  if (!detail.value.reversalNo || submitting.value) return;
  activeRequestKey.value = newRequestKey();
  try {
    await proxy?.$modal.confirm(t('purchaseReversalReview.retry.confirm'));
  } catch {
    activeRequestKey.value = '';
    return;
  }
  submitting.value = true;
  try {
    const res = await retryPurchaseReversalReview(detail.value.reversalNo, { requestKey: activeRequestKey.value });
    await refreshAfterAction(res.data.detail);
    proxy?.$modal[res.data.completed ? 'msgSuccess' : 'msgWarning'](
      t(res.data.completed ? 'purchaseReversalReview.messages.retrySuccess' : 'purchaseReversalReview.messages.retryInsufficient')
    );
  } finally {
    submitting.value = false;
    activeRequestKey.value = '';
  }
};

const openLossDialog = () => {
  activeRequestKey.value = newRequestKey();
  lossForm.reviewNote = '';
  lossDialogOpen.value = true;
  nextTick(() => lossFormRef.value?.clearValidate());
};
const closeLossDialog = () => {
  if (submitting.value) return;
  lossDialogOpen.value = false;
  activeRequestKey.value = '';
};
const submitLoss = async () => {
  if (!detail.value.reversalNo || submitting.value) return;
  const valid = await lossFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  try {
    await proxy?.$modal.confirm(t('purchaseReversalReview.loss.secondConfirm'));
  } catch {
    return;
  }
  submitting.value = true;
  try {
    const res = await acceptPurchaseReversalLoss(detail.value.reversalNo, {
      requestKey: activeRequestKey.value,
      reviewNote: lossForm.reviewNote.trim()
    });
    await refreshAfterAction(res.data.detail);
    lossDialogOpen.value = false;
    proxy?.$modal.msgSuccess(t('purchaseReversalReview.messages.lossAccepted'));
  } finally {
    submitting.value = false;
    activeRequestKey.value = '';
  }
};

onMounted(async () => {
  const reversalNo = typeof route.query.reversalNo === 'string' ? route.query.reversalNo : '';
  if (reversalNo) {
    queryParams.value.reversalNo = reversalNo;
  }
  await getList();
  if (reversalNo) {
    await openDetail(reversalNo);
    await router.replace({ query: { ...route.query, reversalNo: undefined } });
  }
});
</script>

<style scoped>
.reversal-review-page { overflow-x: hidden; }
.review-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.currency-lines { display: flex; flex-wrap: wrap; gap: 4px; }
.review-detail { min-height: 180px; }
.detail-section + .detail-section { margin-top: 22px; }
.detail-section h3 { margin: 0 0 10px; font-size: 15px; font-weight: 600; }
.shortfall { color: var(--el-color-danger); }
.drawer-actions { display: flex; justify-content: flex-end; gap: 8px; }
.loss-lines { display: grid; gap: 6px; margin-bottom: 16px; padding: 10px 12px; border: 1px solid var(--el-border-color); border-radius: 4px; }
.loss-lines > div { display: flex; justify-content: space-between; }
@media (max-width: 767px) {
  .review-toolbar { align-items: flex-start; overflow-x: auto; }
  .review-toolbar :deep(.el-segmented) { flex: 0 0 auto; }
  .drawer-actions { flex-wrap: wrap; }
  .drawer-actions .el-button { flex: 1 1 180px; margin-left: 0; }
}
</style>
