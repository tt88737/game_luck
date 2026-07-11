<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="tt('注单号')" prop="betOrderNo">
              <el-input v-model="queryParams.betOrderNo" :placeholder="tt('请输入注单号')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('会员ID')" prop="memberId">
              <el-input v-model="queryParams.memberId" :placeholder="tt('请输入会员ID')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('币种')" prop="currencyCode">
              <el-select v-model="queryParams.currencyCode" :placeholder="tt('请选择币种')" clearable class="!w-120px">
                <el-option label="SC" value="SC" />
                <el-option label="GC" value="GC" />
                <el-option label="RC" value="RC" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('游戏')" prop="gameCode">
              <el-input v-model="queryParams.gameCode" :placeholder="tt('游戏编码')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('状态')" prop="status">
              <el-select v-model="queryParams.status" :placeholder="tt('请选择状态')" clearable class="!w-150px">
                <el-option :label="tt('待下注')" value="PENDING" />
                <el-option :label="tt('已扣款')" value="BET_SUCCESS" />
                <el-option :label="tt('扣款失败')" value="BET_FAILED" />
                <el-option :label="tt('已结算')" value="SETTLED" />
                <el-option :label="tt('结算失败')" value="SETTLE_FAILED" />
                <el-option :label="tt('已取消')" value="CANCELLED" />
              </el-select>
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
          <el-col :span="1.5">
            <el-button v-hasPermi="['game:bet:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ tt('新增') }}</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="betList">
        <el-table-column :label="tt('注单号')" align="center" prop="betOrderNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="tt('会员ID')" align="center" prop="memberId" width="120" />
        <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="tt('游戏')" align="center" prop="gameCode" width="120" show-overflow-tooltip />
        <el-table-column :label="tt('局号')" align="center" prop="roundNo" min-width="150" show-overflow-tooltip />
        <el-table-column :label="tt('下注金额')" align="right" prop="betAmount" width="120" />
        <el-table-column :label="tt('派彩金额')" align="right" prop="payoutAmount" width="120" />
        <el-table-column :label="tt('净额')" align="right" prop="netAmount" width="120" />
        <el-table-column :label="tt('状态')" align="center" prop="status" width="110">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('下注交易号')" align="center" prop="betWalletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="tt('结算交易号')" align="center" prop="settleWalletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="tt('退款交易号')" align="center" prop="refundWalletTransactionNo" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <el-button
              v-if="scope.row.refundWalletTransactionNo"
              link
              type="primary"
              @click="goWalletTransaction(scope.row.refundWalletTransactionNo)"
            >
              {{ scope.row.refundWalletTransactionNo }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="tt('创建时间')" align="center" prop="createTime" width="170" />
        <el-table-column :label="tt('操作')" align="center" width="180" fixed="right">
          <template #default="scope">
            <el-tooltip :content="tt('查看详情')" placement="top">
              <el-button v-hasPermi="['game:bet:query']" link type="primary" icon="View" @click="handleDetail(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'PENDING'" :content="tt('模拟下注扣款')" placement="top">
              <el-button v-hasPermi="['game:bet:place']" link type="primary" icon="CircleCheck" @click="handlePlace(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'BET_SUCCESS'" :content="tt('模拟结算派彩')" placement="top">
              <el-button v-hasPermi="['game:bet:settle']" link type="success" icon="Money" @click="handleSettle(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.status === 'BET_SUCCESS'" :content="tt('取消退款')" placement="top">
              <el-button v-hasPermi="['game:bet:cancel']" link type="warning" icon="RefreshLeft" @click="handleCancelBet(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="560px" append-to-body>
      <el-form ref="betFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="tt('会员ID')" prop="memberId">
          <el-input v-model="form.memberId" :placeholder="tt('请输入会员ID')" />
        </el-form-item>
        <el-form-item :label="tt('币种')" prop="currencyCode">
          <el-select v-model="form.currencyCode" class="w-full">
            <el-option label="SC" value="SC" />
          </el-select>
        </el-form-item>
        <el-form-item :label="tt('游戏编码')" prop="gameCode">
          <el-input v-model="form.gameCode" :placeholder="tt('默认 SIMULATED')" />
        </el-form-item>
        <el-form-item :label="tt('局号')" prop="roundNo">
          <el-input v-model="form.roundNo" :placeholder="tt('为空则自动生成')" />
        </el-form-item>
        <el-form-item :label="tt('下注金额')" prop="betAmount">
          <el-input-number v-model="form.betAmount" :precision="6" :min="0.000001" class="w-full" />
        </el-form-item>
        <el-form-item :label="tt('派彩金额')" prop="payoutAmount">
          <el-input-number v-model="form.payoutAmount" :precision="6" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item :label="tt('备注')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :placeholder="tt('请输入备注')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ tt('确定') }}</el-button>
          <el-button @click="cancel">{{ tt('取消') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" :title="tt('模拟下注订单详情')" width="720px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="tt('注单号')">{{ detail.betOrderNo }}</el-descriptions-item>
        <el-descriptions-item :label="tt('状态')">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item :label="tt('会员ID')">{{ detail.memberId }}</el-descriptions-item>
        <el-descriptions-item :label="tt('币种')">{{ detail.currencyCode }}</el-descriptions-item>
        <el-descriptions-item :label="tt('游戏')">{{ detail.gameCode }}</el-descriptions-item>
        <el-descriptions-item :label="tt('局号')">{{ detail.roundNo }}</el-descriptions-item>
        <el-descriptions-item :label="tt('下注金额')">{{ detail.betAmount }}</el-descriptions-item>
        <el-descriptions-item :label="tt('派彩金额')">{{ detail.payoutAmount }}</el-descriptions-item>
        <el-descriptions-item :label="tt('下注交易号')">{{ detail.betWalletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item :label="tt('结算交易号')">{{ detail.settleWalletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item :label="tt('退款交易号')">
          <el-button v-if="detail.refundWalletTransactionNo" link type="primary" @click="goWalletTransaction(detail.refundWalletTransactionNo)">
            {{ detail.refundWalletTransactionNo }}
          </el-button>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="tt('下注幂等键')">{{ detail.betIdempotencyKey }}</el-descriptions-item>
        <el-descriptions-item :label="tt('结算幂等键')">{{ detail.settleIdempotencyKey }}</el-descriptions-item>
        <el-descriptions-item :label="tt('退款幂等键')">{{ detail.refundIdempotencyKey }}</el-descriptions-item>
        <el-descriptions-item :label="tt('取消时间')">{{ detail.cancelTime }}</el-descriptions-item>
        <el-descriptions-item :label="tt('失败原因')" :span="2">{{ detail.failReason }}</el-descriptions-item>
        <el-descriptions-item :label="tt('备注')" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="GameBetOrder" lang="ts">
import { tt } from '@/utils/i18nText';
import { normalizeMemberIdQuery } from '@/utils/memberQuery';
import { addGameBet, cancelGameBet, getGameBet, listGameBet, placeGameBet, settleGameBet } from '@/api/game/bet';
import { GameBetOrderForm, GameBetOrderQuery, GameBetOrderVO } from '@/api/game/bet/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();

const betList = ref<GameBetOrderVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const detailOpen = ref(false);
const queryFormRef = ref<ElFormInstance>();
const betFormRef = ref<ElFormInstance>();
const detail = ref<Partial<GameBetOrderVO>>({});

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: GameBetOrderForm = {
  currencyCode: 'SC',
  gameCode: 'SIMULATED',
  betAmount: 1,
  payoutAmount: 0
};

const data = reactive<PageData<GameBetOrderForm, GameBetOrderQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    betOrderNo: '',
    memberId: '',
    currencyCode: '',
    gameCode: '',
    roundNo: '',
    status: ''
  },
  rules: {
    memberId: [{ required: true, message: tt('会员ID不能为空'), trigger: 'blur' }],
    currencyCode: [{ required: true, message: tt('币种不能为空'), trigger: 'change' }],
    betAmount: [{ required: true, message: tt('下注金额不能为空'), trigger: 'blur' }],
    payoutAmount: [{ required: true, message: tt('派彩金额不能为空'), trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

const getList = async () => {
  loading.value = true;
  const res = await listGameBet(normalizeMemberIdQuery(queryParams.value));
  betList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const statusLabel = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: tt('待下注'),
    BET_SUCCESS: tt('已扣款'),
    BET_FAILED: tt('扣款失败'),
    SETTLED: tt('已结算'),
    SETTLE_FAILED: tt('结算失败'),
    CANCELLED: tt('已取消')
  };
  return status ? map[status] || status : '';
};

const statusType = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    BET_SUCCESS: 'primary',
    BET_FAILED: 'danger',
    SETTLED: 'success',
    SETTLE_FAILED: 'danger',
    CANCELLED: 'info'
  };
  return status ? map[status] || '' : '';
};

const reset = () => {
  form.value = { ...initFormData };
  betFormRef.value?.resetFields();
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
  dialog.title = tt('新增模拟下注订单');
};

const submitForm = () => {
  betFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await addGameBet(form.value);
      proxy?.$modal.msgSuccess(tt('新增成功'));
      dialog.visible = false;
      await getList();
    }
  });
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const handleDetail = async (row: GameBetOrderVO) => {
  const res = await getGameBet(row.id);
  detail.value = res.data;
  detailOpen.value = true;
};

const handlePlace = async (row: GameBetOrderVO) => {
  await proxy?.$modal.confirm(tt('确认对该模拟下注订单执行钱包扣款？'));
  await placeGameBet(row.id);
  proxy?.$modal.msgSuccess(tt('下注扣款完成'));
  await getList();
};

const handleSettle = async (row: GameBetOrderVO) => {
  await proxy?.$modal.confirm(tt('确认对该模拟下注订单执行结算派彩？'));
  await settleGameBet(row.id);
  proxy?.$modal.msgSuccess(tt('结算派彩完成'));
  await getList();
};

const handleCancelBet = async (row: GameBetOrderVO) => {
  await proxy?.$modal.confirm(tt('确认取消该模拟下注订单并退回下注金额？'));
  await cancelGameBet(row.id);
  proxy?.$modal.msgSuccess(tt('取消退款完成'));
  await getList();
};

const goWalletTransaction = (transactionNo: string) => {
  router.push({
    path: '/wallet/transaction',
    query: { transactionNo }
  });
};

onMounted(() => {
  getList();
});
</script>
