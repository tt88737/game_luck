<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="t('memberProfile.fields.memberNo')" prop="memberNo">
              <el-input v-model="queryParams.memberNo" :placeholder="t('memberProfile.placeholders.memberNo')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('memberProfile.fields.username')" prop="username">
              <el-input v-model="queryParams.username" :placeholder="t('memberProfile.placeholders.username')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('memberProfile.fields.nickname')" prop="nickname">
              <el-input v-model="queryParams.nickname" :placeholder="t('memberProfile.placeholders.nickname')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('common.status')" prop="status">
              <el-select v-model="queryParams.status" :placeholder="t('memberProfile.placeholders.status')" clearable class="!w-130px">
                <el-option :label="t('memberProfile.status.ACTIVE')" value="ACTIVE" />
                <el-option :label="t('memberProfile.status.FROZEN')" value="FROZEN" />
                <el-option :label="t('memberProfile.status.DISABLED')" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('memberProfile.fields.riskLevel')" prop="riskLevel">
              <el-select v-model="queryParams.riskLevel" :placeholder="t('memberProfile.placeholders.riskLevel')" clearable class="!w-130px">
                <el-option :label="t('memberProfile.risk.NORMAL')" value="NORMAL" />
                <el-option :label="t('memberProfile.risk.WATCH')" value="WATCH" />
                <el-option :label="t('memberProfile.risk.HIGH')" value="HIGH" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('memberProfile.fields.countryCode')" prop="countryCode">
              <el-input v-model="queryParams.countryCode" :placeholder="t('memberProfile.placeholders.countryCode')" clearable class="!w-110px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('memberProfile.fields.stateCode')" prop="stateCode">
              <el-input v-model="queryParams.stateCode" :placeholder="t('memberProfile.placeholders.stateCode')" clearable class="!w-110px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">{{ t('common.search') }}</el-button>
              <el-button icon="Refresh" @click="resetQuery">{{ t('common.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['member:profile:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ t('common.add') }}</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['member:profile:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">
              {{ t('common.delete') }}
            </el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="memberList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column :label="t('memberProfile.fields.memberNo')" align="center" prop="memberNo" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('memberProfile.fields.username')" align="center" prop="username" min-width="140" show-overflow-tooltip />
        <el-table-column :label="t('memberProfile.fields.nickname')" align="center" prop="nickname" min-width="140" show-overflow-tooltip />
        <el-table-column :label="t('memberProfile.fields.countryState')" align="center" min-width="110">
          <template #default="scope">
            {{ formatRegion(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('memberProfile.fields.riskLevel')" align="center" prop="riskLevel" width="100">
          <template #default="scope">
            <el-tag :type="riskType(scope.row.riskLevel)">{{ riskLabel(scope.row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('memberProfile.fields.registerChannel')" align="center" prop="registerChannel" width="120" show-overflow-tooltip />
        <el-table-column :label="t('memberProfile.fields.complianceConsent')" align="center" min-width="190">
          <template #default="scope">
            <el-space wrap :size="4">
              <el-tag size="small" :type="consentTagType(scope.row.ageConfirmed)">{{ t('memberProfile.consent.age') }}</el-tag>
              <el-tag size="small" :type="consentTagType(scope.row.termsAccepted)">{{ t('memberProfile.consent.terms') }}</el-tag>
              <el-tag size="small" :type="consentTagType(scope.row.privacyAccepted)">{{ t('memberProfile.consent.privacy') }}</el-tag>
              <el-tag size="small" :type="consentTagType(scope.row.sweepstakesRulesAccepted)">{{ t('memberProfile.consent.rules') }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column :label="t('memberProfile.fields.lastLoginTime')" align="center" prop="lastLoginTime" width="170" />
        <el-table-column :label="t('common.createTime')" align="center" prop="createTime" width="170" />
        <el-table-column :label="t('common.operation')" align="center" width="240" fixed="right">
          <template #default="scope">
            <el-tooltip :content="t('memberProfile.actions.view')" placement="top">
              <el-button v-hasPermi="['member:profile:query']" link type="primary" icon="View" @click="handleDetail(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip :content="t('memberProfile.actions.edit')" placement="top">
              <el-button v-hasPermi="['member:profile:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip :content="t('memberProfile.actions.setActive')" placement="top">
              <el-button
                v-if="scope.row.status !== 'ACTIVE'"
                v-hasPermi="['member:profile:edit']"
                link
                type="success"
                icon="CircleCheck"
                @click="handleStatus(scope.row, 'ACTIVE')"
              ></el-button>
            </el-tooltip>
            <el-tooltip :content="t('memberProfile.actions.freeze')" placement="top">
              <el-button
                v-if="scope.row.status !== 'FROZEN'"
                v-hasPermi="['member:profile:edit']"
                link
                type="warning"
                icon="Lock"
                @click="handleStatus(scope.row, 'FROZEN')"
              ></el-button>
            </el-tooltip>
            <el-tooltip :content="t('memberProfile.actions.disable')" placement="top">
              <el-button
                v-if="scope.row.status !== 'DISABLED'"
                v-hasPermi="['member:profile:edit']"
                link
                type="danger"
                icon="CircleClose"
                @click="handleStatus(scope.row, 'DISABLED')"
              ></el-button>
            </el-tooltip>
            <el-tooltip :content="t('memberProfile.actions.delete')" placement="top">
              <el-button v-hasPermi="['member:profile:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialogTitle" width="620px" append-to-body>
      <el-form ref="memberFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('memberProfile.fields.username')" prop="username">
          <el-input v-model="form.username" :placeholder="t('memberProfile.placeholders.username')" />
        </el-form-item>
        <el-form-item :label="t('memberProfile.fields.nickname')" prop="nickname">
          <el-input v-model="form.nickname" :placeholder="t('memberProfile.placeholders.nickname')" />
        </el-form-item>
        <el-form-item :label="t('common.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button label="ACTIVE">{{ t('memberProfile.status.ACTIVE') }}</el-radio-button>
            <el-radio-button label="FROZEN">{{ t('memberProfile.status.FROZEN') }}</el-radio-button>
            <el-radio-button label="DISABLED">{{ t('memberProfile.status.DISABLED') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('memberProfile.fields.riskLevel')" prop="riskLevel">
          <el-radio-group v-model="form.riskLevel">
            <el-radio-button label="NORMAL">{{ t('memberProfile.risk.NORMAL') }}</el-radio-button>
            <el-radio-button label="WATCH">{{ t('memberProfile.risk.WATCH') }}</el-radio-button>
            <el-radio-button label="HIGH">{{ t('memberProfile.risk.HIGH') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('memberProfile.fields.registerChannel')" prop="registerChannel">
          <el-input v-model="form.registerChannel" placeholder="ADMIN" />
        </el-form-item>
        <el-form-item :label="t('common.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="t('memberProfile.placeholders.remark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" :title="t('memberProfile.dialog.detail')" width="760px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('memberProfile.fields.memberNo')">{{ detail.memberNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.fields.username')">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.fields.nickname')">{{ detail.nickname }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.fields.countryCode')">{{ detail.countryCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.fields.stateCode')">{{ detail.stateCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.fields.riskLevel')">{{ riskLabel(detail.riskLevel) }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.fields.registerChannel')">{{ detail.registerChannel }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.consent.age')">{{ consentLabel(detail.ageConfirmed) }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.consent.terms')">{{ consentLabel(detail.termsAccepted) }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.consent.privacy')">{{ consentLabel(detail.privacyAccepted) }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.consent.rules')">{{ consentLabel(detail.sweepstakesRulesAccepted) }}</el-descriptions-item>
        <el-descriptions-item :label="t('memberProfile.fields.lastLoginTime')">{{ detail.lastLoginTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.createTime')">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.updateTime')">{{ detail.updateTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.remark')" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="MemberProfile" lang="ts">
import { addMemberProfile, delMemberProfile, getMemberProfile, listMemberProfile, updateMemberProfile, updateMemberProfileStatus } from '@/api/member/profile';
import { MemberProfileForm, MemberProfileQuery, MemberProfileVO } from '@/api/member/profile/types';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { t } = useI18n();

const memberList = ref<MemberProfileVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const detailOpen = ref(false);
const ids = ref<Array<string | number>>([]);
const multiple = ref(true);
const queryFormRef = ref<ElFormInstance>();
const memberFormRef = ref<ElFormInstance>();
const detail = ref<Partial<MemberProfileVO>>({});

const dialog = reactive({
  visible: false,
  mode: 'add' as 'add' | 'edit'
});

const initFormData: MemberProfileForm = {
  status: 'ACTIVE',
  riskLevel: 'NORMAL',
  registerChannel: 'ADMIN'
};

const form = ref<MemberProfileForm>({ ...initFormData });
const queryParams = ref<MemberProfileQuery>({
  pageNum: 1,
  pageSize: 10,
  memberNo: '',
  username: '',
  nickname: '',
  status: '',
  riskLevel: '',
  registerChannel: '',
  countryCode: '',
  stateCode: ''
});

const dialogTitle = computed(() => t(dialog.mode === 'add' ? 'memberProfile.dialog.add' : 'memberProfile.dialog.edit'));

const rules = computed(() => ({
  username: [{ required: true, message: t('memberProfile.rules.username'), trigger: 'blur' }],
  status: [{ required: true, message: t('memberProfile.rules.status'), trigger: 'change' }],
  riskLevel: [{ required: true, message: t('memberProfile.rules.riskLevel'), trigger: 'change' }]
}));

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
  return status ? t(`memberProfile.status.${status}`) || status : '';
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
  return riskLevel ? t(`memberProfile.risk.${riskLevel}`) || riskLevel : '';
};

const riskType = (riskLevel?: string) => {
  const map: Record<string, string> = {
    NORMAL: 'success',
    WATCH: 'warning',
    HIGH: 'danger'
  };
  return riskLevel ? map[riskLevel] || '' : '';
};

const formatRegion = (row: MemberProfileVO) => {
  const country = row.countryCode || '-';
  const state = row.stateCode || '-';
  return `${country}/${state}`;
};

const consentTagType = (accepted?: boolean) => {
  return accepted ? 'success' : 'info';
};

const consentLabel = (accepted?: boolean) => {
  return accepted ? t('memberProfile.consent.accepted') : t('memberProfile.consent.notAccepted');
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
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  dialog.mode = 'add';
  dialog.visible = true;
};

const handleUpdate = async (row: MemberProfileVO) => {
  reset();
  const res = await getMemberProfile(row.id);
  form.value = res.data;
  dialog.mode = 'edit';
  dialog.visible = true;
};

const submitForm = () => {
  memberFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      if (form.value.id) {
        await updateMemberProfile(form.value);
        proxy?.$modal.msgSuccess(t('common.success.edit'));
      } else {
        await addMemberProfile(form.value);
        proxy?.$modal.msgSuccess(t('common.success.add'));
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
  await proxy?.$modal.confirm(t('memberProfile.confirm.status', { username: row.username, status: statusLabel(status) }));
  await updateMemberProfileStatus(row.id, status);
  proxy?.$modal.msgSuccess(t('common.success.statusUpdated'));
  await getList();
};

const handleDelete = async (row?: MemberProfileVO) => {
  const deleteIds = row?.id || ids.value;
  await proxy?.$modal.confirm(t('memberProfile.confirm.delete'));
  await delMemberProfile(deleteIds);
  proxy?.$modal.msgSuccess(t('common.success.delete'));
  await getList();
};

onMounted(() => {
  getList();
});
</script>
