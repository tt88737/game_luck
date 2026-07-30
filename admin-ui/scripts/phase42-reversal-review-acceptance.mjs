import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require('C:\\Users\\Administrator\\.codex\\skills\\webapp-testing\\node_modules\\playwright');

const [scenario, reversalNo, output] = process.argv.slice(2);
if (!scenario || !reversalNo || !output) {
  throw new Error('Usage: node phase42-reversal-review-acceptance.mjs <pending|retry|loss> <reversalNo> <output>');
}

const browser = await chromium.launch({
  headless: true,
  executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
});
const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, deviceScaleFactor: 1 });
const page = await context.newPage();
page.on('console', (message) => console.log(`[browser:${message.type()}] ${message.text()}`));
page.on('pageerror', (error) => console.error(`[browser:error] ${error.message}`));
page.on('response', (response) => {
  if (response.url().includes('purchase-reversal-review')) console.log(`[http:${response.status()}] ${response.url()}`);
});

await page.goto('http://127.0.0.1:5173/login', { waitUntil: 'networkidle' });
const inputs = page.locator('input');
await inputs.nth(1).fill('admin');
await inputs.nth(2).fill('admin123');
await page.getByRole('button', { name: /登\s*录|Log in/i }).click();
await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 });

if (scenario === 'verify-retry' || scenario === 'verify-concurrent') {
  const requestKey = process.env.REQUEST_KEY;
  if (!requestKey) throw new Error('REQUEST_KEY is required for API verification');
  const results = await page.evaluate(async ({ target, key }) => {
    const stored = localStorage.getItem('Admin-Token') || '';
    const token = stored.startsWith('"') ? JSON.parse(stored) : stored;
    const invoke = async (requestKey) => {
      const response = await fetch(`/dev-api/payment/purchase-reversal-review/${target}/retry`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          clientid: 'e5cd7e4891bf95d1d19206ce24a7b32e',
          'Content-Type': 'application/json;charset=UTF-8',
          'Content-Language': 'zh_CN'
        },
        body: JSON.stringify({ requestKey })
      });
      return response.json();
    };
    return target.includes('CONCURRENT')
      ? await Promise.all([invoke(`${key}-a`), invoke(`${key}-b`)])
      : [await invoke(key), await invoke(`${key}-conflict`)];
  }, { target: reversalNo, key: requestKey });
  const successCount = results.filter((result) => result.code === 200).length;
  if (successCount !== 1) {
    throw new Error(`Replay verification failed: ${JSON.stringify(results)}`);
  }
  console.log(`API race verified: codes=${results.map((result) => result.code).join(',')}`);
  await browser.close();
  process.exit(0);
}

await page.goto(`http://127.0.0.1:5173/payment/purchase-reversal-review?reversalNo=${encodeURIComponent(reversalNo)}`, {
  waitUntil: 'domcontentloaded'
});
try {
  await page.getByRole('dialog', { name: '拒付审核详情' }).waitFor({ state: 'visible', timeout: 20000 });
  await page.getByRole('dialog', { name: '拒付审核详情' }).locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 20000 });
} catch (error) {
  console.error((await page.locator('body').innerText()).slice(0, 2000));
  await page.screenshot({ path: output.replace(/\.png$/, '-debug.png'), fullPage: true });
  throw error;
}

if (scenario === 'retry') {
  await page.getByRole('button', { name: '再次全额追偿' }).click();
  await page.locator('.el-message-box').waitFor({ state: 'visible' });
  await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click();
  await page.getByText('全币种追偿成功，案件已结案').waitFor({ timeout: 20000 });
}

if (scenario === 'loss') {
  await page.getByRole('button', { name: '确认损失结案' }).click();
  await page.locator('.el-dialog').getByRole('textbox').fill('Phase 42 运行时验收：确认逐币种损失');
  await page.locator('.el-dialog').getByRole('button', { name: '确认损失并结案' }).click();
  await page.locator('.el-message-box').waitFor({ state: 'visible' });
  await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click();
  await page.getByText('已确认逐币种损失并结案').waitFor({ timeout: 20000 });
}

await page.screenshot({ path: output, fullPage: true });

await page.setViewportSize({ width: 390, height: 844 });
await page.goto(`http://127.0.0.1:5173/payment/purchase-reversal-review?reversalNo=${encodeURIComponent(reversalNo)}`, {
  waitUntil: 'domcontentloaded'
});
await page.getByRole('dialog', { name: '拒付审核详情' }).waitFor({ state: 'visible', timeout: 20000 });
await page.getByRole('dialog', { name: '拒付审核详情' }).locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 20000 });
await page.screenshot({ path: output.replace(/\.png$/, '-mobile.png'), fullPage: true });

const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
if (overflow) {
  throw new Error('Page-level horizontal overflow detected at 390px');
}

await browser.close();
