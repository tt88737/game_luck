<template>
  <div class="recon-page p-2">
    <el-alert v-if="!canList" :title="t('paymentReconciliation.permissionDenied')" type="warning" show-icon :closable="false" />
    <template v-else>
      <el-form ref="queryRef" :model="query" inline class="filter-band">
        <el-form-item :label="t('paymentReconciliation.provider')" prop="providerCode"
          ><el-input v-model="query.providerCode" clearable
        /></el-form-item>
        <el-form-item :label="t('paymentReconciliation.date')" prop="statementDate"
          ><el-date-picker v-model="query.statementDate" value-format="YYYY-MM-DD"
        /></el-form-item>
        <el-form-item :label="t('common.status')" prop="status"
          ><el-select v-model="query.status" clearable><el-option v-for="o in batchOptions" :key="o.value" v-bind="o" /></el-select
        ></el-form-item>
        <el-form-item :label="t('paymentReconciliation.file')" prop="originalFileName"
          ><el-input v-model="query.originalFileName" clearable
        /></el-form-item>
        <el-form-item
          ><el-button type="primary" icon="Search" @click="search">{{ t('common.search') }}</el-button
          ><el-button icon="Refresh" @click="reset">{{ t('common.reset') }}</el-button></el-form-item
        >
      </el-form>
      <el-alert v-if="loadError" :title="t('paymentReconciliation.loadFailed')" type="error" show-icon :closable="false"
        ><el-button link type="primary" @click="load">{{ t('paymentReconciliation.retry') }}</el-button></el-alert
      >
      <div class="toolbar">
        <strong>{{ t('paymentReconciliation.title') }}</strong
        ><el-button v-hasPermi="['payment:reconciliation:upload']" type="primary" icon="Upload" @click="uploadOpen = true">{{
          t('paymentReconciliation.upload')
        }}</el-button>
      </div>
      <div class="table-scroll">
        <el-table
          v-loading="loading"
          :data="batches"
          border
          :empty-text="filtered ? t('paymentReconciliation.filteredEmpty') : t('paymentReconciliation.empty')"
        >
          <el-table-column prop="providerCode" :label="t('paymentReconciliation.provider')" width="130" /><el-table-column
            prop="statementDate"
            :label="t('paymentReconciliation.date')"
            width="120"
          /><el-table-column prop="originalFileName" :label="t('paymentReconciliation.file')" min-width="190" show-overflow-tooltip /><el-table-column
            :label="t('common.status')"
            width="130"
            ><template #default="s"
              ><el-tag :type="statusType(s.row.status)">{{ label('reconciliationBatchStatus', s.row.status) }}</el-tag></template
            ></el-table-column
          >
          <el-table-column :label="t('paymentReconciliation.counts')" min-width="220"
            ><template #default="s">{{ s.row.totalCount }} / {{ s.row.invalidCount }} / {{ s.row.discrepancyCount }}</template></el-table-column
          ><el-table-column prop="creatorName" :label="t('paymentReconciliation.creator')" width="130" /><el-table-column
            prop="createTime"
            :label="t('paymentReconciliation.created')"
            width="170"
          />
          <el-table-column :label="t('common.operation')" width="130" fixed="right"
            ><template #default="s"
              ><el-tooltip :content="t('common.detail')"
                ><el-button v-hasPermi="['payment:reconciliation:query']" link icon="View" type="primary" @click="openBatch(s.row)" /></el-tooltip
              ><el-tooltip :content="t('paymentReconciliation.execute')"
                ><el-button
                  v-if="s.row.status === 'VALIDATED'"
                  v-hasPermi="['payment:reconciliation:execute']"
                  link
                  icon="VideoPlay"
                  type="danger"
                  :disabled="s.row.invalidCount > 0 || executing"
                  @click="confirmExecute(s.row)" /></el-tooltip
              ><el-tooltip v-if="s.row.invalidCount > 0" :content="t('paymentReconciliation.invalidExecuteBlocked')"
                ><el-icon color="var(--el-color-warning)"><Warning /></el-icon></el-tooltip></template
          ></el-table-column>
        </el-table>
      </div>
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load" />
    </template>
    <el-dialog v-model="uploadOpen" :title="t('paymentReconciliation.upload')" width="min(520px,94vw)" @close="resetUpload"
      ><el-form ref="uploadRef" :model="upload" :rules="uploadRules" label-width="110px"
        ><el-form-item :label="t('paymentReconciliation.provider')" prop="providerCode"><el-input v-model="upload.providerCode" /></el-form-item
        ><el-form-item :label="t('paymentReconciliation.date')" prop="statementDate"
          ><el-date-picker v-model="upload.statementDate" value-format="YYYY-MM-DD" /></el-form-item
        ><el-form-item :label="t('paymentReconciliation.file')" prop="file"
          ><el-upload
            :key="uploadControlKey"
            ref="uploadControlRef"
            v-model:file-list="uploadFiles"
            :auto-upload="false"
            :limit="1"
            accept=".csv,text/csv"
            :on-change="selectFile"
            :on-remove="() => (upload.file = undefined)"
            ><el-button icon="DocumentAdd">CSV</el-button></el-upload
          ></el-form-item
        ><el-alert v-if="uploadError" :title="uploadError" type="error" show-icon :closable="false" /></el-form
      ><template #footer
        ><el-button @click="uploadOpen = false">{{ t('common.cancel') }}</el-button
        ><el-button type="primary" :loading="uploading" @click="submitUpload">{{ t('common.confirm') }}</el-button></template
      ></el-dialog
    >
    <el-drawer v-model="batchOpen" :title="t('paymentReconciliation.detail')" :size="drawerSize" append-to-body @closed="closeBatch"
      ><el-skeleton v-if="detailLoading" :rows="8" animated /><template v-else
        ><div class="summary">
          <span>{{ detail.providerCode }}</span
          ><strong>{{ label('reconciliationBatchStatus', detail.status) }}</strong
          ><span>{{ detail.originalFileName }}</span
          ><span>{{ detail.matchedCount || 0 }} {{ t('paymentReconciliation.matched') }}</span
          ><span>{{ detail.discrepancyCount || 0 }} {{ t('paymentReconciliation.issues') }}</span>
        </div>
        <el-alert
          v-if="detail.status === 'FAILED'"
          :title="detail.failureReason || t('paymentReconciliation.failed')"
          type="error"
          show-icon /><el-alert
          v-if="detail.status === 'RECONCILING'"
          :title="t('paymentReconciliation.reconciling')"
          type="info"
          show-icon /><el-alert v-if="detailError" :title="t('paymentReconciliation.detailFailed')" type="error" show-icon :closable="false"
          ><el-button link type="primary" @click="reloadDetail">{{ t('paymentReconciliation.retry') }}</el-button></el-alert
        ><el-tabs v-else v-model="tab" @tab-change="loadTab"
          ><el-tab-pane :label="t('paymentReconciliation.invalid')" name="INVALID" /><el-tab-pane
            :label="t('paymentReconciliation.matched')"
            name="MATCHED" /><el-tab-pane :label="t('paymentReconciliation.issues')" name="ISSUES"
        /></el-tabs>
        <template v-if="!detailError">
          <el-form v-if="tab === 'ISSUES'" :model="issueQuery" inline class="issue-filter">
            <el-form-item :label="t('paymentReconciliation.issueType')"
              ><el-select v-model="issueQuery.issueType" clearable><el-option v-for="o in issueTypeOptions" :key="o.value" v-bind="o" /></el-select
            ></el-form-item>
            <el-form-item :label="t('common.status')"
              ><el-select v-model="issueQuery.status" clearable><el-option v-for="o in issueStatusOptions" :key="o.value" v-bind="o" /></el-select
            ></el-form-item>
            <el-form-item :label="t('paymentReconciliation.orderNo')"><el-input v-model="issueQuery.purchaseOrderNo" clearable /></el-form-item>
            <el-form-item :label="t('paymentReconciliation.sessionNo')"><el-input v-model="issueQuery.sessionNo" clearable /></el-form-item>
            <el-form-item :label="t('paymentReconciliation.providerRecord')"
              ><el-input v-model="issueQuery.providerRecordId" clearable
            /></el-form-item>
            <el-form-item
              ><el-button type="primary" icon="Search" @click="searchIssues">{{ t('common.search') }}</el-button
              ><el-button icon="Refresh" @click="resetIssues">{{ t('common.reset') }}</el-button></el-form-item
            >
          </el-form>
          <el-alert v-if="activeTabState.error" :title="t('paymentReconciliation.tabLoadFailed')" type="error" show-icon :closable="false"
            ><el-button link type="primary" @click="loadTab">{{ t('paymentReconciliation.retry') }}</el-button></el-alert
          >
          <div class="table-scroll">
            <el-table
              v-if="tab !== 'ISSUES'"
              v-loading="activeTabState.loading"
              :data="activeLines"
              border
              :empty-text="lineFilteredEmpty ? t('paymentReconciliation.filteredEmpty') : t('paymentReconciliation.emptyLines')"
            >
              <el-table-column prop="sourceRowNumber" :label="t('paymentReconciliation.rowNumber')" width="70" />
              <el-table-column prop="providerRecordId" :label="t('paymentReconciliation.providerRecord')" min-width="190" />
              <el-table-column prop="purchaseOrderNo" :label="t('paymentReconciliation.orderNo')" min-width="170" />
              <el-table-column prop="amount" :label="t('paymentReconciliation.amount')" width="120" />
              <el-table-column :label="t('paymentReconciliation.validation')" min-width="240"
                ><template #default="s">{{ validationLabel(s.row.parseError) }}</template></el-table-column
              >
            </el-table>
            <el-table
              v-else
              v-loading="issueState.loading"
              :data="issues"
              border
              :empty-text="issueFiltered ? t('paymentReconciliation.filteredEmpty') : t('paymentReconciliation.emptyIssues')"
            >
              <el-table-column :label="t('paymentReconciliation.issueType')" min-width="210"
                ><template #default="s">{{ label('reconciliationIssueType', s.row.issueType, tt) }}</template></el-table-column
              >
              <el-table-column prop="purchaseOrderNo" :label="t('paymentReconciliation.orderNo')" min-width="170" />
              <el-table-column prop="sessionNo" :label="t('paymentReconciliation.sessionNo')" min-width="170" />
              <el-table-column :label="t('common.status')" width="120"
                ><template #default="s">{{ label('reconciliationIssueStatus', s.row.status, tt) }}</template></el-table-column
              >
              <el-table-column :label="t('common.operation')" width="80" fixed="right"
                ><template #default="s"><el-button link icon="View" @click="openIssue(s.row)" /></template
              ></el-table-column>
            </el-table>
          </div>
          <pagination
            v-if="activeTabState.total > 0"
            v-model:page="activeTabState.pageNum"
            v-model:limit="activeTabState.pageSize"
            :total="activeTabState.total"
            @pagination="loadTab"
          /> </template></template
    ></el-drawer>
    <el-drawer v-model="issueOpen" :title="t('paymentReconciliation.issueDetail')" :size="drawerSize" append-to-body @closed="closeIssue"
      ><el-skeleton v-if="issueLoading" :rows="9" animated /><el-alert
        v-else-if="issueError"
        :title="t('paymentReconciliation.issueLoadFailed')"
        type="error"
        show-icon
        :closable="false"
        ><el-button link type="primary" @click="reloadIssue">{{ t('paymentReconciliation.retry') }}</el-button></el-alert
      ><template v-else
        ><el-descriptions border :column="columns"
          ><el-descriptions-item :label="t('paymentReconciliation.issueType')"
            ><el-tag :type="statusType(issue.status)">{{ label('reconciliationIssueStatus', issue.status) }}</el-tag>
            {{ label('reconciliationIssueType', issue.issueType) }}</el-descriptions-item
          ><el-descriptions-item :label="t('paymentReconciliation.providerPlatformAmount')"
            >{{ issue.providerAmount ?? '-' }} / {{ issue.platformAmount ?? '-' }}</el-descriptions-item
          ><el-descriptions-item :label="t('paymentReconciliation.providerPlatformStatus')"
            >{{ issue.providerStatus ?? '-' }} / {{ issue.platformStatus ?? '-' }}</el-descriptions-item
          ><el-descriptions-item :label="t('paymentReconciliation.related')"
            ><el-button v-if="issue.purchaseOrderNo" link @click="go('/payment/purchase-order', { purchaseOrderNo: issue.purchaseOrderNo })">{{
              t('paymentReconciliation.orderNo')
            }}</el-button
            ><el-button v-if="issue.sessionNo" link @click="go('/payment/payment-session', { sessionNo: issue.sessionNo })">{{
              t('paymentReconciliation.sessionNo')
            }}</el-button
            ><el-button v-if="issue.reversalId" link @click="go('/payment/purchase-reversal-review', { reversalId: issue.reversalId })">{{
              t('paymentReconciliation.reversal')
            }}</el-button></el-descriptions-item
          ></el-descriptions
        >
        <h4>{{ t('paymentReconciliation.diagnostics') }}</h4>
        <pre class="readonly">{{ pretty(issue.diagnosticSnapshotJson) }}</pre>
        <h4>{{ t('paymentReconciliation.canonicalFields') }}</h4>
        <el-alert v-if="issue.platformOnly" :title="t('paymentReconciliation.platformOnly')" type="info" show-icon :closable="false" />
        <template v-else>
          <div class="source-row">{{ t('paymentReconciliation.rowNumber') }}: {{ issue.sourceRowNumber ?? '-' }}</div>
          <pre class="readonly" contenteditable="false">{{ pretty(issue.canonicalOriginalFields ?? undefined) }}</pre>
        </template>
        <el-timeline
          ><el-timeline-item v-for="log in issue.actionLogs" :key="log.id" :timestamp="log.createTime"
            >{{ log.operatorName }} | {{ actionLabel(log.actionType) }} | {{ log.remark }}</el-timeline-item
          ></el-timeline
        >
        <div v-if="issue.status === 'OPEN'" class="drawer-actions">
          <el-button v-hasPermi="['payment:reconciliation:resolve']" type="primary" @click="openResolution('resolve')">{{
            t('paymentReconciliation.resolve')
          }}</el-button
          ><el-button v-hasPermi="['payment:reconciliation:resolve']" type="warning" @click="openResolution('ignore')">{{
            t('paymentReconciliation.ignore')
          }}</el-button>
        </div></template
      ></el-drawer
    >
    <el-dialog
      v-model="resolutionOpen"
      :title="resolutionMode === 'resolve' ? t('paymentReconciliation.resolve') : t('paymentReconciliation.ignore')"
      width="min(520px,94vw)"
      ><el-alert :title="t('paymentReconciliation.noMutation')" type="warning" show-icon :closable="false" /><el-form
        ref="resolutionRef"
        :model="resolution"
        :rules="resolutionRules"
        label-width="110px"
        ><el-form-item :label="t('paymentReconciliation.classification')" prop="resolutionType"
          ><el-select v-model="resolution.resolutionType"
            ><el-option v-for="o in resolutionOptions" :key="o.value" v-bind="o" /></el-select></el-form-item
        ><el-form-item :label="t('paymentReconciliation.remark')" prop="remark"
          ><el-input v-model="resolution.remark" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item></el-form
      ><template #footer
        ><el-button @click="resolutionOpen = false">{{ t('common.cancel') }}</el-button
        ><el-button type="danger" :loading="resolving" :disabled="!selectedIssueId || issue.id !== selectedIssueId" @click="submitResolution">{{
          t('common.confirm')
        }}</el-button></template
      ></el-dialog
    >
  </div>
</template>
<script setup name="PaymentReconciliation" lang="ts">
import auth from '@/plugins/auth';
import { useI18n } from 'vue-i18n';
import type { UploadFile, UploadInstance, UploadUserFile } from 'element-plus';
import { businessLabel, businessOptions } from '@/utils/businessLabels';
import { tt } from '@/utils/i18nText';
import {
  executeReconciliationBatch,
  getReconciliationBatch,
  getReconciliationIssue,
  ignoreReconciliationIssue,
  listReconciliationBatches,
  listReconciliationIssues,
  listReconciliationLines,
  resolveReconciliationIssue,
  uploadReconciliationBatch
} from '@/api/payment/paymentReconciliation';
import type {
  ReconciliationBatchQuery,
  ReconciliationBatchVO,
  ReconciliationIssueDetailVO,
  ReconciliationIssueQuery,
  ReconciliationIssueVO,
  ReconciliationLineVO,
  ReconciliationResolutionType
} from '@/api/payment/paymentReconciliation/types';
const { t } = useI18n(),
  router = useRouter(),
  { proxy } = getCurrentInstance() as ComponentInternalInstance,
  { width } = useWindowSize();
const drawerSize = computed(() => (width.value < 768 ? '100%' : '880px')),
  columns = computed(() => (width.value < 768 ? 1 : 2));
const canList = auth.hasPermi('payment:reconciliation:list');
const queryRef = ref<ElFormInstance>(),
  query = ref<ReconciliationBatchQuery>({ pageNum: 1, pageSize: 10, status: '', providerCode: '', statementDate: '', originalFileName: '' }),
  batches = ref<ReconciliationBatchVO[]>([]),
  total = ref(0),
  loading = ref(false),
  loadError = ref(false),
  filtered = computed(() => !!(query.value.providerCode || query.value.statementDate || query.value.status || query.value.originalFileName));
const batchOptions = businessOptions('reconciliationBatchStatus', tt),
  resolutionOptions = businessOptions('reconciliationResolutionType', tt);
const label = businessLabel;
let listGeneration = 0;
const load = async () => {
  const generation = ++listGeneration;
  const snapshot = structuredClone(toRaw(query.value));
  loading.value = true;
  loadError.value = false;
  try {
    const r = await listReconciliationBatches(snapshot);
    if (generation !== listGeneration || JSON.stringify(snapshot) !== JSON.stringify(toRaw(query.value))) return;
    batches.value = r.rows;
    total.value = r.total;
  } catch {
    if (generation !== listGeneration) return;
    loadError.value = true;
    batches.value = [];
  } finally {
    if (generation === listGeneration) loading.value = false;
  }
};
const search = () => {
  query.value.pageNum = 1;
  load();
};
const reset = () => {
  queryRef.value?.resetFields();
  search();
};
const uploadOpen = ref(false),
  uploading = ref(false),
  uploadError = ref(''),
  uploadRef = ref<ElFormInstance>(),
  uploadControlRef = ref<UploadInstance>(),
  uploadControlKey = ref(0),
  uploadFiles = ref<UploadUserFile[]>([]),
  upload = reactive<{ providerCode: string; statementDate: string; file?: File }>({ providerCode: 'SIMULATED', statementDate: '' });
const uploadRules = {
  providerCode: [{ required: true, message: t('paymentReconciliation.providerRequired') }],
  statementDate: [{ required: true, message: t('paymentReconciliation.dateRequired') }],
  file: [{ required: true, message: t('paymentReconciliation.fileRequired') }]
};
const selectFile = (f: UploadFile) => {
  uploadError.value = '';
  const size = f.raw?.size ?? f.size ?? 0;
  if (!f.name.toLowerCase().endsWith('.csv')) uploadError.value = t('paymentReconciliation.csvOnly');
  else if (size > 10 * 1024 * 1024) uploadError.value = t('paymentReconciliation.tooLarge');
  else upload.file = f.raw;
};
const resetUpload = () => {
  upload.providerCode = 'SIMULATED';
  upload.statementDate = '';
  upload.file = undefined;
  uploadFiles.value = [];
  uploadControlKey.value++;
  uploadError.value = '';
  uploadControlRef.value?.clearFiles();
  uploadRef.value?.clearValidate();
};
watch(uploadOpen, (open) => {
  if (!open) resetUpload();
});
const submitUpload = async () => {
  await uploadRef.value?.validate();
  if (uploadError.value || !upload.file) return;
  uploading.value = true;
  try {
    await uploadReconciliationBatch(upload.providerCode, upload.statementDate, upload.file);
    proxy?.$modal.msgSuccess(t('paymentReconciliation.uploaded'));
    uploadOpen.value = false;
    load();
  } catch (error: unknown) {
    uploadError.value = errorMessage(error, t('paymentReconciliation.uploadFailed'));
  } finally {
    uploading.value = false;
  }
};
const batchOpen = ref(false),
  detailLoading = ref(false),
  detailError = ref(false),
  detail = ref<Partial<ReconciliationBatchVO>>({}),
  tab = ref('INVALID'),
  invalidLines = ref<ReconciliationLineVO[]>([]),
  matchedLines = ref<ReconciliationLineVO[]>([]),
  issues = ref<ReconciliationIssueVO[]>([]),
  executing = ref(false);
type TabState = { pageNum: number; pageSize: number; total: number; loading: boolean; error: boolean };
const invalidState = reactive<TabState>({ pageNum: 1, pageSize: 10, total: 0, loading: false, error: false });
const matchedState = reactive<TabState>({ pageNum: 1, pageSize: 10, total: 0, loading: false, error: false });
const issueState = reactive<TabState>({ pageNum: 1, pageSize: 10, total: 0, loading: false, error: false });
const issueQuery = reactive<ReconciliationIssueQuery>({
  pageNum: 1,
  pageSize: 10,
  issueType: '',
  status: '',
  purchaseOrderNo: '',
  sessionNo: '',
  providerRecordId: ''
});
const issueTypeOptions = businessOptions('reconciliationIssueType', tt),
  issueStatusOptions = businessOptions('reconciliationIssueStatus', tt);
const activeTabState = computed(() => (tab.value === 'INVALID' ? invalidState : tab.value === 'MATCHED' ? matchedState : issueState));
const activeLines = computed(() => (tab.value === 'INVALID' ? invalidLines.value : matchedLines.value));
const issueFiltered = computed(
  () => !!(issueQuery.issueType || issueQuery.status || issueQuery.purchaseOrderNo || issueQuery.sessionNo || issueQuery.providerRecordId)
);
const lineFilteredEmpty = computed(() => false);
let detailGeneration = 0;
let tabGeneration = 0;
const openBatch = async (row: ReconciliationBatchVO) => {
  const generation = ++detailGeneration;
  batchOpen.value = true;
  detail.value = { id: row.id };
  await reloadDetail(row.id, generation);
};
const reloadDetail = async (requestedId = detail.value.id, generation = ++detailGeneration) => {
  if (!requestedId) return;
  detailLoading.value = true;
  detailError.value = false;
  try {
    const result = (await getReconciliationBatch(requestedId)).data;
    if (!batchOpen.value || generation !== detailGeneration || detail.value.id !== requestedId) return;
    detail.value = result;
    await loadTab();
  } catch {
    if (generation !== detailGeneration || detail.value.id !== requestedId) return;
    detailError.value = true;
  } finally {
    if (generation === detailGeneration) detailLoading.value = false;
  }
};
const closeBatch = () => {
  detailGeneration++;
  tabGeneration++;
  detail.value = {};
};
const loadTab = async () => {
  if (!detail.value.id) return;
  const generation = ++tabGeneration;
  const batchId = detail.value.id;
  const requestedTab = tab.value;
  const state = activeTabState.value;
  const pageNum = state.pageNum;
  const pageSize = state.pageSize;
  const filters = structuredClone(toRaw(issueQuery));
  const current = () => generation === tabGeneration && batchOpen.value && detail.value.id === batchId && tab.value === requestedTab;
  state.loading = true;
  state.error = false;
  try {
    if (requestedTab === 'ISSUES') {
      const request = { ...filters, pageNum, pageSize };
      const result = await listReconciliationIssues(batchId, request);
      if (!current() || JSON.stringify(filters) !== JSON.stringify(toRaw(issueQuery))) return;
      issues.value = result.rows;
      issueState.total = result.total;
    } else {
      const result = await listReconciliationLines(batchId, {
        pageNum,
        pageSize,
        lineStatus: requestedTab as 'INVALID' | 'MATCHED'
      });
      if (!current() || state.pageNum !== pageNum || state.pageSize !== pageSize) return;
      if (requestedTab === 'INVALID') invalidLines.value = result.rows;
      else matchedLines.value = result.rows;
      state.total = result.total;
    }
  } catch {
    if (!current()) return;
    state.error = true;
    if (requestedTab === 'ISSUES') issues.value = [];
    else if (requestedTab === 'INVALID') invalidLines.value = [];
    else matchedLines.value = [];
  } finally {
    if (current()) state.loading = false;
  }
};
const searchIssues = () => {
  issueState.pageNum = 1;
  loadTab();
};
const resetIssues = () => {
  Object.assign(issueQuery, { issueType: '', status: '', purchaseOrderNo: '', sessionNo: '', providerRecordId: '' });
  searchIssues();
};
const confirmExecute = async (row: ReconciliationBatchVO) => {
  if (row.invalidCount > 0) return;
  await proxy?.$modal.confirm(t('paymentReconciliation.executeConfirm'));
  executing.value = true;
  try {
    await executeReconciliationBatch(row.id);
    proxy?.$modal.msgSuccess(t('paymentReconciliation.completed'));
    load();
  } catch {
    proxy?.$modal.msgError(t('paymentReconciliation.executeFailed'));
  } finally {
    executing.value = false;
  }
};
const issueOpen = ref(false),
  issueLoading = ref(false),
  issueError = ref(false),
  issue = ref<Partial<ReconciliationIssueDetailVO>>({});
const pretty = (v?: string) => {
  if (!v) return '-';
  try {
    return JSON.stringify(JSON.parse(v), null, 2);
  } catch {
    return v;
  }
};
const validationLabel = (code?: string) => {
  if (!code) return t('paymentReconciliation.validationPassed');
  const known: Record<string, string> = {
    DUPLICATE_PROVIDER_RECORD: 'duplicateProviderRecord',
    INVALID_EVENT_TYPE: 'invalidEventType',
    INVALID_AMOUNT: 'invalidAmount',
    INVALID_CURRENCY: 'invalidCurrency',
    INVALID_TIMESTAMP: 'invalidTimestamp'
  };
  return known[code] ? t(`paymentReconciliation.${known[code]}`) : code;
};
const actionLabel = (action?: string) => {
  const known: Record<string, string> = {
    UPLOAD: 'actionUpload',
    VALIDATE: 'actionValidate',
    EXECUTE: 'actionExecute',
    FAIL: 'actionFail',
    RESOLVE: 'actionResolve',
    IGNORE: 'actionIgnore'
  };
  return action && known[action] ? t(`paymentReconciliation.${known[action]}`) : action || '-';
};
let issueGeneration = 0;
const openIssue = async (row: ReconciliationIssueVO) => {
  const generation = ++issueGeneration;
  issueOpen.value = true;
  issue.value = { id: row.id };
  await reloadIssue(row.id, generation);
};
const reloadIssue = async (requestedId = issue.value.id, generation = ++issueGeneration) => {
  if (!requestedId) return;
  issueLoading.value = true;
  issueError.value = false;
  try {
    const result = (await getReconciliationIssue(requestedId)).data;
    if (!issueOpen.value || generation !== issueGeneration || issue.value.id !== requestedId) return;
    issue.value = result;
  } catch {
    if (generation !== issueGeneration || issue.value.id !== requestedId) return;
    issueError.value = true;
  } finally {
    if (generation === issueGeneration) issueLoading.value = false;
  }
};
const closeIssue = () => {
  issueGeneration++;
  issue.value = {};
  selectedIssueId.value = '';
  resolutionOpen.value = false;
};
const go = (path: string, query: Record<string, string>) => router.push({ path, query });
const resolutionOpen = ref(false),
  resolutionMode = ref<'resolve' | 'ignore'>('resolve'),
  resolving = ref(false),
  resolutionRef = ref<ElFormInstance>(),
  resolution = reactive<{ resolutionType: ReconciliationResolutionType | ''; remark: string; expectedVersion: number }>({
    resolutionType: '',
    remark: '',
    expectedVersion: 0
  });
const selectedIssueId = ref('');
const resolutionRules = {
  resolutionType: [{ required: true, message: t('paymentReconciliation.classificationRequired') }],
  remark: [{ required: true, whitespace: true, message: t('paymentReconciliation.remarkRequired') }]
};
const openResolution = (mode: 'resolve' | 'ignore') => {
  if (!issue.value.id) return;
  selectedIssueId.value = issue.value.id;
  resolutionMode.value = mode;
  resolution.resolutionType = '';
  resolution.remark = '';
  resolution.expectedVersion = issue.value.version || 0;
  resolutionOpen.value = true;
};
const submitResolution = async () => {
  await resolutionRef.value?.validate();
  const requestedId = selectedIssueId.value;
  if (!requestedId || issue.value.id !== requestedId) {
    proxy?.$modal.msgError(t('paymentReconciliation.issueChanged'));
    return;
  }
  resolving.value = true;
  try {
    const fn = resolutionMode.value === 'resolve' ? resolveReconciliationIssue : ignoreReconciliationIssue;
    if (!resolution.resolutionType) return;
    const result = (
      await fn(requestedId, {
        resolutionType: resolution.resolutionType,
        remark: resolution.remark,
        expectedVersion: resolution.expectedVersion
      })
    ).data;
    if (issue.value.id !== requestedId || selectedIssueId.value !== requestedId) return;
    issue.value = result;
    resolutionOpen.value = false;
    proxy?.$modal.msgSuccess(t('paymentReconciliation.resolved'));
    await loadTab();
  } catch (error: unknown) {
    proxy?.$modal.msgError(businessErrorCode(error) === 40901 ? t('paymentReconciliation.conflict') : t('paymentReconciliation.resolutionFailed'));
  } finally {
    resolving.value = false;
  }
};
const businessErrorCode = (error: unknown) =>
  typeof error === 'object' && error !== null && 'code' in error && typeof error.code === 'number' ? error.code : undefined;
const statusType = (s?: string) =>
  s === 'COMPLETED' || s === 'RESOLVED' ? 'success' : s === 'FAILED' ? 'danger' : s === 'RECONCILING' || s === 'OPEN' ? 'warning' : 'info';
onMounted(load);
const errorMessage = (error: unknown, fallback: string) => {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = error.response as { status?: number; data?: { msg?: string } };
    if (response.status === 409) return `409 ${response.data?.msg || 'state conflict'}`;
  }
  if (typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string' && error.message.trim())
    return error.message;
  return fallback;
};
</script>
<style scoped>
.recon-page {
  max-width: 100%;
  overflow-x: hidden;
}
.filter-band {
  padding: 12px 12px 2px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-blank);
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}
.table-scroll {
  max-width: 100%;
  overflow-x: auto;
}
.summary {
  display: flex;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
  padding: 12px;
  border-block: 1px solid var(--el-border-color);
  background: var(--el-fill-color-light);
  margin-bottom: 8px;
}
.readonly {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-height: 240px;
  overflow: auto;
  padding: 10px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
}
.drawer-actions {
  position: sticky;
  bottom: 0;
  padding: 12px 0;
  background: var(--el-bg-color);
  text-align: right;
}
@media (max-width: 767px) {
  .filter-band :deep(.el-form-item) {
    display: flex;
    margin-right: 0;
    width: 100%;
  }
  .filter-band :deep(.el-form-item__content) {
    min-width: 0;
  }
  .filter-band :deep(.el-input),
  .filter-band :deep(.el-select),
  .filter-band :deep(.el-date-editor) {
    width: 100%;
  }
  .toolbar {
    gap: 8px;
  }
  .summary {
    gap: 8px;
    font-size: 13px;
  }
}
</style>
