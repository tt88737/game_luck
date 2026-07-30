import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8');
const [api, types] = await Promise.all([
  read('src/api/payment/paymentSettlementPayout/index.ts'),
  read('src/api/payment/paymentSettlementPayout/types.ts')
]);

assert.match(api, /const base = '\/payment\/settlement-payout'/);
for (const action of ['submit', 'approve', 'reject', 'cancel']) assert.match(api, new RegExp(`'${action}'`));
assert.match(api, /method: 'get'/);
assert.match(api, /method: 'post'/);
assert.match(api, /method: 'put'/);
assert.match(api, /exposeBusinessCode: 'true'/);
assert.match(types, /id: string/);
assert.match(types, /settlementBatchId: string/);
assert.match(types, /payoutAmount: string/);
assert.match(types, /version: number/);
assert.match(types, /reason: string/);
assert.match(types, /'DRAFT' \| 'PENDING_APPROVAL' \| 'APPROVED' \| 'REJECTED' \| 'CANCELLED'/);
console.log('Payment settlement payout API contract passed');
