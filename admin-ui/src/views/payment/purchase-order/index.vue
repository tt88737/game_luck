<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="tt('购买订单号')" prop="purchaseOrderNo">
              <el-input v-model="queryParams.purchaseOrderNo" :placeholder="tt('请输入购买订单号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('会员ID')" prop="memberId">
              <el-input v-model="queryParams.memberId" :placeholder="tt('请输入会员ID')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('状态')" prop="status">
              <el-select v-model="queryParams.status" :placeholder="tt('请选择状态')" clearable class="!w-140px">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('支付方')" prop="providerCode">
              <el-input v-model="queryParams.providerCode" :placeholder="tt('请输入支付方')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('支付方订单号')" prop="providerOrderNo">
              <el-input v-model="queryParams.providerOrderNo" :placeholder="tt('请输入支付方订单号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('支付会话号')" prop="paymentSessionNo">
              <el-input v-model="queryParams.paymentSessionNo" :placeholder="tt('请输入支付会话号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('幂等键')" prop="idempotencyKey">
              <el-input v-model="queryParams.idempotencyKey" :placeholder="tt('请输入幂等键')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">{{ tt('搜索') }}</el-button>
              <el-button icon="Refresh" @click="resetQuery">{{ tt('重置') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="14">
            <el-segmented v-model="queryParams.status" :options="statusFilterOptions" @change="handleStatusFilterChange" />
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="orderList">
        <el-table-column :label="tt('购买订单号')" align="center" prop="purchaseOrderNo" min-width="190" show-overflow-tooltip />
        <el-table-column :label="tt('会员ID')" align="center" prop="memberNo" width="110">
          <template #default="scope">{{ scope.row.memberNo || scope.row.memberId }}</template>
        </el-table-column>
        <el-table-column :label="tt('产品号')" align="center" prop="offerNo" min-width="130" show-overflow-tooltip />
        <el-table-column :label="tt('支付币种')" align="center" prop="payCurrencyCode" width="90" />
        <el-table-column :label="tt('支付金额')" align="right" prop="payAmount" width="120" />
        <el-table-column :label="tt('状态')" align="center" prop="status" width="110">
          <template #default="scope">
            <el-tag :type="purchaseOrderStatusType(scope.row.status)">{{ businessLabel('purchaseOrderStatus', scope.row.status, tt) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('支付方')" align="center" prop="providerCode" width="120" show-overflow-tooltip />
        <el-table-column :label="tt('支付方订单号')" align="center" prop="providerOrderNo" min-width="190" show-overflow-tooltip />
        <el-table-column :label="tt('支付会话号')" align="center" prop="paymentSessionNo" min-width="190" show-overflow-tooltip />
        <el-table-column :label="tt('回调事件键')" align="center" prop="callbackEventKey" min-width="220" show-overflow-tooltip />
        <el-table-column :label="tt('到账时间')" align="center" prop="creditedTime" width="170" />
        <el-table-column :label="tt('创建时间')" align="center" prop="createTime" width="170" />
        <el-table-column :label="tt('操作')" align="center" width="190" fixed="right">
          <template #default="scope">
            <el-tooltip :content="tt('查看详情')" placement="top">
              <el-button v-hasPermi="['payment:purchaseOrder:query']" link type="primary" icon="View" @click="handleDetail(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="canClose(scope.row.status)" :content="tt('标记失败')" placement="top">
              <el-button v-hasPermi="['payment:purchaseOrder:manual']" link type="danger" icon="CircleClose" @click="openManual(scope.row, 'failed')"></el-button>
            </el-tooltip>
            <el-tooltip v-if="canClose(scope.row.status)" :content="tt('取消订单')" placement="top">
              <el-button v-hasPermi="['payment:purchaseOrder:manual']" link type="warning" icon="Close" @click="openManual(scope.row, 'cancel')"></el-button>
            </el-tooltip>
            <el-tooltip v-if="canReverse(scope.row.status)" :content="tt('记录退款')" placement="top">
              <el-button v-hasPermi="['payment:purchaseOrder:manual']" link type="primary" icon="RefreshLeft" @click="openManual(scope.row, 'refund')"></el-button>
            </el-tooltip>
            <el-tooltip v-if="canReverse(scope.row.status)" :content="tt('记录拒付')" placement="top">
              <el-button v-hasPermi="['payment:purchaseOrder:manual']" link type="danger" icon="Warning" @click="openManual(scope.row, 'chargeback')"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="detailOpen" :title="tt('购买订单详情')" width="min(980px, 92vw)" append-to-body>
      <el-descriptions class="purchase-order-detail" :column="detailColumns" border>
        <el-descriptions-item :label="tt('购买订单号')">{{ detail.purchaseOrderNo }}</el-descriptions-item>
        <el-descriptions-item :label="tt('状态')">{{ businessLabel('purchaseOrderStatus', detail.status, tt) }}</el-descriptions-item>
        <el-descriptions-item :label="tt('会员ID')">{{ detail.memberNo || detail.memberId }}</el-descriptions-item>
        <el-descriptions-item :label="tt('产品号')">{{ detail.offerNo }}</el-descriptions-item>
        <el-descriptions-item :label="tt('支付币种')">{{ detail.payCurrencyCode }}</el-descriptions-item>
        <el-descriptions-item :label="tt('支付金额')">{{ detail.payAmount }}</el-descriptions-item>
        <el-descriptions-item :label="tt('支付方')">{{ detail.providerCode }}</el-descriptions-item>
        <el-descriptions-item :label="tt('支付方订单号')">{{ detail.providerOrderNo }}</el-descriptions-item>
        <el-descriptions-item :label="tt('支付会话号')">{{ detail.paymentSessionNo }}</el-descriptions-item>
        <el-descriptions-item :label="tt('客户端幂等键')" :span="2">{{ detail.idempotencyKey }}</el-descriptions-item>
        <el-descriptions-item :label="tt('回调事件键')">{{ detail.callbackEventKey }}</el-descriptions-item>
        <el-descriptions-item :label="tt('失败原因')" :span="2">{{ detail.failReason }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">{{ tt('发放快照') }}</el-divider>
      <el-table border :data="detail.grantSnapshots || []" max-height="220">
        <el-table-column :label="tt('发放类型')" align="center" prop="grantType" min-width="120" />
        <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="tt('金额')" align="right" prop="grantAmount" width="120" />
        <el-table-column :label="tt('资金属性')" align="center" prop="fundPropertyCode" min-width="130" />
        <el-table-column :label="tt('钱包交易号')" align="center" prop="walletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="tt('流水任务号')" align="center" prop="turnoverTaskNo" min-width="180" show-overflow-tooltip />
      </el-table>

      <template v-if="detail.reversal">
        <el-divider content-position="left">{{ tt('资产追偿') }}</el-divider>
        <el-descriptions class="purchase-order-detail" :column="detailColumns" border>
          <el-descriptions-item :label="tt('追偿单号')">{{ detail.reversal.reversalNo }}</el-descriptions-item>
          <el-descriptions-item :label="tt('追偿类型')">
            {{ businessLabel('purchaseReversalType', detail.reversal.reversalType, tt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="tt('追偿状态')">
            <el-tag :type="purchaseReversalStatusType(detail.reversal.status)">
              {{ businessLabel('purchaseReversalStatus', detail.reversal.status, tt) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="tt('完成时间')">{{ detail.reversal.completedTime || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="tt('处理原因')" :span="2">{{ detail.reversal.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="tt('复核原因')" :span="2">{{ detail.reversal.reviewReason || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.reversal.dispositionStatus === 'PENDING_REVIEW'" :label="tt('审核处置')" :span="2">
            <el-button
              v-hasPermi="['payment:reversalReview:query']"
              type="primary"
              plain
              icon="Right"
              @click="goToReversalReview(detail.reversal.reversalNo)"
            >
              {{ tt('前往拒付审核') }}
            </el-button>
          </el-descriptions-item>
        </el-descriptions>
        <el-table class="mt-3" border :data="detail.reversal.items || []" max-height="240">
          <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" fixed="left" />
          <el-table-column :label="tt('应追回')" align="right" prop="requiredAmount" min-width="120" />
          <el-table-column :label="tt('可用余额')" align="right" prop="availableAmount" min-width="120" />
          <el-table-column :label="tt('已追回')" align="right" prop="recoveredAmount" min-width="120" />
          <el-table-column :label="tt('短缺金额')" align="right" prop="shortfallAmount" min-width="120">
            <template #default="scope">
              <span :class="{ 'reversal-shortfall': Number(scope.row.shortfallAmount) > 0 }">{{ scope.row.shortfallAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="tt('明细状态')" align="center" prop="status" min-width="130">
            <template #default="scope">
              <el-tag :type="purchaseReversalStatusType(scope.row.status)">
                {{ businessLabel('purchaseReversalStatus', scope.row.status, tt) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="tt('钱包交易号')" align="center" prop="walletTransactionNo" min-width="190" show-overflow-tooltip />
        </el-table>
      </template>
      <template v-else>
        <el-divider content-position="left">{{ tt('资产追偿') }}</el-divider>
        <el-empty :image-size="48" :description="tt('暂无追偿记录')" />
      </template>

      <el-divider content-position="left">{{ tt('支付事件') }}</el-divider>
      <el-table border :data="detail.paymentEvents || []" max-height="260">
        <el-table-column :label="tt('事件键')" align="center" prop="eventKey" min-width="240" show-overflow-tooltip />
        <el-table-column :label="tt('事件类型')" align="center" prop="eventType" width="120">
          <template #default="scope">{{ businessLabel('purchasePaymentEventType', scope.row.eventType, tt) }}</template>
        </el-table-column>
        <el-table-column :label="tt('处理状态')" align="center" prop="eventStatus" width="110">
          <template #default="scope">
            <el-tag :type="purchasePaymentEventStatusType(scope.row.eventStatus)">{{ businessLabel('purchasePaymentEventStatus', scope.row.eventStatus, tt) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('支付方')" align="center" prop="providerCode" width="130" />
        <el-table-column :label="tt('处理结果')" align="center" prop="processResult" min-width="160" show-overflow-tooltip />
        <el-table-column :label="tt('处理时间')" align="center" prop="processTime" width="170" />
        <el-table-column :label="tt('创建时间')" align="center" prop="createTime" width="170" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="manualDialog.visible" :title="manualTitle" width="520px" append-to-body>
      <el-form ref="manualFormRef" :model="manualForm" :rules="manualRules" label-width="90px">
        <el-form-item :label="tt('购买订单号')">
          <span>{{ manualDialog.purchaseOrderNo }}</span>
        </el-form-item>
        <el-form-item :label="tt('当前状态')">
          <span>{{ businessLabel('purchaseOrderStatus', manualDialog.status, tt) }}</span>
        </el-form-item>
        <el-form-item :label="tt('处理原因')" prop="reason">
          <el-input v-model="manualForm.reason" type="textarea" :rows="4" maxlength="500" show-word-limit :placeholder="tt('请输入人工处理原因')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="manualSubmitting" @click="submitManual">{{ tt('确定') }}</el-button>
          <el-button @click="manualDialog.visible = false">{{ tt('取消') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PurchaseOrder" lang="ts">
import {
  cancelPurchaseOrder,
  chargebackPurchaseOrder,
  getPurchaseOrder,
  listPurchaseOrder,
  markPurchaseOrderFailed,
  refundPurchaseOrder
} from '@/api/payment/purchaseOrder';
import { PurchaseManualActionForm, PurchaseOrderDetailVO, PurchaseOrderQuery, PurchaseOrderVO } from '@/api/payment/purchaseOrder/types';
import {
  businessLabel,
  businessOptions,
  purchaseOrderStatusType,
  purchasePaymentEventStatusType,
  purchaseReversalStatusType
} from '@/utils/businessLabels';
import { tt } from '@/utils/i18nText';
import { normalizeMemberIdQuery } from '@/utils/memberQuery';

type ManualAction = 'failed' | 'cancel' | 'refund' | 'chargeback';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();
const { width: viewportWidth } = useWindowSize();
const detailColumns = computed(() => (viewportWidth.value < 768 ? 1 : 2));

const orderList = ref<PurchaseOrderVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const detailOpen = ref(false);
const manualSubmitting = ref(false);
const queryFormRef = ref<ElFormInstance>();
const manualFormRef = ref<ElFormInstance>();
const detail = ref<Partial<PurchaseOrderDetailVO>>({ grantSnapshots: [], paymentEvents: [] });

const goToReversalReview = (reversalNo: string) => {
  detailOpen.value = false;
  router.push({ path: '/payment/purchase-reversal-review', query: { reversalNo } });
};
const statusOptions = businessOptions('purchaseOrderStatus', tt);

const queryParams = ref<PurchaseOrderQuery>({
  pageNum: 1,
  pageSize: 10,
  purchaseOrderNo: '',
  memberId: '',
  status: '',
  providerCode: '',
  providerOrderNo: '',
  paymentSessionNo: '',
  idempotencyKey: ''
});

const manualDialog = reactive({
  visible: false,
  action: '' as ManualAction | '',
  id: '' as string | number,
  purchaseOrderNo: '',
  status: ''
});

const manualForm = reactive<PurchaseManualActionForm>({
  reason: ''
});

const statusFilterOptions = computed(() => [
  { label: tt('全部'), value: '' },
  { label: tt('待支付'), value: 'PENDING' },
  { label: tt('已入账'), value: 'CREDITED' },
  { label: tt('失败'), value: 'FAILED' },
  { label: tt('已取消'), value: 'CANCELLED' },
  { label: tt('已退款'), value: 'REFUNDED' },
  { label: tt('拒付'), value: 'CHARGEBACK' },
  { label: tt('退款待复核'), value: 'REFUND_REVIEW' },
  { label: tt('拒付待复核'), value: 'CHARGEBACK_REVIEW' }
]);

const manualTitle = computed(() => {
  const map: Record<ManualAction, string> = {
    failed: tt('标记失败'),
    cancel: tt('取消订单'),
    refund: tt('记录退款'),
    chargeback: tt('记录拒付')
  };
  return manualDialog.action ? map[manualDialog.action] : tt('人工处理');
});

const manualRules = computed(() => ({
  reason: [{ required: true, message: tt('处理原因不能为空'), trigger: 'blur' }]
}));

const canClose = (status?: string) => status === 'CREATED' || status === 'PENDING';
const canReverse = (status?: string) => status === 'PAID' || status === 'CREDITED';

const getList = async () => {
  loading.value = true;
  try {
    const res = await listPurchaseOrder(normalizeMemberIdQuery(queryParams.value));
    orderList.value = res.rows;
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
  queryParams.value.status = '';
  handleQuery();
};

const handleStatusFilterChange = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const handleDetail = async (row: PurchaseOrderVO) => {
  const res = await getPurchaseOrder(row.id);
  detail.value = res.data;
  detailOpen.value = true;
};

const openManual = (row: PurchaseOrderVO, action: ManualAction) => {
  manualDialog.id = row.id;
  manualDialog.purchaseOrderNo = row.purchaseOrderNo;
  manualDialog.status = row.status;
  manualDialog.action = action;
  manualForm.reason = '';
  manualDialog.visible = true;
  nextTick(() => manualFormRef.value?.clearValidate());
};

const submitManual = async () => {
  if (!manualDialog.id || !manualDialog.action || manualSubmitting.value) {
    return;
  }
  const valid = await manualFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  const reversalAction = manualDialog.action === 'refund' || manualDialog.action === 'chargeback';
  const confirmText = reversalAction
    ? tt('确认提交该处理结果？系统将尝试全额追回已发放资产；余额不足时将进入人工复核。')
    : tt('确认提交该人工处理结果？');
  await proxy?.$modal.confirm(confirmText);
  manualSubmitting.value = true;
  try {
    const actionMap = {
      failed: markPurchaseOrderFailed,
      cancel: cancelPurchaseOrder,
      refund: refundPurchaseOrder,
      chargeback: chargebackPurchaseOrder
    };
    const res = await actionMap[manualDialog.action](manualDialog.id, manualForm);
    detail.value = res.data;
    proxy?.$modal.msgSuccess(tt('操作成功'));
    manualDialog.visible = false;
    await getList();
  } finally {
    manualSubmitting.value = false;
  }
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.purchase-order-detail :deep(.el-descriptions__label) {
  width: 120px;
  white-space: nowrap;
}

.purchase-order-detail :deep(.el-descriptions__content) {
  overflow-wrap: anywhere;
}

.reversal-shortfall {
  color: var(--el-color-danger);
  font-weight: 600;
}

@media (max-width: 767px) {
  .purchase-order-detail :deep(.el-descriptions__label) {
    width: 96px;
  }
}
</style>
