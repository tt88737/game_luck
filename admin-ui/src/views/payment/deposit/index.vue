<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="订单号" prop="depositOrderNo">
              <el-input v-model="queryParams.depositOrderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="会员ID" prop="memberId">
              <el-input v-model="queryParams.memberId" placeholder="请输入会员ID" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="币种" prop="currencyCode">
              <el-select v-model="queryParams.currencyCode" placeholder="请选择币种" clearable class="!w-120px">
                <el-option label="RC" value="RC" />
                <el-option label="SC" value="SC" />
                <el-option label="GC" value="GC" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-140px">
                <el-option label="待支付" value="PENDING" />
                <el-option label="成功" value="SUCCESS" />
                <el-option label="已取消" value="CANCELLED" />
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
            <el-button v-hasPermi="['payment:deposit:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="depositList">
        <el-table-column label="订单号" align="center" prop="depositOrderNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="会员ID" align="center" prop="memberId" width="120" />
        <el-table-column label="币种" align="center" prop="currencyCode" width="90" />
        <el-table-column label="金额" align="right" prop="amount" width="130" />
        <el-table-column label="支付方式" align="center" prop="payMethod" width="120" />
        <el-table-column label="状态" align="center" prop="status" width="110">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="钱包交易号" align="center" prop="walletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="支付时间" align="center" prop="payTime" width="170" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" align="center" width="180" fixed="right">
          <template #default="scope">
            <el-tooltip content="查看详情" placement="top">
              <el-button v-hasPermi="['payment:deposit:query']" link type="primary" icon="View" @click="handleDetail(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'PENDING'" content="模拟支付成功" placement="top">
              <el-button v-hasPermi="['payment:deposit:simulate']" link type="primary" icon="CircleCheck" @click="handleSimulate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'PENDING'" content="取消订单" placement="top">
              <el-button v-hasPermi="['payment:deposit:cancel']" link type="danger" icon="Close" @click="handleCancel(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="520px" append-to-body>
      <el-form ref="depositFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="会员ID" prop="memberId">
          <el-input v-model="form.memberId" placeholder="请输入会员ID" />
        </el-form-item>
        <el-form-item label="币种" prop="currencyCode">
          <el-select v-model="form.currencyCode" class="w-full">
            <el-option label="RC" value="RC" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :precision="6" :min="0.000001" class="w-full" />
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

    <el-dialog v-model="detailOpen" title="充值订单详情" width="640px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.depositOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ detail.memberId }}</el-descriptions-item>
        <el-descriptions-item label="币种">{{ detail.currencyCode }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.payMethod }}</el-descriptions-item>
        <el-descriptions-item label="钱包交易号">{{ detail.walletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item label="支付幂等键">{{ detail.walletIdempotencyKey }}</el-descriptions-item>
        <el-descriptions-item label="失败原因" :span="2">{{ detail.failReason }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="DepositOrder" lang="ts">
import { addDeposit, cancelDeposit, getDeposit, listDeposit, simulateDepositSuccess } from '@/api/payment/deposit';
import { DepositOrderForm, DepositOrderQuery, DepositOrderVO } from '@/api/payment/deposit/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const depositList = ref<DepositOrderVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const detailOpen = ref(false);
const queryFormRef = ref<ElFormInstance>();
const depositFormRef = ref<ElFormInstance>();
const detail = ref<Partial<DepositOrderVO>>({});

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: DepositOrderForm = {
  currencyCode: 'RC',
  payMethod: 'SIMULATED',
  payChannel: 'SIMULATED'
};

const data = reactive<PageData<DepositOrderForm, DepositOrderQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    depositOrderNo: '',
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
  const res = await listDeposit(queryParams.value);
  depositList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const statusLabel = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: '待支付',
    SUCCESS: '成功',
    CANCELLED: '已取消',
    FAILED: '失败'
  };
  return status ? map[status] || status : '';
};

const statusType = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    SUCCESS: 'success',
    CANCELLED: 'info',
    FAILED: 'danger'
  };
  return status ? map[status] || '' : '';
};

const reset = () => {
  form.value = { ...initFormData };
  depositFormRef.value?.resetFields();
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
  dialog.title = '新增模拟充值订单';
};

const submitForm = () => {
  depositFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await addDeposit(form.value);
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

const handleDetail = async (row: DepositOrderVO) => {
  const res = await getDeposit(row.id);
  detail.value = res.data;
  detailOpen.value = true;
};

const handleSimulate = async (row: DepositOrderVO) => {
  await proxy?.$modal.confirm('确认将该充值订单标记为模拟支付成功并执行钱包入账？');
  await simulateDepositSuccess(row.id);
  proxy?.$modal.msgSuccess('模拟支付成功');
  await getList();
};

const handleCancel = async (row: DepositOrderVO) => {
  await proxy?.$modal.confirm('确认取消该充值订单？');
  await cancelDeposit(row.id);
  proxy?.$modal.msgSuccess('取消成功');
  await getList();
};

onMounted(() => {
  getList();
});
</script>
