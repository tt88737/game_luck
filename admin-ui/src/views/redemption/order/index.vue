<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="t('redemptionOrder.fields.redemptionOrderNo')" prop="redemptionOrderNo">
              <el-input v-model="queryParams.redemptionOrderNo" :placeholder="t('redemptionOrder.placeholders.redemptionOrderNo')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('redemptionOrder.fields.memberId')" prop="memberId">
              <el-input v-model="queryParams.memberId" :placeholder="t('redemptionOrder.placeholders.memberId')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('common.currency')" prop="currencyCode">
              <el-select v-model="queryParams.currencyCode" :placeholder="t('redemptionOrder.placeholders.currency')" clearable class="!w-120px">
                <el-option label="RC" value="RC" />
                <el-option label="SC" value="SC" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('common.status')" prop="status">
              <el-select v-model="queryParams.status" :placeholder="t('redemptionOrder.placeholders.status')" clearable class="!w-140px">
                <el-option :label="t('redemptionOrder.status.PENDING')" value="PENDING" />
                <el-option :label="t('redemptionOrder.status.APPROVED')" value="APPROVED" />
                <el-option :label="t('redemptionOrder.status.REJECTED')" value="REJECTED" />
                <el-option :label="t('redemptionOrder.status.FAILED')" value="FAILED" />
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
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['redemption:order:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ t('common.add') }}</el-button>
          </el-col>
          <el-col :span="12">
            <el-segmented v-model="queryParams.status" :options="statusFilterOptions" @change="handleStatusFilterChange" />
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="orderList">
        <el-table-column :label="t('redemptionOrder.fields.redemptionOrderNo')" align="center" prop="redemptionOrderNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('redemptionOrder.fields.memberId')" align="center" prop="memberNo" width="110">
          <template #default="scope">{{ scope.row.memberNo || scope.row.memberId }}</template>
        </el-table-column>
        <el-table-column :label="t('common.currency')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="t('common.amount')" align="right" prop="amount" width="130" />
        <el-table-column :label="t('redemptionOrder.fields.redemptionMethod')" align="center" prop="redemptionMethod" width="120" />
        <el-table-column :label="t('common.status')" align="center" prop="status" width="110">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('redemptionOrder.fields.freezeNo')" align="center" prop="freezeNo" min-width="170" show-overflow-tooltip />
        <el-table-column :label="t('redemptionOrder.fields.freezeWalletTransactionNo')" align="center" prop="freezeWalletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('redemptionOrder.fields.settleWalletTransactionNo')" align="center" prop="settleWalletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('redemptionOrder.fields.releaseWalletTransactionNo')" align="center" prop="releaseWalletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('common.createTime')" align="center" prop="createTime" width="170" />
        <el-table-column :label="t('common.operation')" align="center" width="170" fixed="right">
          <template #default="scope">
            <el-tooltip :content="t('redemptionOrder.actions.view')" placement="top">
              <el-button v-hasPermi="['redemption:order:query']" link type="primary" icon="View" @click="handleDetail(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'PENDING'" :content="t('redemptionOrder.actions.approve')" placement="top">
              <el-button v-hasPermi="['redemption:order:approve']" link type="success" icon="CircleCheck" @click="openAudit(scope.row, 'approve')"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'PENDING'" :content="t('redemptionOrder.actions.reject')" placement="top">
              <el-button v-hasPermi="['redemption:order:reject']" link type="danger" icon="CircleClose" @click="openAudit(scope.row, 'reject')"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="t('redemptionOrder.dialog.add')" width="560px" append-to-body>
      <el-form ref="orderFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('redemptionOrder.fields.memberId')" prop="memberId">
          <el-input v-model="form.memberId" :placeholder="t('redemptionOrder.placeholders.memberId')" />
        </el-form-item>
        <el-form-item :label="t('common.currency')" prop="currencyCode">
          <el-select v-model="form.currencyCode" class="w-full">
            <el-option label="RC" value="RC" />
            <el-option label="SC" value="SC" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.amount')" prop="amount">
          <el-input-number v-model="form.amount" :precision="6" :min="0.000001" class="w-full" />
        </el-form-item>
        <el-form-item :label="t('redemptionOrder.fields.redemptionMethod')" prop="redemptionMethod">
          <el-input v-model="form.redemptionMethod" :placeholder="t('redemptionOrder.placeholders.redemptionMethod')" />
        </el-form-item>
        <el-form-item :label="t('redemptionOrder.fields.accountRef')" prop="accountRef">
          <el-input v-model="form.accountRef" :placeholder="t('redemptionOrder.placeholders.accountRef')" />
        </el-form-item>
        <el-form-item :label="t('common.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :placeholder="t('redemptionOrder.placeholders.remark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="auditDialog.visible" :title="auditTitle" width="480px" append-to-body>
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item :label="t('redemptionOrder.fields.redemptionOrderNo')">
          <span>{{ auditForm.redemptionOrderNo }}</span>
        </el-form-item>
        <el-form-item :label="t('redemptionOrder.fields.auditReason')" prop="auditReason">
          <el-input v-model="auditForm.auditReason" type="textarea" :rows="3" :placeholder="t('redemptionOrder.placeholders.auditReason')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :type="auditDialog.action === 'approve' ? 'success' : 'danger'" :loading="auditSubmitting" @click="submitAudit">{{ t('common.confirm') }}</el-button>
          <el-button @click="auditDialog.visible = false">{{ t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" :title="t('redemptionOrder.dialog.detail')" width="760px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('redemptionOrder.fields.redemptionOrderNo')">{{ detail.redemptionOrderNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.memberId')">{{ detail.memberNo || detail.memberId }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.currency')">{{ detail.currencyCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.amount')">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.redemptionMethod')">{{ detail.redemptionMethod }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.freezeNo')">{{ detail.freezeNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.freezeWalletTransactionNo')">{{ detail.freezeWalletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.settleWalletTransactionNo')">{{ detail.settleWalletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.releaseWalletTransactionNo')">{{ detail.releaseWalletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.auditTime')">{{ detail.auditTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.auditReason')">{{ detail.auditReason }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.failReason')" :span="2">{{ detail.failReason }}</el-descriptions-item>
        <el-descriptions-item :label="t('redemptionOrder.fields.accountRef')" :span="2">{{ detail.accountRef }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.remark')" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="RedemptionOrder" lang="ts">
import { addRedemptionOrder, approveRedemptionOrder, getRedemptionOrder, listRedemptionOrder, rejectRedemptionOrder } from '@/api/redemption/order';
import { RedemptionOrderForm, RedemptionOrderQuery, RedemptionOrderVO } from '@/api/redemption/order/types';
import { normalizeMemberIdQuery } from '@/utils/memberQuery';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { t } = useI18n();

const orderList = ref<RedemptionOrderVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const detailOpen = ref(false);
const queryFormRef = ref<ElFormInstance>();
const orderFormRef = ref<ElFormInstance>();
const auditFormRef = ref<ElFormInstance>();
const auditSubmitting = ref(false);
const detail = ref<Partial<RedemptionOrderVO>>({});

const dialog = reactive({
  visible: false
});

const auditDialog = reactive({
  visible: false,
  action: '' as 'approve' | 'reject' | ''
});

const auditForm = reactive<RedemptionOrderForm & { redemptionOrderNo?: string; id?: string | number }>({});

const initFormData: RedemptionOrderForm = {
  currencyCode: 'RC',
  redemptionMethod: 'SIMULATED'
};

const form = ref<RedemptionOrderForm>({ ...initFormData });
const queryParams = ref<RedemptionOrderQuery>({
  pageNum: 1,
  pageSize: 10,
  redemptionOrderNo: '',
  memberId: '',
  currencyCode: '',
  status: 'PENDING'
});

const auditTitle = computed(() => t(auditDialog.action === 'approve' ? 'redemptionOrder.dialog.approve' : 'redemptionOrder.dialog.reject'));

const statusFilterOptions = computed(() => [
  { label: t('redemptionOrder.filters.pending'), value: 'PENDING' },
  { label: t('redemptionOrder.filters.approved'), value: 'APPROVED' },
  { label: t('redemptionOrder.filters.rejected'), value: 'REJECTED' },
  { label: t('redemptionOrder.filters.failed'), value: 'FAILED' },
  { label: t('redemptionOrder.filters.all'), value: '' }
]);

const rules = computed(() => ({
  memberId: [{ required: true, message: t('redemptionOrder.rules.memberId'), trigger: 'blur' }],
  currencyCode: [{ required: true, message: t('redemptionOrder.rules.currency'), trigger: 'change' }],
  amount: [{ required: true, message: t('redemptionOrder.rules.amount'), trigger: 'blur' }]
}));

const auditRules = computed(() => ({
  auditReason: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (auditDialog.action === 'reject' && !value?.trim()) {
          callback(new Error(t('redemptionOrder.rules.rejectReason')));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ]
}));

const getList = async () => {
  loading.value = true;
  try {
    const res = await listRedemptionOrder(normalizeMemberIdQuery(queryParams.value));
    orderList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const statusLabel = (status?: string) => {
  return status ? t(`redemptionOrder.status.${status}`) || status : '';
};

const statusType = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'info',
    FAILED: 'danger'
  };
  return status ? map[status] || '' : '';
};

const reset = () => {
  form.value = { ...initFormData };
  orderFormRef.value?.resetFields();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  queryParams.value.status = 'PENDING';
  handleQuery();
};

const handleStatusFilterChange = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
};

const submitForm = () => {
  orderFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await addRedemptionOrder(form.value);
      proxy?.$modal.msgSuccess(t('common.success.add'));
      dialog.visible = false;
      await getList();
    }
  });
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const handleDetail = async (row: RedemptionOrderVO) => {
  const res = await getRedemptionOrder(row.id);
  detail.value = res.data;
  detailOpen.value = true;
};

const openAudit = (row: RedemptionOrderVO, action: 'approve' | 'reject') => {
  auditForm.id = row.id;
  auditForm.redemptionOrderNo = row.redemptionOrderNo;
  auditForm.auditReason = '';
  auditDialog.action = action;
  auditDialog.visible = true;
  nextTick(() => auditFormRef.value?.clearValidate());
};

const submitAudit = async () => {
  if (!auditForm.id || !auditDialog.action || auditSubmitting.value) {
    return;
  }
  const valid = await auditFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  const actionText = auditDialog.action === 'approve' ? t('redemptionOrder.actions.approve') : t('redemptionOrder.actions.reject');
  auditSubmitting.value = true;
  try {
    await proxy?.$modal.confirm(t('redemptionOrder.confirm.audit', { action: actionText }));
    if (auditDialog.action === 'approve') {
      await approveRedemptionOrder(auditForm.id, auditForm);
    } else {
      await rejectRedemptionOrder(auditForm.id, auditForm);
    }
    proxy?.$modal.msgSuccess(t('common.success.operate'));
    auditDialog.visible = false;
    await getList();
  } finally {
    auditSubmitting.value = false;
  }
};

onMounted(() => {
  getList();
});
</script>
