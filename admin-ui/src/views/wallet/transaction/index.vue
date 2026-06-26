<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="交易号" prop="transactionNo">
              <el-input v-model="queryParams.transactionNo" placeholder="请输入交易号" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="会员ID" prop="memberId">
              <el-input v-model="queryParams.memberId" placeholder="请输入会员ID" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="币种" prop="currencyCode">
              <el-input v-model="queryParams.currencyCode" placeholder="币种" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="操作" prop="operation">
              <el-select v-model="queryParams.operation" placeholder="操作类型" clearable>
                <el-option v-for="item in operationOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="交易状态" clearable>
                <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="业务单号" prop="businessNo">
              <el-input v-model="queryParams.businessNo" placeholder="请输入业务单号" clearable @keyup.enter="handleQuery" />
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
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>
      <el-table v-loading="loading" border :data="transactionList">
        <el-table-column label="交易号" align="center" prop="transactionNo" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column label="会员ID" align="center" prop="memberId" min-width="120" />
        <el-table-column label="币种" align="center" prop="currencyCode" width="90" />
        <el-table-column label="操作" align="center" prop="operation" width="110" />
        <el-table-column label="来源" align="center" prop="sourceType" min-width="120" :show-overflow-tooltip="true" />
        <el-table-column label="业务单号" align="center" prop="businessNo" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column label="金额" align="right" prop="amount" min-width="130" />
        <el-table-column label="变更前" align="right" prop="balanceBefore" min-width="130" />
        <el-table-column label="变更后" align="right" prop="balanceAfter" min-width="130" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="transactionStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" align="left" prop="failReason" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup name="WalletTransaction" lang="ts">
import { listTransaction } from '@/api/wallet/transaction';
import { TransactionQuery, TransactionVO } from '@/api/wallet/transaction/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const transactionList = ref<TransactionVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const operationOptions = ['CREDIT', 'DEBIT', 'FREEZE', 'UNFREEZE', 'SETTLE', 'ADJUST', 'REVERSE', 'TURNOVER'];
const statusOptions = ['PENDING', 'SUCCESS', 'FAILED', 'REVERSED'];

const queryParams = ref<TransactionQuery>({
  pageNum: 1,
  pageSize: 10,
  transactionNo: '',
  memberId: undefined,
  currencyCode: '',
  operation: '',
  sourceType: '',
  businessNo: '',
  status: ''
});

const transactionStatusType = (value?: string) => {
  if (value === 'SUCCESS') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'PENDING') return 'warning';
  return 'info';
};

const getList = async () => {
  loading.value = true;
  const res = await listTransaction(queryParams.value);
  transactionList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

onMounted(() => {
  getList();
});
</script>
