<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="会员ID" prop="memberId">
              <el-input v-model="queryParams.memberId" placeholder="请输入会员ID" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="币种" prop="currencyCode">
              <el-input v-model="queryParams.currencyCode" placeholder="请输入币种编码" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="账户状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="账户状态" clearable>
                <el-option label="正常" value="0" />
                <el-option label="冻结" value="1" />
                <el-option label="禁用" value="2" />
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
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>
      <el-table v-loading="loading" border :data="accountList">
        <el-table-column label="会员ID" align="center" prop="memberId" min-width="130" />
        <el-table-column label="币种" align="center" prop="currencyCode" width="100" />
        <el-table-column label="可用余额" align="right" prop="availableBalance" min-width="140" />
        <el-table-column label="冻结余额" align="right" prop="frozenBalance" min-width="140" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ accountStatus(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="170">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" align="center" prop="updateTime" width="170">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.updateTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup name="WalletAccount" lang="ts">
import { listAccount } from '@/api/wallet/account';
import { AccountQuery, AccountVO } from '@/api/wallet/account/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const accountList = ref<AccountVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();

const queryParams = ref<AccountQuery>({
  pageNum: 1,
  pageSize: 10,
  memberId: undefined,
  currencyCode: '',
  status: ''
});

const accountStatus = (value?: string) => {
  if (value === '0') return '正常';
  if (value === '1') return '冻结';
  if (value === '2') return '禁用';
  return value || '-';
};

const statusType = (value?: string) => {
  if (value === '0') return 'success';
  if (value === '1') return 'warning';
  return 'info';
};

const getList = async () => {
  loading.value = true;
  const res = await listAccount(queryParams.value);
  accountList.value = res.rows;
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
