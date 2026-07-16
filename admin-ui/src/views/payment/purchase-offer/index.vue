<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="tt('产品名称')" prop="offerName">
              <el-input v-model="queryParams.offerName" :placeholder="tt('请输入产品名称')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="tt('类型')" prop="offerType">
              <el-select v-model="queryParams.offerType" :placeholder="tt('请选择类型')" clearable class="!w-150px">
                <el-option v-for="item in gl_purchase_offer_type" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('支付币种')" prop="payCurrencyCode">
              <el-select v-model="queryParams.payCurrencyCode" :placeholder="tt('请选择支付币种')" clearable class="!w-120px">
                <el-option label="USD" value="USD" />
                <el-option label="SC" value="SC" />
                <el-option label="GC" value="GC" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('状态')" prop="status">
              <el-select v-model="queryParams.status" :placeholder="tt('请选择状态')" clearable class="!w-120px">
                <el-option v-for="item in gl_purchase_offer_status" :key="item.value" :label="item.label" :value="item.value" />
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
            <el-button v-hasPermi="['payment:purchaseOffer:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ tt('新增') }}</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="offerList" :empty-text="tt('暂无购买产品')">
        <el-table-column :label="tt('产品编号')" align="center" prop="offerNo" min-width="170" show-overflow-tooltip />
        <el-table-column :label="tt('产品名称')" align="center" prop="offerName" min-width="170" show-overflow-tooltip />
        <el-table-column :label="tt('类型')" align="center" prop="offerType" width="120">
          <template #default="scope">{{ dictLabel(gl_purchase_offer_type, scope.row.offerType) }}</template>
        </el-table-column>
        <el-table-column :label="tt('支付金额')" align="right" min-width="120">
          <template #default="scope">{{ scope.row.payAmount }} {{ scope.row.payCurrencyCode }}</template>
        </el-table-column>
        <el-table-column :label="tt('发放内容')" align="left" min-width="220" show-overflow-tooltip>
          <template #default="scope">{{ grantSummary(scope.row.grantItems) }}</template>
        </el-table-column>
        <el-table-column :label="tt('用户范围')" align="center" width="110">
          <template #default="scope">{{ scope.row.userScopeType || 'ALL' }}</template>
        </el-table-column>
        <el-table-column :label="tt('状态')" align="center" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ dictLabel(gl_purchase_offer_status, scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('时间')" align="center" min-width="190" show-overflow-tooltip>
          <template #default="scope">{{ timeSummary(scope.row) }}</template>
        </el-table-column>
        <el-table-column :label="tt('操作')" align="center" width="110" fixed="right">
          <template #default="scope">
            <el-tooltip :content="tt('编辑')" placement="top">
              <el-button v-hasPermi="['payment:purchaseOffer:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="min(1080px, calc(100vw - 32px))" append-to-body class="purchase-offer-dialog">
      <el-alert
        :title="tt('运营只需要配置购买金额、发放内容和流水要求。GC 当前默认不需要流水；SC 赠送可配置流水倍数或固定流水金额。')"
        type="warning"
        :closable="false"
        class="mb-3"
      />
      <el-form ref="offerFormRef" :model="form" :rules="rules" label-width="110px">
        <div class="form-section">
          <div class="section-title">{{ tt('基础信息') }}</div>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item :label="tt('产品名称')" prop="offerName">
                <el-input v-model="form.offerName" :placeholder="tt('例如 Starter Pack')" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item :label="tt('类型')" prop="offerType">
                <el-select v-model="form.offerType" class="w-full">
                  <el-option v-for="item in gl_purchase_offer_type" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item :label="tt('状态')" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio-button label="0">{{ tt('启用') }}</el-radio-button>
                  <el-radio-button label="1">{{ tt('停用') }}</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">{{ tt('支付设置') }}</div>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item :label="tt('支付币种')" prop="payCurrencyCode">
                <el-select v-model="form.payCurrencyCode" class="w-full">
                  <el-option label="USD" value="USD" />
                  <el-option label="SC" value="SC" />
                  <el-option label="GC" value="GC" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="tt('支付金额')" prop="payAmount">
                <el-input-number v-model="form.payAmount" :min="0.000001" :precision="6" controls-position="right" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="tt('限购方式')" prop="purchaseLimitType">
                <el-select v-model="form.purchaseLimitType" class="w-full">
                  <el-option v-for="item in gl_purchase_limit_type" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">{{ tt('发放项') }}</div>
          <div v-for="(item, index) in form.grantItems" :key="index" class="grant-row">
            <div class="grant-head">
              <span>{{ tt('发放项') }} {{ index + 1 }}</span>
              <el-button v-if="canRemoveGrant" icon="Delete" circle @click="removeGrant(index)" />
            </div>
            <el-row :gutter="12">
              <el-col :span="5">
                <el-form-item :label="tt('发放类型')" :prop="`grantItems.${index}.grantType`" :rules="[{ required: true, message: tt('请选择发放类型'), trigger: 'change' }]">
                  <el-select v-model="item.grantType" class="w-full">
                    <el-option v-for="option in gl_purchase_grant_type" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item :label="tt('币种')" :prop="`grantItems.${index}.currencyCode`" :rules="[{ required: true, message: tt('请选择币种'), trigger: 'change' }]">
                  <el-select v-model="item.currencyCode" class="w-full">
                    <el-option label="GC" value="GC" />
                    <el-option label="SC" value="SC" />
                    <el-option label="RC" value="RC" />
                    <el-option label="USD" value="USD" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="5">
                <el-form-item :label="tt('发放金额')" :prop="`grantItems.${index}.grantAmount`" :rules="amountRule(tt('请输入发放金额'))">
                  <el-input-number v-model="item.grantAmount" :min="0.000001" :precision="6" controls-position="right" class="w-full" />
                </el-form-item>
              </el-col>
              <el-col :span="5">
                <el-form-item :label="tt('流水模式')" :prop="`grantItems.${index}.wageringMode`">
                  <el-select v-model="item.wageringMode" class="w-full" @change="normalizeGrantWagering(item)">
                    <el-option v-for="option in enabledWageringOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="item.wageringMode === 'FIXED'" :span="5">
                <el-form-item :label="tt('固定流水')" :prop="`grantItems.${index}.wageringRequiredAmount`" :rules="amountRule(tt('请输入固定流水金额'))">
                  <el-input-number v-model="item.wageringRequiredAmount" :min="0.000001" :precision="6" controls-position="right" class="w-full" />
                </el-form-item>
              </el-col>
              <el-col v-if="item.wageringMode === 'MULTIPLIER'" :span="5">
                <el-form-item :label="tt('流水倍数')" :prop="`grantItems.${index}.wageringMultiplier`" :rules="amountRule(tt('请输入流水倍数'))">
                  <el-input-number v-model="item.wageringMultiplier" :min="0.0001" :precision="4" :step="1" controls-position="right" class="w-full" />
                </el-form-item>
              </el-col>
              <el-col :span="5">
                <el-form-item :label="tt('游戏范围')">
                  <el-select v-model="item.gameScopeType" class="w-full">
                    <el-option v-for="option in gameScopeOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="item.gameScopeType !== 'ALL'" :span="7">
                <el-form-item :label="tt('范围值')" :prop="`grantItems.${index}.gameScopeValue`" :rules="[{ required: true, message: tt('请输入范围值'), trigger: 'blur' }]">
                  <el-input v-model="item.gameScopeValue" :placeholder="tt('多个值用英文逗号分隔')" />
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item :label="tt('有效天数')">
                  <el-input-number v-model="item.wageringExpireDays" :min="0" :precision="0" controls-position="right" class="w-full" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
          <el-button icon="Plus" @click="addGrant">{{ tt('新增发放项') }}</el-button>
        </div>

        <div class="form-section">
          <div class="section-title">{{ tt('适用范围') }}</div>
          <el-row :gutter="12">
            <el-col :span="6">
              <el-form-item :label="tt('用户范围')">
                <el-select v-model="form.userScopeType" class="w-full">
                  <el-option label="ALL" value="ALL" />
                  <el-option label="NEW_USER" value="NEW_USER" />
                  <el-option label="RECALL" value="RECALL" />
                  <el-option label="TAG" value="TAG" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item :label="tt('地区范围')">
                <el-select v-model="form.regionScopeType" class="w-full">
                  <el-option label="ALL" value="ALL" />
                  <el-option label="COUNTRY" value="COUNTRY" />
                  <el-option label="STATE" value="STATE" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item :label="tt('排序')">
                <el-input-number v-model="form.sortOrder" :min="0" :precision="0" controls-position="right" class="w-full" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">{{ tt('时间和备注') }}</div>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item :label="tt('开始时间')">
                <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="tt('结束时间')">
                <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="tt('备注')">
                <el-input v-model="form.remark" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ tt('确定') }}</el-button>
          <el-button @click="cancel">{{ tt('取消') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PurchaseOffer" lang="ts">
import { addPurchaseOffer, getPurchaseOffer, listPurchaseOffer, updatePurchaseOffer } from '@/api/payment/purchaseOffer';
import { PurchaseOfferForm, PurchaseOfferGrantItem, PurchaseOfferQuery, PurchaseOfferVO } from '@/api/payment/purchaseOffer/types';
import { tt } from '@/utils/i18nText';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { gl_purchase_offer_type, gl_purchase_offer_status, gl_purchase_grant_type, gl_purchase_wagering_mode, gl_purchase_limit_type } = toRefs<any>(
  proxy?.useDict('gl_purchase_offer_type', 'gl_purchase_offer_status', 'gl_purchase_grant_type', 'gl_purchase_wagering_mode', 'gl_purchase_limit_type')
);

const offerList = ref<PurchaseOfferVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const offerFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({ visible: false, title: '' });

const gameScopeOptions = [
  { label: tt('全部游戏'), value: 'ALL' },
  { label: tt('指定分类'), value: 'CATEGORY' },
  { label: tt('指定厂商'), value: 'PROVIDER' },
  { label: tt('指定游戏'), value: 'GAME' }
];
const enabledWageringOptions = computed(() => (gl_purchase_wagering_mode.value || []).filter((item: DictDataOption) => item.value !== 'COMBINED_MULTIPLIER'));

const defaultGrantItems = (): PurchaseOfferGrantItem[] => [
  {
    grantType: 'PURCHASE_GRANT',
    currencyCode: 'GC',
    grantAmount: 10000,
    wageringMode: 'NONE',
    gameScopeType: 'ALL',
    wageringExpireDays: 0
  },
  {
    grantType: 'PURCHASE_BONUS',
    currencyCode: 'SC',
    grantAmount: 1,
    wageringMode: 'MULTIPLIER',
    wageringMultiplier: 10,
    gameScopeType: 'ALL',
    wageringExpireDays: 0
  }
];

const initForm: PurchaseOfferForm = {
  offerType: 'STANDARD',
  payCurrencyCode: 'USD',
  payAmount: 10,
  userScopeType: 'ALL',
  regionScopeType: 'ALL',
  purchaseLimitType: 'NONE',
  stackable: '1',
  status: '1',
  sortOrder: 0,
  grantItems: defaultGrantItems()
};

const form = ref<PurchaseOfferForm>({ ...initForm, grantItems: defaultGrantItems() });
const queryParams = reactive<PurchaseOfferQuery>({
  pageNum: 1,
  pageSize: 10,
  offerNo: '',
  offerName: '',
  offerType: '',
  payCurrencyCode: '',
  status: ''
});

const canRemoveGrant = computed(() => (form.value.grantItems?.length || 0) > 1);
const amountRule = (message: string) => [{ required: true, type: 'number' as const, min: 0.000001, message, trigger: 'blur' }];
const rules = {
  offerName: [{ required: true, message: tt('请输入产品名称'), trigger: 'blur' }],
  offerType: [{ required: true, message: tt('请选择类型'), trigger: 'change' }],
  payCurrencyCode: [{ required: true, message: tt('请选择支付币种'), trigger: 'change' }],
  payAmount: amountRule(tt('请输入支付金额')),
  purchaseLimitType: [{ required: true, message: tt('请选择限购方式'), trigger: 'change' }],
  status: [{ required: true, message: tt('请选择状态'), trigger: 'change' }]
};

const dictLabel = (options: DictDataOption[] = [], value?: string | number) => options.find((item) => item.value === String(value))?.label || value || '';
const grantSummary = (items?: PurchaseOfferGrantItem[]) =>
  (items || [])
    .map((item) => {
      const wagering = item.wageringMode === 'MULTIPLIER' ? `${item.wageringMultiplier || 0}x` : item.wageringMode === 'FIXED' ? `${item.wageringRequiredAmount || 0}` : tt('无流水');
      return `${item.grantAmount} ${item.currencyCode} / ${wagering}`;
    })
    .join(', ');
const timeSummary = (row: PurchaseOfferVO) => {
  if (!row.startTime && !row.endTime) return tt('长期有效');
  return `${row.startTime || '-'} ~ ${row.endTime || '-'}`;
};

const getList = async () => {
  loading.value = true;
  try {
    const res = await listPurchaseOffer(queryParams);
    offerList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const reset = () => {
  form.value = { ...initForm, grantItems: defaultGrantItems() };
  offerFormRef.value?.resetFields();
};

const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleAdd = () => {
  reset();
  dialog.title = tt('新增购买产品');
  dialog.visible = true;
};

const handleUpdate = async (row: PurchaseOfferVO) => {
  reset();
  const res = await getPurchaseOffer(row.id);
  form.value = {
    ...res.data,
    grantItems: res.data.grantItems?.length ? res.data.grantItems : defaultGrantItems()
  };
  dialog.title = tt('编辑购买产品');
  dialog.visible = true;
};

const normalizeGrantWagering = (item: PurchaseOfferGrantItem) => {
  if (item.wageringMode !== 'FIXED') item.wageringRequiredAmount = 0;
  if (item.wageringMode !== 'MULTIPLIER') item.wageringMultiplier = 0;
  if (!item.gameScopeType) item.gameScopeType = 'ALL';
};

const addGrant = () => {
  form.value.grantItems = [
    ...(form.value.grantItems || []),
    { grantType: 'PURCHASE_BONUS', currencyCode: 'SC', grantAmount: 1, wageringMode: 'MULTIPLIER', wageringMultiplier: 10, gameScopeType: 'ALL', wageringExpireDays: 0 }
  ];
};

const removeGrant = (index: number) => {
  const items = [...(form.value.grantItems || [])];
  items.splice(index, 1);
  form.value.grantItems = items.length ? items : defaultGrantItems();
};

const normalizePayload = (): PurchaseOfferForm => ({
  ...form.value,
  grantItems: (form.value.grantItems || []).map((item, index) => ({
    ...item,
    gameScopeType: item.gameScopeType || 'ALL',
    wageringRequiredAmount: item.wageringMode === 'FIXED' ? item.wageringRequiredAmount : 0,
    wageringMultiplier: item.wageringMode === 'MULTIPLIER' ? item.wageringMultiplier : 0,
    wageringExpireDays: item.wageringExpireDays || 0,
    sortOrder: item.sortOrder ?? index * 10
  }))
});

const submitForm = () =>
  offerFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    if (!form.value.grantItems?.length) {
      proxy?.$modal.msgError(tt('至少配置一个发放项'));
      return;
    }
    const payload = normalizePayload();
    payload.id ? await updatePurchaseOffer(payload) : await addPurchaseOffer(payload);
    proxy?.$modal.msgSuccess(tt('操作成功'));
    dialog.visible = false;
    await getList();
  });

const cancel = () => {
  dialog.visible = false;
  reset();
};

onMounted(getList);
</script>

<style scoped lang="scss">
.purchase-offer-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 180px);
  overflow-y: auto;
}

.form-section {
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.form-section:first-of-type {
  padding-top: 0;
  border-top: 0;
}

.section-title {
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.grant-row {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}

.grant-head {
  display: flex;
  min-height: 32px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

@media (max-width: 900px) {
  .purchase-offer-dialog :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
