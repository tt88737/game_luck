import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdir, stat } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const require = createRequire(import.meta.url);
const { chromium } = require('C:\\Users\\Administrator\\.codex\\skills\\webapp-testing\\node_modules\\playwright');
const origin = 'http://127.0.0.1:5173';
const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const outputDir = path.resolve(scriptDir, '..', '..', 'docs', 'implementation');
const desktopEvidence = path.join(outputDir, 'phase46-payment-settlement-report-desktop.png');
const mobileEvidence = path.join(outputDir, 'phase46-payment-settlement-report-mobile.png');
const mysql = 'C:\\tools\\mysql-8.0.46-winx64\\bin\\mysql.exe';
const redis = 'C:\\tools\\redis-windows\\8.8.0\\Redis-8.8.0-Windows-x64-msys2\\redis-cli.exe';
const clientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';
const startDate = '2026-07-28';
const endDate = '2026-07-29';
const sourceTables = [
  'gl_payment_settlement_batch',
  'gl_purchase_order',
  'gl_purchase_payment_event',
  'gl_purchase_reversal',
  'gl_member_risk_event',
  'gl_wallet_turnover_task',
  'gl_wallet_account',
  'gl_wallet_transaction'
];

const sql = (statement) =>
  execFileSync(mysql, ['-uroot', '-proot', '-N', '-D', 'gameluck_vue', '-e', statement], {
    encoding: 'utf8',
    windowsHide: true
  }).trim();

const checksums = () =>
  Object.fromEntries(
    sourceTables.map((table) => {
      const parts = sql(`checksum table ${table}`).split(/\s+/);
      return [table, parts.at(-1)];
    })
  );

const expectedRows = () => {
  const statement = `select date_format(date(convert_tz(period_start,@@session.time_zone,'+00:00')),'%Y-%m-%d'),provider_code,currency_code,count(*),sum(event_count),sum(payment_count),sum(refund_count),sum(chargeback_count),cast(sum(gross_payment) as char),cast(sum(refund_amount) as char),cast(sum(chargeback_amount) as char),cast(sum(total_fee) as char),cast(sum(net_settlement) as char) from gl_payment_settlement_batch where tenant_id='000000' and status='CLOSED' and period_start>='${startDate} 00:00:00' and period_start<'2026-07-30 00:00:00' group by date(convert_tz(period_start,@@session.time_zone,'+00:00')),provider_code,currency_code order by date(convert_tz(period_start,@@session.time_zone,'+00:00')) desc,provider_code,currency_code`;
  return sql(statement)
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => {
      const [
        settlementDate,
        providerCode,
        currencyCode,
        batchCount,
        eventCount,
        paymentCount,
        refundCount,
        chargebackCount,
        grossPayment,
        refundAmount,
        chargebackAmount,
        totalFee,
        netSettlement
      ] = line.split('\t');
      return {
        settlementDate,
        providerCode,
        currencyCode,
        batchCount: Number(batchCount),
        eventCount: Number(eventCount),
        paymentCount: Number(paymentCount),
        refundCount: Number(refundCount),
        chargebackCount: Number(chargebackCount),
        grossPayment,
        refundAmount,
        chargebackAmount,
        totalFee,
        netSettlement
      };
    });
};

const browser = await chromium.launch({ headless: true, executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe' });
const browserErrors = [];
const attachDiagnostics = (page, label) => {
  page.on('pageerror', (error) => browserErrors.push(`${label}: ${error.message}`));
  page.on('console', (message) => {
    if (message.type() === 'error') browserErrors.push(`${label}: ${message.text()}`);
  });
};

async function login(page) {
  const captchaResponse = page.waitForResponse((response) => response.url().includes('/dev-api/auth/code'));
  await page.goto(`${origin}/login`, { waitUntil: 'networkidle' });
  const captcha = await (await captchaResponse).json();
  await page.getByPlaceholder(/用户名|Username/i).fill('admin');
  await page.getByPlaceholder(/密码|Password/i).fill('admin123');
  if (captcha.data?.captchaEnabled) {
    const key = `global:captcha_codes:${captcha.data.uuid}`;
    const code = execFileSync(redis, ['-a', 'gameluck123', '--no-auth-warning', 'GET', key], {
      encoding: 'utf8',
      windowsHide: true
    })
      .trim()
      .replace(/^"|"$/g, '');
    assert.ok(code && code !== '(nil)', `captcha missing: ${key}`);
    await page.getByPlaceholder(/验证码|Verification code/i).fill(code);
  }
  await page.getByRole('button', { name: /登\s*录|Log in/i }).click();
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30_000 });
}

const apiJson = (page, url) =>
  page.evaluate(
    async ({ url, clientId }) => {
      const stored = localStorage.getItem('Admin-Token') || '';
      const token = stored.startsWith('"') ? JSON.parse(stored) : stored;
      const response = await fetch(`/dev-api${url}`, {
        headers: { Authorization: `Bearer ${token}`, clientid: clientId, 'Content-Language': 'en_US' }
      });
      return { status: response.status, body: await response.json() };
    },
    { url, clientId }
  );

const apiCsv = (page, url) =>
  page.evaluate(
    async ({ url, clientId }) => {
      const stored = localStorage.getItem('Admin-Token') || '';
      const token = stored.startsWith('"') ? JSON.parse(stored) : stored;
      const response = await fetch(`/dev-api${url}`, {
        headers: { Authorization: `Bearer ${token}`, clientid: clientId, 'Content-Language': 'en_US' }
      });
      const bytes = [...new Uint8Array(await response.arrayBuffer())];
      return { status: response.status, type: response.headers.get('content-type'), disposition: response.headers.get('content-disposition'), bytes };
    },
    { url, clientId }
  );

const parseCsv = (text) => {
  const rows = [];
  let row = [],
    cell = '',
    quoted = false;
  for (let index = 0; index < text.length; index++) {
    const char = text[index];
    if (quoted && char === '"' && text[index + 1] === '"') {
      cell += '"';
      index++;
    } else if (char === '"') quoted = !quoted;
    else if (!quoted && char === ',') {
      row.push(cell);
      cell = '';
    } else if (!quoted && (char === '\n' || char === '\r')) {
      if (char === '\r' && text[index + 1] === '\n') index++;
      row.push(cell);
      if (row.some((value) => value !== '')) rows.push(row);
      row = [];
      cell = '';
    } else cell += char;
  }
  if (cell || row.length) {
    row.push(cell);
    rows.push(row);
  }
  return rows;
};

const assertNoOverflow = async (page, label) => {
  const metrics = await page.evaluate(() => ({
    document: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    body: document.body.scrollWidth - document.body.clientWidth,
    text: document.body.innerText.trim().length
  }));
  assert.ok(metrics.document <= 1 && metrics.body <= 1, `${label} overflow: ${JSON.stringify(metrics)}`);
  assert.ok(metrics.text > 100, `${label} rendered blank`);
};
const closeNotifications = async (page) => {
  for (const button of await page.locator('.el-notification__closeBtn').all()) await button.click();
};

await mkdir(outputDir, { recursive: true });
const before = checksums();
const expected = expectedRows();
assert.ok(expected.length >= 4, 'expected report fixtures are missing');
assert.ok(expected.some((row) => row.currencyCode === 'EUR') && expected.some((row) => row.currencyCode === 'USD'));
assert.ok(expected.some((row) => row.netSettlement.startsWith('-')));

const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, acceptDownloads: true });
const page = await context.newPage();
attachDiagnostics(page, 'desktop');
await login(page);
const query = `startDate=${startDate}&endDate=${endDate}&pageNum=1&pageSize=2`;
const first = await apiJson(page, `/payment/settlement-report/list?${query}`);
assert.equal(first.status, 200);
assert.equal(first.body.total, expected.length);
assert.equal(first.body.rows.length, 2);
for (const row of first.body.rows) {
  const match = expected.find(
    (item) => item.settlementDate === row.settlementDate && item.providerCode === row.providerCode && item.currencyCode === row.currencyCode
  );
  assert.ok(match, `unexpected report row: ${JSON.stringify(row)}`);
  for (const key of [
    'batchCount',
    'eventCount',
    'paymentCount',
    'refundCount',
    'chargebackCount',
    'grossPayment',
    'refundAmount',
    'chargebackAmount',
    'totalFee',
    'netSettlement'
  ])
    assert.equal(row[key], match[key], `${key} mismatch`);
}
const second = await apiJson(page, `/payment/settlement-report/list?startDate=${startDate}&endDate=${endDate}&pageNum=2&pageSize=2`);
assert.deepEqual(second.body.currencyTotals, first.body.currencyTotals, 'currency totals changed with pagination');
assert.equal(first.body.currencyTotals.length, 2, 'currency totals must remain separate');
assert.ok(!JSON.stringify(first.body).includes('999.000000'), 'cross-tenant settlement leaked');
const empty = await apiJson(
  page,
  `/payment/settlement-report/list?startDate=${startDate}&endDate=${endDate}&providerCode=SIMULATED&currencyCode=EUR&pageNum=1&pageSize=10`
);
assert.deepEqual(empty.body.rows, []);
assert.deepEqual(empty.body.currencyTotals, []);
const simulated = expected.find((row) => row.providerCode === 'SIMULATED');
const detail = await apiJson(page, `/payment/settlement-report/${simulated.settlementDate}/SIMULATED/USD/batches`);
assert.equal(detail.body.code, 200);
assert.ok(detail.body.data.length >= 1);
assert.ok(detail.body.data.every((batch) => batch.providerCode === 'SIMULATED' && batch.currencyCode === 'USD'));

const csv = await apiCsv(page, `/payment/settlement-report/export?startDate=${startDate}&endDate=${endDate}`);
assert.equal(csv.status, 200);
assert.match(csv.type, /text\/csv/i, `unexpected CSV content type; body=${new TextDecoder().decode(new Uint8Array(csv.bytes.slice(0, 80)))}`);
assert.match(csv.disposition, new RegExp(`payment-settlement-report_${startDate}_${endDate}\\.csv`));
assert.deepEqual(csv.bytes.slice(0, 3), [0xef, 0xbb, 0xbf]);
const parsed = parseCsv(new TextDecoder().decode(new Uint8Array(csv.bytes.slice(3))));
assert.equal(parsed[0].length, 17);
assert.equal(parsed.length - 1, expected.length);
assert.ok(
  parsed.some((row) => row[1] === "'=FORMULA"),
  'formula Provider was not protected'
);
const unauthorizedExport = await page.evaluate(
  async ({ startDate, endDate }) => {
    const response = await fetch(`/dev-api/payment/settlement-report/export?startDate=${startDate}&endDate=${endDate}`);
    return { status: response.status, body: await response.text() };
  },
  { startDate, endDate }
);
assert.ok(unauthorizedExport.status === 401 || unauthorizedExport.body.includes('401'), 'unauthorized export was not denied');
for (let index = 0; index < expected.length; index++) {
  const row = parsed[index + 1];
  const source = expected[index];
  assert.deepEqual(row.slice(0, 13), [
    source.settlementDate,
    source.providerCode.startsWith('=') ? `'${source.providerCode}` : source.providerCode,
    source.currencyCode,
    String(source.batchCount),
    String(source.eventCount),
    String(source.paymentCount),
    String(source.refundCount),
    String(source.chargebackCount),
    source.grossPayment,
    source.refundAmount,
    source.chargebackAmount,
    source.totalFee,
    source.netSettlement
  ]);
}

await page.goto(`${origin}/payment/payment-settlement-report`, { waitUntil: 'networkidle' });
await page.getByRole('button', { name: /最近 31 天|Last 31 days/i }).click();
await page
  .getByText(/支付结算报表|Settlement report/i)
  .first()
  .waitFor();
await closeNotifications(page);
await assertNoOverflow(page, 'desktop');
await page.screenshot({ path: desktopEvidence, fullPage: true });

const mobile = await context.newPage();
attachDiagnostics(mobile, 'mobile');
await mobile.setViewportSize({ width: 390, height: 844 });
await mobile.goto(`${origin}/payment/payment-settlement-report`, { waitUntil: 'networkidle' });
await mobile
  .getByText(/支付结算报表|Settlement report/i)
  .first()
  .waitFor();
await closeNotifications(mobile);
await assertNoOverflow(mobile, 'mobile');
await mobile.screenshot({ path: mobileEvidence, fullPage: true });

const after = checksums();
assert.deepEqual(after, before, 'report reads changed source tables');
assert.deepEqual(browserErrors, [], `browser errors: ${browserErrors.join('\n')}`);
await browser.close();
assert.ok((await stat(desktopEvidence)).size > 10_000);
assert.ok((await stat(mobileEvidence)).size > 10_000);

console.log(
  JSON.stringify(
    {
      expectedGroups: expected.length,
      firstPageRows: first.body.rows.length,
      currencies: first.body.currencyTotals.length,
      csvRows: parsed.length - 1,
      sourceChecksums: before,
      desktopEvidence,
      mobileEvidence
    },
    null,
    2
  )
);
