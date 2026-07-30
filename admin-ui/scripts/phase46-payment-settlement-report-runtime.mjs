import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdir, readdir, stat } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const require = createRequire(import.meta.url);
const { chromium } = require('C:\\Users\\Administrator\\.codex\\skills\\webapp-testing\\node_modules\\playwright');
const adminOrigin = 'http://127.0.0.1:5173';
const clientId = 'e5cd7e4891bf95d1d19206ce24a7b32e';
const mysql = 'C:\\tools\\mysql-8.0.46-winx64\\bin\\mysql.exe';
const dump = 'C:\\tools\\mysql-8.0.46-winx64\\bin\\mysqldump.exe';
const redis = 'C:\\tools\\redis-windows\\8.8.0\\Redis-8.8.0-Windows-x64-msys2\\redis-cli.exe';
const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '..', '..');
const evidenceDir = path.join(repoRoot, 'docs', 'implementation');
const desktopEvidence = path.join(evidenceDir, 'phase46-payment-settlement-report-desktop.png');
const mobileEvidence = path.join(evidenceDir, 'phase46-payment-settlement-report-mobile.png');
const fixtureIds = ['2099000000000004601', '2099000000000004602', '2099000000000004603', '2099000000000004604', '2099000000000004605', '2099000000000004699'];
const reportTables = [
  'gl_payment_settlement_batch', 'gl_payment_settlement_item', 'gl_purchase_order', 'gl_purchase_order_grant_snapshot',
  'gl_purchase_payment_event', 'gl_purchase_reversal', 'gl_purchase_reversal_item', 'gl_payment_session',
  'gl_payment_webhook_event', 'gl_wallet_transaction', 'gl_wallet_turnover_task', 'gl_wallet_account', 'gl_member_profile'
];

const sql = (statement) => execFileSync(mysql, ['-uroot', '-proot', '-N', '-B', '-D', 'gameluck_vue', '-e', statement], { encoding: 'utf8', windowsHide: true }).trim();
const seedFixtures = () => {
  const values = [
    ['2099000000000004601', '000000', 'P46-USD-1', 'SIMULATED', 'USD', '2026-07-28 23:30:00', '2026-07-29 00:15:00', 5, 3, 1, 1, '100.000000', '10.000000', '5.000000', '3.000000', '82.000000'],
    ['2099000000000004602', '000000', 'P46-USD-2', 'SIMULATED', 'USD', '2026-07-28 23:50:00', '2026-07-29 01:00:00', 4, 2, 1, 1, '20.000000', '30.000000', '89.000000', '1.000000', '-100.000000'],
    ['2099000000000004603', '000000', 'P46-EUR-1', 'ALT_PROVIDER', 'EUR', '2026-07-29 12:00:00', '2026-07-29 13:00:00', 2, 2, 0, 0, '7.000000', '0.000000', '0.000000', '0.500000', '6.500000'],
    ['2099000000000004604', '000000', 'P46-USD-3', 'SIMULATED', 'USD', '2026-07-29 14:00:00', '2026-07-29 15:00:00', 1, 1, 0, 0, '4.000000', '0.000000', '0.000000', '0.100000', '3.900000'],
    ['2099000000000004605', '000000', 'P46-FORMULA', '=FORMULA', 'USD', '2026-07-29 16:00:00', '2026-07-29 17:00:00', 1, 1, 0, 0, '1.000000', '0.000000', '0.000000', '0.000000', '1.000000'],
    ['2099000000000004699', '999999', 'P46-OTHER-TENANT', 'SIMULATED', 'USD', '2026-07-29 10:00:00', '2026-07-29 11:00:00', 99, 99, 0, 0, '999.000000', '0.000000', '0.000000', '0.000000', '999.000000']
  ];
  sql(`delete from gl_payment_settlement_batch where id in (${fixtureIds.join(',')})`);
  const rows = values.map((v) => `(${v.slice(0, 7).map((x) => `'${x}'`).join(',')},'CLOSED',0,0,0,${v.slice(7).join(',')},0,0,'{}',1,'phase46',1,'phase46',1,'phase46','runtime fixture','2026-07-29 12:00:00','2026-07-29 12:01:00',1,'2026-07-29 12:00:00','2026-07-29 12:01:00')`).join(',');
  sql(`insert into gl_payment_settlement_batch (id,tenant_id,settlement_no,provider_code,currency_code,period_start,period_end,status,payment_fee_rate,payment_fixed_fee,chargeback_fixed_fee,event_count,payment_count,refund_count,chargeback_count,gross_payment,refund_amount,chargeback_amount,total_fee,net_settlement,reconciliation_coverage_count,open_issue_count,evidence_snapshot_json,creator_id,creator_name,calculator_id,calculator_name,closer_id,closer_name,close_remark,calculated_time,closed_time,version,create_time,update_time) values ${rows}`);
};
const snapshot = () => {
  const hash = createHash('sha256');
  for (const table of reportTables) {
    hash.update(table);
    hash.update(execFileSync(dump, ['-uroot', '-proot', '--skip-comments', '--skip-dump-date', '--compact', '--no-create-info', '--skip-add-locks', '--skip-disable-keys', '--order-by-primary', 'gameluck_vue', table], { encoding: 'utf8', windowsHide: true }));
  }
  return hash.digest('hex');
};
const browser = await chromium.launch({ headless: true, executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe' });
const errors = [];
const attachDiagnostics = (page, label) => {
  page.on('pageerror', (error) => errors.push(`${label}: ${error.message}`));
  page.on('console', (message) => { if (message.type() === 'error') errors.push(`${label}: ${message.text()}`); });
};
async function login(page) {
  const captchaResponse = page.waitForResponse((response) => response.url().includes('/dev-api/auth/code'));
  await page.goto(`${adminOrigin}/login`, { waitUntil: 'networkidle' });
  const captcha = await (await captchaResponse).json();
  const inputs = page.locator('input');
  await inputs.nth(1).fill('admin');
  await inputs.nth(2).fill('admin123');
  if (captcha.data?.captchaEnabled) {
    const key = `global:captcha_codes:${captcha.data.uuid}`;
    const code = execFileSync(redis, ['-a', 'gameluck123', '--no-auth-warning', 'GET', key], { encoding: 'utf8', windowsHide: true }).trim().replace(/^"|"$/g, '');
    assert.ok(code && code !== '(nil)', `Missing captcha ${key}`);
    await inputs.nth(3).fill(code);
  }
  await page.getByRole('button', { name: /Log in|登\s*录/i }).click();
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30_000 });
}
const token = async (page) => page.evaluate(() => { const raw = localStorage.getItem('Admin-Token') || ''; return raw.startsWith('"') ? JSON.parse(raw) : raw; });
async function request(page, url, options = {}) {
  const authorization = await token(page);
  return page.evaluate(async ({ url, options, authorization, clientId }) => {
    const response = await fetch(`/dev-api${url}`, { ...options, headers: { Authorization: `Bearer ${authorization}`, clientid: clientId, 'Content-Language': 'zh_CN', ...(options.headers || {}) } });
    const type = response.headers.get('content-type') || '';
    return type.includes('text/csv') ? { status: response.status, bytes: Array.from(new Uint8Array(await response.arrayBuffer())) } : { status: response.status, body: await response.json() };
  }, { url, options, authorization, clientId });
}
const data = (result, label) => { assert.equal(result.status, 200, label); assert.ok([0, 200].includes(result.body.code), `${label}: ${JSON.stringify(result.body)}`); return result.body.data; };
const countFields = ['batchCount', 'eventCount', 'paymentEventCount', 'refundEventCount', 'chargebackEventCount'];
const moneyFields = ['grossPayment', 'refundAmount', 'chargebackAmount', 'totalFee', 'netSettlement'];
const micros = (value) => { const [whole, fraction = ''] = String(value).split('.'); const sign = whole.startsWith('-') ? -1n : 1n; return sign * (BigInt(whole.replace('-', '')) * 1_000_000n + BigInt(fraction.padEnd(6, '0').slice(0, 6))); };
const decimal = (value) => { const sign = value < 0n ? '-' : ''; const absolute = value < 0n ? -value : value; return `${sign}${absolute / 1_000_000n}.${String(absolute % 1_000_000n).padStart(6, '0')}`; };
const aggregate = (raw) => {
  const groups = new Map();
  for (const row of raw) {
    const date = row.periodStart.slice(0, 10); const key = `${date}|${row.providerCode}|${row.currencyCode}`;
    const target = groups.get(key) || { reportDate: date, providerCode: row.providerCode, currencyCode: row.currencyCode, batchCount: 0, eventCount: 0, paymentEventCount: 0, refundEventCount: 0, chargebackEventCount: 0, grossPayment: 0n, refundAmount: 0n, chargebackAmount: 0n, totalFee: 0n, netSettlement: 0n };
    target.batchCount++; target.eventCount += +row.eventCount; target.paymentEventCount += +row.paymentCount; target.refundEventCount += +row.refundCount; target.chargebackEventCount += +row.chargebackCount;
    for (const field of moneyFields) target[field] += micros(row[field]);
    groups.set(key, target);
  }
  return [...groups.values()].map((row) => ({ ...row, ...Object.fromEntries(moneyFields.map((field) => [field, decimal(row[field])])) }));
};
const assertRow = (actual, expected) => {
  assert.equal(actual.reportDate, expected.reportDate); assert.equal(actual.providerCode, expected.providerCode); assert.equal(actual.currencyCode, expected.currencyCode);
  for (const field of countFields) assert.equal(+actual[field], +expected[field], `${actual.providerCode}.${field}`);
  for (const field of moneyFields) assert.equal(actual[field], expected[field], `${actual.providerCode}.${field}`);
};
const parseCsv = (text) => {
  const rows = []; let row = []; let field = ''; let quoted = false;
  for (let i = 0; i < text.length; i++) { const c = text[i]; if (quoted && c === '"' && text[i + 1] === '"') { field += '"'; i++; } else if (c === '"') quoted = !quoted; else if (c === ',' && !quoted) { row.push(field); field = ''; } else if ((c === '\n' || c === '\r') && !quoted) { if (c === '\r' && text[i + 1] === '\n') i++; row.push(field); if (row.some(Boolean)) rows.push(row); row = []; field = ''; } else field += c; }
  if (field || row.length) { row.push(field); rows.push(row); } return rows;
};
const assertNoOverflow = async (page, label) => { const value = await page.evaluate(() => ({ doc: document.documentElement.scrollWidth - document.documentElement.clientWidth, body: document.body.scrollWidth - document.body.clientWidth, text: document.body.innerText.length })); assert.ok(value.doc <= 1 && value.body <= 1 && value.text > 100, `${label}: ${JSON.stringify(value)}`); };

seedFixtures();
const before = snapshot();
await mkdir(evidenceDir, { recursive: true });
const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
const page = await context.newPage(); attachDiagnostics(page, 'admin');
try {
  await login(page);
  const query = '?startDate=2026-07-28&endDate=2026-07-29&pageNum=1&pageSize=2';
  const first = data(await request(page, `/payment/settlement-report/list${query}`), 'report page 1');
  const second = data(await request(page, '/payment/settlement-report/list?startDate=2026-07-28&endDate=2026-07-29&pageNum=2&pageSize=2'), 'report page 2');
  const all = data(await request(page, '/payment/settlement-report/list?startDate=2026-07-28&endDate=2026-07-29&pageNum=1&pageSize=500'), 'full report');
  const raw = JSON.parse(sql(`select json_arrayagg(json_object('periodStart',date_format(convert_tz(period_start,@@session.time_zone,'+00:00'),'%Y-%m-%dT%H:%i:%sZ'),'providerCode',provider_code,'currencyCode',currency_code,'eventCount',event_count,'paymentCount',payment_count,'refundCount',refund_count,'chargebackCount',chargeback_count,'grossPayment',gross_payment,'refundAmount',refund_amount,'chargebackAmount',chargeback_amount,'totalFee',total_fee,'netSettlement',net_settlement)) from gl_payment_settlement_batch where tenant_id='000000' and status='CLOSED' and period_start>=convert_tz('2026-07-28 00:00:00','+00:00',@@session.time_zone) and period_start<convert_tz('2026-07-30 00:00:00','+00:00',@@session.time_zone)`));
  const expected = aggregate(raw); const expectedByKey = new Map(expected.map((row) => [`${row.reportDate}|${row.providerCode}|${row.currencyCode}`, row])); assert.equal(first.total, expected.length); assert.equal(all.rows.length, expected.length); all.rows.forEach((row) => assertRow(row, expectedByKey.get(`${row.reportDate}|${row.providerCode}|${row.currencyCode}`)));
  assert.deepEqual(second.currencyTotals, first.currencyTotals, 'Currency totals changed with page');
  assert.equal(first.currencyTotals.length, 2); assert.ok(all.rows.some((row) => row.negativeNet));
  const provider = data(await request(page, '/payment/settlement-report/list?startDate=2026-07-28&endDate=2026-07-29&providerCode=SIMULATED&pageNum=1&pageSize=20'), 'provider filter'); assert.ok(provider.rows.every((row) => row.providerCode === 'SIMULATED'));
  const currency = data(await request(page, '/payment/settlement-report/list?startDate=2026-07-28&endDate=2026-07-29&currencyCode=EUR&pageNum=1&pageSize=20'), 'currency filter'); assert.ok(currency.rows.every((row) => row.currencyCode === 'EUR'));
  const empty = data(await request(page, '/payment/settlement-report/list?startDate=2026-07-28&endDate=2026-07-29&currencyCode=GBP&pageNum=1&pageSize=20'), 'empty filter'); assert.deepEqual(empty.rows, []); assert.deepEqual(empty.currencyTotals, []);
  const drill = data(await request(page, '/payment/settlement-report/2026-07-28/SIMULATED/USD/batches'), 'drill down'); assert.deepEqual(drill.map((row) => row.id), ['2099000000000004601', '2099000000000004602']);
  assert.ok(!first.rows.some((row) => +row.grossPayment === 999), 'Other tenant leaked into report');
  const exported = await request(page, '/payment/settlement-report/export?startDate=2026-07-28&endDate=2026-07-29'); assert.equal(exported.status, 200); assert.deepEqual(exported.bytes.slice(0, 3), [239, 187, 191]);
  const csvRows = parseCsv(Buffer.from(exported.bytes.slice(3)).toString('utf8')); assert.equal(csvRows[0].length, 17); assert.equal(csvRows.length, expected.length + 1); assert.ok(csvRows.some((row) => row[1] === "'=FORMULA"), 'Formula protection missing'); all.rows.forEach((row, i) => { assert.equal(csvRows[i + 1][0], row.reportDate); assert.equal(csvRows[i + 1][1], row.providerCode.startsWith('=') ? `'${row.providerCode}` : row.providerCode); assert.equal(csvRows[i + 1][2], row.currencyCode); for (let c = 3; c <= 12; c++) assert.equal(+csvRows[i + 1][c], +Object.values(row)[c], `CSV parity row ${i} col ${c}`); });
  const denied = await page.evaluate(async ({ clientId }) => { const response = await fetch('/dev-api/payment/settlement-report/export?startDate=2026-07-28&endDate=2026-07-29', { headers: { clientid: clientId } }); const text = await response.text(); let code; try { code = JSON.parse(text).code; } catch {} return { status: response.status, code, text }; }, { clientId }); assert.ok(denied.status === 401 || denied.code === 401 || /401|unauthorized|未登录/i.test(denied.text), JSON.stringify(denied));
  const filesBefore = (await readdir(repoRoot, { recursive: true })).filter((name) => /payment-settlement-report.*\.csv$/i.test(name)); assert.deepEqual(filesBefore, [], 'Server-side CSV file was created');
  assert.equal(snapshot(), before, 'Report calls mutated settlement or financial source tables');

  await page.goto(`${adminOrigin}/payment/payment-settlement-report`, { waitUntil: 'domcontentloaded' }); await page.getByText('=FORMULA', { exact: true }).waitFor({ timeout: 20_000 }); await page.getByText(/Negative net|负净额/i).first().waitFor(); await page.locator('tr', { hasText: 'SIMULATED' }).last().getByRole('button').click(); await page.locator('.el-drawer:visible').waitFor(); await page.getByText('P46-USD-1', { exact: true }).waitFor(); await assertNoOverflow(page, 'desktop'); await page.screenshot({ path: desktopEvidence, fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 }); await page.goto(`${adminOrigin}/payment/payment-settlement-report`, { waitUntil: 'domcontentloaded' }); await page.getByText('=FORMULA', { exact: true }).waitFor({ timeout: 20_000 }); await page.locator('tr', { hasText: 'SIMULATED' }).last().getByRole('button').click(); await page.locator('.el-drawer:visible').waitFor(); await page.getByText('P46-USD-1', { exact: true }).waitFor(); await assertNoOverflow(page, 'mobile'); await page.screenshot({ path: mobileEvidence, fullPage: true });
  assert.ok((await stat(desktopEvidence)).size > 10_000); assert.ok((await stat(mobileEvidence)).size > 10_000); assert.deepEqual(errors, []);
  console.log(JSON.stringify({ fixtureIds, expectedGroups: expected.length, currencyTotals: first.currencyTotals, drillBatchIds: drill.map((row) => row.id), sourceSnapshotSha256: before, csvRows: csvRows.length - 1, unauthorized: { httpStatus: denied.status, businessCode: denied.code }, evidence: [desktopEvidence, mobileEvidence] }, null, 2));
} finally { await context.close(); await browser.close(); }
