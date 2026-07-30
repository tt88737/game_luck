import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const read = (path) => {
  const file = resolve(root, path)
  assert.ok(existsSync(file), `missing required file: ${path}`)
  return readFileSync(file, 'utf8')
}

const api = read('src/api/client.ts')
const types = read('src/types/client.ts')
const router = read('src/router/index.ts')
const vite = read('vite.config.ts')
const purchase = read('src/views/PurchaseView.vue')
const checkout = read('src/views/SimulatedCheckoutView.vue')
const result = read('src/views/PurchaseResultView.vue')

for (const value of [
  '/api/client/purchase/orders/${encodeURIComponent(orderNo)}/payment-sessions',
  '/api/client/purchase/payment-sessions/${encodeURIComponent(sessionNo)}',
  '/payment/simulated/checkout/${encodeURIComponent(providerSessionNo)}',
  '/payment/simulated/checkout/${encodeURIComponent(providerSessionNo)}/actions',
  '/payment/simulated/checkout/${encodeURIComponent(providerSessionNo)}/replay',
]) {
  assert.ok(api.includes(value), `missing encoded API path: ${value}`)
}

for (const typeName of ['ClientPaymentSession', 'SimulatedCheckout', 'SimulatedPaymentAction']) {
  assert.match(types, new RegExp(`export (?:interface|type) ${typeName}\\b`), `missing type ${typeName}`)
}
for (const action of ['PAYMENT_SUCCEEDED', 'PAYMENT_FAILED', 'PAYMENT_CANCELLED', 'REFUND_SUCCEEDED', 'CHARGEBACK_CREATED']) {
  assert.ok(types.includes(`'${action}'`), `missing simulated action ${action}`)
}

assert.ok(router.includes("path: '/simulated-checkout/:providerSessionNo'"), 'missing simulated checkout route')
assert.ok(router.includes("path: '/purchase-result/:sessionNo'"), 'missing purchase result route')
assert.match(vite, /['"]\/payment['"]\s*:\s*\{[\s\S]*?target:\s*['"]http:\/\/localhost:8080['"]/, 'dev server must proxy hosted Provider HTTP routes')
assert.match(purchase, /payPurchaseOffer[\s\S]*createPaymentSession[\s\S]*(checkoutUrl|location\.assign)/, 'purchase must create an order, then a payment session, then use checkoutUrl')
assert.doesNotMatch(purchase, /loadWallet\(\)[\s\S]{0,300}(payPurchaseOffer|createPaymentSession)/, 'purchase must not refresh wallet before provider success')

assert.match(checkout, /simulatedCheckout[\s\S]*allowedActions/, 'checkout must render only server-authorized actions')
assert.match(checkout, /executeSimulatedPaymentAction/, 'checkout must dispatch actions through the backend')
assert.match(checkout, /replaySimulatedPayment/, 'checkout must expose explicit replay as a secondary test action')

assert.match(result, /paymentSession[\s\S]*(setInterval|setTimeout)/, 'result must poll the platform payment-session endpoint')
assert.match(result, /SUCCEEDED[\s\S]*walletAccounts/, 'wallet refresh must occur only after a SUCCEEDED platform session')
assert.match(result, /effectiveStatus[\s\S]*expireTime[\s\S]*EXPIRED/, 'result must derive expiry only from platform status and expireTime')
assert.doesNotMatch(result, /route\.query|URLSearchParams|location\.search/, 'result must not trust query-string payment status')

console.log('H5 payment session contract OK')
