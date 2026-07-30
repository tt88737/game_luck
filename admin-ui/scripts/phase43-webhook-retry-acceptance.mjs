import assert from 'node:assert/strict'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require('C:\\Users\\Administrator\\.codex\\skills\\webapp-testing\\node_modules\\playwright')
const [eventId, providerEventId, output] = process.argv.slice(2)
if (!eventId || !providerEventId || !output) throw new Error('Usage: <eventId> <providerEventId> <output>')

const browser = await chromium.launch({ headless: true, executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe' })
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
try {
  await page.goto('http://127.0.0.1:5173/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder(/用户名|Username/i).fill('admin')
  await page.getByPlaceholder(/密码|Password/i).fill('admin123')
  await page.getByRole('button', { name: /登\s*录|Log in/i }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
  const result = await page.evaluate(async (id) => {
    const stored = localStorage.getItem('Admin-Token') || ''
    const token = stored.startsWith('"') ? JSON.parse(stored) : stored
    const response = await fetch(`/dev-api/payment/webhook-event/${id}/retry`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, clientid: 'e5cd7e4891bf95d1d19206ce24a7b32e', 'Content-Language': 'zh_CN' },
    })
    return response.json()
  }, eventId)
  assert.ok([0, 200].includes(result.code), JSON.stringify(result))
  await page.goto('http://127.0.0.1:5173/payment/payment-webhook-event', { waitUntil: 'networkidle' })
  await page.locator('.ops-filter input').first().fill(providerEventId)
  await page.getByRole('button', { name: /搜索|Search/ }).click()
  await page.getByText(providerEventId, { exact: true }).waitFor()
  await page.locator('table').getByRole('button').first().click()
  await page.getByText(providerEventId, { exact: true }).last().waitFor()
  await page.screenshot({ path: output, fullPage: true })
  console.log(JSON.stringify(result))
} finally {
  await browser.close()
}
