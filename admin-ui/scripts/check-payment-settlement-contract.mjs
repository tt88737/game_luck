import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8');
const [api, types, page, zh, en] = await Promise.all([
  read('src/api/payment/paymentSettlement/index.ts'),
  read('src/api/payment/paymentSettlement/types.ts'),
  read('src/views/payment/payment-settlement/index.vue'),
  read('src/lang/zh_CN.ts'),
  read('src/lang/en_US.ts')
]);

for (const [name, method, path] of [
  ['listSettlementBatches', 'get', '`${base}/list`'],
  ['createSettlementBatch', 'post', 'base'],
  ['getSettlementBatch', 'get', '`${base}/${batchId}`'],
  ['listSettlementItems', 'get', '`${base}/${batchId}/items`'],
  ['calculateSettlementBatch', 'post', '`${base}/${batchId}/calculate`'],
  ['closeSettlementBatch', 'post', '`${base}/${batchId}/close`']
]) {
  const start = api.indexOf(`export const ${name}`);
  assert.notEqual(start, -1, `missing ${name}`);
  const end = api.indexOf('export const ', start + 13);
  const body = api.slice(start, end === -1 ? api.length : end);
  assert.ok(body.includes(`method: '${method}'`), `${name} must use ${method.toUpperCase()}`);
  assert.ok(body.includes(`url: ${path}`), `${name} path mismatch`);
  assert.match(body, /AxiosPromise<[^>]+>/, `${name} must have a typed response`);
}

assert.doesNotMatch(types, /\b(?:id|batchId|webhookEventId|paymentSessionId|purchaseOrderId|creatorId|calculatorId|closerId|operatorId):\s*number\b/);
for (const field of [
  'grossPayment',
  'refundAmount',
  'chargebackAmount',
  'totalFee',
  'netSettlement',
  'paymentFeeRate',
  'paymentFixedFee',
  'chargebackFixedFee'
])
  assert.match(types, new RegExp(`${field}: string`), `${field} must remain a string`);
for (const state of ['CREATED', 'CALCULATING', 'CALCULATED', 'CLOSED', 'FAILED']) assert.ok(types.includes(`'${state}'`));
for (const action of ['CREATE', 'CALCULATE', 'CALCULATION_FAILED', 'CLOSE_REJECTED', 'CLOSE']) assert.ok(types.includes(`'${action}'`));
for (const field of ['periodStart', 'periodEnd', 'paymentFeeRate', 'paymentFixedFee', 'chargebackFixedFee'])
  assert.match(types + page, new RegExp(field));
assert.match(page, /31\s*\*\s*24\s*\*\s*60\s*\*\s*60\s*\*\s*1000/);
assert.match(page, /paymentFeeRate.+100|100.+paymentFeeRate/s);
assert.match(page, /eventType/);
for (const tab of ['items', 'evidence', 'history']) assert.match(page, new RegExp(`name="${tab}"`));
for (const permission of [
  'payment:settlement:list',
  'payment:settlement:query',
  'payment:settlement:create',
  'payment:settlement:calculate',
  'payment:settlement:close'
])
  assert.match(page, new RegExp(permission));
assert.match(page, /detail\.status === 'CREATED'/);
assert.match(page, /detail\.status === 'CALCULATED'/);
assert.doesNotMatch(page, /detail\.status === 'CLOSED'.+(?:calculate|close)/s);
assert.match(page, /remark/);
assert.match(page, /\/payment\/payment-reconciliation/);
assert.match(page, /noMutation/);
assert.match(page, /table-scroll/);
assert.match(page, /@media \(max-width: 600px\)/);
assert.match(page, /route\.query\.batchId/);
assert.match(page, /prop="eventType"[^>]+min-width="190"/, 'mobile event names need a stable readable column width');
assert.match(
  page,
  /\.summary-band\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)/s,
  'mobile settlement summary must use a non-clipping one-column track'
);
assert.doesNotMatch(api, /updateSettlementItem|deleteSettlementItem|updateSettlementTotals/);
assert.ok(zh.includes('关闭结算只记录财务确认，不会修改支付订单、冲正或钱包余额'));
assert.match(en, /Closing a settlement records financial confirmation only.+does not change payment orders, reversals, or wallet balances/i);

console.log('Payment settlement frontend contract check passed.');
