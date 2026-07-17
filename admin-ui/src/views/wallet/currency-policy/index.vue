<template>
  <div class="p-2">
    <el-card v-show="showSearch" class="mb-[10px]" shadow="hover">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item :label="tt('策略名称')" prop="policyName">
          <el-input v-model="queryParams.policyName" :placeholder="tt('请输入策略名称')" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item :label="tt('币种')" prop="currencyCode">
          <el-input v-model="queryParams.currencyCode" :placeholder="tt('请输入币种')" clearable class="!w-120px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item :label="tt('国家')" prop="countryCode">
          <el-input v-model="queryParams.countryCode" :placeholder="tt('国家')" clearable class="!w-120px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item :label="tt('渠道')" prop="channel">
          <el-input v-model="queryParams.channel" :placeholder="tt('渠道')" clearable class="!w-120px" @keyup.enter="handleQuery" />
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

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['wallet:currencyPolicy:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ tt('新增') }}</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>
      <el-table v-loading="loading" border :data="policyList" :empty-text="tt('暂无币种策略')">
        <el-table-column :label="tt('策略名称')" align="center" prop="policyName" min-width="160" show-overflow-tooltip />
        <el-table-column :label="tt('币种')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="tt('国家')" align="center" prop="countryCode" width="90" />
        <el-table-column :label="tt('州/省')" align="center" prop="stateCode" width="90" />
        <el-table-column :label="tt('渠道')" align="center" prop="channel" width="90" />
        <el-table-column :label="tt('可见')" align="center" width="80"><template #default="scope"><el-tag :type="tagType(scope.row.visibleEnabled)">{{ allowText(scope.row.visibleEnabled) }}</el-tag></template></el-table-column>
        <el-table-column :label="tt('充值')" align="center" width="80"><template #default="scope"><el-tag :type="tagType(scope.row.depositEnabled)">{{ allowText(scope.row.depositEnabled) }}</el-tag></template></el-table-column>
        <el-table-column :label="tt('提现')" align="center" width="80"><template #default="scope"><el-tag :type="tagType(scope.row.withdrawEnabled)">{{ allowText(scope.row.withdrawEnabled) }}</el-tag></template></el-table-column>
        <el-table-column :label="tt('兑付')" align="center" width="80"><template #default="scope"><el-tag :type="tagType(scope.row.exchangeEnabled)">{{ allowText(scope.row.exchangeEnabled) }}</el-tag></template></el-table-column>
        <el-table-column :label="tt('游戏')" align="center" width="80"><template #default="scope"><el-tag :type="tagType(scope.row.playEnabled)">{{ allowText(scope.row.playEnabled) }}</el-tag></template></el-table-column>
        <el-table-column :label="tt('优先级')" align="center" prop="priority" width="90" />
        <el-table-column :label="tt('状态')" align="center" width="90"><template #default="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ statusText(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column :label="tt('操作')" align="center" width="90" fixed="right">
          <template #default="scope">
            <el-button v-hasPermi="['wallet:currencyPolicy:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="760px" append-to-body>
      <el-form ref="policyFormRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item :label="tt('策略名称')" prop="policyName"><el-input v-model="form.policyName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="tt('币种')" prop="currencyCode"><el-input v-model="form.currencyCode" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('国家')"><el-input v-model="form.countryCode" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('州/省')"><el-input v-model="form.stateCode" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('渠道')"><el-input v-model="form.channel" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="tt('优先级')"><el-input-number v-model="form.priority" :min="0" class="w-full" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item :label="tt('状态')"><el-switch v-model="form.status" active-value="0" inactive-value="1" :active-text="tt('启用')" :inactive-text="tt('停用')" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">{{ tt('C端能力') }}</el-divider>
        <el-row :gutter="12">
          <el-col v-for="item in switchFields" :key="item.prop" :span="8">
            <el-form-item :label="item.label"><el-switch v-model="form[item.prop]" active-value="0" inactive-value="1" :active-text="tt('允许')" :inactive-text="tt('禁止')" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="tt('备注')"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">{{ tt('确定') }}</el-button>
        <el-button @click="cancel">{{ tt('取消') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WalletCurrencyPolicy" lang="ts">
import { tt } from '@/utils/i18nText';
import { addCurrencyPolicy, getCurrencyPolicy, listCurrencyPolicy, updateCurrencyPolicy } from '@/api/wallet/currencyPolicy';
import { CurrencyPolicyForm, CurrencyPolicyQuery, CurrencyPolicyVO } from '@/api/wallet/currencyPolicy/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const policyList = ref<CurrencyPolicyVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const policyFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({ visible: false, title: '' });

const initForm: CurrencyPolicyForm = { policyName: '', currencyCode: '', visibleEnabled: '0', depositEnabled: '1', withdrawEnabled: '1', exchangeEnabled: '1', playEnabled: '1', priority: 0, status: '0' };
const queryParams = reactive<CurrencyPolicyQuery>({ pageNum: 1, pageSize: 10, policyName: '', currencyCode: '', countryCode: '', stateCode: '', channel: '', status: '' });
const form = ref<CurrencyPolicyForm>({ ...initForm });
const rules = { policyName: [{ required: true, message: tt('请输入策略名称'), trigger: 'blur' }], currencyCode: [{ required: true, message: tt('请输入币种'), trigger: 'blur' }] };
const switchFields = [
  { prop: 'visibleEnabled', label: tt('可见') },
  { prop: 'depositEnabled', label: tt('充值') },
  { prop: 'withdrawEnabled', label: tt('提现') },
  { prop: 'exchangeEnabled', label: tt('兑付') },
  { prop: 'playEnabled', label: tt('游戏') }
] as const;

const tagType = (value?: string) => (value === '0' ? 'success' : 'info');
const allowText = (value?: string) => (value === '0' ? tt('允许') : tt('禁止'));
const statusText = (value?: string) => (value === '0' ? tt('启用') : tt('停用'));

const getList = async () => { loading.value = true; const res = await listCurrencyPolicy(queryParams); policyList.value = res.rows; total.value = res.total; loading.value = false; };
const reset = () => { form.value = { ...initForm }; policyFormRef.value?.resetFields(); };
const handleQuery = () => { queryParams.pageNum = 1; getList(); };
const resetQuery = () => { queryFormRef.value?.resetFields(); handleQuery(); };
const handleAdd = () => { reset(); dialog.title = tt('新增币种策略'); dialog.visible = true; };
const handleUpdate = async (row: CurrencyPolicyVO) => { reset(); const res = await getCurrencyPolicy(row.id); form.value = { ...res.data }; dialog.title = tt('编辑币种策略'); dialog.visible = true; };
const cancel = () => { dialog.visible = false; reset(); };
const submitForm = () => policyFormRef.value?.validate(async (valid: boolean) => { if (!valid) return; form.value.id ? await updateCurrencyPolicy(form.value) : await addCurrencyPolicy(form.value); proxy?.$modal.msgSuccess(tt('操作成功')); dialog.visible = false; await getList(); });

onMounted(getList);
</script>
