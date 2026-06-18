import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.route('/api/v1/wallet/summary', async (route) => {
    await route.fulfill({
      json: {
        wallet: { gcBalance: '10000.0000', scBalance: '0.5000', scFrozen: '0.0000', scRedeemable: '0.5000' },
        scSourceSummary: [{ source: 'register_bonus', amount: '0.5000' }],
        notices: ['SC is promotional and not sold in P0-A.'],
      },
    })
  })
  await page.route('/api/v1/wallet/ledger?currency=SC', async (route) => {
    await route.fulfill({
      json: {
        items: [{ ledgerId: 1, currency: 'SC', amount: '0.5000', direction: 'credit', businessType: 'register_bonus', businessId: 'WELCOME_BONUS', status: 'posted', createdAt: '2026-06-18T00:00:00Z' }],
        page: 1,
        pageSize: 20,
        total: 1,
      },
    })
  })
  await page.route('/api/v1/campaigns', async (route) => {
    await route.fulfill({ json: [{ campaignCode: 'WELCOME_BONUS', campaignType: 'register_bonus', status: 'active' }] })
  })
  await page.route('/api/v1/tasks/daily', async (route) => {
    await route.fulfill({ json: [{ taskId: 'DAILY_LOGIN', taskCode: 'DAILY_LOGIN', target: 1, status: 'in_progress' }] })
  })
  await page.route('/api/v1/compliance/documents', async (route) => {
    await route.fulfill({
      json: [
        { documentType: 'terms', version: 'terms-v1', title: 'Terms of Use', contentUrl: '/legal/terms-v1' },
        { documentType: 'sweepstakes_rules', version: 'rules-v1', title: 'Sweepstakes Rules', contentUrl: '/legal/rules-v1' },
        { documentType: 'privacy', version: 'privacy-v1', title: 'Privacy Policy', contentUrl: '/legal/privacy-v1' },
        { documentType: 'amoe', version: 'amoe-v1', title: 'AMOE / No Purchase Necessary', contentUrl: '/legal/amoe-v1' },
      ],
    })
  })
  await page.route('/api/v1/auth/register', async (route) => {
    await route.fulfill({
      json: {
        user: { userId: 1, email: 'demo.ca@example.com', countryCode: 'US', stateCode: 'CA', riskLevel: 'normal', status: 'active' },
        wallet: { gcBalance: '0', scBalance: '0', scFrozen: '0' },
        token: 'demo-token',
      },
    })
  })
  await page.route('/api/v1/campaigns/WELCOME_BONUS/claim', async (route) => {
    await route.fulfill({
      json: { claimId: 1, campaignCode: 'WELCOME_BONUS', status: 'claimed', riskAction: 'normal', rewards: [{ currency: 'GC', amount: '10000' }, { currency: 'SC', amount: '0.50' }], ledgerIds: [1, 2] },
    })
  })
  await page.route('/api/v1/admin/dashboard/summary', async (route) => {
    await route.fulfill({ json: { registrations: 12, claims: 8, scGranted: '3.5000', riskEvents: 1 } })
  })
  await page.route('/api/v1/admin/campaigns', async (route) => {
    await route.fulfill({ json: { campaignCode: 'OPS_SC_BONUS', status: 'draft' } })
  })
  await page.route('/api/v1/admin/audit-logs**', async (route) => {
    await route.fulfill({
      json: [{ id: 1, operatorId: 1, operatorRole: 'ops_admin', action: 'campaign_publish', targetType: 'promotion_campaign', targetId: 'OPS_SC_BONUS', beforeJson: '{"status":"draft"}', afterJson: '{"status":"active"}', ip: '127.0.0.1', createdAt: '2026-06-18T00:00:00Z' }],
    })
  })
})

test('P0-A C-side and admin pages render demo workflow', async ({ page }) => {
  await page.goto('/app/register')
  await expect(page.getByText('Register for P0-A demo')).toBeVisible()
  await page.getByLabel('Terms of Use terms-v1').check()
  await page.getByLabel('Sweepstakes Rules rules-v1').check()
  await page.getByLabel('Privacy Policy privacy-v1').check()
  await page.getByRole('button', { name: 'Register and continue' }).click()

  await expect(page.getByText('Rewards wallet')).toBeVisible()
  await expect(page.getByText('WELCOME_BONUS')).toBeVisible()
  await expect(page.getByText('AMOE / No Purchase Necessary')).toBeVisible()
  await page.getByRole('button', { name: 'Claim' }).click()
  await expect(page.getByText('Claimed 10,000 GC + 0.50 SC')).toBeVisible()

  await page.getByRole('link', { name: 'Wallet' }).click()
  await expect(page.getByText('Balances and ledger')).toBeVisible()
  await expect(page.getByText('register_bonus: 0.50 SC')).toBeVisible()

  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
  await expect(page.getByText('Risk events')).toBeVisible()

  await page.goto('/admin/campaigns')
  await expect(page.getByText('OPS_SC_BONUS')).toBeVisible()
  await expect(page.getByText('LEGAL-2026-0618-SC')).toBeVisible()

  await page.goto('/admin/audit-logs')
  await expect(page.getByText('campaign_publish')).toBeVisible()
  await expect(page.getByText('ops_admin')).toBeVisible()
})
