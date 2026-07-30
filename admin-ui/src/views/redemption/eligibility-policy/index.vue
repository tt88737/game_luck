<template>
  <div class="p-2">
    <el-card v-show="showSearch" class="mb-[10px]" shadow="hover">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item :label="tt('策略名称')" prop="policyName">
          <el-input v-model="queryParams.policyName" :placeholder="tt('请输入策略名称')" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item :label="tt('币种')" prop="currencyCode">
          <el-select v-model="queryParams.currencyCode" :placeholder="tt('请选择币种')" clearable class="!w-110px">
            <el-option label="SC" value="SC" />
            <el-option label="RC" value="RC" />
          </el-select>
        </el-form-item>
        <el-form-item :label="tt('国家')" prop="countryCode">
          <el-input v-model="queryParams.countryCode" :placeholder="tt('国家码')" clearable class="!w-110px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item :label="tt('州/省')" prop="stateCode">
          <el-input v-model="queryParams.stateCode" :placeholder="tt('州/省码')" clearable class="!w-110px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item :label="tt('渠道')" prop="channel">
          <el-input v-model="queryParams.channel" :placeholder="tt('渠道')" clearable class="!w-110px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item :label="tt('动作')" prop="effect">
          <el-select v-model="queryParams.effect" :placeholder="tt('请选择动作')" clearable class="!w-110px">
            <el-option :label="tt('允许')" value="ALLOW" />
            <el-option :label="tt('禁止')" value="DENY" />
          </el-select>
        </el-form-item>
        <el-form-item :label="tt('状态')" prop="status">
          <el-select v-model="queryParams.status" :placeholder="tt('请选择状态')" clearable class="!w-110px">
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

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['redemption:eligibilityPolicy:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ tt('新增') }}</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="policyList" :empty-text="tt('暂无兑换资格策略')">
        <el-table-column :label="tt('策略名称')" align="center" prop="policyName" min-width="180" show-overflow-tooltip />
        <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="tt('国家/州')" align="center" min-width="110">
          <template #default="scope">{{ formatRegion(scope.row) }}</template>
        </el-table-column>
        <el-table-column :label="tt('渠道')" align="center" prop="channel" width="90">
          <template #default="scope">{{ scope.row.channel || tt('全部') }}</template>
        </el-table-column>
        <el-table-column :label="tt('动作')" align="center" prop="effect" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.effect === 'ALLOW' ? 'success' : 'danger'">{{ effectText(scope.row.effect) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('优先级')" align="center" prop="priority" width="90" />
        <el-table-column :label="tt('状态')" align="center" prop="status" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="tt('开始时间')" align="center" prop="startTime" width="170" />
        <el-table-column :label="tt('结束时间')" align="center" prop="endTime" width="170" />
        <el-table-column :label="tt('备注')" align="center" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column :label="tt('操作')" align="center" width="90" fixed="right">
          <template #default="scope">
            <el-button v-hasPermi="['redemption:eligibilityPolicy:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="760px" append-to-body>
      <el-form ref="policyFormRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="tt('策略名称')" prop="policyName">
              <el-input v-model="form.policyName" :placeholder="tt('请输入策略名称')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="tt('币种')" prop="currencyCode">
              <el-select v-model="form.currencyCode" class="w-full">
                <el-option label="SC" value="SC" />
                <el-option label="RC" value="RC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('国家')">
              <el-input v-model="form.countryCode" placeholder="US" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('州/省')">
              <el-input v-model="form.stateCode" placeholder="WA" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('渠道')">
              <el-input v-model="form.channel" placeholder="H5" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('动作')" prop="effect">
              <el-radio-group v-model="form.effect">
                <el-radio-button label="DENY">{{ tt('禁止') }}</el-radio-button>
                <el-radio-button label="ALLOW">{{ tt('允许') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('优先级')">
              <el-input-number v-model="form.priority" :min="0" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="tt('状态')">
              <el-switch v-model="form.status" active-value="0" inactive-value="1" :active-text="tt('启用')" :inactive-text="tt('停用')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="tt('开始时间')">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="tt('结束时间')">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="RedemptionEligibilityPolicy" lang="ts">
import {
  addRedemptionEligibilityPolicy,
  getRedemptionEligibilityPolicy,
  listRedemptionEligibilityPolicy,
  updateRedemptionEligibilityPolicy
} from '@/api/redemption/eligibilityPolicy';
import {
  RedemptionEligibilityPolicyForm,
  RedemptionEligibilityPolicyQuery,
  RedemptionEligibilityPolicyVO
} from '@/api/redemption/eligibilityPolicy/types';
import { tt } from '@/utils/i18nText';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const policyList = ref<RedemptionEligibilityPolicyVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const policyFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({ visible: false, title: '' });

const initForm: RedemptionEligibilityPolicyForm = {
  policyName: '',
  currencyCode: 'SC',
  channel: 'H5',
  effect: 'DENY',
  priority: 100,
  status: '0'
};

const queryParams = reactive<RedemptionEligibilityPolicyQuery>({
  pageNum: 1,
  pageSize: 10,
  policyName: '',
  currencyCode: '',
  countryCode: '',
  stateCode: '',
  channel: '',
  effect: '',
  status: ''
});
const form = ref<RedemptionEligibilityPolicyForm>({ ...initForm });
const rules = {
  policyName: [{ required: true, message: tt('请输入策略名称'), trigger: 'blur' }],
  currencyCode: [{ required: true, message: tt('请选择币种'), trigger: 'change' }],
  effect: [{ required: true, message: tt('请选择动作'), trigger: 'change' }]
};

const getList = async () => {
  loading.value = true;
  try {
    const res = await listRedemptionEligibilityPolicy(queryParams);
    policyList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const formatRegion = (row: RedemptionEligibilityPolicyVO) => {
  const country = row.countryCode || tt('全部');
  const state = row.stateCode || tt('全部');
  return `${country}/${state}`;
};

const effectText = (effect?: string) => (effect === 'ALLOW' ? tt('允许') : tt('禁止'));
const statusText = (status?: string) => (status === '0' ? tt('启用') : tt('停用'));

const reset = () => {
  form.value = { ...initForm };
  policyFormRef.value?.resetFields();
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
  dialog.title = tt('新增兑换资格策略');
  dialog.visible = true;
};

const handleUpdate = async (row: RedemptionEligibilityPolicyVO) => {
  reset();
  const res = await getRedemptionEligibilityPolicy(row.id);
  form.value = { ...res.data };
  dialog.title = tt('编辑兑换资格策略');
  dialog.visible = true;
};

const cancel = () => {
  dialog.visible = false;
  reset();
};

const submitForm = () => {
  policyFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }
    if (form.value.id) {
      await updateRedemptionEligibilityPolicy(form.value);
    } else {
      await addRedemptionEligibilityPolicy(form.value);
    }
    proxy?.$modal.msgSuccess(tt('操作成功'));
    dialog.visible = false;
    await getList();
  });
};

onMounted(getList);
</script>
