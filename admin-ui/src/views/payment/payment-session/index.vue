<template>
  <div class="payment-ops-page p-2">
    <el-alert v-if="!canList" :title="t('paymentSession.messages.permissionDenied')" type="warning" show-icon :closable="false" />
    <template v-else>
    <div v-show="showSearch" class="mb-[10px]">
      <el-card shadow="never">
        <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="ops-filter">
          <el-form-item :label="t('paymentSession.fields.sessionNo')" prop="sessionNo"><el-input v-model="queryParams.sessionNo" clearable /></el-form-item>
          <el-form-item :label="t('paymentSession.fields.purchaseOrderNo')" prop="purchaseOrderNo"><el-input v-model="queryParams.purchaseOrderNo" clearable /></el-form-item>
          <el-form-item :label="t('paymentSession.fields.providerSessionNo')" prop="providerSessionNo"><el-input v-model="queryParams.providerSessionNo" clearable /></el-form-item>
          <el-form-item :label="t('paymentSession.fields.member')" prop="memberNo"><el-input v-model="queryParams.memberNo" clearable /></el-form-item>
          <el-form-item :label="t('paymentSession.fields.providerCode')" prop="providerCode"><el-input v-model="queryParams.providerCode" clearable /></el-form-item>
          <el-form-item :label="t('common.status')" prop="status">
            <el-select v-model="queryParams.status" clearable class="!w-160px"><el-option v-for="item in statusOptions" :key="item.value" v-bind="item" /></el-select>
          </el-form-item>
          <el-form-item :label="t('common.currency')" prop="payCurrencyCode"><el-input v-model="queryParams.payCurrencyCode" clearable class="!w-120px" /></el-form-item>
          <el-form-item :label="t('paymentSession.fields.createdRange')">
            <el-date-picker v-model="dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" :range-separator="t('paymentSession.range.to')" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">{{ t('common.search') }}</el-button>
            <el-button icon="Refresh" @click="resetQuery">{{ t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <el-alert v-if="loadError" :title="t('paymentSession.messages.loadFailed')" type="error" show-icon :closable="false" class="mb-[10px]">
      <el-button link type="primary" @click="getList">{{ t('paymentSession.actions.retryLoad') }}</el-button>
    </el-alert>
    <el-card shadow="never">
      <template #header><div class="ops-header"><span>{{ t('paymentSession.title') }}</span><right-toolbar v-model:show-search="showSearch" @query-table="getList" /></div></template>
      <div class="table-scroll">
        <el-table v-loading="loading" border :data="sessionList" :empty-text="t('paymentSession.empty')">
          <el-table-column :label="t('paymentSession.fields.sessionNo')" prop="sessionNo" min-width="190" show-overflow-tooltip />
          <el-table-column :label="t('paymentSession.fields.purchaseOrderNo')" min-width="180"><template #default="scope"><el-button link type="primary" @click="goPurchase(scope.row.purchaseOrderNo)">{{ scope.row.purchaseOrderNo }}</el-button></template></el-table-column>
          <el-table-column :label="t('paymentSession.fields.member')" width="130"><template #default="scope">{{ scope.row.memberNo || scope.row.memberId }}</template></el-table-column>
          <el-table-column :label="t('paymentSession.fields.amount')" width="145" align="right"><template #default="scope">{{ scope.row.payAmount }} {{ scope.row.payCurrencyCode }}</template></el-table-column>
          <el-table-column :label="t('common.status')" width="120" align="center"><template #default="scope"><el-tag :type="paymentSessionStatusType(scope.row.status)">{{ businessLabel('paymentSessionStatus', scope.row.status, tt) }}</el-tag></template></el-table-column>
          <el-table-column :label="t('paymentSession.fields.providerCode')" prop="providerCode" width="130" />
          <el-table-column :label="t('paymentSession.fields.providerSessionNo')" prop="providerSessionNo" min-width="210" show-overflow-tooltip />
          <el-table-column :label="t('paymentSession.fields.expireTime')" prop="expireTime" width="170" />
          <el-table-column :label="t('common.createTime')" prop="createTime" width="170" />
          <el-table-column :label="t('common.operation')" width="76" fixed="right" align="center">
            <template #default="scope"><el-tooltip :content="t('common.detail')"><el-button v-hasPermi="['payment:paymentSession:query']" link type="primary" icon="View" @click="openDetail(scope.row)" /></el-tooltip></template>
          </el-table-column>
        </el-table>
      </div>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-drawer v-model="detailOpen" :title="t('paymentSession.detail.title')" :size="drawerSize" append-to-body>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <el-alert v-else-if="detailError" :title="t('paymentSession.messages.detailFailed')" type="error" show-icon :closable="false" />
      <el-descriptions v-else :column="detailColumns" border>
        <el-descriptions-item :label="t('paymentSession.fields.sessionNo')" class-name="break-value">{{ detail.sessionNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.status')"><el-tag :type="paymentSessionStatusType(detail.status)">{{ businessLabel('paymentSessionStatus', detail.status, tt) }}</el-tag></el-descriptions-item>
        <el-descriptions-item :label="t('paymentSession.fields.purchaseOrderNo')"><el-button link type="primary" @click="goPurchase(detail.purchaseOrderNo)">{{ detail.purchaseOrderNo }}</el-button></el-descriptions-item>
        <el-descriptions-item :label="t('paymentSession.fields.member')">{{ detail.memberNo || detail.memberId }}</el-descriptions-item>
        <el-descriptions-item :label="t('paymentSession.fields.amount')">{{ detail.payAmount }} {{ detail.payCurrencyCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('paymentSession.fields.providerCode')">{{ detail.providerCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('paymentSession.fields.providerSessionNo')" class-name="break-value">{{ detail.providerSessionNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('paymentSession.fields.checkoutUrl')" class-name="break-value">{{ detail.checkoutUrl }}</el-descriptions-item>
        <el-descriptions-item :label="t('paymentSession.fields.expireTime')">{{ detail.expireTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('paymentSession.fields.completedTime')">{{ detail.completedTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.createTime')">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.updateTime')">{{ detail.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
    </template>
  </div>
</template>

<script setup name="PaymentSession" lang="ts">
import { getPaymentSession, listPaymentSession } from '@/api/payment/paymentSession';
import type { PaymentSessionQuery, PaymentSessionVO } from '@/api/payment/paymentSession/types';
import { businessLabel, businessOptions, paymentSessionStatusType } from '@/utils/businessLabels';
import { tt } from '@/utils/i18nText';
import auth from '@/plugins/auth';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const router = useRouter();
const { width } = useWindowSize();
const queryFormRef = ref<ElFormInstance>();
const showSearch = ref(true);
const loading = ref(false);
const loadError = ref(false);
const sessionList = ref<PaymentSessionVO[]>([]);
const total = ref(0);
const dateRange = ref<[string, string] | []>([]);
const detailOpen = ref(false);
const detailLoading = ref(false);
const detailError = ref(false);
const detail = ref<Partial<PaymentSessionVO>>({});
const detailColumns = computed(() => (width.value < 768 ? 1 : 2));
const drawerSize = computed(() => (width.value < 768 ? '100%' : '720px'));
const statusOptions = businessOptions('paymentSessionStatus', tt);
const canList = auth.hasPermi('payment:paymentSession:list');
const queryParams = ref<PaymentSessionQuery>({ pageNum: 1, pageSize: 10, sessionNo: '', purchaseOrderNo: '', providerSessionNo: '', memberNo: '', providerCode: '', status: '', payCurrencyCode: '' });

const getList = async () => {
  loading.value = true;
  loadError.value = false;
  try {
    const params = { ...queryParams.value, beginTime: dateRange.value[0], endTime: dateRange.value[1] };
    const res = await listPaymentSession(params);
    sessionList.value = res.rows;
    total.value = res.total;
  } catch {
    loadError.value = true;
    sessionList.value = [];
    total.value = 0;
  } finally { loading.value = false; }
};
const handleQuery = () => { queryParams.value.pageNum = 1; getList(); };
const resetQuery = () => { queryFormRef.value?.resetFields(); dateRange.value = []; handleQuery(); };
const goPurchase = (purchaseOrderNo?: string) => { if (purchaseOrderNo) router.push({ path: '/payment/purchase-order', query: { purchaseOrderNo } }); };
const openDetail = async (row: PaymentSessionVO) => {
  detailOpen.value = true;
  detailLoading.value = true;
  detailError.value = false;
  try { detail.value = (await getPaymentSession(row.id)).data; } catch { detailError.value = true; } finally { detailLoading.value = false; }
};
if (canList) getList();
</script>

<style scoped>
.payment-ops-page { max-width: 100%; overflow-x: hidden; }
.ops-header { display: flex; align-items: center; justify-content: space-between; min-height: 28px; }
.table-scroll { max-width: 100%; overflow-x: auto; }
:deep(.break-value) { overflow-wrap: anywhere; word-break: break-word; }
@media (max-width: 767px) { .ops-filter :deep(.el-form-item) { margin-right: 0; width: 100%; } .ops-filter :deep(.el-form-item__content), .ops-filter :deep(.el-input), .ops-filter :deep(.el-select), .ops-filter :deep(.el-date-editor) { width: 100% !important; min-width: 0; } }
</style>
