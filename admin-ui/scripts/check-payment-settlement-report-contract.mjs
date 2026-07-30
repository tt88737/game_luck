import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8');
const [api, types, page, zh, en] = await Promise.all([
  read('src/api/payment/paymentSettlementReport/index.ts'),
  read('src/api/payment/paymentSettlementReport/types.ts'),
  read('src/views/payment/payment-settlement-report/index.vue'),
  read('src/lang/zh_CN.ts'),
  read('src/lang/en_US.ts')
]);

for (const typeName of [
  'SettlementReportQuery',
  'SettlementReportRowVO',
  'SettlementReportCurrencyTotalVO',
  'SettlementReportPageVO',
  'SettlementReportBatchVO'
]) {
  assert.match(types, new RegExp(`export interface ${typeName}\\b`), `missing ${typeName}`);
}

assert.match(types, /interface SettlementReportQuery extends PageQuery/);
for (const field of ['startDate', 'endDate']) assert.match(types, new RegExp(`${field}: string`));
for (const field of ['providerCode', 'currencyCode']) assert.match(types, new RegExp(`${field}\\?: string`));

for (const field of [
  'id',
  'grossPayment',
  'refundAmount',
  'chargebackAmount',
  'totalFee',
  'netSettlement',
  'paymentFeeRate',
  'paymentFixedFee',
  'chargebackFixedFee'
]) {
  assert.doesNotMatch(types, new RegExp(`\\b${field}: number\\b`), `${field} must not be numeric`);
  assert.match(types, new RegExp(`\\b${field}: string\\b`), `${field} must remain a string`);
}

for (const [name, method, path] of [
  ['listSettlementReport', 'get', '`${base}/list`'],
  ['listSettlementReportBatches', 'get', '`${base}/${date}/${providerCode}/${currencyCode}/batches`'],
  ['exportSettlementReport', 'get', '`${base}/export`']
]) {
  const start = api.indexOf(`export const ${name}`);
  assert.notEqual(start, -1, `missing ${name}`);
  const end = api.indexOf('export const ', start + 13);
  const body = api.slice(start, end === -1 ? api.length : end);
  assert.ok(body.includes(`method: '${method}'`), `${name} must use ${method.toUpperCase()}`);
  assert.ok(body.includes(`url: ${path}`), `${name} path mismatch`);
}

assert.match(api, /exportSettlementReport[\s\S]+AxiosPromise<Blob>/);
assert.match(api, /exportSettlementReport[\s\S]+responseType: 'blob'/);
for (const permission of [
  'payment:settlementReport:list',
  'payment:settlementReport:query',
  'payment:settlementReport:export'
]) {
  assert.match(api, new RegExp(permission));
}
assert.doesNotMatch(api + types, /\bNumber\s*\(/, 'money and IDs must not be converted through Number()');

for (const marker of [
  'setQuickRange(7)',
  'setQuickRange(31)',
  'validateRange',
  'v-loading="loading"',
  'loadError',
  'permissionDenied',
  'filteredEmpty',
  'currency-summary-band',
  'table-scroll',
  'batchDrawerOpen',
  'exporting',
  "from 'file-saver'",
  '/payment/payment-settlement',
  'batchId'
]) {
  assert.ok(page.includes(marker), `missing report workbench behavior: ${marker}`);
}
for (const permission of [
  'payment:settlementReport:list',
  'payment:settlementReport:query',
  'payment:settlementReport:export'
]) {
  assert.match(page, new RegExp(permission));
}
assert.match(page, /const isNegative\s*=\s*\(value: string\)\s*=>\s*\/\^-\//);
assert.doesNotMatch(page, /\bNumber\s*\(/, 'the report page must not convert money through Number()');
assert.match(page, /@media \(max-width: 600px\)/);
assert.match(page, /overflow-x:\s*auto/);
assert.ok(zh.includes('paymentSettlementReport'));
assert.ok(en.includes('paymentSettlementReport'));

console.log('Payment settlement report frontend contract check passed.');
