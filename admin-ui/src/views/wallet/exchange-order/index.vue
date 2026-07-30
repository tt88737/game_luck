<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="tt('兑换订单号')" prop="exchangeOrderNo">
              <el-input v-model="queryParams.exchangeOrderNo" :placeholder="tt('请输入兑换订单号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('会员ID')" prop="memberId">
              <el-input v-model="queryParams.memberId" :placeholder="tt('请输入会员ID')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('转出币种')" prop="fromCurrencyCode">
              <el-input v-model="queryParams.fromCurrencyCode" clearable class="!w-110px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('转入币种')" prop="toCurrencyCode">
              <el-input v-model="queryParams.toCurrencyCode" clearable class="!w-110px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('状态')" prop="status">
              <el-select v-model="queryParams.status" :placeholder="tt('订单状态')" clearable class="!w-130px">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('扣款流水')" prop="debitTransactionNo">
              <el-input v-model="queryParams.debitTransactionNo" :placeholder="tt('请输入扣款流水号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('入账流水')" prop="creditTransactionNo">
              <el-input v-model="queryParams.creditTransactionNo" :placeholder="tt('请输入入账流水号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('创建时间')">
              <el-date-picker
                v-model="dateRange"
                value-format="YYYY-MM-DD HH:mm:ss"
                type="daterange"
                range-separator="-"
                :start-placeholder="tt('开始日期')"
                :end-placeholder="tt('结束日期')"
              />
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
      <el-table v-loading="loading" border :data="orderList" :empty-text="tt('暂无钱包兑换订单')">
        <el-table-column :label="tt('兑换订单号')" align="center" prop="exchangeOrderNo" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column :label="tt('会员ID')" align="center" prop="memberNo" min-width="120">
          <template #default="scope">{{ scope.row.memberNo || scope.row.memberId }}</template>
        </el-table-column>
        <el-table-column :label="tt('兑换方向')" align="center" width="130">
          <template #default="scope">{{ scope.row.fromCurrencyCode }} -> {{ scope.row.toCurrencyCode }}</template>
        </el-table-column>
        <el-table-column :label="tt('转出金额')" align="right" prop="fromAmount" min-width="120" />
        <el-table-column :label="tt('手续费')" align="right" prop="feeAmount" min-width="110" />
        <el-table-column :label="tt('转入金额')" align="right" prop="toAmount" min-width="120" />
        <el-table-column :label="tt('状态')" align="center" prop="status" width="110">
          <template #default="scope">
            <el-tag :type="walletStatusType(scope.row.status)">{{ businessLabel('walletExchangeOrderStatus', scope.row.status, tt) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('扣款流水')" align="center" prop="debitTransactionNo" min-width="170" :show-overflow-tooltip="true">
          <template #default="scope">
            <el-link v-if="scope.row.debitTransactionNo" type="primary" @click="openTransaction(scope.row.debitTransactionNo)">
              {{ scope.row.debitTransactionNo }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column :label="tt('入账流水')" align="center" prop="creditTransactionNo" min-width="170" :show-overflow-tooltip="true">
          <template #default="scope">
            <el-link v-if="scope.row.creditTransactionNo" type="primary" @click="openTransaction(scope.row.creditTransactionNo)">
              {{ scope.row.creditTransactionNo }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column :label="tt('打码任务')" align="center" prop="turnoverTaskNo" min-width="150" :show-overflow-tooltip="true" />
        <el-table-column :label="tt('失败原因')" align="left" prop="failReason" min-width="160" :show-overflow-tooltip="true" />
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

<script setup name="WalletExchangeOrder" lang="ts">
import { tt } from '@/utils/i18nText';
import { normalizeMemberIdQuery } from '@/utils/memberQuery';
import { businessLabel, businessOptions, walletStatusType } from '@/utils/businessLabels';
import { listExchangeOrder } from '@/api/wallet/exchangeOrder';
import { ExchangeOrderQuery, ExchangeOrderVO } from '@/api/wallet/exchangeOrder/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();
const route = useRoute();

const orderList = ref<ExchangeOrderVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const dateRange = ref<[DateModelType, DateModelType]>(['', '']);
const statusOptions = businessOptions('walletExchangeOrderStatus', tt);

const queryParams = ref<ExchangeOrderQuery>({
  pageNum: 1,
  pageSize: 10,
  exchangeOrderNo: typeof route.query.exchangeOrderNo === 'string' ? route.query.exchangeOrderNo : '',
  memberId: undefined,
  fromCurrencyCode: '',
  toCurrencyCode: '',
  debitTransactionNo: '',
  creditTransactionNo: '',
  status: ''
});

const getList = async () => {
  loading.value = true;
  const params = proxy?.addDateRange(normalizeMemberIdQuery(queryParams.value), dateRange.value);
  const res = await listExchangeOrder(params);
  orderList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  dateRange.value = ['', ''];
  handleQuery();
};

const openTransaction = (transactionNo: string) => {
  router.push({ path: '/wallet/transaction', query: { transactionNo } });
};

watch(
  () => route.query.exchangeOrderNo,
  (exchangeOrderNo) => {
    queryParams.value.exchangeOrderNo = typeof exchangeOrderNo === 'string' ? exchangeOrderNo : '';
    handleQuery();
  }
);

onMounted(() => {
  getList();
});
</script>
