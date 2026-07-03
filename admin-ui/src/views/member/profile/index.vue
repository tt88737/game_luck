<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="会员编号" prop="memberNo">
              <el-input v-model="queryParams.memberNo" placeholder="请输入会员编号" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-130px">
                <el-option label="正常" value="ACTIVE" />
                <el-option label="冻结" value="FROZEN" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item label="风险" prop="riskLevel">
              <el-select v-model="queryParams.riskLevel" placeholder="请选择风险" clearable class="!w-130px">
                <el-option label="正常" value="NORMAL" />
                <el-option label="观察" value="WATCH" />
                <el-option label="高风险" value="HIGH" />
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
            <el-button v-hasPermi="['member:profile:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['member:profile:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">删除</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="memberList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column label="会员编号" align="center" prop="memberNo" min-width="160" show-overflow-tooltip />
        <el-table-column label="用户名" align="center" prop="username" min-width="140" show-overflow-tooltip />
        <el-table-column label="昵称" align="center" prop="nickname" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险" align="center" prop="riskLevel" width="100">
          <template #default="scope">
            <el-tag :type="riskType(scope.row.riskLevel)">{{ riskLabel(scope.row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册渠道" align="center" prop="registerChannel" width="120" show-overflow-tooltip />
        <el-table-column label="最后登录" align="center" prop="lastLoginTime" width="170" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" align="center" width="240" fixed="right">
          <template #default="scope">
            <el-tooltip content="查看详情" placement="top">
              <el-button v-hasPermi="['member:profile:query']" link type="primary" icon="View" @click="handleDetail(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="编辑资料" placement="top">
              <el-button v-hasPermi="['member:profile:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="设为正常" placement="top">
              <el-button
                v-if="scope.row.status !== 'ACTIVE'"
                v-hasPermi="['member:profile:edit']"
                link
                type="success"
                icon="CircleCheck"
                @click="handleStatus(scope.row, 'ACTIVE')"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="冻结会员" placement="top">
              <el-button
                v-if="scope.row.status !== 'FROZEN'"
                v-hasPermi="['member:profile:edit']"
                link
                type="warning"
                icon="Lock"
                @click="handleStatus(scope.row, 'FROZEN')"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="禁用会员" placement="top">
              <el-button
                v-if="scope.row.status !== 'DISABLED'"
                v-hasPermi="['member:profile:edit']"
                link
                type="danger"
                icon="CircleClose"
                @click="handleStatus(scope.row, 'DISABLED')"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="删除资料" placement="top">
              <el-button v-hasPermi="['member:profile:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="620px" append-to-body>
      <el-form ref="memberFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button label="ACTIVE">正常</el-radio-button>
            <el-radio-button label="FROZEN">冻结</el-radio-button>
            <el-radio-button label="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="风险等级" prop="riskLevel">
          <el-radio-group v-model="form.riskLevel">
            <el-radio-button label="NORMAL">正常</el-radio-button>
            <el-radio-button label="WATCH">观察</el-radio-button>
            <el-radio-button label="HIGH">高风险</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="注册渠道" prop="registerChannel">
          <el-input v-model="form.registerChannel" placeholder="默认 ADMIN" />
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

    <el-dialog v-model="detailOpen" title="会员详情" width="760px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="会员编号">{{ detail.memberNo }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">{{ riskLabel(detail.riskLevel) }}</el-descriptions-item>
        <el-descriptions-item label="注册渠道">{{ detail.registerChannel }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ detail.lastLoginTime }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="MemberProfile" lang="ts">
import { addMemberProfile, delMemberProfile, getMemberProfile, listMemberProfile, updateMemberProfile, updateMemberProfileStatus } from '@/api/member/profile';
import { MemberProfileForm, MemberProfileQuery, MemberProfileVO } from '@/api/member/profile/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const memberList = ref<MemberProfileVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const detailOpen = ref(false);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const queryFormRef = ref<ElFormInstance>();
const memberFormRef = ref<ElFormInstance>();
const detail = ref<Partial<MemberProfileVO>>({});

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: MemberProfileForm = {
  status: 'ACTIVE',
  riskLevel: 'NORMAL',
  registerChannel: 'ADMIN'
};

const data = reactive<PageData<MemberProfileForm, MemberProfileQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    memberNo: '',
    username: '',
    nickname: '',
    status: '',
    riskLevel: '',
    registerChannel: ''
  },
  rules: {
    username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
    riskLevel: [{ required: true, message: '风险等级不能为空', trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

const getList = async () => {
  loading.value = true;
  try {
    const res = await listMemberProfile(queryParams.value);
    memberList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const statusLabel = (status?: string) => {
  const map: Record<string, string> = {
    ACTIVE: '正常',
    FROZEN: '冻结',
    DISABLED: '禁用'
  };
  return status ? map[status] || status : '';
};

const statusType = (status?: string) => {
  const map: Record<string, string> = {
    ACTIVE: 'success',
    FROZEN: 'warning',
    DISABLED: 'danger'
  };
  return status ? map[status] || '' : '';
};

const riskLabel = (riskLevel?: string) => {
  const map: Record<string, string> = {
    NORMAL: '正常',
    WATCH: '观察',
    HIGH: '高风险'
  };
  return riskLevel ? map[riskLevel] || riskLevel : '';
};

const riskType = (riskLevel?: string) => {
  const map: Record<string, string> = {
    NORMAL: 'success',
    WATCH: 'warning',
    HIGH: 'danger'
  };
  return riskLevel ? map[riskLevel] || '' : '';
};

const reset = () => {
  form.value = { ...initFormData };
  memberFormRef.value?.resetFields();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleSelectionChange = (selection: MemberProfileVO[]) => {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '新增会员';
};

const handleUpdate = async (row: MemberProfileVO) => {
  reset();
  const res = await getMemberProfile(row.id);
  form.value = res.data;
  dialog.visible = true;
  dialog.title = '编辑会员';
};

const submitForm = () => {
  memberFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      if (form.value.id) {
        await updateMemberProfile(form.value);
        proxy?.$modal.msgSuccess('修改成功');
      } else {
        await addMemberProfile(form.value);
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

const handleDetail = async (row: MemberProfileVO) => {
  const res = await getMemberProfile(row.id);
  detail.value = res.data;
  detailOpen.value = true;
};

const handleStatus = async (row: MemberProfileVO, status: 'ACTIVE' | 'FROZEN' | 'DISABLED') => {
  await proxy?.$modal.confirm(`确认将会员 ${row.username} 状态改为${statusLabel(status)}？`);
  await updateMemberProfileStatus(row.id, status);
  proxy?.$modal.msgSuccess('状态已更新');
  await getList();
};

const handleDelete = async (row?: MemberProfileVO) => {
  const deleteIds = row?.id || ids.value;
  await proxy?.$modal.confirm('确认删除选中的会员资料？');
  await delMemberProfile(deleteIds);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

onMounted(() => {
  getList();
});
</script>
