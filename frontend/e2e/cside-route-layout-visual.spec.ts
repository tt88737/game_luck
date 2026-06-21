import { expect, test } from '@playwright/test'

const routes = ['/store', '/promo', '/lobby', '/inbox', '/me']

test.beforeEach(async ({ page }) => {
  await page.route('/api/v1/auth/guest', async (route) => {
    await route.fulfill({
      json: {
        user: { userId: 77, email: 'guest_77@guest.tangluck.local', countryCode: 'US', stateCode: 'CA', riskLevel: 'normal', status: 'guest' },
        wallet: { gcBalance: '10000', scBalance: '0', scFrozen: '0' },
        token: 'guest-token',
        accountType: 'guest',
      },
    })
  })
  await page.route('/api/v1/wallet/summary', async (route) => {
    await route.fulfill({
      json: {
        wallet: { gcBalance: '10000.0000', scBalance: '0.5000', scFrozen: '0.0000', scRedeemable: '0.5000' },
        scSourceSummary: [{ source: 'register_bonus', amount: '0.5000' }],
        notices: ['SC is promotional and not sold.'],
      },
    })
  })
  await page.route('/api/v1/lobby', async (route) => {
    await route.fulfill({
      json: {
        cards: [{ cardCode: 'slots_main', title: 'Lucky Slots', subtitle: 'GC play with configured rewards', imageUrl: '', targetUrl: '/lobby/slots/lucky_slots', status: 'active', sortOrder: 10 }],
        campaigns: [{ campaignCode: 'WELCOME_BONUS', campaignType: 'register_bonus', status: 'active' }],
        tasks: [{ taskId: 'DAILY_LOGIN', taskCode: 'DAILY_LOGIN', target: 1, status: 'in_progress' }],
      },
    })
  })
  await page.route('/api/v1/compliance/documents', async (route) => {
    await route.fulfill({ json: [{ documentType: 'amoe', version: 'amoe-v1', title: 'AMOE / No Purchase Necessary', contentUrl: '/legal/amoe-v1' }] })
  })
  await page.route('/api/v1/purchase/packages', async (route) => {
    await route.fulfill({ json: [{ packageCode: 'gc_499', name: 'GC 5,000 Pack', priceAmount: '4.9900', priceCurrency: 'USD', gcAmount: '5000.0000', sandboxOnly: false }] })
  })
  await page.route('/api/v1/campaigns', async (route) => {
    await route.fulfill({ json: [{ campaignCode: 'WELCOME_BONUS', campaignType: 'register_bonus', status: 'active' }] })
  })
  await page.route('/api/v1/tasks/daily', async (route) => {
    await route.fulfill({ json: [{ taskId: 'DAILY_LOGIN', taskCode: 'DAILY_LOGIN', target: 1, status: 'in_progress' }] })
  })
  await page.route('/api/v1/player/activity-summary', async (route) => {
    await route.fulfill({ json: { tasks: [], claimableCount: 0 } })
  })
  await page.route('/api/v1/player/notifications', async (route) => {
    await route.fulfill({ json: [] })
  })
})

for (const route of routes) {
  test(`C-side route layout renders ${route}`, async ({ page }, testInfo) => {
    await page.goto(route)
    await expect(page.locator('.bottom-nav')).toContainText('Store')
    await expect(page.locator('.bottom-nav')).toContainText('Promo')
    await expect(page.locator('.bottom-nav')).toContainText('Lobby')
    await expect(page.locator('.bottom-nav')).toContainText('Inbox')
    await expect(page.locator('.bottom-nav')).toContainText('Me')
    await expect(page.locator('.bottom-nav')).not.toContainText('Wallet')
    await expect(page.locator('.bottom-nav')).not.toContainText('Activity')
    await page.screenshot({ path: `../artifacts/cside-route-layout/${testInfo.project.name}-${route.slice(1)}.png`, fullPage: true })
  })
}
