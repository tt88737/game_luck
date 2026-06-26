<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="币种编码" prop="currencyCode">
              <el-input v-model="queryParams.currencyCode" placeholder="请输入币种编码" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="币种名称" prop="currencyName">
              <el-input v-model="queryParams.currencyName" placeholder="请输入币种名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="状态" prop="enabled">
              <el-select v-model="queryParams.enabled" placeholder="启用状态" clearable>
                <el-option label="启用" value="0" />
                <el-option label="停用" value="1" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
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

      <el-table v-loading="loading" border :data="currencyList">
        <el-table-column label="币种编码" align="center" prop="currencyCode" width="110" />
        <el-table-column label="币种名称" align="center" prop="currencyName" min-width="130" />
        <el-table-column label="精度" align="center" prop="scaleNum" width="80" />
        <el-table-column label="状态" align="center" prop="enabled" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.enabled === '0' ? 'success' : 'info'">{{ formatEnable(scope.row.enabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入账" align="center" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.creditEnabled === '0' ? 'success' : 'info'">{{ formatAllow(scope.row.creditEnabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="扣账" align="center" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.debitEnabled === '0' ? 'success' : 'info'">{{ formatAllow(scope.row.debitEnabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="冻结" align="center" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.freezeEnabled === '0' ? 'success' : 'info'">{{ formatAllow(scope.row.freezeEnabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提现能力" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.withdrawEnabled === '0' ? 'warning' : 'info'">{{ formatCapability(scope.row.withdrawEnabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="兑换能力" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.exchangeEnabled === '0' ? 'warning' : 'info'">{{ formatCapability(scope.row.exchangeEnabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
        <el-table-column label="备注" align="left" prop="remark" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column label="操作" align="center" width="90" fixed="right">
          <template #default="scope">
            <el-tooltip content="编辑能力" placement="top">
              <el-button v-hasPermi="['wallet:currency:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="560px" append-to-body>
      <el-form ref="currencyFormRef" :model="form" label-width="100px">
        <el-form-item label="币种编码">
          <el-input v-model="form.currencyCode" disabled />
        </el-form-item>
        <el-form-item label="币种名称">
          <el-input v-model="form.currencyName" disabled />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" active-value="0" inactive-value="1" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="允许入账">
          <el-switch v-model="form.creditEnabled" active-value="0" inactive-value="1" active-text="允许" inactive-text="禁止" />
        </el-form-item>
        <el-form-item label="允许扣账">
          <el-switch v-model="form.debitEnabled" active-value="0" inactive-value="1" active-text="允许" inactive-text="禁止" />
        </el-form-item>
        <el-form-item label="允许冻结">
          <el-switch v-model="form.freezeEnabled" active-value="0" inactive-value="1" active-text="允许" inactive-text="禁止" />
        </el-form-item>
        <el-form-item label="提现能力">
          <el-switch v-model="form.withdrawEnabled" active-value="0" inactive-value="1" active-text="具备" inactive-text="不具备" />
        </el-form-item>
        <el-form-item label="兑换能力">
          <el-switch v-model="form.exchangeEnabled" active-value="0" inactive-value="1" active-text="具备" inactive-text="不具备" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WalletCurrency" lang="ts">
import { getCurrency, listCurrency, updateCurrency } from '@/api/wallet/currency';
import { CurrencyForm, CurrencyQuery, CurrencyVO } from '@/api/wallet/currency/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const currencyList = ref<CurrencyVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const currencyFormRef = ref<ElFormInstance>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: CurrencyForm = {
  id: undefined,
  currencyCode: '',
  currencyName: '',
  scaleNum: 6,
  enabled: '0',
  creditEnabled: '0',
  debitEnabled: '0',
  freezeEnabled: '0',
  withdrawEnabled: '1',
  exchangeEnabled: '1',
  negativeAllowed: '1',
  sortOrder: 0,
  remark: ''
};

const data = reactive<PageData<CurrencyForm, CurrencyQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    currencyCode: '',
    currencyName: '',
    enabled: ''
  },
  rules: {}
});

const { queryParams, form } = toRefs(data);

const formatEnable = (value?: string) => (value === '0' ? '启用' : '停用');
const formatAllow = (value?: string) => (value === '0' ? '允许' : '禁止');
const formatCapability = (value?: string) => (value === '0' ? '具备' : '不具备');

const getList = async () => {
  loading.value = true;
  const res = await listCurrency(queryParams.value);
  currencyList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const reset = () => {
  form.value = { ...initFormData };
  currencyFormRef.value?.resetFields();
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

const handleUpdate = async (row: CurrencyVO) => {
  reset();
  const res = await getCurrency(row.id);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '编辑币种能力';
};

const submitForm = async () => {
  await updateCurrency(form.value);
  proxy?.$modal.msgSuccess('操作成功');
  dialog.visible = false;
  await getList();
};

onMounted(() => {
  getList();
});
</script>
