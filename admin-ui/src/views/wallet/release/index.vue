<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="tt('释放号')" prop="releaseNo">
              <el-input v-model="queryParams.releaseNo" :placeholder="tt('请输入释放号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('会员ID')" prop="memberId">
              <el-input v-model="queryParams.memberId" :placeholder="tt('请输入会员ID')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('币种')" prop="currencyCode">
              <el-input v-model="queryParams.currencyCode" :placeholder="tt('币种')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('释放模式')" prop="releaseMode">
              <el-select v-model="queryParams.releaseMode" :placeholder="tt('释放模式')" clearable>
                <el-option v-for="item in modeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('释放状态')" prop="releaseStatus">
              <el-select v-model="queryParams.releaseStatus" :placeholder="tt('释放状态')" clearable>
                <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('业务单号')" prop="businessNo">
              <el-input v-model="queryParams.businessNo" :placeholder="tt('请输入业务单号')" clearable @keyup.enter="handleQuery" />
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
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>
      <el-table v-loading="loading" border :data="releaseList">
        <el-table-column :label="tt('释放号')" align="center" prop="releaseNo" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column :label="tt('会员ID')" align="center" prop="memberId" min-width="120" />
        <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="tt('来源')" align="center" prop="sourceType" min-width="120" :show-overflow-tooltip="true" />
        <el-table-column :label="tt('业务单号')" align="center" prop="businessNo" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column :label="tt('入账金额')" align="right" prop="amount" min-width="130" />
        <el-table-column :label="tt('已释放')" align="right" prop="releasedAmount" min-width="130" />
        <el-table-column :label="tt('已消费')" align="right" prop="consumedAmount" min-width="130" />
        <el-table-column :label="tt('所需流水')" align="right" prop="requiredTurnover" min-width="130" />
        <el-table-column :label="tt('完成流水')" align="right" prop="completedTurnover" min-width="130" />
        <el-table-column :label="tt('模式')" align="center" prop="releaseMode" min-width="140" />
        <el-table-column :label="tt('状态')" align="center" prop="releaseStatus" width="110">
          <template #default="scope">
            <el-tag :type="releaseStatusType(scope.row.releaseStatus)">{{ scope.row.releaseStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('创建时间')" align="center" prop="createTime" width="170">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup name="WalletRelease" lang="ts">
import { tt } from '@/utils/i18nText';
import { normalizeMemberIdQuery } from '@/utils/memberQuery';
import { listRelease } from '@/api/wallet/release';
import { ReleaseQuery, ReleaseVO } from '@/api/wallet/release/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const releaseList = ref<ReleaseVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const modeOptions = ['IMMEDIATE', 'AFTER_TURNOVER', 'NEVER', 'MANUAL_REVIEW'];
const statusOptions = ['RELEASED', 'LOCKED', 'NEVER', 'REVIEWING', 'REJECTED', 'CONSUMED'];

const queryParams = ref<ReleaseQuery>({
  pageNum: 1,
  pageSize: 10,
  releaseNo: '',
  memberId: undefined,
  currencyCode: '',
  sourceType: '',
  businessNo: '',
  releaseMode: '',
  releaseStatus: ''
});

const releaseStatusType = (value?: string) => {
  if (value === 'RELEASED') return 'success';
  if (value === 'LOCKED' || value === 'REVIEWING') return 'warning';
  if (value === 'REJECTED') return 'danger';
  return 'info';
};

const getList = async () => {
  loading.value = true;
  const res = await listRelease(normalizeMemberIdQuery(queryParams.value));
  releaseList.value = res.rows;
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
