import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8');
const [api, types] = await Promise.all([
  read('src/api/payment/paymentSettlementReport/index.ts'),
  read('src/api/payment/paymentSettlementReport/types.ts')
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

console.log('Payment settlement report API contract check passed.');
