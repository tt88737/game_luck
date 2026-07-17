<template>
  <div class="p-2">
    <el-card shadow="hover">
      <template #header>
        <div class="manual-adjust-header">
          <span>{{ tt('人工调账') }}</span>
        </div>
      </template>

      <el-form ref="manualAdjustFormRef" :model="form" :rules="rules" label-width="120px" class="manual-adjust-form">
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item :label="tt('调账单号')" prop="adjustmentNo">
              <el-input v-model="form.adjustmentNo" :placeholder="tt('请输入调账单号')" readonly>
                <template #append>
                  <el-tooltip :content="tt('复制')" placement="top">
                    <el-button icon="CopyDocument" @click="copyAdjustmentNo" />
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item :label="tt('会员ID')" prop="memberId">
              <el-input v-model="form.memberId" :placeholder="tt('请输入会员ID')" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item :label="tt('币种')" prop="currencyCode">
              <el-select v-model="form.currencyCode" :placeholder="tt('请选择币种')" class="w-full">
                <el-option label="GC" value="GC" />
                <el-option label="SC" value="SC" />
                <el-option label="RC" value="RC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item :label="tt('调账金额')" prop="amount">
              <el-input-number v-model="form.amount" :min="0.000001" :precision="6" :step="1" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="tt('到账方式')" prop="strategy">
              <el-radio-group v-model="form.strategy" class="strategy-radio-group">
                <el-radio-button label="IMMEDIATE">{{ tt('立即到账') }}</el-radio-button>
                <el-radio-button label="MANUAL_REVIEW">{{ tt('人工审核') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="tt('流水要求')" prop="turnoverRequired">
              <el-radio-group v-model="form.turnoverRequired" class="strategy-radio-group">
                <el-radio-button :label="false">{{ tt('不需要流水') }}</el-radio-button>
                <el-radio-button :label="true">{{ tt('需要流水') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="form.turnoverRequired" :xs="24" :md="12">
            <el-form-item :label="tt('流水计算')" prop="turnoverMode">
              <el-radio-group v-model="form.turnoverMode">
                <el-radio-button label="FIXED">{{ tt('固定流水金额') }}</el-radio-button>
                <el-radio-button label="MULTIPLIER">{{ tt('调账金额倍数') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="form.turnoverRequired && form.turnoverMode === 'FIXED'" :xs="24" :md="12">
            <el-form-item :label="tt('固定流水金额')" prop="requiredTurnover">
              <el-input-number v-model="form.requiredTurnover" :min="0" :precision="6" :step="1" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.turnoverRequired && form.turnoverMode === 'MULTIPLIER'" :xs="24" :md="12">
            <el-form-item :label="tt('流水倍数')" prop="turnoverMultiplier">
              <el-input-number v-model="form.turnoverMultiplier" :min="0" :precision="4" :step="1" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="tt('调账原因')" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="4" :placeholder="tt('请输入调账原因')" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="manual-adjust-actions">
        <el-button type="primary" :loading="submitLoading" :disabled="submitLoading" @click="submitForm">{{ tt('提交') }}</el-button>
        <el-button :disabled="submitLoading" @click="resetForm">{{ tt('重置') }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="WalletManualAdjust" lang="ts">
import type { FormRules } from 'element-plus';
import { manualAdjust } from '@/api/wallet/manualAdjust';
import { ManualAdjustForm } from '@/api/wallet/manualAdjust/types';
import { tt } from '@/utils/i18nText';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const manualAdjustFormRef = ref<ElFormInstance>();
const submitLoading = ref(false);

const generateAdjustmentNo = () => {
  const random = Math.floor(Math.random() * 10000)
    .toString()
    .padStart(4, '0');
  return `MA${Date.now()}${random}`;
};

const createForm = (): ManualAdjustForm => ({
  adjustmentNo: generateAdjustmentNo(),
  memberId: '',
  currencyCode: 'GC',
  amount: undefined,
  strategy: 'IMMEDIATE',
  turnoverRequired: false,
  turnoverMode: 'MULTIPLIER',
  requiredTurnover: 0,
  turnoverMultiplier: 1,
  reason: ''
});

const form = ref<ManualAdjustForm>(createForm());
const minimumAmount = 0.000001;

const toFiniteNumber = (value: unknown) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
};

const isValidMemberId = (value: string | number) => {
  if (typeof value === 'number') {
    return Number.isSafeInteger(value) && value > 0;
  }
  return /^[1-9]\d*$/.test(value.trim());
};

const normalizePayload = (): ManualAdjustForm | undefined => {
  const amount = toFiniteNumber(form.value.amount);
  const requiredTurnover = toFiniteNumber(form.value.requiredTurnover);
  const turnoverMultiplier = toFiniteNumber(form.value.turnoverMultiplier);
  if (amount === undefined || amount < minimumAmount) return undefined;
  if (!isValidMemberId(form.value.memberId)) return undefined;
  if (form.value.turnoverRequired && form.value.turnoverMode === 'FIXED' && (requiredTurnover === undefined || requiredTurnover <= 0)) return undefined;
  if (form.value.turnoverRequired && form.value.turnoverMode === 'MULTIPLIER' && (turnoverMultiplier === undefined || turnoverMultiplier <= 0)) return undefined;
  const strategy = form.value.strategy === 'IMMEDIATE' && form.value.turnoverRequired ? 'AFTER_TURNOVER' : form.value.strategy;

  return {
    adjustmentNo: form.value.adjustmentNo.trim(),
    memberId: typeof form.value.memberId === 'string' ? form.value.memberId.trim() : form.value.memberId,
    currencyCode: form.value.currencyCode,
    amount,
    strategy,
    turnoverRequired: form.value.turnoverRequired,
    turnoverMode: form.value.turnoverRequired ? form.value.turnoverMode : undefined,
    requiredTurnover: form.value.turnoverRequired && form.value.turnoverMode === 'FIXED' ? requiredTurnover : 0,
    turnoverMultiplier: form.value.turnoverRequired && form.value.turnoverMode === 'MULTIPLIER' ? turnoverMultiplier : 0,
    reason: form.value.reason.trim()
  };
};

const validatePositiveAmount = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
  const amount = toFiniteNumber(value);
  if (amount === undefined || amount < minimumAmount) {
    callback(new Error(tt('请输入调账金额')));
    return;
  }
  callback();
};

const validateRequiredTurnover = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
  const requiredTurnover = toFiniteNumber(value);
  if (form.value.turnoverRequired && form.value.turnoverMode === 'FIXED' && (requiredTurnover === undefined || requiredTurnover <= 0)) {
    callback(new Error(tt('流水金额必须大于0')));
    return;
  }
  callback();
};

const validateTurnoverMultiplier = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
  const turnoverMultiplier = toFiniteNumber(value);
  if (form.value.turnoverRequired && form.value.turnoverMode === 'MULTIPLIER' && (turnoverMultiplier === undefined || turnoverMultiplier <= 0)) {
    callback(new Error(tt('流水倍数必须大于0')));
    return;
  }
  callback();
};

const validateMemberId = (_rule: unknown, value: string | number, callback: (error?: Error) => void) => {
  if (!isValidMemberId(value)) {
    callback(new Error(tt('会员ID必须为正整数')));
    return;
  }
  callback();
};

const rules: FormRules<ManualAdjustForm> = {
  adjustmentNo: [{ required: true, message: tt('请输入调账单号'), trigger: 'blur' }],
  memberId: [{ required: true, validator: validateMemberId, trigger: 'blur' }],
  currencyCode: [{ required: true, message: tt('请选择币种'), trigger: 'change' }],
  amount: [{ required: true, validator: validatePositiveAmount, trigger: 'blur' }],
  strategy: [{ required: true, message: tt('请选择到账方式'), trigger: 'change' }],
  turnoverRequired: [{ required: true, message: tt('请选择流水要求'), trigger: 'change' }],
  turnoverMode: [{ required: true, message: tt('请选择流水计算方式'), trigger: 'change' }],
  requiredTurnover: [{ validator: validateRequiredTurnover, trigger: 'blur' }],
  turnoverMultiplier: [{ validator: validateTurnoverMultiplier, trigger: 'blur' }],
  reason: [{ required: true, message: tt('请输入调账原因'), trigger: 'blur' }]
};

watch(
  () => [form.value.strategy, form.value.turnoverRequired, form.value.turnoverMode],
  () => {
    nextTick(() => manualAdjustFormRef.value?.clearValidate(['turnoverRequired', 'turnoverMode', 'requiredTurnover', 'turnoverMultiplier']));
  }
);

const copyAdjustmentNo = async () => {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(form.value.adjustmentNo);
  } else {
    const textarea = document.createElement('textarea');
    textarea.value = form.value.adjustmentNo;
    textarea.setAttribute('readonly', 'readonly');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
  }
  proxy?.$modal.msgSuccess(tt('复制成功'));
};

const resetForm = () => {
  form.value = createForm();
  nextTick(() => manualAdjustFormRef.value?.clearValidate());
};

const submitForm = () => {
  if (submitLoading.value) return;
  manualAdjustFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    const payload = normalizePayload();
    if (!payload) return;
    submitLoading.value = true;
    try {
      await manualAdjust(payload);
      proxy?.$modal.msgSuccess(tt('调账成功'));
      resetForm();
    } finally {
      submitLoading.value = false;
    }
  });
};
</script>

<style scoped>
.manual-adjust-header {
  display: flex;
  align-items: center;
  min-height: 24px;
  font-weight: 600;
}

.manual-adjust-form {
  max-width: 920px;
}

.manual-adjust-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  max-width: 920px;
  padding-top: 8px;
}

.strategy-radio-group {
  display: flex;
  flex-wrap: wrap;
  row-gap: 8px;
}

@media (max-width: 768px) {
  .strategy-radio-group :deep(.el-radio-button__inner) {
    min-width: 100%;
  }

  .manual-adjust-actions {
    justify-content: flex-start;
  }
}
</style>
