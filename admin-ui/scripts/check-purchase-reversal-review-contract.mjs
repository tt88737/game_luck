import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8');

const [api, types, orderTypes, labels, workbench, orderPage, zh, en] = await Promise.all([
  read('src/api/payment/purchaseReversalReview/index.ts'),
  read('src/api/payment/purchaseReversalReview/types.ts'),
  read('src/api/payment/purchaseOrder/types.ts'),
  read('src/utils/businessLabels.ts'),
  read('src/views/payment/purchase-reversal-review/index.vue'),
  read('src/views/payment/purchase-order/index.vue'),
  read('src/lang/zh_CN.ts'),
  read('src/lang/en_US.ts')
]);

for (const path of [
  '/payment/purchase-reversal-review/list',
  '/payment/purchase-reversal-review/',
  '/retry',
  '/accept-loss'
]) {
  assert.match(api, new RegExp(path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
}

for (const contract of [
  'PurchaseReversalReviewQuery',
  'PurchaseReversalReviewVO',
  'PurchaseReversalReviewDetailVO',
  'PurchaseReversalReviewActionForm',
  'PurchaseReversalReviewActionResultVO',
  'PurchaseReversalReviewLogVO'
]) {
  assert.match(types, new RegExp(`interface ${contract}`));
}

assert.doesNotMatch(types, /totalLoss|totalShortfall|lossAmount/);
assert.match(orderTypes, /dispositionStatus: string/);
assert.match(orderTypes, /reviewedBy: string \| number/);
assert.match(labels, /purchaseReversalDispositionStatus/);
assert.match(labels, /purchaseReversalReviewOperationType/);
assert.match(labels, /PENDING_REVIEW/);
assert.match(labels, /RECOVERY_COMPLETED/);
assert.match(labels, /LOSS_ACCEPTED/);

for (const requirement of [
  'payment:reversalReview:query',
  'payment:reversalReview:retry',
  'payment:reversalReview:acceptLoss',
  'useWindowSize',
  'el-segmented',
  'el-drawer',
  'reviewLogs',
  'route.query.reversalNo'
]) {
  assert.match(workbench, new RegExp(requirement.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
}

assert.match(orderPage, /PENDING_REVIEW/);
assert.match(orderPage, /purchase-reversal-review/);
assert.match(zh, /purchaseReversalReview:/);
assert.match(en, /purchaseReversalReview:/);

console.log('Purchase reversal review frontend contract check passed.');
