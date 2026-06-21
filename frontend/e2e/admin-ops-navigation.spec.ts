import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.route('/api/v1/admin/dashboard/summary', async (route) => {
    await route.fulfill({ json: { registrations: 12, claims: 8, scGranted: '3.5000', riskEvents: 1 } })
  })
})

test('admin grouped operations navigation maps B-side modules to C-side impact', async ({ page }, testInfo) => {
  await page.goto('/admin')

  await expect(page.getByText('Store & Packages')).toBeVisible()
  await expect(page.getByText('Impacts Store', { exact: true })).toBeVisible()
  await expect(page.getByText('Promo & Rewards')).toBeVisible()
  await expect(page.getByText('Impacts Promo', { exact: true })).toBeVisible()
  await expect(page.getByText('Game Lobby')).toBeVisible()
  await expect(page.getByText('Impacts Lobby', { exact: true })).toBeVisible()
  await expect(page.getByText('Inbox & Notification')).toBeVisible()
  await expect(page.getByText('Impacts Inbox', { exact: true })).toBeVisible()
  await expect(page.getByText('Wallet & Ledger')).toBeVisible()
  await expect(page.getByText('Impacts Me > Wallet')).toBeVisible()
  await expect(page.getByText('Roles / Permissions')).toBeVisible()
  await page.screenshot({ path: `../artifacts/admin-ops-navigation/${testInfo.project.name}.png`, fullPage: true })
})
