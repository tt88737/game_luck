<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <el-card v-show="showSearch" class="mb-[10px]" shadow="hover">
        <el-form ref="queryFormRef" :model="queryParams" :inline="true">
          <el-form-item :label="tt('资金属性')" prop="propertyCode">
            <el-input v-model="queryParams.propertyCode" :placeholder="tt('请输入资金属性')" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="tt('名称')" prop="propertyName">
            <el-input v-model="queryParams.propertyName" :placeholder="tt('请输入名称')" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="tt('流水模式')" prop="defaultTurnoverMode">
            <el-select v-model="queryParams.defaultTurnoverMode" :placeholder="tt('请选择流水模式')" clearable class="!w-150px">
              <el-option v-for="item in turnoverModeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item :label="tt('状态')" prop="status">
            <el-select v-model="queryParams.status" :placeholder="tt('请选择状态')" clearable class="!w-120px">
              <el-option :label="tt('启用')" value="0" />
              <el-option :label="tt('停用')" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">{{ tt('搜索') }}</el-button>
            <el-button icon="Refresh" @click="resetQuery">{{ tt('重置') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['wallet:fundProperty:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ tt('新增') }}</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>
      <el-table v-loading="loading" border :data="propertyList" :empty-text="tt('暂无资金属性模板')">
        <el-table-column :label="tt('资金属性')" align="center" prop="propertyCode" min-width="160" show-overflow-tooltip />
        <el-table-column :label="tt('名称')" align="center" prop="propertyName" min-width="150" show-overflow-tooltip />
        <el-table-column :label="tt('默认来源')" align="center" prop="defaultSourceType" min-width="130">
          <template #default="scope">{{ businessLabel('sourceType', scope.row.defaultSourceType, tt) }}</template>
        </el-table-column>
        <el-table-column :label="tt('流水要求')" align="center" min-width="160">
          <template #default="scope">{{ turnoverText(scope.row) }}</template>
        </el-table-column>
        <el-table-column :label="tt('游戏范围')" align="center" width="110">
          <template #default="scope">{{ gameScopeText(scope.row.defaultGameScopeType) }}</template>
        </el-table-column>
        <el-table-column :label="tt('状态')" align="center" width="90">
          <template #default="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ statusText(scope.row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="tt('备注')" align="left" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column :label="tt('操作')" align="center" width="90" fixed="right">
          <template #default="scope">
            <el-button v-hasPermi="['wallet:fundProperty:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="860px" append-to-body>
      <el-alert
        :title="tt('资金属性是默认模板，只配置默认来源、流水要求和游戏范围。活动、人工调账、兑换仍可在业务内覆盖流水和游戏范围。新增模板默认停用，确认后再启用。')"
        type="warning"
        :closable="false"
        class="mb-3"
      />
      <el-form ref="propertyFormRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item :label="tt('资金属性')" prop="propertyCode">
              <el-input v-model="form.propertyCode" :disabled="!!form.id" placeholder="ACTIVITY_REWARD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('名称')" prop="propertyName">
              <el-input v-model="form.propertyName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('默认来源')" prop="defaultSourceType">
              <el-select v-model="form.defaultSourceType" class="w-full">
                <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('流水模式')" prop="defaultTurnoverMode">
              <el-segmented v-model="form.defaultTurnoverMode" :options="turnoverModeOptions" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('状态')">
              <el-switch v-model="form.status" active-value="0" inactive-value="1" :active-text="tt('启用')" :inactive-text="tt('停用')" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.defaultTurnoverMode === 'FIXED'" :span="8">
            <el-form-item :label="tt('固定流水金额')" prop="defaultTurnoverRequiredAmount">
              <el-input-number v-model="form.defaultTurnoverRequiredAmount" :min="0" :precision="6" :step="1" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.defaultTurnoverMode === 'MULTIPLIER'" :span="8">
            <el-form-item :label="tt('流水倍数')" prop="defaultTurnoverMultiplier">
              <el-input-number v-model="form.defaultTurnoverMultiplier" :min="0" :precision="4" :step="1" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('游戏范围')" prop="defaultGameScopeType">
              <el-select v-model="form.defaultGameScopeType" class="w-full">
                <el-option v-for="item in gameScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('排序')">
              <el-input-number v-model="form.sortOrder" :min="0" :precision="0" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="form.defaultGameScopeType !== 'ALL'" :label="tt('范围值')">
          <el-input v-model="form.defaultGameScopeValue" :placeholder="tt('多个值用英文逗号分隔')" />
        </el-form-item>
        <el-form-item :label="tt('备注')">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">{{ tt('确定') }}</el-button>
        <el-button @click="cancel">{{ tt('取消') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WalletFundProperty" lang="ts">
import { tt } from '@/utils/i18nText';
import { businessLabel, businessOptions } from '@/utils/businessLabels';
import { addFundProperty, getFundProperty, listFundProperty, updateFundProperty } from '@/api/wallet/fundProperty';
import { FundPropertyForm, FundPropertyQuery, FundPropertyVO } from '@/api/wallet/fundProperty/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const propertyList = ref<FundPropertyVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const propertyFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({ visible: false, title: '' });

const sourceTypeOptions = computed(() => businessOptions('sourceType', tt));
const turnoverModeOptions = [
  { label: tt('不需要流水'), value: 'NONE' },
  { label: tt('固定流水金额'), value: 'FIXED' },
  { label: tt('流水倍数'), value: 'MULTIPLIER' }
];
const gameScopeOptions = [
  { label: tt('全部游戏'), value: 'ALL' },
  { label: tt('指定分类'), value: 'CATEGORY' },
  { label: tt('指定厂商'), value: 'PROVIDER' },
  { label: tt('指定游戏'), value: 'GAME' }
];

const initForm: FundPropertyForm = {
  propertyCode: '',
  propertyName: '',
  defaultSourceType: 'PROMOTION',
  defaultTurnoverMode: 'NONE',
  defaultTurnoverRequiredAmount: 0,
  defaultTurnoverMultiplier: 0,
  defaultGameScopeType: 'ALL',
  status: '1',
  sortOrder: 0,
  remark: ''
};

const form = ref<FundPropertyForm>({ ...initForm });
const queryParams = reactive<FundPropertyQuery>({
  pageNum: 1,
  pageSize: 10,
  propertyCode: '',
  propertyName: '',
  defaultTurnoverMode: '',
  status: ''
});

const toFiniteNumber = (value?: number) => (Number.isFinite(Number(value)) ? Number(value) : undefined);

const validateFixedTurnover = (_rule: unknown, value: number, callback: (error?: Error) => void) => {
  const amount = toFiniteNumber(value);
  if (form.value.defaultTurnoverMode === 'FIXED' && (amount === undefined || amount <= 0)) {
    callback(new Error(tt('固定流水金额必须大于0')));
    return;
  }
  callback();
};

const validateTurnoverMultiplier = (_rule: unknown, value: number, callback: (error?: Error) => void) => {
  const multiplier = toFiniteNumber(value);
  if (form.value.defaultTurnoverMode === 'MULTIPLIER' && (multiplier === undefined || multiplier <= 0)) {
    callback(new Error(tt('流水倍数必须大于0')));
    return;
  }
  callback();
};

const rules = {
  propertyCode: [{ required: true, message: tt('请输入资金属性'), trigger: 'blur' }],
  propertyName: [{ required: true, message: tt('请输入名称'), trigger: 'blur' }],
  defaultSourceType: [{ required: true, message: tt('请选择默认来源'), trigger: 'change' }],
  defaultTurnoverMode: [{ required: true, message: tt('请选择流水模式'), trigger: 'change' }],
  defaultTurnoverRequiredAmount: [{ validator: validateFixedTurnover, trigger: 'blur' }],
  defaultTurnoverMultiplier: [{ validator: validateTurnoverMultiplier, trigger: 'blur' }]
};

watch(
  () => form.value.defaultTurnoverMode,
  (mode) => {
    if (mode !== 'FIXED') form.value.defaultTurnoverRequiredAmount = 0;
    if (mode !== 'MULTIPLIER') form.value.defaultTurnoverMultiplier = 0;
    nextTick(() => propertyFormRef.value?.clearValidate(['defaultTurnoverRequiredAmount', 'defaultTurnoverMultiplier']));
  }
);

const statusText = (value?: string) => (value === '0' ? tt('启用') : tt('停用'));
const gameScopeText = (value?: string) => gameScopeOptions.find((item) => item.value === value)?.label || value || '';
const turnoverText = (row: FundPropertyVO) => {
  if (row.defaultTurnoverMode === 'FIXED') return `${tt('固定')} ${row.defaultTurnoverRequiredAmount || 0}`;
  if (row.defaultTurnoverMode === 'MULTIPLIER') return `${row.defaultTurnoverMultiplier || 0}x`;
  return tt('不需要');
};

const getList = async () => {
  loading.value = true;
  const res = await listFundProperty(queryParams);
  propertyList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const reset = () => {
  form.value = { ...initForm };
  propertyFormRef.value?.resetFields();
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
  dialog.title = tt('新增资金属性');
  dialog.visible = true;
};

const handleUpdate = async (row: FundPropertyVO) => {
  reset();
  const res = await getFundProperty(row.id);
  form.value = { ...res.data };
  dialog.title = tt('编辑资金属性');
  dialog.visible = true;
};

const cancel = () => {
  dialog.visible = false;
  reset();
};

const submitForm = () =>
  propertyFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    form.value.id ? await updateFundProperty(form.value) : await addFundProperty(form.value);
    proxy?.$modal.msgSuccess(tt('操作成功'));
    dialog.visible = false;
    await getList();
  });

onMounted(getList);
</script>
