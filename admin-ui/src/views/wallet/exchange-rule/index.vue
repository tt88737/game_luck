<template>
  <div class="p-2">
    <el-card v-show="showSearch" class="mb-[10px]" shadow="hover">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item :label="tt('规则名称')" prop="ruleName"><el-input v-model="queryParams.ruleName" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item :label="tt('转出币种')" prop="fromCurrencyCode"><el-input v-model="queryParams.fromCurrencyCode" clearable class="!w-110px" @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item :label="tt('转入币种')" prop="toCurrencyCode"><el-input v-model="queryParams.toCurrencyCode" clearable class="!w-110px" @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item :label="tt('状态')" prop="status">
          <el-select v-model="queryParams.status" clearable class="!w-120px">
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
          <el-col :span="1.5"><el-button v-hasPermi="['wallet:exchangeRule:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ tt('新增') }}</el-button></el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>
      <el-table v-loading="loading" border :data="ruleList" :empty-text="tt('暂无币种兑换规则')">
        <el-table-column :label="tt('规则名称')" align="center" prop="ruleName" min-width="160" show-overflow-tooltip />
        <el-table-column :label="tt('币种兑换方向')" align="center" width="130"><template #default="scope">{{ scope.row.fromCurrencyCode }} -> {{ scope.row.toCurrencyCode }}</template></el-table-column>
        <el-table-column :label="tt('汇率')" align="center" prop="rateValue" width="110" />
        <el-table-column :label="tt('单笔范围')" align="center" min-width="150"><template #default="scope">{{ amountRange(scope.row) }}</template></el-table-column>
        <el-table-column :label="tt('手续费')" align="center" width="120"><template #default="scope">{{ feeText(scope.row) }}</template></el-table-column>
        <el-table-column :label="tt('打码')" align="center" width="120"><template #default="scope">{{ turnoverText(scope.row) }}</template></el-table-column>
        <el-table-column :label="tt('渠道')" align="center" prop="channel" width="90" />
        <el-table-column :label="tt('状态')" align="center" width="90"><template #default="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ statusText(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column :label="tt('操作')" align="center" width="90" fixed="right"><template #default="scope"><el-button v-hasPermi="['wallet:exchangeRule:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button></template></el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="820px" append-to-body>
      <el-alert :title="tt('新增规则默认停用，确认费率、限额、手续费和打码后再启用')" type="warning" :closable="false" class="mb-3" />
      <el-form ref="ruleFormRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item :label="tt('规则名称')" prop="ruleName"><el-input v-model="form.ruleName" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item :label="tt('转出币种')" prop="fromCurrencyCode"><el-input v-model="form.fromCurrencyCode" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item :label="tt('转入币种')" prop="toCurrencyCode"><el-input v-model="form.toCurrencyCode" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('汇率类型')"><el-select v-model="form.rateType" class="w-full"><el-option label="固定汇率" value="FIXED" /><el-option label="活动汇率" value="ACTIVITY" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('汇率')" prop="rateValue"><el-input-number v-model="form.rateValue" :min="0.00000001" :precision="8" class="w-full" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('状态')"><el-switch v-model="form.status" active-value="0" inactive-value="1" :active-text="tt('启用')" :inactive-text="tt('停用')" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('最小转出')"><el-input-number v-model="form.minFromAmount" :min="0" :precision="6" class="w-full" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('最大转出')"><el-input-number v-model="form.maxFromAmount" :min="0" :precision="6" class="w-full" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('每日限额')"><el-input-number v-model="form.dailyFromLimit" :min="0" :precision="6" class="w-full" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('手续费类型')"><el-select v-model="form.feeType" class="w-full"><el-option label="无" value="NONE" /><el-option label="固定" value="FIXED" /><el-option label="百分比" value="PERCENT" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('手续费值')"><el-input-number v-model="form.feeValue" :min="0" :precision="6" class="w-full" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('转入需打码')"><el-switch v-model="form.turnoverRequired" active-value="0" inactive-value="1" :active-text="tt('需要')" :inactive-text="tt('不需要')" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('打码倍数')"><el-input-number v-model="form.turnoverMultiplier" :min="0" :precision="4" class="w-full" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('游戏范围')"><el-select v-model="form.gameScopeType" class="w-full"><el-option label="全部游戏" value="ALL" /><el-option label="指定分类" value="CATEGORY" /><el-option label="指定厂商" value="PROVIDER" /><el-option label="指定游戏" value="GAME" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="tt('渠道')"><el-input v-model="form.channel" /></el-form-item></el-col>
        </el-row>
        <el-form-item :label="tt('范围值')"><el-input v-model="form.gameScopeValue" :placeholder="tt('多个值用英文逗号分隔')" /></el-form-item>
        <el-form-item :label="tt('备注')"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">{{ tt('确定') }}</el-button><el-button @click="cancel">{{ tt('取消') }}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="WalletExchangeRule" lang="ts">
import { tt } from '@/utils/i18nText';
import { addExchangeRule, getExchangeRule, listExchangeRule, updateExchangeRule } from '@/api/wallet/exchangeRule';
import { ExchangeRuleForm, ExchangeRuleQuery, ExchangeRuleVO } from '@/api/wallet/exchangeRule/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const ruleList = ref<ExchangeRuleVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const ruleFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({ visible: false, title: '' });
const queryParams = reactive<ExchangeRuleQuery>({ pageNum: 1, pageSize: 10, ruleName: '', fromCurrencyCode: '', toCurrencyCode: '', status: '' });
const initForm: ExchangeRuleForm = { ruleName: '', fromCurrencyCode: 'GC', toCurrencyCode: 'SC', rateType: 'FIXED', rateValue: 1, minFromAmount: 0, maxFromAmount: 0, dailyFromLimit: 0, feeType: 'NONE', feeValue: 0, turnoverRequired: '1', turnoverMultiplier: 0, gameScopeType: 'ALL', status: '1' };
const form = ref<ExchangeRuleForm>({ ...initForm });
const rules = { ruleName: [{ required: true, message: tt('请输入规则名称'), trigger: 'blur' }], fromCurrencyCode: [{ required: true, message: tt('请输入转出币种'), trigger: 'blur' }], toCurrencyCode: [{ required: true, message: tt('请输入转入币种'), trigger: 'blur' }], rateValue: [{ required: true, message: tt('请输入汇率'), trigger: 'blur' }] };
const statusText = (value?: string) => (value === '0' ? tt('启用') : tt('停用'));
const amountRange = (row: ExchangeRuleVO) => `${row.minFromAmount || 0} - ${row.maxFromAmount && row.maxFromAmount > 0 ? row.maxFromAmount : tt('不限')}`;
const feeText = (row: ExchangeRuleVO) => (row.feeType === 'NONE' ? tt('无') : `${row.feeValue} ${row.feeType === 'PERCENT' ? '%' : row.fromCurrencyCode}`);
const turnoverText = (row: ExchangeRuleVO) => (row.turnoverRequired === '0' ? `${row.turnoverMultiplier || 0}x` : tt('不需要'));
const getList = async () => { loading.value = true; const res = await listExchangeRule(queryParams); ruleList.value = res.rows; total.value = res.total; loading.value = false; };
const reset = () => { form.value = { ...initForm }; ruleFormRef.value?.resetFields(); };
const handleQuery = () => { queryParams.pageNum = 1; getList(); };
const resetQuery = () => { queryFormRef.value?.resetFields(); handleQuery(); };
const handleAdd = () => { reset(); dialog.title = tt('新增币种兑换规则'); dialog.visible = true; };
const handleUpdate = async (row: ExchangeRuleVO) => { reset(); const res = await getExchangeRule(row.id); form.value = { ...res.data }; dialog.title = tt('编辑币种兑换规则'); dialog.visible = true; };
const cancel = () => { dialog.visible = false; reset(); };
const submitForm = () => ruleFormRef.value?.validate(async (valid: boolean) => { if (!valid) return; form.value.id ? await updateExchangeRule(form.value) : await addExchangeRule(form.value); proxy?.$modal.msgSuccess(tt('操作成功')); dialog.visible = false; await getList(); });
onMounted(getList);
</script>
