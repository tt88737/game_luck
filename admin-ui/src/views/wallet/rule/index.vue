<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="tt('币种')" prop="currencyCode">
              <el-select v-model="queryParams.currencyCode" :placeholder="tt('请选择币种')" clearable class="!w-120px">
                <el-option label="GC" value="GC" />
                <el-option label="SC" value="SC" />
                <el-option label="RC" value="RC" />
              </el-select>
            </el-form-item>
            <el-form-item :label="tt('来源类型')" prop="sourceType">
              <el-select v-model="queryParams.sourceType" :placeholder="tt('请选择来源')" clearable class="!w-140px">
                <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      </div>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['wallet:rule:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ tt('新增') }}</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['wallet:rule:seed']" type="success" plain icon="Finished" @click="openDefaultRulePreview">
              {{ tt('补齐默认规则') }}
            </el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="ruleList">
        <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="tt('来源类型')" align="center" prop="sourceType" min-width="130">
          <template #default="scope">{{ businessLabel('sourceType', scope.row.sourceType, tt) }}</template>
        </el-table-column>
        <el-table-column :label="tt('规则名称')" align="center" prop="ruleName" min-width="150">
          <template #default="scope">{{ walletRuleNameLabel(scope.row.ruleName, scope.row.sourceType, scope.row.currencyCode, tt) }}</template>
        </el-table-column>
        <el-table-column :label="tt('释放模式')" align="center" prop="releaseMode" min-width="130">
          <template #default="scope">{{ businessLabel('walletReleaseMode', scope.row.releaseMode, tt) }}</template>
        </el-table-column>
        <el-table-column :label="tt('需要流水')" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.turnoverRequired === '0' ? 'warning' : 'info'">{{ yesNoText(scope.row.turnoverRequired) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('默认流水')" align="right" prop="defaultRequiredTurnover" width="120" />
        <el-table-column :label="tt('提现')" align="center" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.withdrawEnabled === '0' ? 'success' : 'info'">{{ capabilityText(scope.row.withdrawEnabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('兑换')" align="center" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.exchangeEnabled === '0' ? 'success' : 'info'">{{ capabilityText(scope.row.exchangeEnabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('状态')" align="center" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('排序')" align="center" prop="sortOrder" width="80" />
        <el-table-column :label="tt('备注')" align="left" prop="remark" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column :label="tt('操作')" align="center" width="90" fixed="right">
          <template #default="scope">
            <el-tooltip :content="tt('编辑规则')" placement="top">
              <el-button v-hasPermi="['wallet:rule:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-drawer v-model="previewDrawer.visible" :title="tt('默认规则预览')" size="70%" append-to-body>
      <el-alert :title="tt('只会新增缺失规则，不会覆盖已有规则')" type="info" show-icon :closable="false" class="mb-[12px]" />
      <el-table v-loading="previewLoading" border :data="ruleTemplateList">
        <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="tt('来源')" align="center" min-width="140">
          <template #default="scope">{{ previewSourceText(scope.row) }}</template>
        </el-table-column>
        <el-table-column :label="tt('到账策略')" align="center" min-width="150">
          <template #default="scope">{{ businessLabel('walletReleaseMode', scope.row.releaseMode, tt) }}</template>
        </el-table-column>
        <el-table-column :label="tt('默认流水')" align="right" prop="defaultRequiredTurnover" width="120" />
        <el-table-column :label="tt('处理方式')" align="center" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.willCreate ? 'success' : 'info'">{{ scope.row.willCreate ? tt('将创建') : tt('已存在') }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="seedLoading" :disabled="missingRuleCount === 0" @click="confirmSeedDefaultRules">
            {{ tt('确认补齐') }}
          </el-button>
          <el-button @click="previewDrawer.visible = false">{{ tt('取消') }}</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="640px" append-to-body>
      <el-form ref="ruleFormRef" :model="form" :rules="rules" label-width="120px">
        <div class="rule-form-section">{{ tt('基础信息') }}</div>
        <el-form-item :label="tt('币种')" prop="currencyCode">
          <el-select v-model="form.currencyCode" :placeholder="tt('请选择币种')">
            <el-option label="GC" value="GC" />
            <el-option label="SC" value="SC" />
            <el-option label="RC" value="RC" />
          </el-select>
        </el-form-item>
        <el-form-item :label="tt('来源类型')" prop="sourceType">
          <el-select v-model="form.sourceType" :placeholder="tt('请选择来源')" class="w-full">
            <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="tt('规则名称')" prop="ruleName">
          <el-input v-model="form.ruleName" :placeholder="tt('请输入规则名称')" />
        </el-form-item>

        <div class="rule-form-section">{{ tt('资金方向') }}</div>
        <el-form-item :label="tt('增加余额')">
          <el-switch v-model="form.creditEnabled" active-value="0" inactive-value="1" :active-text="tt('允许')" :inactive-text="tt('禁止')" />
        </el-form-item>
        <el-form-item :label="tt('扣减余额')">
          <el-switch v-model="form.debitEnabled" active-value="0" inactive-value="1" :active-text="tt('允许')" :inactive-text="tt('禁止')" />
        </el-form-item>

        <div class="rule-form-section">{{ tt('资金用途') }}</div>
        <el-form-item :label="tt('可提现')">
          <el-switch v-model="form.withdrawEnabled" active-value="0" inactive-value="1" :active-text="tt('具备')" :inactive-text="tt('不具备')" />
        </el-form-item>
        <el-form-item :label="tt('可兑换')">
          <el-switch v-model="form.exchangeEnabled" active-value="0" inactive-value="1" :active-text="tt('具备')" :inactive-text="tt('不具备')" />
        </el-form-item>

        <div class="rule-form-section">{{ tt('到账策略') }}</div>
        <el-form-item :label="tt('释放模式')" prop="releaseMode">
          <el-select v-model="form.releaseMode" :placeholder="tt('请选择释放模式')">
            <el-option v-for="item in releaseModeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <div class="rule-form-section">{{ tt('流水要求') }}</div>
        <el-form-item :label="tt('业务提供流水')">
          <el-switch v-model="form.turnoverRequired" active-value="0" inactive-value="1" :active-text="tt('需要')" :inactive-text="tt('不需要')" />
        </el-form-item>
        <el-form-item :label="tt('默认流水')">
          <el-input-number v-model="form.defaultRequiredTurnover" :min="0" :precision="6" :step="1" controls-position="right" />
        </el-form-item>

        <div class="rule-form-section">{{ tt('其他设置') }}</div>
        <el-form-item :label="tt('状态')">
          <el-switch v-model="form.status" active-value="0" inactive-value="1" :active-text="tt('启用')" :inactive-text="tt('停用')" />
        </el-form-item>
        <el-form-item :label="tt('排序')">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item :label="tt('备注')">
          <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="tt('请输入备注')" />
        </el-form-item>
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

<script setup name="WalletRule" lang="ts">
import { tt } from '@/utils/i18nText';
import { businessLabel, businessOptions, walletRuleNameLabel } from '@/utils/businessLabels';
import { addRule, getRule, listRule, previewDefaultRules, seedDefaultRules, updateRule } from '@/api/wallet/rule';
import { RuleForm, RuleQuery, RuleTemplateVO, RuleVO } from '@/api/wallet/rule/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const ruleList = ref<RuleVO[]>([]);
const ruleTemplateList = ref<RuleTemplateVO[]>([]);
const loading = ref(true);
const previewLoading = ref(false);
const seedLoading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const ruleFormRef = ref<ElFormInstance>();
const sourceOptions = businessOptions('walletRuleSourceType', tt);
const releaseModeOptions = businessOptions('walletReleaseMode', tt);

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const previewDrawer = reactive({
  visible: false
});

const missingRuleCount = computed(() => ruleTemplateList.value.filter((item) => item.willCreate).length);

const initFormData: RuleForm = {
  id: undefined,
  currencyCode: 'GC',
  sourceType: '',
  ruleName: '',
  creditEnabled: '0',
  debitEnabled: '0',
  withdrawEnabled: '1',
  exchangeEnabled: '1',
  releaseMode: 'NEVER',
  turnoverRequired: '1',
  defaultRequiredTurnover: 0,
  status: '0',
  sortOrder: 0,
  remark: ''
};

const data = reactive<PageData<RuleForm, RuleQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    currencyCode: '',
    sourceType: '',
    status: ''
  },
  rules: {
    currencyCode: [{ required: true, message: tt('请选择币种'), trigger: 'change' }],
    sourceType: [{ required: true, message: tt('请选择来源'), trigger: 'change' }],
    ruleName: [{ required: true, message: tt('请输入规则名称'), trigger: 'blur' }],
    releaseMode: [{ required: true, message: tt('请选择释放模式'), trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

const statusText = (value?: string) => (value === '0' ? tt('启用') : tt('停用'));
const yesNoText = (value?: string) => (value === '0' ? tt('需要') : tt('不需要'));
const capabilityText = (value?: string) => (value === '0' ? tt('具备') : tt('不具备'));

const previewSourceText = (row: RuleTemplateVO) => row.sourceLabel || businessLabel('walletRuleSourceType', row.sourceType, tt);

const getList = async () => {
  loading.value = true;
  const res = await listRule(queryParams.value);
  ruleList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const reset = () => {
  form.value = { ...initFormData };
  ruleFormRef.value?.resetFields();
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const openDefaultRulePreview = async () => {
  previewDrawer.visible = true;
  previewLoading.value = true;
  try {
    const res = await previewDefaultRules();
    ruleTemplateList.value = res.data || [];
  } finally {
    previewLoading.value = false;
  }
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = tt('新增钱包规则');
};

const handleUpdate = async (row: RuleVO) => {
  reset();
  const res = await getRule(row.id);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = tt('编辑钱包规则');
};

const submitForm = () => {
  ruleFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    if (form.value.id) {
      await updateRule(form.value);
    } else {
      await addRule(form.value);
    }
    proxy?.$modal.msgSuccess(tt('操作成功'));
    dialog.visible = false;
    await getList();
  });
};

const confirmSeedDefaultRules = async () => {
  if (missingRuleCount.value === 0) return;
  seedLoading.value = true;
  try {
    await seedDefaultRules();
    proxy?.$modal.msgSuccess(tt('默认规则已补齐'));
    previewDrawer.visible = false;
    await getList();
  } finally {
    seedLoading.value = false;
  }
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.rule-form-section {
  margin: 14px 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--el-color-primary);
  color: var(--el-text-color-primary);
  font-weight: 600;
  line-height: 20px;
}

.rule-form-section:first-child {
  margin-top: 0;
}
</style>
