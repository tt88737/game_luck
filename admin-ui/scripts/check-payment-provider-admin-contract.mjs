import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const read = (path) => readFileSync(resolve(root, path), 'utf8');

const sessionApi = read('src/api/payment/paymentSession/index.ts');
const sessionTypes = read('src/api/payment/paymentSession/types.ts');
const webhookApi = read('src/api/payment/paymentWebhookEvent/index.ts');
const webhookTypes = read('src/api/payment/paymentWebhookEvent/types.ts');
const sessionPage = read('src/views/payment/payment-session/index.vue');
const webhookPage = read('src/views/payment/payment-webhook-event/index.vue');
const labels = read('src/utils/businessLabels.ts');
const titles = read('src/utils/i18nTitle.ts');
const zh = read('src/lang/zh_CN.ts');
const en = read('src/lang/en_US.ts');

for (const [source, value] of [
  [sessionApi, "url: '/payment/payment-session/list'"],
  [sessionApi, "url: '/payment/payment-session/' + id"],
  [webhookApi, "url: '/payment/webhook-event/list'"],
  [webhookApi, "url: '/payment/webhook-event/' + id"],
  [webhookApi, "url: '/payment/webhook-event/' + id + '/retry'"]
]) assert.ok(source.includes(value), `missing exact API contract: ${value}`);

for (const field of ['sessionNo', 'purchaseOrderNo', 'providerSessionNo', 'memberId', 'memberNo', 'providerCode', 'status', 'payCurrencyCode', 'beginTime', 'endTime']) {
  assert.match(sessionTypes, new RegExp(`\\b${field}\\??:`), `session query/VO field missing: ${field}`);
}
for (const field of ['providerEventId', 'purchaseOrderNo', 'sessionNo', 'providerSessionNo', 'eventType', 'status', 'providerCode', 'beginTime', 'endTime']) {
  assert.match(webhookTypes, new RegExp(`\\b${field}\\??:`), `webhook query/VO field missing: ${field}`);
}
assert.match(sessionTypes, /payAmount:\s*string/, 'money must remain a string');
assert.match(sessionTypes, /type PaymentSessionStatus\s*=\s*'CREATED'.*'EXPIRED'/s, 'session status must be an explicit union');
assert.match(webhookTypes, /type PaymentWebhookStatus\s*=\s*'RECEIVED'.*'IGNORED'/s, 'webhook status must be an explicit union');
assert.match(webhookTypes, /interface PaymentWebhookEventDetailVO[^{]*\{[^}]*rawBody:\s*string;[^}]*signatureDigest:\s*string;/s, 'raw body and digest belong to detail only');
assert.doesNotMatch(webhookTypes.match(/interface PaymentWebhookEventVO[^{]*\{[\s\S]*?\n\}/)?.[0] || '', /rawBody|signatureDigest/, 'list summary must not expose raw body or digest');

for (const permission of ['payment:paymentSession:list', 'payment:paymentSession:query']) assert.ok(sessionPage.includes(permission), `missing permission ${permission}`);
for (const permission of ['payment:webhookEvent:list', 'payment:webhookEvent:query', 'payment:webhookEvent:retry']) assert.ok(webhookPage.includes(permission), `missing permission ${permission}`);
assert.match(webhookPage, /v-if="[^\"]*status\s*===\s*'FAILED'[^\"]*"[\s\S]{0,300}v-hasPermi="\['payment:webhookEvent:retry'\]"/, 'retry must only render for FAILED with retry permission');
assert.match(webhookPage, /<pre[^>]*>[\s\S]*formattedRawBody[\s\S]*<\/pre>/, 'raw payload must use a read-only pre element');
assert.match(webhookPage, /formattedRawBody[\s\S]*detail\.value\.rawBody/, 'formatted payload must derive from detail rawBody');
assert.doesNotMatch(webhookPage, /v-model[^>]*(rawBody|signatureDigest)/, 'raw payload and digest must never be editable');

for (const group of ['paymentSessionStatus', 'paymentWebhookEventType', 'paymentWebhookStatus']) assert.ok(labels.includes(group), `missing business label group ${group}`);
for (const key of ['paymentSessions', 'paymentWebhookEvents']) {
  assert.ok(titles.includes(`route.${key}`), `missing title map ${key}`);
  assert.ok(zh.includes(`${key}:`), `missing zh route key ${key}`);
  assert.ok(en.includes(`${key}:`), `missing en route key ${key}`);
}
for (const namespace of ['paymentSession:', 'paymentWebhookEvent:']) {
  assert.ok(zh.includes(namespace), `missing zh namespace ${namespace}`);
  assert.ok(en.includes(namespace), `missing en namespace ${namespace}`);
}

console.log('payment provider Admin contract OK');
