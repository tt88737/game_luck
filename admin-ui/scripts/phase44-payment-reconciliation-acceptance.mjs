import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require('C:\\Users\\Administrator\\.codex\\skills\\webapp-testing\\node_modules\\playwright');
const origin = 'http://127.0.0.1:5173';
const output = '../docs/implementation';
const browser = await chromium.launch({ headless: true, executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe' });

const baseBatch = {
  id: '44001',
  tenantId: '1',
  providerCode: 'SIMULATED',
  statementDate: '2026-07-28',
  originalFileName: 'provider-statement-corrected.csv',
  fileDigest: 'bd87143264b47c',
  totalCount: 12,
  validCount: 12,
  invalidCount: 0,
  matchedCount: 7,
  discrepancyCount: 5,
  status: 'COMPLETED',
  failureReason: '',
  creatorId: '1',
  creatorName: 'admin',
  createTime: '2026-07-28 14:20:00',
  updateTime: '2026-07-28 14:21:04'
};
const matchedLine = {
  id: '66001',
  batchId: '44001',
  sourceRowNumber: 3,
  providerRecordId: 'evt_44001',
  eventType: 'PAYMENT_SUCCEEDED',
  providerSessionNo: 'provider-session-1',
  purchaseOrderNo: 'PO-20260728-001',
  currencyCode: 'USD',
  amount: '100.00',
  occurredTime: '2026-07-28T02:00:00Z',
  status: 'MATCHED',
  parseError: '',
  rawFieldsJson: '["evt_44001","PAYMENT_SUCCEEDED","provider-session-1","PO-20260728-001","USD","100.00"]',
  createTime: '2026-07-28 14:20:01'
};
const invalidLine = {
  ...matchedLine,
  id: '66002',
  sourceRowNumber: 4,
  providerRecordId: 'duplicate_evt',
  status: 'INVALID',
  parseError: 'DUPLICATE_PROVIDER_RECORD'
};
const issueBase = {
  id: '55001',
  batchId: '44001',
  lineId: '66001',
  issueType: 'AMOUNT_MISMATCH',
  status: 'OPEN',
  paymentSessionId: '1',
  sessionNo: 'PS-20260728-001',
  purchaseOrderId: '2',
  purchaseOrderNo: 'PO-20260728-001',
  webhookEventId: '3',
  reversalId: '4',
  providerEventType: 'PAYMENT_SUCCEEDED',
  platformEventType: 'PAYMENT_SUCCEEDED',
  providerCurrencyCode: 'USD',
  platformCurrencyCode: 'USD',
  providerAmount: '100.00',
  platformAmount: '90.00',
  providerStatus: 'SUCCEEDED',
  platformStatus: 'SUCCEEDED',
  diagnosticSnapshotJson: '{"differences":[{"field":"amount","provider":"100.00","platform":"90.00"}]}',
  resolutionType: '',
  resolutionRemark: '',
  resolvedBy: '',
  resolvedTime: '',
  version: 2,
  createTime: '2026-07-28 14:21:00',
  updateTime: '2026-07-28 14:21:00',
  sourceRowNumber: 3,
  sourceLine: matchedLine,
  canonicalOriginalFields: matchedLine.rawFieldsJson,
  platformOnly: false,
  actionLogs: []
};

const installRoutes = async (page, state, permissions = ['*:*:*']) => {
  await page.route('**/dev-api/auth/code', (r) => r.fulfill({ json: { code: 200, data: { captchaEnabled: false } } }));
  await page.route('**/dev-api/auth/tenant/list', (r) => r.fulfill({ json: { code: 200, data: { tenantEnabled: false, voList: [] } } }));
  await page.route('**/dev-api/auth/login', (r) => r.fulfill({ json: { code: 200, data: { access_token: 'phase44-token' } } }));
  await page.route('**/dev-api/system/user/getInfo', (r) =>
    r.fulfill({
      json: {
        code: 200,
        data: { user: { userId: '1', tenantId: '1', userName: 'admin', nickName: 'Admin', avatar: '' }, roles: ['admin'], permissions }
      }
    })
  );
  await page.route('**/dev-api/system/menu/getRouters', (r) =>
    r.fulfill({
      json: {
        code: 200,
        data: [
          {
            path: '/payment',
            component: 'Layout',
            name: 'Payment',
            meta: { title: 'Payment Center' },
            children: [
              {
                path: 'payment-reconciliation',
                component: 'payment/payment-reconciliation/index',
                name: 'PaymentReconciliation',
                meta: { title: 'Payment Reconciliation' }
              }
            ]
          }
        ]
      }
    })
  );
  await page.route('**/dev-api/payment/reconciliation/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (state.delay) await new Promise((resolve) => setTimeout(resolve, 350));
    if (state.delays?.[path]) await new Promise((resolve) => setTimeout(resolve, state.delays[path]));
    if (state.networkError && path.endsWith('/list')) return route.abort('failed');
    const batch = state.batch || baseBatch;
    let body = { code: 200, data: batch };
    if (path.endsWith('/list')) {
      const provider = new URL(route.request().url()).searchParams.get('providerCode') || '';
      if (state.listDelays?.[provider]) await new Promise((resolve) => setTimeout(resolve, state.listDelays[provider]));
      const rows = state.empty ? [] : state.listFixtures?.[provider] || state.batches || [batch];
      body = { code: 200, rows, total: rows.length };
    } else if (path.endsWith('/lines')) {
      const invalid = new URL(route.request().url()).searchParams.get('lineStatus') === 'INVALID';
      const lineStatus = invalid ? 'INVALID' : 'MATCHED';
      if (state.lineDelays?.[lineStatus]) await new Promise((resolve) => setTimeout(resolve, state.lineDelays[lineStatus]));
      const rows = invalid ? (batch.invalidCount > 0 ? [invalidLine] : []) : batch.matchedCount > 0 ? [matchedLine] : [];
      body = { code: 200, rows, total: rows.length };
    } else if (path.endsWith('/issues')) {
      const rows = state.issues || [state.issue || issueBase];
      body = { code: 200, rows, total: rows.length };
    } else if (/\/issues\/\d+$/.test(path)) {
      const issueId = path.split('/').pop();
      body = { code: 200, data: state.issueDetails?.[issueId] || state.issue || issueBase };
    } else if (/\/payment\/reconciliation\/\d+$/.test(path)) {
      const batchId = path.split('/').pop();
      body = { code: 200, data: state.batchDetails?.[batchId] || batch };
    } else if (path.endsWith('/resolve') || path.endsWith('/ignore')) {
      state.resolutionRequests = (state.resolutionRequests || 0) + 1;
      state.lastResolutionUrl = path;
      state.lastResolutionCommand = route.request().postDataJSON();
      body = state.conflict
        ? { code: 40901, msg: 'state conflict' }
        : {
            code: 200,
            data: {
              ...issueBase,
              status: path.endsWith('/ignore') ? 'IGNORED' : 'RESOLVED',
              resolutionType: 'PLATFORM_CONFIRMED',
              resolutionRemark: 'Verified conclusion',
              version: 3,
              actionLogs: [
                {
                  id: '1',
                  batchId: '44001',
                  issueId: '55001',
                  actionType: path.endsWith('/ignore') ? 'IGNORE' : 'RESOLVE',
                  beforeStatus: 'OPEN',
                  afterStatus: path.endsWith('/ignore') ? 'IGNORED' : 'RESOLVED',
                  operatorId: '1',
                  operatorName: 'admin',
                  remark: 'Verified conclusion',
                  createTime: '2026-07-28 14:30:00'
                }
              ]
            }
          };
    }
    await route.fulfill({ status: 200, json: body });
  });
};
const login = async (page) => {
  await page.goto(`${origin}/login`, { waitUntil: 'networkidle' });
  await page.locator('input[type="text"]').fill('admin');
  await page.locator('input[type="password"]').fill('admin123');
  await page.locator('.login-form .el-button--primary').click();
  await page.waitForURL((u) => !u.pathname.includes('/login'));
};
const openBatch = async (page) => {
  await page.locator('table').getByRole('button').first().click();
  await page.locator('.el-drawer.open').waitFor();
  await page.waitForTimeout(350);
};
const openIssue = async (page) => {
  await openBatch(page);
  await page.getByRole('tab', { name: /异常|Issues/ }).click();
  await page.locator('.el-drawer table').getByRole('button').click();
  await page
    .getByText(/AMOUNT_MISMATCH|金额不一致/)
    .last()
    .waitFor();
  await page.waitForTimeout(350);
};
const assertLayout = async (page) => {
  const result = await page.evaluate(() => {
    const visible = (el) => {
      const r = el.getBoundingClientRect();
      const style = getComputedStyle(el);
      const top = document.elementFromPoint(
        Math.max(0, Math.min(innerWidth - 1, r.left + r.width / 2)),
        Math.max(0, Math.min(innerHeight - 1, r.top + r.height / 2))
      );
      return (
        r.width > 0 &&
        r.height > 0 &&
        style.display !== 'none' &&
        style.visibility !== 'hidden' &&
        Number(style.opacity) > 0 &&
        !!top &&
        el.contains(top)
      );
    };
    const clippedCommands = [...document.querySelectorAll('button')]
      .filter(visible)
      .filter((el) => el.scrollWidth > el.clientWidth + 1 || el.scrollHeight > el.clientHeight + 1).length;
    const raw = [...document.querySelectorAll('pre.readonly')];
    const buttons = [...document.querySelectorAll('.el-dialog button,.el-drawer button')].filter(visible);
    const overlapPairs = buttons.flatMap((a, i) =>
      buttons
        .slice(i + 1)
        .filter((b) => {
          if (a.getAttribute('aria-label') === b.getAttribute('aria-label') && /close|关闭/i.test(a.getAttribute('aria-label') || '')) return false;
          const x = a.getBoundingClientRect(),
            y = b.getBoundingClientRect();
          return x.left < y.right && x.right > y.left && x.top < y.bottom && x.bottom > y.top;
        })
        .map((b) => `${a.textContent?.trim() || a.getAttribute('aria-label')} <> ${b.textContent?.trim() || b.getAttribute('aria-label')}`)
    );
    return {
      pageOverflow: document.documentElement.scrollWidth > document.documentElement.clientWidth,
      clippedCommands,
      overlap: overlapPairs.length > 0,
      overlapPairs,
      rawEditable: raw.some((el) => el.contentEditable === 'true'),
      rawInputs: [...document.querySelectorAll('textarea,input')].some((el) => String(el.value).includes('differences'))
    };
  });
  assert.deepEqual(result, { pageOverflow: false, clippedCommands: 0, overlap: false, overlapPairs: [], rawEditable: false, rawInputs: false });
  return result;
};

const screenshots = [];
for (const [viewportName, viewport] of Object.entries({ desktop: { width: 1440, height: 960 }, mobile: { width: 390, height: 844 } })) {
  const page = await browser.newPage({ viewport });
  const state = {};
  await installRoutes(page, state);
  await login(page);
  const go = async () => {
    await page.goto(`${origin}/payment/payment-reconciliation`, { waitUntil: 'networkidle' });
    await page.getByText((state.batch || baseBatch).originalFileName).waitFor();
  };
  for (const scenario of ['upload-validation', 'completed-summary', 'invalid-lines', 'open-issue', 'resolved', 'ignored', 'conflict']) {
    Object.assign(state, {
      empty: false,
      networkError: false,
      delay: false,
      conflict: false,
      resolutionRequests: 0,
      lastResolutionCommand: undefined,
      batch: baseBatch,
      issue: issueBase
    });
    if (scenario === 'invalid-lines')
      state.batch = {
        ...baseBatch,
        status: 'VALIDATED',
        originalFileName: 'provider-statement-invalid.csv',
        totalCount: 2,
        validCount: 1,
        invalidCount: 1,
        matchedCount: 0,
        discrepancyCount: 0
      };
    if (scenario === 'resolved')
      state.issue = {
        ...issueBase,
        status: 'RESOLVED',
        resolutionType: 'PLATFORM_CONFIRMED',
        resolutionRemark: 'Verified conclusion',
        actionLogs: [{ id: '1', operatorName: 'admin', actionType: 'RESOLVE', remark: 'Verified conclusion', createTime: '2026-07-28 14:30:00' }]
      };
    if (scenario === 'conflict') state.conflict = true;
    await go();
    if (scenario === 'upload-validation') {
      await page.getByRole('button', { name: /上传账单|Upload statement/ }).click();
      await page.locator('.el-upload input').setInputFiles({ name: 'statement.txt', mimeType: 'text/plain', buffer: Buffer.from('bad') });
      await page.getByText(/仅支持 CSV|Only CSV/).waitFor();
    } else if (scenario === 'completed-summary') {
      await openBatch(page);
      await page.getByText(/7 已匹配|7 Matched/).waitFor();
    } else if (scenario === 'invalid-lines') {
      await openBatch(page);
      await page.getByText(/渠道记录号重复|Duplicate provider record ID/).waitFor();
      assert.equal((await page.locator('button.is-disabled').count()) > 0, true);
    } else if (scenario === 'open-issue') {
      await openIssue(page);
      await page.getByText(/确认结论|Resolve/).waitFor();
      assert.equal(
        await page
          .locator('pre.readonly')
          .last()
          .textContent()
          .then((v) => v.includes('evt_44001')),
        true
      );
    } else if (scenario === 'resolved') {
      await openIssue(page);
      assert.equal(await page.getByRole('button', { name: /确认结论|Resolve/ }).count(), 0);
    } else if (scenario === 'ignored') {
      await openIssue(page);
      await page.getByRole('button', { name: /忽略异常|Ignore/ }).click();
      await page.locator('.el-dialog .el-select').click();
      await page
        .getByText(/预期差异|Expected Difference/)
        .last()
        .click();
      await page.locator('.el-dialog textarea').fill('Known provider reporting delay');
      await page
        .locator('.el-dialog')
        .getByRole('button', { name: /确定|Confirm/ })
        .click();
      await page.waitForTimeout(300);
      assert.deepEqual(state.lastResolutionCommand, {
        resolutionType: 'EXPECTED_DIFFERENCE',
        remark: 'Known provider reporting delay',
        expectedVersion: 2
      });
      assert.equal(await page.getByRole('button', { name: /忽略异常|Ignore/ }).count(), 0);
    } else {
      await openIssue(page);
      await page.getByRole('button', { name: /确认结论|Resolve/ }).click();
      await page.locator('.el-dialog .el-select').click();
      await page
        .getByText(/平台数据确认|Platform Confirmed/)
        .last()
        .click();
      await page.locator('.el-dialog textarea').fill('Concurrent decision');
      await page
        .locator('.el-dialog')
        .getByRole('button', { name: /确定|Confirm/ })
        .click();
      await page.waitForTimeout(300);
      assert.equal(state.resolutionRequests, 1, 'conflict fixture must receive one resolution command');
      try {
        await page.getByText(/其他操作员|another operator/).waitFor({ timeout: 5000 });
      } catch (error) {
        console.error((await page.locator('body').innerText()).slice(-1200));
        throw error;
      }
    }
    await page.waitForTimeout(250);
    const path = `${output}/phase44-reconciliation-${scenario}-${viewportName}.png`;
    await page.screenshot({ path, fullPage: true });
    screenshots.push(path);
    await assertLayout(page);
  }
  await page.close();
}

// State coverage that does not need dedicated evidence images.
const statePage = await browser.newPage({ viewport: { width: 1440, height: 960 } });
const state = { delay: true };
await installRoutes(statePage, state);
await login(statePage);
const navigation = statePage.goto(`${origin}/payment/payment-reconciliation`);
await statePage.locator('.el-loading-mask').waitFor();
await navigation;
state.delay = false;
state.empty = true;
await statePage.reload({ waitUntil: 'networkidle' });
await statePage.getByText(/暂无对账批次|No reconciliation batches/).waitFor();
await statePage.locator('.filter-band input').first().fill('SIMULATED');
await statePage.getByRole('button', { name: /搜索|Search/ }).click();
await statePage.getByText(/筛选条件|current filters/).waitFor();
state.empty = false;
state.networkError = true;
await statePage.reload({ waitUntil: 'networkidle' });
await statePage.getByText(/批次加载失败|Failed to load reconciliation batches/).waitFor();
state.networkError = false;
await statePage.getByRole('button', { name: /重试|Retry/ }).click();
await statePage.getByText(baseBatch.originalFileName).waitFor();
for (const status of ['FAILED', 'RECONCILING']) {
  state.batch = { ...baseBatch, status, failureReason: status === 'FAILED' ? 'RECONCILIATION_FILE_PROCESSING_FAILED' : '' };
  await statePage.reload({ waitUntil: 'networkidle' });
  await openBatch(statePage);
  await statePage.getByText(status === 'FAILED' ? 'RECONCILIATION_FILE_PROCESSING_FAILED' : /正在执行对账|Reconciliation is running/).waitFor();
}
state.batch = baseBatch;
state.issue = { ...issueBase, status: 'IGNORED', resolutionType: 'EXPECTED_DIFFERENCE' };
await statePage.reload({ waitUntil: 'networkidle' });
await openIssue(statePage);
assert.equal(await statePage.getByRole('button', { name: /忽略异常|Ignore/ }).count(), 0);
await statePage.close();

const uploadResetPage = await browser.newPage({ viewport: { width: 1440, height: 960 } });
await installRoutes(uploadResetPage, {});
await login(uploadResetPage);
await uploadResetPage.goto(`${origin}/payment/payment-reconciliation`, { waitUntil: 'networkidle' });
await uploadResetPage.getByRole('button', { name: /上传账单|Upload statement/ }).click();
await uploadResetPage.locator('.el-upload input').setInputFiles({ name: 'stale.txt', mimeType: 'text/plain', buffer: Buffer.from('bad') });
await uploadResetPage.getByText(/仅支持 CSV|Only CSV/).waitFor();
await uploadResetPage
  .locator('.el-dialog')
  .getByRole('button', { name: /取消|Cancel/ })
  .click();
await uploadResetPage.locator('.el-dialog').waitFor({ state: 'hidden' });
await uploadResetPage.getByRole('button', { name: /上传账单|Upload statement/ }).click();
assert.equal(await uploadResetPage.getByText(/仅支持 CSV|Only CSV/).count(), 0);
assert.equal(await uploadResetPage.locator('.el-dialog:visible .el-upload-list__item:visible').count(), 0);
assert.equal(await uploadResetPage.locator('.el-dialog input').first().inputValue(), 'SIMULATED');
assert.equal(await uploadResetPage.locator('.el-dialog input').nth(1).inputValue(), '');
await uploadResetPage.close();

const listRacePage = await browser.newPage({ viewport: { width: 1440, height: 960 } });
const listRaceState = {
  listFixtures: {
    A: [{ ...baseBatch, id: '44101', providerCode: 'A', originalFileName: 'late-list-a.csv' }],
    B: [{ ...baseBatch, id: '44102', providerCode: 'B', originalFileName: 'current-list-b.csv' }]
  },
  listDelays: { A: 600, B: 20 }
};
await installRoutes(listRacePage, listRaceState);
await login(listRacePage);
await listRacePage.goto(`${origin}/payment/payment-reconciliation`, { waitUntil: 'networkidle' });
const providerInput = listRacePage.locator('.filter-band input').first();
await providerInput.fill('A');
await listRacePage.getByRole('button', { name: /搜索|Search/ }).click();
await providerInput.fill('B');
await listRacePage.getByRole('button', { name: /搜索|Search/ }).click();
await listRacePage.getByText('current-list-b.csv').waitFor();
await listRacePage.waitForTimeout(700);
assert.equal(await listRacePage.getByText('late-list-a.csv').count(), 0, 'late list A must not overwrite current filter B');
await listRacePage.close();

const batchRacePage = await browser.newPage({ viewport: { width: 1440, height: 960 } });
const batchA = { ...baseBatch, id: '44001', originalFileName: 'late-batch-a.csv' };
const batchB = { ...baseBatch, id: '44002', originalFileName: 'current-batch-b.csv', invalidCount: 1 };
const batchRaceState = {
  batches: [batchA, batchB],
  batchDetails: { '44001': batchA, '44002': batchB },
  delays: { '/dev-api/payment/reconciliation/44001': 600, '/dev-api/payment/reconciliation/44002': 20 },
  lineDelays: { INVALID: 500, MATCHED: 20 }
};
await installRoutes(batchRacePage, batchRaceState);
await login(batchRacePage);
await batchRacePage.goto(`${origin}/payment/payment-reconciliation`, { waitUntil: 'networkidle' });
const batchButtons = batchRacePage.locator('table').getByRole('button');
await batchButtons.nth(0).click();
await batchRacePage.locator('.el-drawer__close-btn:visible').last().click();
await batchRacePage.waitForTimeout(350);
await batchButtons.nth(1).click();
await batchRacePage.getByText('current-batch-b.csv').waitFor();
await batchRacePage.getByRole('tab', { name: /已匹配|Matched/ }).click();
await batchRacePage.getByText('evt_44001').waitFor();
await batchRacePage.waitForTimeout(700);
assert.equal(await batchRacePage.getByText('late-batch-a.csv').count(), 1, 'late batch A detail must not overwrite batch B');
assert.equal(await batchRacePage.getByRole('tab', { name: /已匹配|Matched/ }).getAttribute('aria-selected'), 'true');
await batchRacePage.close();

const racePage = await browser.newPage({ viewport: { width: 1440, height: 960 } });
const issueA = { ...issueBase, id: '55001', providerAmount: '111.00' };
const issueB = { ...issueBase, id: '55002', providerAmount: '222.00', version: 7 };
const raceState = {
  issues: [issueA, issueB],
  issueDetails: { '55001': issueA, '55002': issueB },
  delays: {
    '/dev-api/payment/reconciliation/issues/55001': 600,
    '/dev-api/payment/reconciliation/issues/55002': 20
  }
};
await installRoutes(racePage, raceState);
await login(racePage);
await racePage.goto(`${origin}/payment/payment-reconciliation`, { waitUntil: 'networkidle' });
await openBatch(racePage);
await racePage.getByRole('tab', { name: /异常|Issues/ }).click();
const issueButtons = racePage.locator('.el-drawer table').getByRole('button');
await issueButtons.nth(0).click();
await racePage.locator('.el-drawer__close-btn:visible').last().click();
await racePage.waitForTimeout(350);
await issueButtons.nth(1).click();
await racePage.getByText('222.00 / 90.00').waitFor();
await racePage.waitForTimeout(700);
assert.equal(await racePage.getByText('111.00 / 90.00').count(), 0, 'late issue A must not overwrite issue B');
await racePage.getByRole('button', { name: /确认结论|Resolve/ }).click();
await racePage.locator('.el-dialog .el-select').click();
await racePage
  .getByText(/平台数据确认|Platform Confirmed/)
  .last()
  .click();
await racePage.locator('.el-dialog textarea').fill('Resolve selected issue B');
await racePage
  .locator('.el-dialog')
  .getByRole('button', { name: /确定|Confirm/ })
  .click();
await racePage.waitForTimeout(300);
assert.equal(raceState.lastResolutionUrl, '/dev-api/payment/reconciliation/issues/55002/resolve');
assert.equal(raceState.lastResolutionCommand.expectedVersion, 7);
await racePage.close();
const denied = await browser.newPage({ viewport: { width: 390, height: 844 } });
const deniedState = {};
await installRoutes(denied, deniedState, []);
await login(denied);
await denied.goto(`${origin}/payment/payment-reconciliation`);
await denied.getByText(/无权查看|do not have permission/).waitFor();
await denied.close();

const hashes = {};
for (const path of screenshots)
  hashes[path.split('/').pop()] = createHash('sha256')
    .update(await readFile(path))
    .digest('hex');
assert.equal(new Set(Object.values(hashes)).size, screenshots.length);
console.log(
  JSON.stringify(
    {
      screenshots: hashes,
      dom: { pageOverflow: false, clippedCommands: 0, overlap: false, rawEditable: false, rawInputs: false },
      states: ['loading', 'empty', 'filtered-empty', 'network-error-retry', 'permission-denied', 'failed', 'reconciling', 'ignored']
    },
    null,
    2
  )
);
await browser.close();
