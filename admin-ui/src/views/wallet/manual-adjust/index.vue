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
              <el-input-number v-model="form.amount" :min="0" :precision="6" :step="1" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="tt('资金策略')" prop="strategy">
              <el-radio-group v-model="form.strategy" class="strategy-radio-group">
                <el-radio-button label="IMMEDIATE">{{ tt('无流水，立即到账') }}</el-radio-button>
                <el-radio-button label="AFTER_TURNOVER">{{ tt('需要流水') }}</el-radio-button>
                <el-radio-button label="MANUAL_REVIEW">{{ tt('人工审核') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="form.strategy === 'AFTER_TURNOVER'" :xs="24" :md="12">
            <el-form-item :label="tt('流水金额')" prop="requiredTurnover">
              <el-input-number v-model="form.requiredTurnover" :min="0" :precision="6" :step="1" controls-position="right" class="w-full" />
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
  requiredTurnover: 0,
  reason: ''
});

const form = ref<ManualAdjustForm>(createForm());

const validatePositiveAmount = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
  if (value === undefined || value === null || Number(value) <= 0) {
    callback(new Error(tt('请输入调账金额')));
    return;
  }
  callback();
};

const validateRequiredTurnover = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
  if (form.value.strategy === 'AFTER_TURNOVER' && (value === undefined || value === null || Number(value) <= 0)) {
    callback(new Error(tt('流水金额必须大于0')));
    return;
  }
  callback();
};

const rules: FormRules<ManualAdjustForm> = {
  adjustmentNo: [{ required: true, message: tt('请输入调账单号'), trigger: 'blur' }],
  memberId: [{ required: true, message: tt('请输入会员ID'), trigger: 'blur' }],
  currencyCode: [{ required: true, message: tt('请选择币种'), trigger: 'change' }],
  amount: [{ required: true, validator: validatePositiveAmount, trigger: 'blur' }],
  strategy: [{ required: true, message: tt('请选择资金策略'), trigger: 'change' }],
  requiredTurnover: [{ validator: validateRequiredTurnover, trigger: 'blur' }],
  reason: [{ required: true, message: tt('请输入调账原因'), trigger: 'blur' }]
};

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
    submitLoading.value = true;
    try {
      await manualAdjust({
        ...form.value,
        requiredTurnover: form.value.strategy === 'AFTER_TURNOVER' ? form.value.requiredTurnover : 0
      });
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
