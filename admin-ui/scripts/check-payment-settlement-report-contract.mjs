import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8');
const [api, types, page, zh, en, settlementPage] = await Promise.all([
  read('src/api/payment/paymentSettlementReport/index.ts'),
  read('src/api/payment/paymentSettlementReport/types.ts'),
  read('src/views/payment/payment-settlement-report/index.vue'),
  read('src/lang/zh_CN.ts'),
  read('src/lang/en_US.ts'),
  read('src/views/payment/payment-settlement/index.vue')
]);

for (const type of [
  'SettlementReportQuery',
  'SettlementReportRowVO',
  'SettlementReportCurrencyTotalVO',
  'SettlementReportPageVO',
  'SettlementReportBatchVO'
])
  assert.match(types, new RegExp(`interface ${type}`), `missing ${type}`);

for (const field of ['grossPayment', 'refundAmount', 'chargebackAmount', 'totalFee', 'netSettlement'])
  assert.match(types, new RegExp(`${field}: string`), `${field} must remain a string`);
assert.doesNotMatch(types, /\b(?:id|batchId):\s*number\b/);

for (const [name, method, path] of [
  ['listSettlementReport', 'get', '`${base}/list`'],
  ['listSettlementReportBatches', 'get', '`${base}/${date}/${providerCode}/${currencyCode}/batches`'],
  ['exportSettlementReport', 'get', '`${base}/export`']
]) {
  const start = api.indexOf(`export const ${name}`);
  assert.notEqual(start, -1, `missing ${name}`);
  const end = api.indexOf('export const ', start + 13);
  const body = api.slice(start, end === -1 ? api.length : end);
  assert.ok(body.includes(`method: '${method}'`), `${name} method mismatch`);
  assert.ok(body.includes(`url: ${path}`), `${name} path mismatch`);
}

assert.match(api, /exportSettlementReport[\s\S]+AxiosPromise<Blob>/);
assert.match(api, /responseType:\s*'blob'/);
for (const permission of ['payment:settlementReport:list', 'payment:settlementReport:query', 'payment:settlementReport:export'])
  assert.ok(types.includes(permission), `missing ${permission}`);
assert.doesNotMatch(api + types, /Number\([^)]*(?:grossPayment|refundAmount|chargebackAmount|totalFee|netSettlement)/);

assert.match(page, /setUtcRange\(7\)/);
assert.match(page, /setUtcRange\(31\)/);
assert.match(page, /type="daterange"/);
assert.match(page, /currencyTotals/);
assert.match(page, /isNegative/);
assert.doesNotMatch(page, /Number\([^)]*(?:grossPayment|refundAmount|chargebackAmount|totalFee|netSettlement)/);
for (const state of ['loading', 'filteredEmpty', 'empty', 'loadFailed', 'permissionDenied', 'exporting', 'exportFailed'])
  assert.ok(page.includes(state), `missing ${state} state`);
for (const permission of ['payment:settlementReport:list', 'payment:settlementReport:query', 'payment:settlementReport:export'])
  assert.ok(page.includes(permission), `page missing ${permission}`);
assert.match(page, /FileSaver\.saveAs/);
assert.match(page, /batchId/);
assert.match(page, /payment\/payment-settlement/);
assert.match(page, /table-scroll/);
assert.match(page, /@media \(max-width: 600px\)/);
assert.ok(zh.includes('支付结算报表'));
assert.ok(en.includes('Settlement report'));
assert.match(settlementPage, /route\.query\.batchId/);

console.log('Payment settlement report API contract check passed.');
