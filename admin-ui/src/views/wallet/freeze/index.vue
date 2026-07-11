<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="tt('冻结号')" prop="freezeNo">
              <el-input v-model="queryParams.freezeNo" :placeholder="tt('请输入冻结号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('会员ID')" prop="memberId">
              <el-input v-model="queryParams.memberId" :placeholder="tt('请输入会员ID')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('币种')" prop="currencyCode">
              <el-input v-model="queryParams.currencyCode" :placeholder="tt('币种')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('来源')" prop="sourceType">
              <el-select v-model="queryParams.sourceType" :placeholder="tt('请选择来源')" clearable class="!w-140px">
                <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('状态')" prop="status">
              <el-select v-model="queryParams.status" :placeholder="tt('冻结状态')" clearable>
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table v-loading="loading" border :data="freezeList">
        <el-table-column :label="tt('冻结号')" align="center" prop="freezeNo" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column :label="tt('会员ID')" align="center" prop="memberNo" min-width="110">
          <template #default="scope">{{ scope.row.memberNo || scope.row.memberId }}</template>
        </el-table-column>
        <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="tt('冻结金额')" align="right" prop="amount" min-width="130" />
        <el-table-column :label="tt('来源')" align="center" prop="sourceType" min-width="120" :show-overflow-tooltip="true">
          <template #default="scope">{{ businessLabel('sourceType', scope.row.sourceType, tt) }}</template>
        </el-table-column>
        <el-table-column :label="tt('业务单号')" align="center" prop="businessNo" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column :label="tt('状态')" align="center" prop="status" width="110">
          <template #default="scope">
            <el-tag :type="walletStatusType(scope.row.status)">{{ businessLabel('walletFreezeStatus', scope.row.status, tt) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('创建时间')" align="center" prop="createTime" width="170">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="tt('更新时间')" align="center" prop="updateTime" width="170">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.updateTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>
  </div>
</template>

<script setup name="WalletFreeze" lang="ts">
import { tt } from '@/utils/i18nText';
import { normalizeMemberIdQuery } from '@/utils/memberQuery';
import { businessLabel, businessOptions, walletStatusType } from '@/utils/businessLabels';
import { listFreeze } from '@/api/wallet/freeze';
import { FreezeQuery, FreezeVO } from '@/api/wallet/freeze/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const freezeList = ref<FreezeVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const sourceOptions = businessOptions('sourceType', tt);
const statusOptions = businessOptions('walletFreezeStatus', tt);

const queryParams = ref<FreezeQuery>({
  pageNum: 1,
  pageSize: 10,
  freezeNo: '',
  memberId: undefined,
  currencyCode: '',
  sourceType: '',
  businessNo: '',
  status: ''
});

const getList = async () => {
  loading.value = true;
  const res = await listFreeze(normalizeMemberIdQuery(queryParams.value));
  freezeList.value = res.rows;
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
