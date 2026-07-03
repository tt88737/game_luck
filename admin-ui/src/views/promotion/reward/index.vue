<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="活动编号" prop="promotionNo">
              <el-input v-model="queryParams.promotionNo" placeholder="请输入活动编号" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="活动名称" prop="promotionName">
              <el-input v-model="queryParams.promotionName" placeholder="请输入活动名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="币种" prop="currencyCode">
              <el-select v-model="queryParams.currencyCode" placeholder="请选择币种" clearable class="!w-120px">
                <el-option label="SC" value="SC" />
                <el-option label="RC" value="RC" />
                <el-option label="GC" value="GC" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-140px">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
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
          <el-col :span="1.5">
            <el-button v-hasPermi="['promotion:reward:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['promotion:reward:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">删除</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="rewardList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column label="活动编号" align="center" prop="promotionNo" min-width="170" show-overflow-tooltip />
        <el-table-column label="活动名称" align="center" prop="promotionName" min-width="160" show-overflow-tooltip />
        <el-table-column label="币种" align="center" prop="currencyCode" width="90" />
        <el-table-column label="奖励金额" align="right" prop="rewardAmount" width="130" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" align="center" prop="startTime" width="170" />
        <el-table-column label="结束时间" align="center" prop="endTime" width="170" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" align="center" width="240" fixed="right">
          <template #default="scope">
            <el-tooltip content="编辑配置" placement="top">
              <el-button v-hasPermi="['promotion:reward:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip :content="scope.row.status === 'ACTIVE' ? '停用活动' : '启用活动'" placement="top">
              <el-button
                v-hasPermi="['promotion:reward:edit']"
                link
                :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'"
                :icon="scope.row.status === 'ACTIVE' ? 'CircleClose' : 'CircleCheck'"
                @click="handleStatus(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="会员领取" placement="top">
              <el-button v-hasPermi="['promotion:reward:claim']" link type="success" icon="Present" @click="openClaim(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="领取记录" placement="top">
              <el-button v-hasPermi="['promotion:reward:query']" link type="primary" icon="Tickets" @click="openClaims(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="删除配置" placement="top">
              <el-button v-hasPermi="['promotion:reward:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="620px" append-to-body>
      <el-form ref="rewardFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动名称" prop="promotionName">
          <el-input v-model="form.promotionName" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="币种" prop="currencyCode">
          <el-select v-model="form.currencyCode" class="w-full">
            <el-option label="SC" value="SC" />
            <el-option label="RC" value="RC" />
            <el-option label="GC" value="GC" />
          </el-select>
        </el-form-item>
        <el-form-item label="奖励金额" prop="rewardAmount">
          <el-input-number v-model="form.rewardAmount" :precision="6" :min="0.000001" class="w-full" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button label="ACTIVE">启用</el-radio-button>
            <el-radio-button label="INACTIVE">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="不填则立即可用" class="w-full" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="不填则长期有效" class="w-full" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="claimDialog.visible" title="会员领取奖励" width="480px" append-to-body>
      <el-form ref="claimFormRef" :model="claimForm" :rules="claimRules" label-width="100px">
        <el-form-item label="活动名称">
          <span>{{ claimDialog.promotionName }}</span>
        </el-form-item>
        <el-form-item label="会员ID" prop="memberId">
          <el-input v-model="claimForm.memberId" placeholder="请输入会员ID" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="claimForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitClaim">确定</el-button>
          <el-button @click="claimDialog.visible = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="claimDrawer.visible" :title="claimDrawer.title" size="70%" append-to-body>
      <el-table v-loading="claimLoading" border :data="claimList">
        <el-table-column label="领取单号" align="center" prop="claimNo" min-width="170" show-overflow-tooltip />
        <el-table-column label="会员ID" align="center" prop="memberId" width="120" />
        <el-table-column label="币种" align="center" prop="currencyCode" width="90" />
        <el-table-column label="奖励金额" align="right" prop="rewardAmount" width="130" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'">{{ claimStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="钱包交易号" align="center" prop="walletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="失败原因" align="center" prop="failReason" min-width="160" show-overflow-tooltip />
        <el-table-column label="领取时间" align="center" prop="createTime" width="170" />
      </el-table>
      <pagination v-show="claimTotal > 0" v-model:page="claimQuery.pageNum" v-model:limit="claimQuery.pageSize" :total="claimTotal" @pagination="getClaimList" />
    </el-drawer>
  </div>
</template>

<script setup name="PromotionReward" lang="ts">
import {
  addPromotionReward,
  claimPromotionReward,
  delPromotionReward,
  getPromotionReward,
  listPromotionClaim,
  listPromotionReward,
  updatePromotionReward,
  updatePromotionRewardStatus
} from '@/api/promotion/reward';
import { PromotionClaimForm, PromotionClaimQuery, PromotionClaimVO, PromotionRewardForm, PromotionRewardQuery, PromotionRewardVO } from '@/api/promotion/reward/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const rewardList = ref<PromotionRewardVO[]>([]);
const claimList = ref<PromotionClaimVO[]>([]);
const loading = ref(true);
const claimLoading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const claimTotal = ref(0);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const queryFormRef = ref<ElFormInstance>();
const rewardFormRef = ref<ElFormInstance>();
const claimFormRef = ref<ElFormInstance>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const claimDialog = reactive({
  visible: false,
  promotionId: '' as string | number,
  promotionName: ''
});

const claimDrawer = reactive({
  visible: false,
  title: ''
});

const initFormData: PromotionRewardForm = {
  currencyCode: 'SC',
  status: 'INACTIVE'
};

const initClaimData: PromotionClaimForm = {};

const data = reactive<PageData<PromotionRewardForm, PromotionRewardQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    promotionNo: '',
    promotionName: '',
    currencyCode: '',
    status: ''
  },
  rules: {
    promotionName: [{ required: true, message: '活动名称不能为空', trigger: 'blur' }],
    currencyCode: [{ required: true, message: '币种不能为空', trigger: 'change' }],
    rewardAmount: [{ required: true, message: '奖励金额不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  }
});

const claimForm = ref<PromotionClaimForm>({ ...initClaimData });
const claimQuery = ref<PromotionClaimQuery>({
  pageNum: 1,
  pageSize: 10,
  promotionId: ''
});

const claimRules = reactive({
  memberId: [{ required: true, message: '会员ID不能为空', trigger: 'blur' }]
});

const { queryParams, form, rules } = toRefs(data);

const getList = async () => {
  loading.value = true;
  try {
    const res = await listPromotionReward(queryParams.value);
    rewardList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const getClaimList = async () => {
  claimLoading.value = true;
  try {
    const res = await listPromotionClaim(claimQuery.value);
    claimList.value = res.rows;
    claimTotal.value = res.total;
  } finally {
    claimLoading.value = false;
  }
};

const statusLabel = (status?: string) => {
  const map: Record<string, string> = {
    ACTIVE: '启用',
    INACTIVE: '停用'
  };
  return status ? map[status] || status : '';
};

const claimStatusLabel = (status?: string) => {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    FAILED: '失败'
  };
  return status ? map[status] || status : '';
};

const reset = () => {
  form.value = { ...initFormData };
  rewardFormRef.value?.resetFields();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleSelectionChange = (selection: PromotionRewardVO[]) => {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '新增促销奖励';
};

const handleUpdate = async (row: PromotionRewardVO) => {
  reset();
  const res = await getPromotionReward(row.id);
  form.value = res.data;
  dialog.visible = true;
  dialog.title = '编辑促销奖励';
};

const submitForm = () => {
  rewardFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      if (form.value.id) {
        await updatePromotionReward(form.value);
        proxy?.$modal.msgSuccess('修改成功');
      } else {
        await addPromotionReward(form.value);
        proxy?.$modal.msgSuccess('新增成功');
      }
      dialog.visible = false;
      await getList();
    }
  });
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const handleStatus = async (row: PromotionRewardVO) => {
  const nextStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  await proxy?.$modal.confirm(`确认${nextStatus === 'ACTIVE' ? '启用' : '停用'}该促销奖励？`);
  await updatePromotionRewardStatus(row.id, nextStatus);
  proxy?.$modal.msgSuccess('状态已更新');
  await getList();
};

const handleDelete = async (row?: PromotionRewardVO) => {
  const deleteIds = row?.id || ids.value;
  await proxy?.$modal.confirm('确认删除选中的促销奖励？');
  await delPromotionReward(deleteIds);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

const openClaim = (row: PromotionRewardVO) => {
  claimForm.value = { ...initClaimData };
  claimDialog.promotionId = row.id;
  claimDialog.promotionName = row.promotionName;
  claimDialog.visible = true;
  claimFormRef.value?.resetFields();
};

const submitClaim = () => {
  claimFormRef.value?.validate(async (valid: boolean) => {
    if (valid && claimDialog.promotionId) {
      await claimPromotionReward(claimDialog.promotionId, claimForm.value);
      proxy?.$modal.msgSuccess('领取成功');
      claimDialog.visible = false;
      await getClaimList();
    }
  });
};

const openClaims = async (row: PromotionRewardVO) => {
  claimQuery.value = {
    pageNum: 1,
    pageSize: 10,
    promotionId: row.id
  };
  claimDrawer.title = `${row.promotionName} - 领取记录`;
  claimDrawer.visible = true;
  await getClaimList();
};

onMounted(() => {
  getList();
});
</script>
