import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8');
const [api, types, page, labels, zh, en] = await Promise.all([
  read('src/api/payment/paymentReconciliation/index.ts'),
  read('src/api/payment/paymentReconciliation/types.ts'),
  read('src/views/payment/payment-reconciliation/index.vue'),
  read('src/utils/businessLabels.ts'),
  read('src/lang/zh_CN.ts'),
  read('src/lang/en_US.ts')
]);

const i18nText = await read('src/utils/i18nText.ts');
assert.equal([...i18nText.matchAll(/^\s*失败:\s*'Failed',?$/gm)].length, 1, 'generic Failed text mapping must not be duplicated');

const functions = [
  ['listReconciliationBatches', 'get', '`\${base}/list`'],
  ['uploadReconciliationBatch', 'post', '`\${base}/upload`'],
  ['getReconciliationBatch', 'get', '`\${base}/\${batchId}`'],
  ['listReconciliationLines', 'get', '`\${base}/\${batchId}/lines`'],
  ['listReconciliationIssues', 'get', '`\${base}/\${batchId}/issues`'],
  ['executeReconciliationBatch', 'post', '`\${base}/\${batchId}/execute`'],
  ['getReconciliationIssue', 'get', '`\${base}/issues/\${issueId}`'],
  ['resolveReconciliationIssue', 'post', '`\${base}/issues/\${issueId}/resolve`'],
  ['ignoreReconciliationIssue', 'post', '`\${base}/issues/\${issueId}/ignore`']
];
assert.equal([...api.matchAll(/export const (\w+)/g)].map((match) => match[1]).filter((name) => name.includes('Reconciliation')).length, 9);
for (const [name, method, path] of functions) {
  const start = api.indexOf(`export const ${name}`);
  assert.notEqual(start, -1, `missing ${name}`);
  const end = api.indexOf('export const ', start + 13);
  const body = api.slice(start, end === -1 ? api.length : end);
  assert.ok(body.includes(`method: '${method}'`), `${name} must use ${method.toUpperCase()}`);
  assert.ok(body.includes(`url: ${path}`), `${name} path mismatch`);
  assert.match(body, /AxiosPromise<[^>]+>/, `${name} must expose a typed response wrapper`);
}
for (const field of ['providerCode', 'statementDate', 'file']) assert.match(api, new RegExp(`data\\.append\\('${field}'`));
assert.match(api, /new FormData\(\)/);
assert.match(api, /multipart\/form-data/);
for (const command of ['resolveReconciliationIssue', 'ignoreReconciliationIssue']) {
  const body = api.slice(api.indexOf(`export const ${command}`));
  for (const field of ['resolutionType', 'remark', 'expectedVersion']) assert.match(body, new RegExp(`data\\.${field}`));
}
assert.doesNotMatch(types, /\bid:\s*number\b|\b(?:batchId|lineId|issueId|paymentSessionId|purchaseOrderId|webhookEventId|reversalId):\s*number\b/);
for (const association of ['lineId', 'paymentSessionId', 'purchaseOrderId', 'webhookEventId', 'reversalId', 'resolvedBy']) {
  assert.match(types, new RegExp(`${association}:\\s*string\\s*\\|\\s*null`), `${association} must preserve backend nullability`);
}
assert.match(types, /resolutionType:\s*ReconciliationResolutionType;/);
assert.doesNotMatch(types, /resolutionType\?:\s*ReconciliationResolutionType/);
for (const money of ['amount', 'providerAmount', 'platformAmount']) assert.match(types, new RegExp(`${money}: string`));
for (const union of [
  'ReconciliationBatchStatus',
  'ReconciliationLineStatus',
  'ReconciliationIssueStatus',
  'ReconciliationIssueType',
  'ReconciliationResolutionType'
])
  assert.match(types, new RegExp(`type ${union} =`));
for (const projection of ['sourceRowNumber', 'sourceLine', 'canonicalOriginalFields', 'platformOnly'])
  assert.match(types + page, new RegExp(projection));
assert.match(types, /sourceRowNumber:\s*number\s*\|\s*null/);
assert.match(types, /canonicalOriginalFields:\s*string\s*\|\s*null/);
assert.match(types, /sourceLine:\s*ReconciliationLineVO\s*\|\s*null/);
for (const state of ['invalidState', 'matchedState', 'issueState']) {
  assert.match(page, new RegExp(`const ${state} = reactive<TabState>\\(\\{ pageNum: 1, pageSize: 10, total: 0`));
}
assert.match(page, /v-model:page="activeTabState\.pageNum"/);
assert.match(page, /v-model:limit="activeTabState\.pageSize"/);
assert.match(page, /:total="activeTabState\.total"/);
for (const filter of ['issueType', 'status', 'purchaseOrderNo', 'sessionNo', 'providerRecordId'])
  assert.match(page, new RegExp(`issueQuery\\.${filter}`));
for (const category of [
  'reconciliationBatchStatus',
  'reconciliationLineStatus',
  'reconciliationIssueStatus',
  'reconciliationIssueType',
  'reconciliationResolutionType'
])
  assert.match(labels, new RegExp(category));
for (const permission of [
  'payment:reconciliation:list',
  'payment:reconciliation:query',
  'payment:reconciliation:upload',
  'payment:reconciliation:execute',
  'payment:reconciliation:resolve'
])
  assert.match(page, new RegExp(permission));
for (const route of ['/payment/purchase-order', '/payment/payment-session', '/payment/purchase-reversal-review']) assert.ok(page.includes(route));
assert.match(page, /issue\.status === 'OPEN'/);
assert.match(page, /contenteditable="false"/);
assert.doesNotMatch(page, /rawFieldsJson/);
assert.match(page, /issue\.sourceRowNumber \?\? '-'/);
assert.match(page, /actionLabel\(log\.actionType\)/);
assert.match(page, /validationLabel\(s\.row\.parseError\)/);
assert.match(page, /import type \{ UploadFile, UploadInstance, UploadUserFile \} from 'element-plus'/);
assert.match(page, /selectFile = \(f: UploadFile\)/);
assert.match(page, /f\.raw\?\.size \?\? f\.size \?\? 0/);
for (const generation of ['listGeneration', 'detailGeneration', 'tabGeneration', 'issueGeneration']) assert.match(page, new RegExp(generation));
assert.match(page, /selectedIssueId/);
assert.match(page, /businessErrorCode\(error\) === 40901/);
assert.doesNotMatch(page, /\/409\|conflict\|state\/i/);
assert.match(api, /exposeBusinessCode: 'true'/);
assert.ok(zh.includes('只记录对账结论，不改变支付状态、冲正或钱包余额'));
assert.match(en, /records the reconciliation conclusion only.+does not change payment status, reversals, or wallet balances/i);
console.log('Payment reconciliation frontend contract check passed.');
