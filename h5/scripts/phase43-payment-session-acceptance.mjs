import assert from 'node:assert/strict'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require('C:\\Users\\Administrator\\.codex\\skills\\webapp-testing\\node_modules\\playwright')

const baseUrl = 'http://127.0.0.1:5174'
const evidenceDir = 'C:\\codex\\project\\docs\\implementation'
const browser = await chromium.launch({
  headless: true,
  executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
})

async function createCheckout(page, suffix) {
  await page.goto(baseUrl)
  const created = await page.evaluate(async (unique) => {
    const headers = { 'Content-Type': 'application/json', 'X-Channel-Code': 'h5', 'X-Brand-Code': 'demo' }
    const unwrap = async (response) => {
      const payload = await response.json()
      if (!response.ok || ![0, 200].includes(payload.code)) throw new Error(`${response.url}: ${payload.msg || `HTTP ${response.status}`}`)
      return payload.data
    }
    const auth = await unwrap(await fetch('/api/client/auth/register', {
      method: 'POST', headers, body: JSON.stringify({
        username: `phase43_${unique}`, password: 'Demo123456', nickname: `Phase 43 ${unique}`,
        countryCode: 'US', stateCode: 'CA', ageConfirmed: true, termsAccepted: true,
        privacyAccepted: true, sweepstakesRulesAccepted: true,
      }),
    }))
    localStorage.setItem('gameluck.client.token', auth.accessToken)
    const authorized = { ...headers, Authorization: `Bearer ${auth.accessToken}` }
    const offers = await unwrap(await fetch('/api/client/purchase/offers', { headers: authorized }))
    if (!offers.length) throw new Error('No active purchase offers')
    const order = await unwrap(await fetch('/api/client/purchase/orders/pay', {
      method: 'POST', headers: authorized,
      body: JSON.stringify({ offerId: offers[0].offerId, idempotencyKey: `phase43-order-${unique}` }),
    }))
    const session = await unwrap(await fetch(`/api/client/purchase/orders/${encodeURIComponent(order.orderNo)}/payment-sessions`, {
      method: 'POST', headers: authorized,
      body: JSON.stringify({ providerCode: 'SIMULATED', requestKey: `phase43-session-${unique}` }),
    }))
    return { session, checkoutUrl: session.checkoutUrl }
  }, suffix)
  await page.goto(created.checkoutUrl)
  await page.getByRole('heading', { name: '托管支付结算' }).waitFor()
  return created.session
}

async function runOutcome(actionText, expectedTitle, screenshotName, mobile = false) {
  const context = await browser.newContext({ viewport: mobile ? { width: 390, height: 844 } : { width: 1440, height: 1000 } })
  const page = await context.newPage()
  const suffix = `${Date.now()}_${Math.random().toString(16).slice(2, 8)}`
  const session = await createCheckout(page, suffix)
  await page.screenshot({ path: `${evidenceDir}/${screenshotName}-checkout.png`, fullPage: true })
  await page.getByRole('button', { name: actionText }).click()
  await page.getByRole('heading', { name: expectedTitle }).waitFor({ timeout: 15000 })
  await page.reload()
  await page.getByRole('heading', { name: expectedTitle }).waitFor({ timeout: 15000 })
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
  assert.equal(overflow, false, `${screenshotName} has page-level horizontal overflow`)
  await page.screenshot({ path: `${evidenceDir}/${screenshotName}-result.png`, fullPage: true })
  assert.ok(page.url().endsWith(`/purchase-result/${session.sessionNo}`), 'result route must use platform session number')
  if (expectedTitle === '支付成功') {
    await page.goto(session.checkoutUrl)
    await page.getByRole('button', { name: '回放最近一次回调' }).click()
    await page.getByText(/^回调 /).waitFor({ timeout: 15000 })
    await page.screenshot({ path: `${evidenceDir}/${screenshotName}-replay.png`, fullPage: true })
  }
  await context.close()
  return session.sessionNo
}

async function runExpired(username, providerSessionNo, sessionNo) {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 } })
  const page = await context.newPage()
  await page.goto(baseUrl)
  await page.evaluate(async ({ loginName }) => {
    const response = await fetch('/api/client/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Channel-Code': 'h5', 'X-Brand-Code': 'demo' },
      body: JSON.stringify({ username: loginName, password: 'Demo123456' }),
    })
    const payload = await response.json()
    if (!response.ok || ![0, 200].includes(payload.code)) throw new Error(payload.msg || `HTTP ${response.status}`)
    localStorage.setItem('gameluck.client.token', payload.data.accessToken)
  }, { loginName: username })
  await page.goto(`${baseUrl}/simulated-checkout/${encodeURIComponent(providerSessionNo)}`)
  await page.getByText('EXPIRED', { exact: true }).waitFor()
  for (const label of ['模拟支付成功', '模拟支付失败', '取消支付']) {
    assert.equal(await page.getByRole('button', { name: label }).count(), 0, `expired checkout exposed ${label}`)
  }
  await page.screenshot({ path: `${evidenceDir}/phase43-h5-expired-mobile-checkout.png`, fullPage: true })
  await page.goto(`${baseUrl}/purchase-result/${encodeURIComponent(sessionNo)}`)
  await page.getByRole('heading', { name: '支付会话已过期' }).waitFor()
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
  assert.equal(overflow, false, 'expired result has page-level horizontal overflow')
  await page.screenshot({ path: `${evidenceDir}/phase43-h5-expired-mobile-result.png`, fullPage: true })
  await context.close()
  return sessionNo
}

try {
  if (process.argv[2] === 'expired') {
    const expired = await runExpired(process.argv[3], process.argv[4], process.argv[5])
    console.log(JSON.stringify({ expired }))
  } else {
    const success = await runOutcome('模拟支付成功', '支付成功', 'phase43-h5-success-desktop')
    const failed = await runOutcome('模拟支付失败', '支付失败', 'phase43-h5-failed-mobile', true)
    const cancelled = await runOutcome('取消支付', '支付已取消', 'phase43-h5-cancelled-mobile', true)
    console.log(JSON.stringify({ success, failed, cancelled }))
  }
} finally {
  await browser.close()
}
