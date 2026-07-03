<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="订单号" prop="redemptionOrderNo">
              <el-input v-model="queryParams.redemptionOrderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="会员ID" prop="memberId">
              <el-input v-model="queryParams.memberId" placeholder="请输入会员ID" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="币种" prop="currencyCode">
              <el-select v-model="queryParams.currencyCode" placeholder="请选择币种" clearable class="!w-120px">
                <el-option label="RC" value="RC" />
                <el-option label="SC" value="SC" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-140px">
                <el-option label="待审核" value="PENDING" />
                <el-option label="已通过" value="APPROVED" />
                <el-option label="已拒绝" value="REJECTED" />
                <el-option label="失败" value="FAILED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['redemption:order:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="orderList">
        <el-table-column label="订单号" align="center" prop="redemptionOrderNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="会员ID" align="center" prop="memberId" width="120" />
        <el-table-column label="币种" align="center" prop="currencyCode" width="90" />
        <el-table-column label="金额" align="right" prop="amount" width="130" />
        <el-table-column label="方式" align="center" prop="redemptionMethod" width="120" />
        <el-table-column label="状态" align="center" prop="status" width="110">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="冻结单号" align="center" prop="freezeNo" min-width="170" show-overflow-tooltip />
        <el-table-column label="冻结交易" align="center" prop="freezeWalletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="结算交易" align="center" prop="settleWalletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="释放交易" align="center" prop="releaseWalletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" align="center" width="170" fixed="right">
          <template #default="scope">
            <el-tooltip content="查看详情" placement="top">
              <el-button v-hasPermi="['redemption:order:query']" link type="primary" icon="View" @click="handleDetail(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'PENDING'" content="审核通过" placement="top">
              <el-button v-hasPermi="['redemption:order:approve']" link type="success" icon="CircleCheck" @click="openAudit(scope.row, 'approve')"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'PENDING'" content="审核拒绝" placement="top">
              <el-button v-hasPermi="['redemption:order:reject']" link type="danger" icon="CircleClose" @click="openAudit(scope.row, 'reject')"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="560px" append-to-body>
      <el-form ref="orderFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="会员ID" prop="memberId">
          <el-input v-model="form.memberId" placeholder="请输入会员ID" />
        </el-form-item>
        <el-form-item label="币种" prop="currencyCode">
          <el-select v-model="form.currencyCode" class="w-full">
            <el-option label="RC" value="RC" />
            <el-option label="SC" value="SC" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :precision="6" :min="0.000001" class="w-full" />
        </el-form-item>
        <el-form-item label="方式" prop="redemptionMethod">
          <el-input v-model="form.redemptionMethod" placeholder="默认 SIMULATED" />
        </el-form-item>
        <el-form-item label="账户备注" prop="accountRef">
          <el-input v-model="form.accountRef" placeholder="仅填写模拟或脱敏账户信息" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="auditDialog.visible" :title="auditDialog.title" width="480px" append-to-body>
      <el-form :model="auditForm" label-width="90px">
        <el-form-item label="订单号">
          <span>{{ auditForm.redemptionOrderNo }}</span>
        </el-form-item>
        <el-form-item label="原因" prop="auditReason">
          <el-input v-model="auditForm.auditReason" type="textarea" :rows="3" placeholder="请输入审核原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :type="auditDialog.action === 'approve' ? 'success' : 'danger'" @click="submitAudit">确定</el-button>
          <el-button @click="auditDialog.visible = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" title="兑换订单详情" width="760px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.redemptionOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ detail.memberId }}</el-descriptions-item>
        <el-descriptions-item label="币种">{{ detail.currencyCode }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="方式">{{ detail.redemptionMethod }}</el-descriptions-item>
        <el-descriptions-item label="冻结单号">{{ detail.freezeNo }}</el-descriptions-item>
        <el-descriptions-item label="冻结交易">{{ detail.freezeWalletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item label="结算交易">{{ detail.settleWalletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item label="释放交易">{{ detail.releaseWalletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detail.auditTime }}</el-descriptions-item>
        <el-descriptions-item label="审核原因">{{ detail.auditReason }}</el-descriptions-item>
        <el-descriptions-item label="失败原因" :span="2">{{ detail.failReason }}</el-descriptions-item>
        <el-descriptions-item label="账户备注" :span="2">{{ detail.accountRef }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="RedemptionOrder" lang="ts">
import { addRedemptionOrder, approveRedemptionOrder, getRedemptionOrder, listRedemptionOrder, rejectRedemptionOrder } from '@/api/redemption/order';
import { RedemptionOrderForm, RedemptionOrderQuery, RedemptionOrderVO } from '@/api/redemption/order/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const orderList = ref<RedemptionOrderVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const detailOpen = ref(false);
const queryFormRef = ref<ElFormInstance>();
const orderFormRef = ref<ElFormInstance>();
const detail = ref<Partial<RedemptionOrderVO>>({});

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const auditDialog = reactive({
  visible: false,
  title: '',
  action: ''
});

const auditForm = reactive<RedemptionOrderForm & { redemptionOrderNo?: string; id?: string | number }>({});

const initFormData: RedemptionOrderForm = {
  currencyCode: 'RC',
  redemptionMethod: 'SIMULATED'
};

const data = reactive<PageData<RedemptionOrderForm, RedemptionOrderQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    redemptionOrderNo: '',
    memberId: '',
    currencyCode: '',
    status: ''
  },
  rules: {
    memberId: [{ required: true, message: '会员ID不能为空', trigger: 'blur' }],
    currencyCode: [{ required: true, message: '币种不能为空', trigger: 'change' }],
    amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

const getList = async () => {
  loading.value = true;
  try {
    const res = await listRedemptionOrder(queryParams.value);
    orderList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const statusLabel = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    FAILED: '失败'
  };
  return status ? map[status] || status : '';
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
  handleQuery();
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '新增模拟兑换订单';
};

const submitForm = () => {
  orderFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await addRedemptionOrder(form.value);
      proxy?.$modal.msgSuccess('新增成功');
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
  auditDialog.title = action === 'approve' ? '审核通过' : '审核拒绝';
  auditDialog.visible = true;
};

const submitAudit = async () => {
  if (!auditForm.id) {
    return;
  }
  const actionText = auditDialog.action === 'approve' ? '通过' : '拒绝';
  await proxy?.$modal.confirm(`确认${actionText}该兑换订单？`);
  if (auditDialog.action === 'approve') {
    await approveRedemptionOrder(auditForm.id, auditForm);
  } else {
    await rejectRedemptionOrder(auditForm.id, auditForm);
  }
  proxy?.$modal.msgSuccess('操作成功');
  auditDialog.visible = false;
  await getList();
};

onMounted(() => {
  getList();
});
</script>
