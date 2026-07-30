import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdir, stat } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const require = createRequire(import.meta.url);
const { chromium } = require('C:\\Users\\Administrator\\.codex\\skills\\webapp-testing\\node_modules\\playwright');
const adminOrigin = 'http://127.0.0.1:5173';
const h5Origin = 'http://127.0.0.1:5174';
const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const outputDir = path.resolve(scriptDir, '..', '..', 'docs', 'implementation');
const desktopEvidence = path.join(outputDir, 'phase45-payment-settlement-runtime-desktop.png');
const mobileEvidence = path.join(outputDir, 'phase45-payment-settlement-runtime-mobile.png');
const clientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';
const dumpExecutable = 'C:\\tools\\mysql-8.0.46-winx64\\bin\\mysqldump.exe';
const mysqlExecutable = 'C:\\tools\\mysql-8.0.46-winx64\\bin\\mysql.exe';
const redisExecutable = 'C:\\tools\\redis-windows\\8.8.0\\Redis-8.8.0-Windows-x64-msys2\\redis-cli.exe';

const browser = await chromium.launch({
  headless: true,
  executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
});

const browserErrors = [];
const attachDiagnostics = (page, label) => {
  page.on('pageerror', (error) => browserErrors.push(`${label}: ${error.message}`));
  page.on('console', (message) => {
    if (message.type() === 'error') browserErrors.push(`${label}: ${message.text()}`);
  });
};

const apiResult = async (page, request) =>
  page.evaluate(
    async ({ request, clientId }) => {
      const stored = localStorage.getItem('Admin-Token') || '';
      const token = stored.startsWith('"') ? JSON.parse(stored) : stored;
      const headers = {
        Authorization: `Bearer ${token}`,
        clientid: clientId,
        'Content-Language': 'zh_CN',
        ...(request.body === undefined ? {} : { 'Content-Type': 'application/json;charset=UTF-8' })
      };
      const response = await fetch(`/dev-api${request.url}`, {
        method: request.method || 'GET',
        headers,
        body: request.body === undefined ? undefined : JSON.stringify(request.body)
      });
      const payload = await response.json();
      return { httpStatus: response.status, ...payload };
    },
    { request, clientId }
  );

const ok = (result, label) => {
  assert.ok([0, 200].includes(result.code), `${label}: ${JSON.stringify(result)}`);
  return result.data;
};

async function loginAdmin(page) {
  const captchaResponse = page.waitForResponse((response) => response.url().includes('/dev-api/auth/code'));
  await page.goto(`${adminOrigin}/login`, { waitUntil: 'networkidle' });
  const captchaPayload = await (await captchaResponse).json();
  await page.getByPlaceholder(/用户名|Username/i).fill('admin');
  await page.getByPlaceholder(/密码|Password/i).fill('admin123');
  if (captchaPayload.data?.captchaEnabled) {
    const redisKey = `global:captcha_codes:${captchaPayload.data.uuid}`;
    const captchaCode = execFileSync(redisExecutable, ['-a', 'gameluck123', '--no-auth-warning', 'GET', redisKey], {
      encoding: 'utf8',
      windowsHide: true
    })
      .trim()
      .replace(/^"|"$/g, '');
    assert.ok(captchaCode && captchaCode !== '(nil)', `Captcha code was not found for ${redisKey}`);
    await page.getByPlaceholder(/验证码|Verification code/i).fill(captchaCode);
  }
  await page.getByRole('button', { name: /登\s*录|Log in/i }).click();
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30_000 });
}

async function createFinancialSource(page, suffix, terminalAction) {
  await page.goto(h5Origin, { waitUntil: 'domcontentloaded' });
  return page.evaluate(
    async ({ suffix, terminalAction }) => {
      const baseHeaders = { 'Content-Type': 'application/json', 'X-Channel-Code': 'h5', 'X-Brand-Code': 'demo' };
      const unwrap = async (response) => {
        const payload = await response.json();
        if (!response.ok || ![0, 200].includes(payload.code)) throw new Error(`${response.url}: ${payload.msg || response.status}`);
        return payload.data;
      };
      const auth = await unwrap(
        await fetch('/api/client/auth/register', {
          method: 'POST',
          headers: baseHeaders,
          body: JSON.stringify({
            username: `phase45_${suffix}`,
            password: 'Demo123456',
            nickname: `Phase 45 ${suffix}`,
            countryCode: 'US',
            stateCode: 'CA',
            ageConfirmed: true,
            termsAccepted: true,
            privacyAccepted: true,
            sweepstakesRulesAccepted: true
          })
        })
      );
      const headers = { ...baseHeaders, Authorization: `Bearer ${auth.accessToken}` };
      const offers = await unwrap(await fetch('/api/client/purchase/offers', { headers }));
      if (!offers.length) throw new Error('No active purchase offers');
      const order = await unwrap(
        await fetch('/api/client/purchase/orders/pay', {
          method: 'POST',
          headers,
          body: JSON.stringify({ offerId: offers[0].offerId, idempotencyKey: `phase45-order-${suffix}` })
        })
      );
      const session = await unwrap(
        await fetch(`/api/client/purchase/orders/${encodeURIComponent(order.orderNo)}/payment-sessions`, {
          method: 'POST',
          headers,
          body: JSON.stringify({ providerCode: 'SIMULATED', requestKey: `phase45-session-${suffix}` })
        })
      );
      const direct = async (response) => {
        const text = await response.text();
        let payload;
        try {
          payload = text ? JSON.parse(text) : null;
        } catch {
          throw new Error(`${response.url}: HTTP ${response.status}, invalid JSON: ${text.slice(0, 200)}`);
        }
        if (!response.ok || !payload) throw new Error(`${response.url}: HTTP ${response.status}, ${text.slice(0, 200)}`);
        return payload;
      };
      const action = async (value) =>
        direct(
          await fetch(`/payment/simulated/checkout/${encodeURIComponent(session.providerSessionNo)}/actions`, {
            method: 'POST',
            headers: baseHeaders,
            body: JSON.stringify({ action: value })
          })
        );
      const payment = await action('PAYMENT_SUCCEEDED');
      const terminal = terminalAction ? await action(terminalAction) : null;
      const checkout = await direct(
        await fetch(`/payment/simulated/checkout/${encodeURIComponent(session.providerSessionNo)}`, { headers: baseHeaders })
      );
      return { order, session, checkout, payment, terminal, terminalAction };
    },
    { suffix, terminalAction }
  );
}

const csvEscape = (value) => `"${String(value).replaceAll('"', '""')}"`;
const csvLine = (source, eventType, providerEventId, occurredTime, amount) =>
  [providerEventId, eventType, source.session.providerSessionNo, source.order.orderNo, source.checkout.payCurrencyCode, amount, occurredTime]
    .map(csvEscape)
    .join(',');

const quoted = (values) => values.map((value) => `'${String(value).replaceAll("'", "''")}'`).join(',');
const nextSettlementStart = () => {
  const value = execFileSync(
    mysqlExecutable,
    [
      '-uroot',
      '-proot',
      '-N',
      '-D',
      'gameluck_vue',
      '-e',
      "select coalesce(unix_timestamp(max(period_end)),0) from gl_payment_settlement_batch where status <> 'FAILED'"
    ],
    { encoding: 'utf8', windowsHide: true }
  ).trim();
  const previousEnd = Number(value) * 1000 + 1;
  return new Date(Math.max(Date.now() - 60_000, previousEnd));
};
const sourceSnapshot = (sources) => {
  const orderNos = sources.map((source) => source.order.orderNo);
  const orderWhere = `purchase_order_no in (${quoted(orderNos)})`;
  const memberIds = execFileSync(
    mysqlExecutable,
    ['-uroot', '-proot', '-N', '-D', 'gameluck_vue', '-e', `select distinct member_id from gl_purchase_order where ${orderWhere} order by member_id`],
    { encoding: 'utf8', windowsHide: true }
  )
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  assert.ok(memberIds.length > 0, 'Source member IDs were not found');
  const memberWhere = `member_id in (${quoted(memberIds)})`;
  const targets = [
    ['gl_purchase_order', orderWhere],
    ['gl_purchase_order_grant_snapshot', orderWhere],
    ['gl_purchase_payment_event', orderWhere],
    ['gl_purchase_reversal', orderWhere],
    ['gl_purchase_reversal_item', orderWhere],
    ['gl_payment_session', orderWhere],
    ['gl_payment_webhook_event', orderWhere],
    ['gl_wallet_transaction', `business_no in (${quoted(orderNos)})`],
    ['gl_wallet_turnover_task', `business_no in (${quoted(orderNos)})`],
    ['gl_wallet_account', memberWhere],
    ['gl_member_profile', `id in (${quoted(memberIds)})`]
  ];
  const hash = createHash('sha256');
  for (const [table, where] of targets) {
    hash.update(table);
    hash.update(
      execFileSync(
        dumpExecutable,
        [
          '-uroot',
          '-proot',
          '--skip-comments',
          '--skip-dump-date',
          '--compact',
          '--no-create-info',
          '--skip-add-locks',
          '--skip-disable-keys',
          '--order-by-primary',
          `--where=${where}`,
          'gameluck_vue',
          table
        ],
        { encoding: 'utf8', windowsHide: true }
      )
    );
  }
  return hash.digest('hex');
};

async function uploadStatement(page, statementDate, csv) {
  return page.evaluate(
    async ({ statementDate, csv, clientId }) => {
      const stored = localStorage.getItem('Admin-Token') || '';
      const token = stored.startsWith('"') ? JSON.parse(stored) : stored;
      const data = new FormData();
      data.append('providerCode', 'SIMULATED');
      data.append('statementDate', statementDate);
      data.append('file', new File([csv], `phase45-${Date.now()}.csv`, { type: 'text/csv' }));
      const response = await fetch('/dev-api/payment/reconciliation/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, clientid: clientId, 'Content-Language': 'zh_CN' },
        body: data
      });
      return { httpStatus: response.status, ...(await response.json()) };
    },
    { statementDate, csv, clientId }
  );
}

const assertNoOverflow = async (page, label) => {
  const overflow = await page.evaluate(() => ({
    document: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    body: document.body.scrollWidth - document.body.clientWidth,
    text: document.body.innerText.trim().length
  }));
  assert.ok(overflow.document <= 1 && overflow.body <= 1, `${label} page overflow: ${JSON.stringify(overflow)}`);
  assert.ok(overflow.text > 100, `${label} page is blank`);
};

async function captureEvidence(page, settlementNo) {
  await page.goto(`${adminOrigin}/payment/payment-settlement`, { waitUntil: 'commit' });
  await page.locator('.filter-band input').first().fill(settlementNo);
  await page.getByRole('button', { name: /搜索|Search/ }).click();
  await page.getByText(settlementNo, { exact: true }).waitFor({ timeout: 20_000 });
  await page.getByRole('button', { name: /创建结算批次|Create settlement/i }).click();
  await page.getByRole('dialog').waitFor();
  await page
    .getByRole('dialog')
    .getByRole('button', { name: /取消|Cancel/ })
    .click();
  await page.locator('table').getByRole('button').first().hover();
  await page.locator('table').getByRole('button').first().click();
  await page.locator('.el-drawer').getByText(settlementNo, { exact: true }).waitFor();
  await page.getByRole('tab', { name: /历史|History/ }).click();
  await page.getByText('CLOSE_REJECTED', { exact: false }).first().waitFor();
  await assertNoOverflow(page, 'desktop');
  await page.screenshot({ path: desktopEvidence, fullPage: true });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`${adminOrigin}/payment/payment-settlement`, { waitUntil: 'commit' });
  await page.locator('.filter-band input').first().fill(settlementNo);
  await page.getByRole('button', { name: /搜索|Search/ }).click();
  await page.getByText(settlementNo, { exact: true }).waitFor({ timeout: 20_000 });
  await page.locator('table').getByRole('button').first().click();
  await page.locator('.el-drawer').getByText(settlementNo, { exact: true }).waitFor();
  await assertNoOverflow(page, 'mobile');
  await page.screenshot({ path: mobileEvidence, fullPage: true });
  assert.ok((await stat(desktopEvidence)).size > 10_000, 'Desktop screenshot is blank or incomplete');
  assert.ok((await stat(mobileEvidence)).size > 10_000, 'Mobile screenshot is blank or incomplete');
}

await mkdir(outputDir, { recursive: true });
const adminContext = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
const adminPage = await adminContext.newPage();
const h5Context = await browser.newContext({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 1 });
const h5Page = await h5Context.newPage();
attachDiagnostics(adminPage, 'admin');
attachDiagnostics(h5Page, 'h5');

const evidenceOnlySettlementNo = process.argv[2];
if (evidenceOnlySettlementNo) {
  try {
    await loginAdmin(adminPage);
    await captureEvidence(adminPage, evidenceOnlySettlementNo);
    assert.deepEqual(browserErrors, [], `Browser errors: ${browserErrors.join('\n')}`);
    console.log(JSON.stringify({ settlementNo: evidenceOnlySettlementNo, evidence: [desktopEvidence, mobileEvidence] }, null, 2));
  } finally {
    await h5Context.close();
    await adminContext.close();
    await browser.close();
  }
  process.exit(0);
}

try {
  await loginAdmin(adminPage);
  const startedAt = nextSettlementStart();
  const unique = `${Date.now()}_${Math.random().toString(16).slice(2, 8)}`;
  const paymentSource = await createFinancialSource(h5Page, `${unique}_pay`, null);
  const refundSource = await createFinancialSource(h5Page, `${unique}_refund`, 'REFUND_SUCCEEDED');
  const chargebackSource = await createFinancialSource(h5Page, `${unique}_chargeback`, 'CHARGEBACK_CREATED');
  const sources = [paymentSource, refundSource, chargebackSource];
  const currency = paymentSource.checkout.payCurrencyCode;
  const amount = String(paymentSource.checkout.payAmount);
  assert.ok(
    sources.every((source) => source.checkout.payCurrencyCode === currency),
    'Source currencies differ'
  );
  assert.ok(
    sources.every((source) => String(source.checkout.payAmount) === amount),
    'Source amounts differ'
  );
  assert.equal(paymentSource.payment.status, 'PROCESSED');
  assert.equal(refundSource.terminal.status, 'PROCESSED');
  assert.equal(chargebackSource.terminal.status, 'PROCESSED');
  const sourceSnapshotBefore = sourceSnapshot(sources);

  await adminPage.waitForTimeout(1500);
  const periodEnd = new Date();
  const createResult = await apiResult(adminPage, {
    url: '/payment/settlement',
    method: 'POST',
    body: {
      providerCode: 'SIMULATED',
      currencyCode: currency,
      periodStart: startedAt.toISOString(),
      periodEnd: periodEnd.toISOString(),
      paymentFeeRate: '0.02900000',
      paymentFixedFee: '0.300000',
      chargebackFixedFee: '15.000000'
    }
  });
  const created = ok(createResult, 'create settlement');
  const calculated = ok(await apiResult(adminPage, { url: `/payment/settlement/${created.id}/calculate`, method: 'POST' }), 'calculate settlement');
  assert.equal(calculated.status, 'CALCULATED');
  assert.equal(calculated.paymentCount, 3);
  assert.equal(calculated.refundCount, 1);
  assert.equal(calculated.chargebackCount, 1);
  assert.equal(calculated.eventCount, 5);

  const missingClose = await apiResult(adminPage, {
    url: `/payment/settlement/${created.id}/close`,
    method: 'POST',
    body: { version: calculated.version, remark: 'Phase 45 missing-date blocker' }
  });
  assert.ok(![0, 200].includes(missingClose.code), `Missing-date close unexpectedly succeeded: ${JSON.stringify(missingClose)}`);

  const occurredTime = new Date(Date.now() - 2000).toISOString();
  const mismatched = (Number(amount) + 1).toFixed(6);
  const rows = [
    csvLine(paymentSource, 'PAYMENT_SUCCEEDED', paymentSource.payment.providerEventId, occurredTime, mismatched),
    csvLine(refundSource, 'PAYMENT_SUCCEEDED', refundSource.payment.providerEventId, occurredTime, amount),
    csvLine(refundSource, 'REFUND_SUCCEEDED', refundSource.terminal.providerEventId, occurredTime, amount),
    csvLine(chargebackSource, 'PAYMENT_SUCCEEDED', chargebackSource.payment.providerEventId, occurredTime, amount),
    csvLine(chargebackSource, 'CHARGEBACK_CREATED', chargebackSource.terminal.providerEventId, occurredTime, amount)
  ];
  const header = 'provider_record_id,event_type,provider_session_no,purchase_order_no,pay_currency_code,pay_amount,occurred_time';
  const statementDate = periodEnd.toISOString().slice(0, 10);
  const uploaded = ok(await uploadStatement(adminPage, statementDate, `${header}\n${rows.join('\n')}\n`), 'upload reconciliation');
  const reconciled = ok(
    await apiResult(adminPage, { url: `/payment/reconciliation/${uploaded.id}/execute`, method: 'POST' }),
    'execute reconciliation'
  );
  assert.equal(reconciled.status, 'COMPLETED');

  const currentBeforeOpenClose = ok(await apiResult(adminPage, { url: `/payment/settlement/${created.id}` }), 'reload calculated settlement');
  const openClose = await apiResult(adminPage, {
    url: `/payment/settlement/${created.id}/close`,
    method: 'POST',
    body: { version: currentBeforeOpenClose.version, remark: 'Phase 45 open-issue blocker' }
  });
  assert.ok(![0, 200].includes(openClose.code), `Open-issue close unexpectedly succeeded: ${JSON.stringify(openClose)}`);

  const issuePage = await apiResult(adminPage, { url: `/payment/reconciliation/${uploaded.id}/issues?pageNum=1&pageSize=500&status=OPEN` });
  assert.ok([0, 200].includes(issuePage.code), JSON.stringify(issuePage));
  const openIssues = (issuePage.rows || []).filter((issue) => issue.status === 'OPEN');
  assert.ok(openIssues.length > 0, 'Expected at least one open reconciliation issue');
  for (const issue of openIssues) {
    ok(
      await apiResult(adminPage, {
        url: `/payment/reconciliation/issues/${issue.id}/ignore`,
        method: 'POST',
        body: { resolutionType: 'EXPECTED_DIFFERENCE', remark: 'Phase 45 runtime close-gate acceptance', expectedVersion: issue.version }
      }),
      `ignore reconciliation issue ${issue.id}`
    );
  }

  const closeReady = ok(await apiResult(adminPage, { url: `/payment/settlement/${created.id}` }), 'reload close-ready settlement');
  const closed = ok(
    await apiResult(adminPage, {
      url: `/payment/settlement/${created.id}/close`,
      method: 'POST',
      body: { version: closeReady.version, remark: 'Phase 45 runtime financial confirmation' }
    }),
    'close settlement'
  );
  assert.equal(closed.status, 'CLOSED');
  const replay = await apiResult(adminPage, {
    url: `/payment/settlement/${created.id}/close`,
    method: 'POST',
    body: { version: closed.version, remark: 'Phase 45 terminal replay' }
  });
  assert.ok(![0, 200].includes(replay.code), `Terminal replay unexpectedly succeeded: ${JSON.stringify(replay)}`);
  const sourceSnapshotAfter = sourceSnapshot(sources);
  assert.equal(
    sourceSnapshotAfter,
    sourceSnapshotBefore,
    'Settlement or reconciliation mutated payment, reversal, risk, turnover, or wallet source rows'
  );

  await captureEvidence(adminPage, closed.settlementNo);
  assert.deepEqual(browserErrors, [], `Browser errors: ${browserErrors.join('\n')}`);
  console.log(
    JSON.stringify(
      {
        settlementId: closed.id,
        settlementNo: closed.settlementNo,
        reconciliationBatchId: uploaded.id,
        sourceOrderNos: sources.map((source) => source.order.orderNo),
        sourceProviderEventIds: sources.flatMap((source) => [source.payment.providerEventId, source.terminal?.providerEventId].filter(Boolean)),
        currency,
        amount,
        totals: {
          eventCount: closed.eventCount,
          grossPayment: closed.grossPayment,
          refundAmount: closed.refundAmount,
          chargebackAmount: closed.chargebackAmount,
          totalFee: closed.totalFee,
          netSettlement: closed.netSettlement
        },
        blockers: { missingDateCode: missingClose.code, openIssueCode: openClose.code, terminalReplayCode: replay.code },
        ignoredIssueCount: openIssues.length,
        sourceSnapshotSha256: sourceSnapshotAfter,
        evidence: [desktopEvidence, mobileEvidence]
      },
      null,
      2
    )
  );
} finally {
  await h5Context.close();
  await adminContext.close();
  await browser.close();
}
