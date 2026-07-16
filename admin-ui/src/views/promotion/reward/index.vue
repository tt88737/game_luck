<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item :label="t('promotionReward.fields.promotionNo')" prop="promotionNo">
              <el-input v-model="queryParams.promotionNo" :placeholder="t('promotionReward.placeholders.promotionNo')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('promotionReward.fields.promotionName')" prop="promotionName">
              <el-input v-model="queryParams.promotionName" :placeholder="t('promotionReward.placeholders.promotionName')" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item :label="t('promotionReward.fields.promotionType')" prop="promotionType">
              <el-select v-model="queryParams.promotionType" :placeholder="t('promotionReward.placeholders.promotionType')" clearable class="!w-150px">
                <el-option :label="promotionTypeLabel('GENERAL')" value="GENERAL" />
                <el-option :label="promotionTypeLabel('DAILY_LOGIN')" value="DAILY_LOGIN" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('common.currency')" prop="currencyCode">
              <el-select v-model="queryParams.currencyCode" :placeholder="t('promotionReward.placeholders.currency')" clearable class="!w-120px">
                <el-option label="SC" value="SC" />
                <el-option label="RC" value="RC" />
                <el-option label="GC" value="GC" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('common.status')" prop="status">
              <el-select v-model="queryParams.status" :placeholder="t('promotionReward.placeholders.status')" clearable class="!w-140px">
                <el-option :label="t('promotionReward.status.ACTIVE')" value="ACTIVE" />
                <el-option :label="t('promotionReward.status.INACTIVE')" value="INACTIVE" />
              </el-select>
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
            <el-button v-hasPermi="['promotion:reward:add']" type="primary" plain icon="Plus" @click="handleAdd">{{ t('common.add') }}</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['promotion:reward:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">
              {{ t('common.delete') }}
            </el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="rewardList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column :label="t('promotionReward.fields.promotionNo')" align="center" prop="promotionNo" min-width="170" show-overflow-tooltip />
        <el-table-column :label="t('promotionReward.fields.promotionName')" align="center" prop="promotionName" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('promotionReward.fields.promotionType')" align="center" prop="promotionType" width="130">
          <template #default="scope">
            <el-tag :type="scope.row.promotionType === 'DAILY_LOGIN' ? 'warning' : 'info'">{{ promotionTypeLabel(scope.row.promotionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.currency')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="t('promotionReward.fields.rewardAmount')" align="right" prop="rewardAmount" width="130" />
        <el-table-column :label="t('common.status')" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('promotionReward.fields.startTime')" align="center" prop="startTime" width="170" />
        <el-table-column :label="t('promotionReward.fields.endTime')" align="center" prop="endTime" width="170" />
        <el-table-column :label="t('common.createTime')" align="center" prop="createTime" width="170" />
        <el-table-column :label="t('common.operation')" align="center" width="240" fixed="right">
          <template #default="scope">
            <el-tooltip :content="t('promotionReward.actions.edit')" placement="top">
              <el-button v-hasPermi="['promotion:reward:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip :content="scope.row.status === 'ACTIVE' ? t('promotionReward.actions.disable') : t('promotionReward.actions.enable')" placement="top">
              <el-button
                v-hasPermi="['promotion:reward:edit']"
                link
                :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'"
                :icon="scope.row.status === 'ACTIVE' ? 'CircleClose' : 'CircleCheck'"
                @click="handleStatus(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip :content="t('promotionReward.actions.claim')" placement="top">
              <el-button v-hasPermi="['promotion:reward:claim']" link type="success" icon="Present" @click="openClaim(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip :content="t('promotionReward.actions.claims')" placement="top">
              <el-button v-hasPermi="['promotion:reward:query']" link type="primary" icon="Tickets" @click="openClaims(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip :content="t('promotionReward.actions.delete')" placement="top">
              <el-button v-hasPermi="['promotion:reward:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialogTitle" width="min(980px, calc(100vw - 32px))" append-to-body class="reward-dialog">
      <el-form ref="rewardFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('promotionReward.fields.promotionName')" prop="promotionName">
          <el-input v-model="form.promotionName" :placeholder="t('promotionReward.placeholders.promotionName')" />
        </el-form-item>
        <el-form-item :label="t('promotionReward.fields.promotionType')" prop="promotionType">
          <el-select v-model="form.promotionType" :placeholder="t('promotionReward.placeholders.promotionType')" class="w-full">
            <el-option :label="promotionTypeLabel('GENERAL')" value="GENERAL" />
            <el-option :label="promotionTypeLabel('DAILY_LOGIN')" value="DAILY_LOGIN" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('promotionReward.fields.rewardItems')" prop="rewardItems">
          <div class="reward-items-editor">
            <el-alert
              :title="tt('配置奖励金额和流水要求。充值、首充、折扣、召回等购买类活动，请在支付/购买中心配置。')"
              type="warning"
              :closable="false"
              class="mb-2"
            />
            <div v-for="(item, index) in rewardItems" :key="index" class="reward-item-row">
              <div class="reward-item-head">
                <span class="reward-item-title">{{ tt('奖励项') }} {{ index + 1 }}</span>
                <el-tooltip v-if="canRemoveRewardItem" :content="t('promotionReward.actions.removeRewardItem')" placement="top">
                  <el-button icon="Delete" circle @click="removeRewardItem(index)" />
                </el-tooltip>
              </div>
              <div class="reward-item-grid">
                <div class="reward-field reward-field-sm">
                  <span class="reward-field-label">{{ tt('币种') }}</span>
                  <el-select v-model="item.currencyCode" :placeholder="t('promotionReward.placeholders.currency')" class="w-full">
                    <el-option label="GC" value="GC" />
                    <el-option label="SC" value="SC" />
                    <el-option label="RC" value="RC" />
                  </el-select>
                </div>
                <div class="reward-field">
                  <span class="reward-field-label">{{ tt('奖励金额') }}</span>
                  <el-input-number v-model="item.rewardAmount" :precision="6" :min="0.000001" controls-position="right" class="w-full" />
                </div>
                <div class="reward-field reward-field-mode">
                  <span class="reward-field-label">{{ tt('流水要求') }}</span>
                  <el-segmented v-model="item.turnoverMode" :options="turnoverModeOptions" class="w-full" />
                </div>
                <div v-if="item.turnoverMode === 'FIXED'" class="reward-field">
                  <span class="reward-field-label">{{ tt('固定流水金额') }}</span>
                  <el-input-number v-model="item.turnoverRequiredAmount" :min="0" :precision="6" :step="1" controls-position="right" class="w-full" />
                </div>
                <div v-if="item.turnoverMode === 'MULTIPLIER'" class="reward-field">
                  <span class="reward-field-label">{{ tt('流水倍数') }}</span>
                  <el-input-number v-model="item.turnoverMultiplier" :min="0" :precision="4" :step="1" controls-position="right" class="w-full" />
                </div>
                <div class="reward-field">
                  <span class="reward-field-label">{{ tt('有效天数') }}</span>
                  <el-input-number v-model="item.turnoverExpireDays" :min="0" :precision="0" :step="1" controls-position="right" class="w-full" />
                </div>
                <div class="reward-field">
                  <span class="reward-field-label">{{ tt('游戏范围') }}</span>
                  <el-select v-model="item.gameScopeType" class="w-full">
                    <el-option v-for="option in gameScopeOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </div>
                <div v-if="item.gameScopeType !== 'ALL'" class="reward-field reward-field-lg">
                  <span class="reward-field-label">{{ tt('范围值') }}</span>
                  <el-input v-model="item.gameScopeValue" :placeholder="tt('多个值用英文逗号分隔')" />
                </div>
              </div>
            </div>
            <el-button v-if="form.promotionType === 'DAILY_LOGIN'" icon="Plus" @click="addRewardItem">{{ t('promotionReward.actions.addRewardItem') }}</el-button>
          </div>
        </el-form-item>
        <el-form-item :label="t('common.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button label="ACTIVE">{{ t('promotionReward.status.ACTIVE') }}</el-radio-button>
            <el-radio-button label="INACTIVE">{{ t('promotionReward.status.INACTIVE') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('promotionReward.fields.startTime')" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" :placeholder="t('promotionReward.placeholders.startTime')" class="w-full" />
        </el-form-item>
        <el-form-item :label="t('promotionReward.fields.endTime')" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" :placeholder="t('promotionReward.placeholders.endTime')" class="w-full" />
        </el-form-item>
        <el-form-item :label="t('common.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="t('promotionReward.placeholders.remark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="claimDialog.visible" :title="t('promotionReward.dialog.claim')" width="480px" append-to-body>
      <el-form ref="claimFormRef" :model="claimForm" :rules="claimRules" label-width="100px">
        <el-form-item :label="t('promotionReward.fields.promotionName')">
          <span>{{ claimDialog.promotionName }}</span>
        </el-form-item>
        <el-form-item :label="t('promotionReward.fields.memberId')" prop="memberNo">
          <el-input v-model.trim="claimForm.memberNo" :placeholder="t('promotionReward.placeholders.memberId')" />
        </el-form-item>
        <el-form-item :label="t('common.remark')" prop="remark">
          <el-input v-model="claimForm.remark" type="textarea" :rows="3" :placeholder="t('promotionReward.placeholders.remark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitClaim">{{ t('common.confirm') }}</el-button>
          <el-button @click="claimDialog.visible = false">{{ t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="claimDrawer.visible" :title="claimDrawerTitle" size="70%" append-to-body>
      <el-table v-loading="claimLoading" border :data="claimList">
        <el-table-column :label="t('promotionReward.fields.claimNo')" align="center" prop="claimNo" min-width="170" show-overflow-tooltip />
        <el-table-column :label="t('promotionReward.fields.promotionType')" align="center" prop="promotionType" width="130">
          <template #default="scope">
            <el-tag :type="scope.row.promotionType === 'DAILY_LOGIN' ? 'warning' : 'info'">{{ promotionTypeLabel(scope.row.promotionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('promotionReward.fields.memberId')" align="center" prop="memberNo" width="120">
          <template #default="scope">{{ scope.row.memberNo || scope.row.memberId }}</template>
        </el-table-column>
        <el-table-column :label="t('common.currency')" align="center" prop="currencyCode" width="90" />
        <el-table-column :label="t('promotionReward.fields.rewardAmount')" align="right" prop="rewardAmount" width="130" />
        <el-table-column :label="t('promotionReward.fields.claimDate')" align="center" prop="claimDate" width="120" />
        <el-table-column :label="t('promotionReward.fields.rewardSnapshot')" align="center" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            {{ formatRewardSnapshot(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'">{{ claimStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('promotionReward.fields.walletTransactionNo')" align="center" prop="walletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('promotionReward.fields.failReason')" align="center" prop="failReason" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('promotionReward.fields.claimTime')" align="center" prop="createTime" width="170" />
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
import {
  PromotionClaimForm,
  PromotionClaimQuery,
  PromotionClaimVO,
  PromotionRewardForm,
  PromotionRewardItem,
  PromotionRewardQuery,
  PromotionRewardVO
} from '@/api/promotion/reward/types';
import { tt } from '@/utils/i18nText';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { t } = useI18n();

const rewardList = ref<PromotionRewardVO[]>([]);
const claimList = ref<PromotionClaimVO[]>([]);
const loading = ref(true);
const claimLoading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const claimTotal = ref(0);
const ids = ref<Array<string | number>>([]);
const multiple = ref(true);
const queryFormRef = ref<ElFormInstance>();
const rewardFormRef = ref<ElFormInstance>();
const claimFormRef = ref<ElFormInstance>();

const dialog = reactive({
  visible: false,
  mode: 'add' as 'add' | 'edit'
});

const claimDialog = reactive({
  visible: false,
  promotionId: '' as string | number,
  promotionName: ''
});

const claimDrawer = reactive({
  visible: false,
  promotionName: ''
});

const initFormData: PromotionRewardForm = {
  promotionType: 'GENERAL',
  currencyCode: 'SC',
  claimCycle: 'ONCE',
  status: 'INACTIVE'
};

const initClaimData: PromotionClaimForm = {};

const form = ref<PromotionRewardForm>({ ...initFormData });
const queryParams = ref<PromotionRewardQuery>({
  pageNum: 1,
  pageSize: 10,
  promotionNo: '',
  promotionName: '',
  promotionType: '',
  currencyCode: '',
  status: ''
});

const claimForm = ref<PromotionClaimForm>({ ...initClaimData });
const claimQuery = ref<PromotionClaimQuery>({
  pageNum: 1,
  pageSize: 10,
  promotionId: ''
});

const dialogTitle = computed(() => t(dialog.mode === 'add' ? 'promotionReward.dialog.add' : 'promotionReward.dialog.edit'));
const claimDrawerTitle = computed(() => t('promotionReward.dialog.claimRecords', { name: claimDrawer.promotionName }));
const rewardItems = computed<PromotionRewardItem[]>(() => (Array.isArray(form.value.rewardItems) ? form.value.rewardItems : []));
const canRemoveRewardItem = computed(() => form.value.promotionType === 'DAILY_LOGIN' && rewardItems.value.length > 1);
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
const validateRewardItems = (_rule: unknown, value: PromotionRewardItem[] | string | undefined, callback: (error?: Error) => void) => {
  const items = parseRewardItems(value, form.value.promotionType);
  if (!items.length) {
    callback(new Error(t('promotionReward.messages.rewardItemsRequired')));
    return;
  }
  const invalid = items.some((item) => {
    if (!item.currencyCode || Number(item.rewardAmount) <= 0) return true;
    if (item.turnoverMode === 'FIXED' && Number(item.turnoverRequiredAmount || 0) <= 0) return true;
    if (item.turnoverMode === 'MULTIPLIER' && Number(item.turnoverMultiplier || 0) <= 0) return true;
    if (item.gameScopeType && item.gameScopeType !== 'ALL' && !String(item.gameScopeValue || '').trim()) return true;
    return false;
  });
  callback(invalid ? new Error(tt('请检查奖励配置、流水要求和游戏范围')) : undefined);
};

const rules = computed(() => ({
  promotionName: [{ required: true, message: t('promotionReward.rules.promotionName'), trigger: 'blur' }],
  promotionType: [{ required: true, message: t('promotionReward.rules.promotionType'), trigger: 'change' }],
  currencyCode: [{ required: true, message: t('promotionReward.rules.currency'), trigger: 'change' }],
  rewardAmount: [{ required: true, message: t('promotionReward.rules.rewardAmount'), trigger: 'blur' }],
  rewardItems: [{ validator: validateRewardItems, trigger: 'change' }],
  status: [{ required: true, message: t('promotionReward.rules.status'), trigger: 'change' }]
}));

const claimRules = computed(() => ({
  memberNo: [{ required: true, message: t('promotionReward.rules.memberId'), trigger: 'blur' }]
}));

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
  return status ? t(`promotionReward.status.${status}`) || status : '';
};

const claimStatusLabel = (status?: string) => {
  return status ? t(`promotionReward.status.${status}`) || status : '';
};

const promotionTypeLabel = (type?: string) => {
  const promotionType = type || 'GENERAL';
  return t(`promotionReward.types.${promotionType}`) || promotionType;
};

const defaultFundPropertyCode = (promotionType?: string) => (promotionType === 'DAILY_LOGIN' ? 'DAILY_REWARD' : 'ACTIVITY_REWARD');

const deriveTurnoverMode = (item: PromotionRewardItem): PromotionRewardItem['turnoverMode'] => {
  if (Number(item.turnoverRequiredAmount || 0) > 0) return 'FIXED';
  if (Number(item.turnoverMultiplier || 0) > 0) return 'MULTIPLIER';
  return item.turnoverMode || 'NONE';
};

const normalizeRewardItem = (item: PromotionRewardItem, promotionType?: string): PromotionRewardItem => {
  const turnoverMode = deriveTurnoverMode(item);
  const normalized: PromotionRewardItem = {
    currencyCode: item.currencyCode,
    rewardAmount: Number(item.rewardAmount),
    fundPropertyCode: item.fundPropertyCode || defaultFundPropertyCode(promotionType),
    turnoverMode,
    gameScopeType: item.gameScopeType || 'ALL'
  };
  if (turnoverMode === 'FIXED') {
    normalized.turnoverRequiredAmount = Number(item.turnoverRequiredAmount || 0);
  }
  if (turnoverMode === 'MULTIPLIER') {
    normalized.turnoverMultiplier = Number(item.turnoverMultiplier || 0);
  }
  if (normalized.gameScopeType !== 'ALL') {
    normalized.gameScopeValue = String(item.gameScopeValue || '').trim();
  }
  if (Number(item.turnoverExpireDays || 0) > 0) {
    normalized.turnoverExpireDays = Number(item.turnoverExpireDays);
  }
  return normalized;
};

const defaultRewardItems = (promotionType?: string): PromotionRewardItem[] => [
  normalizeRewardItem({ currencyCode: promotionType === 'DAILY_LOGIN' ? 'GC' : 'SC', rewardAmount: promotionType === 'DAILY_LOGIN' ? 100 : 1 }, promotionType),
  ...(promotionType === 'DAILY_LOGIN' ? [normalizeRewardItem({ currencyCode: 'SC', rewardAmount: 1 }, promotionType)] : [])
];

const parseRewardItems = (rewardItems?: PromotionRewardItem[] | string, promotionType = form.value.promotionType || 'GENERAL'): PromotionRewardItem[] => {
  if (Array.isArray(rewardItems)) {
    return rewardItems
      .filter((item) => item.currencyCode && Number(item.rewardAmount) > 0)
      .map((item) => normalizeRewardItem(item, promotionType));
  }
  if (typeof rewardItems === 'string' && rewardItems.trim()) {
    try {
      const parsed = JSON.parse(rewardItems) as PromotionRewardItem[];
      return Array.isArray(parsed) ? parseRewardItems(parsed, promotionType) : [];
    } catch {
      return [];
    }
  }
  return [];
};

const rewardItemsFromReward = (reward: PromotionRewardVO | PromotionRewardForm): PromotionRewardItem[] => {
  const parsed = parseRewardItems(reward.rewardItems, reward.promotionType);
  if (parsed.length) {
    return parsed;
  }
  if (reward.currencyCode && Number(reward.rewardAmount) > 0) {
    return [normalizeRewardItem({ currencyCode: reward.currencyCode, rewardAmount: Number(reward.rewardAmount) }, reward.promotionType)];
  }
  return [];
};

const ensureRewardItems = () => {
  const items = parseRewardItems(form.value.rewardItems);
  form.value.rewardItems = items.length ? items : defaultRewardItems(form.value.promotionType);
};

const addRewardItem = () => {
  const items = parseRewardItems(form.value.rewardItems);
  form.value.rewardItems = [...items, normalizeRewardItem({ currencyCode: 'GC', rewardAmount: 1 }, form.value.promotionType)];
};

const removeRewardItem = (index: number) => {
  const items = parseRewardItems(form.value.rewardItems);
  items.splice(index, 1);
  form.value.rewardItems = items.length ? items : defaultRewardItems(form.value.promotionType);
};

const normalizePayload = (): PromotionRewardForm => {
  const promotionType = form.value.promotionType || 'GENERAL';
  const parsedItems = parseRewardItems(form.value.rewardItems);
  const rewardItems = promotionType === 'DAILY_LOGIN' ? parsedItems : parsedItems.slice(0, 1);
  const firstItem = rewardItems[0];
  if (promotionType === 'DAILY_LOGIN') {
    return {
      ...form.value,
      promotionType,
      currencyCode: firstItem?.currencyCode || 'GC',
      rewardAmount: firstItem?.rewardAmount || 100,
      claimCycle: 'DAILY',
      dailyClaimLimit: 1,
      rewardItems
    };
  }
  return {
    ...form.value,
    promotionType,
    currencyCode: firstItem?.currencyCode || form.value.currencyCode || 'SC',
    rewardAmount: firstItem?.rewardAmount || form.value.rewardAmount || 1,
    claimCycle: 'ONCE',
    dailyClaimLimit: 1,
    rewardItems
  };
};

const formatRewardSnapshot = (claim: PromotionClaimVO) => {
  const items = parseRewardItems(claim.rewardSnapshot);
  if (items.length) {
    return items.map((item) => `${item.rewardAmount} ${item.currencyCode}`).join(', ');
  }
  if (claim.currencyCode && Number(claim.rewardAmount) > 0) {
    return `${claim.rewardAmount} ${claim.currencyCode}`;
  }
  return '-';
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
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  ensureRewardItems();
  dialog.mode = 'add';
  dialog.visible = true;
};

const handleUpdate = async (row: PromotionRewardVO) => {
  reset();
  const res = await getPromotionReward(row.id);
  form.value = {
    ...res.data,
    promotionType: res.data.promotionType || 'GENERAL',
    claimCycle: res.data.claimCycle || (res.data.promotionType === 'DAILY_LOGIN' ? 'DAILY' : 'ONCE'),
    rewardItems: rewardItemsFromReward(res.data)
  };
  dialog.mode = 'edit';
  dialog.visible = true;
};

const submitForm = () => {
  rewardFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      if (!parseRewardItems(form.value.rewardItems).length) {
        proxy?.$modal.msgError(t('promotionReward.messages.rewardItemsRequired'));
        return;
      }
      const payload = normalizePayload();
      if (form.value.id) {
        await updatePromotionReward(payload);
        proxy?.$modal.msgSuccess(t('common.success.edit'));
      } else {
        await addPromotionReward(payload);
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

watch(
  () => form.value.promotionType,
  (promotionType) => {
    if (promotionType === 'DAILY_LOGIN') {
      form.value.claimCycle = 'DAILY';
      form.value.dailyClaimLimit = 1;
      ensureRewardItems();
    } else {
      form.value.claimCycle = 'ONCE';
      form.value.dailyClaimLimit = undefined;
      const items = parseRewardItems(form.value.rewardItems, promotionType);
      form.value.rewardItems = items.length ? items.slice(0, 1) : defaultRewardItems(promotionType);
    }
  }
);

const handleStatus = async (row: PromotionRewardVO) => {
  const nextStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  const action = nextStatus === 'ACTIVE' ? t('promotionReward.actions.enable') : t('promotionReward.actions.disable');
  await proxy?.$modal.confirm(t('promotionReward.confirm.status', { action }));
  await updatePromotionRewardStatus(row.id, nextStatus);
  proxy?.$modal.msgSuccess(t('common.success.statusUpdated'));
  await getList();
};

const handleDelete = async (row?: PromotionRewardVO) => {
  const deleteIds = row?.id || ids.value;
  await proxy?.$modal.confirm(t('promotionReward.confirm.delete'));
  await delPromotionReward(deleteIds);
  proxy?.$modal.msgSuccess(t('common.success.delete'));
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
      proxy?.$modal.msgSuccess(t('promotionReward.messages.claimSuccess'));
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
  claimDrawer.promotionName = row.promotionName;
  claimDrawer.visible = true;
  await getClaimList();
};

onMounted(() => {
  getList();
});
</script>

<style scoped lang="scss">
.reward-items-editor {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 12px;
}

.reward-item-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}

.reward-item-head {
  display: flex;
  min-height: 32px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.reward-item-title {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.reward-item-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 12px;
}

.reward-field {
  display: flex;
  min-width: 0;
  grid-column: span 3;
  flex-direction: column;
  gap: 6px;
}

.reward-field-sm {
  grid-column: span 2;
}

.reward-field-lg {
  grid-column: span 4;
}

.reward-field-mode {
  grid-column: span 5;
}

.reward-field-label {
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 18px;
  white-space: nowrap;
}

.reward-field :deep(.el-segmented) {
  --el-segmented-item-selected-color: var(--el-color-white);
}

.reward-field :deep(.el-segmented__item-label) {
  padding: 0 8px;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .reward-field,
  .reward-field-sm,
  .reward-field-lg,
  .reward-field-mode {
    grid-column: span 6;
  }
}

@media (max-width: 640px) {
  .reward-field,
  .reward-field-sm,
  .reward-field-lg,
  .reward-field-mode {
    grid-column: span 12;
  }
}
</style>
