import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.route('/api/v1/wallet/summary', async (route) => {
    await route.fulfill({
      json: {
        wallet: { gcBalance: '10000.0000', scBalance: '0.5000', scFrozen: '0.0000', scRedeemable: '0.5000' },
        scSourceSummary: [{ source: 'register_bonus', amount: '0.5000' }],
        notices: ['SC is promotional and not sold.'],
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
  await page.route('/api/v1/lobby', async (route) => {
    await route.fulfill({
      json: {
        cards: [
          { cardCode: 'slots_main', title: 'Lucky Slots', subtitle: 'GC play with configured rewards', imageUrl: '/assets/lobby/slots-main.png', targetUrl: '/app/activity', status: 'active', sortOrder: 10 },
          { cardCode: 'jackpot_events', title: 'Live Events', subtitle: 'Configured bonus events', imageUrl: '/assets/lobby/jackpot-events.png', targetUrl: '/app/activity', status: 'active', sortOrder: 20 },
        ],
        campaigns: [{ campaignCode: 'WELCOME_BONUS', campaignType: 'register_bonus', status: 'active' }],
        tasks: [{ taskId: 'DAILY_LOGIN', taskCode: 'DAILY_LOGIN', target: 1, status: 'in_progress' }],
      },
    })
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
        user: { userId: 1, email: 'player.ca@example.com', countryCode: 'US', stateCode: 'CA', riskLevel: 'normal', status: 'active' },
        wallet: { gcBalance: '0', scBalance: '0', scFrozen: '0' },
        token: 'user-token',
      },
    })
  })
  await page.route('/api/v1/auth/login', async (route) => {
    const request = route.request().postDataJSON() as { email?: string }
    await route.fulfill({
      json: {
        user: { userId: 1, email: request.email ?? 'player.ca@example.com', countryCode: 'US', stateCode: 'CA', riskLevel: 'normal', status: 'active' },
        wallet: { gcBalance: '0', scBalance: '0', scFrozen: '0' },
        token: 'user-token',
      },
    })
  })
  await page.route('/api/v1/me', async (route) => {
    await route.fulfill({
      json: {
        user: { userId: 1, email: 'player.ca@example.com', countryCode: 'US', stateCode: 'CA', riskLevel: 'normal', status: 'active' },
        wallet: { gcBalance: '0', scBalance: '0', scFrozen: '0' },
        token: 'user-token',
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
    await route.fulfill({
      json: [{
        campaignCode: 'register_bonus_v1',
        name: 'Welcome Bonus',
        campaignType: 'register_bonus',
        status: 'active',
        scStrategy: 'default_small_sc',
        rulesVersion: 'rules-v1',
        legalApprovalId: 'LEGAL-2026-0617-CA',
        riskAction: 'gc_only',
        eligibleRegionsJson: '["CA","TX"]',
        blockedRegionsJson: '["WA"]',
        rewardPolicyJson: '[{"currency":"GC","amount":10000},{"currency":"SC","amount":"0.50"}]',
      }],
    })
  })
  await page.route('/api/v1/admin/audit-logs**', async (route) => {
    await route.fulfill({
      json: [{ id: 1, operatorId: 1, operatorRole: 'ops_admin', action: 'campaign_publish', targetType: 'promotion_campaign', targetId: 'OPS_SC_BONUS', beforeJson: '{"status":"draft"}', afterJson: '{"status":"active"}', ip: '127.0.0.1', createdAt: '2026-06-18T00:00:00Z' }],
    })
  })
  await page.route('/api/v1/purchase/packages', async (route) => {
    await route.fulfill({
      json: [{ packageCode: 'gc_499', name: 'GC 5,000 Pack', priceAmount: '4.9900', priceCurrency: 'USD', gcAmount: '5000.0000', sandboxOnly: false }],
    })
  })
  await page.route('/api/v1/purchase/orders', async (route) => {
    await route.fulfill({
      json: { orderId: 'ord_1001', userId: 1, packageCode: 'gc_499', priceAmount: '4.9900', priceCurrency: 'USD', status: 'paid', provider: 'manual', currencyGranted: 'GC', amountGranted: '5000.0000', createdAt: '2026-06-19T00:00:00Z' },
    })
  })
  let kycStatus = 'not_started'
  await page.route('/api/v1/kyc/status', async (route) => {
    await route.fulfill({ json: { userId: 1, status: kycStatus, legalName: kycStatus === 'not_started' ? null : 'P1 User', reviewReason: kycStatus === 'approved' ? 'approved by ops' : null, updatedAt: '2026-06-19T00:00:00Z' } })
  })
  await page.route('/api/v1/kyc/applications', async (route) => {
    kycStatus = 'reviewing'
    await route.fulfill({ json: { userId: 1, status: 'reviewing', legalName: 'P1 User', reviewReason: null, updatedAt: '2026-06-19T00:00:00Z' } })
  })
  await page.route('/api/v1/redemptions', async (route) => {
    await route.fulfill({ json: { redemptionId: 'red_1001', userId: 1, scAmount: '0.5000', method: 'gift_card', status: 'reviewing', sandboxOnly: false, createdAt: '2026-06-19T00:00:00Z' } })
  })
  await page.route('/api/v1/admin/kyc/1/approve', async (route) => {
    kycStatus = 'approved'
    await route.fulfill({ json: { userId: 1, status: 'approved', legalName: 'P1 User', reviewReason: 'approved by ops', updatedAt: '2026-06-19T00:00:01Z' } })
  })
  await page.route('/api/v1/admin/p1/operations', async (route) => {
    await route.fulfill({
      json: {
        purchaseOrders: [{ orderId: 'ord_1001', userId: 1, packageCode: 'gc_499', priceAmount: '4.9900', priceCurrency: 'USD', status: 'paid', provider: 'manual', currencyGranted: 'GC', amountGranted: '5000.0000', createdAt: '2026-06-19T00:00:00Z' }],
        kycApplications: [{ userId: 1, status: kycStatus === 'approved' ? 'approved' : 'reviewing', legalName: 'P1 User', reviewReason: null, updatedAt: '2026-06-19T00:00:00Z' }],
        redemptionRequests: [{ redemptionId: 'red_1001', userId: 1, scAmount: '0.5000', method: 'gift_card', status: 'reviewing', sandboxOnly: false, createdAt: '2026-06-19T00:00:00Z' }],
      },
    })
  })
})

test('C-side and admin pages render production workflow', async ({ page }) => {
  await page.goto('/app/register')
  await expect(page.getByRole('heading', { name: 'Create your account' })).toBeVisible()
  await page.getByLabel('Terms of Use terms-v1').check()
  await page.getByLabel('Sweepstakes Rules rules-v1').check()
  await page.getByLabel('Privacy Policy privacy-v1').check()
  await page.getByRole('button', { name: 'Register and continue' }).click()

  await expect(page.getByRole('heading', { name: 'Lobby', level: 1 })).toBeVisible()
  await expect(page.getByText('Featured games')).toBeVisible()
  await expect(page.getByText('Lucky Slots')).toBeVisible()
  await expect(page.getByText('KYC required')).toBeVisible()
  await expect(page.getByText('Daily bonus')).toBeVisible()
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
  await expect(page.getByText('register_bonus_v1')).toBeVisible()
  await expect(page.getByText('LEGAL-2026-0617-CA')).toBeVisible()

  await page.goto('/admin/audit-logs')
  await expect(page.getByText('campaign_publish')).toBeVisible()
  await expect(page.getByText('ops_admin')).toBeVisible()
})

test('store, KYC, redemption, and ops pages render operating loop', async ({ page }) => {
  const email = `e2e.${Date.now()}@example.com`
  await page.goto('/app/register')
  await page.locator('input[type="email"]').fill(email)
  await page.getByLabel('Terms of Use terms-v1').check()
  await page.getByLabel('Sweepstakes Rules rules-v1').check()
  await page.getByLabel('Privacy Policy privacy-v1').check()
  await page.getByRole('button', { name: 'Register and continue' }).click()

  await page.evaluate(() => {
    localStorage.removeItem('tangluck_token')
    localStorage.removeItem('tangluck_user_id')
  })
  await page.goto('/app/login')
  await page.locator('input[type="email"]').fill(email)
  await page.locator('input[type="password"]').fill('Password123!')
  await page.locator('[data-test="login-submit"]').click()
  await expect(page.getByRole('heading', { name: 'Lobby', level: 1 })).toBeVisible()

  await page.getByLabel('App navigation').getByRole('link', { name: 'Store' }).click()
  await expect(page.getByText('GC 5,000 Pack')).toBeVisible()
  await page.getByRole('button', { name: 'Buy' }).click()
  await expect(page.getByText('Order paid')).toBeVisible()
  await expect(page.getByText('ord_1001')).toBeVisible()

  await page.getByRole('link', { name: 'KYC' }).click()
  await expect(page.getByText('not_started')).toBeVisible()
  await page.getByLabel('Legal name').fill('P1 User')
  await page.getByLabel('Address').fill('100 Main Street')
  await page.locator('[data-test="submit-kyc"]').click()
  await expect(page.getByText('reviewing')).toBeVisible()

  await page.getByLabel('App navigation').getByRole('link', { name: 'Redeem' }).click()
  await expect(page.getByText('KYC approval is required')).toBeVisible()

  await page.goto('/admin/p1')
  await expect(page.getByRole('heading', { name: 'Purchase, KYC, redemption' })).toBeVisible()
  await expect(page.getByText('ord_1001')).toBeVisible()
  await page.getByRole('button', { name: 'Approve' }).click()
  await expect(page.getByText('KYC approved for user 1.')).toBeVisible()

  await page.goto('/app/redemption')
  await page.locator('[data-test="submit-redemption"]').click()
  await expect(page.getByText('Request red_1001 is waiting for manual review.')).toBeVisible()
})

test('uses English by default and Chinese for zh browser language', async ({ browser }) => {
  const englishContext = await browser.newContext({ locale: 'en-US' })
  const englishPage = await englishContext.newPage()
  await englishPage.goto('/app/register')
  await expect(englishPage.getByRole('heading', { name: 'Create your account' })).toBeVisible()
  await englishContext.close()

  const chineseContext = await browser.newContext({ locale: 'zh-CN' })
  const chinesePage = await chineseContext.newPage()
  await chinesePage.goto('/app/register')
  await expect(chinesePage.getByRole('heading', { name: '创建你的账号' })).toBeVisible()
  await chineseContext.close()
})
