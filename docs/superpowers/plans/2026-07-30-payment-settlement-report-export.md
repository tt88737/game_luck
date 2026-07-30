# Payment Settlement Report And Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tenant-scoped read-only Admin report over closed payment settlement batches with exact per-currency totals, drill-down, and safe bounded UTF-8 CSV export.

**Architecture:** Add a dedicated settlement-report read model, mapper, service, controller, and CSV writer inside `gameluck-payment`; do not extend the Phase 45 settlement command service. Add a typed Admin API and operational report page that links to existing settlement details. Query and export share one validated filter and one grouping contract so screen and download cannot diverge.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Hutool CSV, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, Vite, Playwright runtime scripts, MySQL.

---

## File Map

**Backend contracts and read model**

- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementReportQueryBo.java`: validated UTC date and dimension filter.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementReportRowVo.java`: one date/Provider/currency aggregate with string money.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementReportCurrencyTotalVo.java`: one currency footer.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementReportPageVo.java`: paged rows, currency totals, and `generatedAt`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementReportMapper.java`: grouped rows, count, totals, and exact-group batch drill-down.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementReportService.java`: list, drill-down, and export contract.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementReportServiceImpl.java`: normalization, UTC bounds, tenant scoping, projection, and export limit.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/report/SettlementReportCsvWriter.java`: fixed columns, UTF-8 BOM, structured escaping, and formula protection.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementReportController.java`: three permission-scoped endpoints.

**Backend tests and metadata**

- Create `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentSettlementReportContractTest.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementReportServiceImplTest.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/report/SettlementReportCsvWriterTest.java`.
- Create `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentSettlementReportControllerContractTest.java`.
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages.properties`.
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`.
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`.
- Modify `backend/script/sql/gameluck_wallet.sql`: idempotent page `2034` and permissions `20341`-`20343`.

**Admin UI and verification**

- Create `admin-ui/src/api/payment/paymentSettlementReport/types.ts`.
- Create `admin-ui/src/api/payment/paymentSettlementReport/index.ts`.
- Create `admin-ui/src/views/payment/payment-settlement-report/index.vue`.
- Create `admin-ui/scripts/check-payment-settlement-report-contract.mjs`.
- Create `admin-ui/scripts/phase46-payment-settlement-report-runtime.mjs`.
- Modify `admin-ui/package.json`.
- Modify `admin-ui/src/lang/en_US.ts`.
- Modify `admin-ui/src/lang/zh_CN.ts`.
- Create runtime evidence `docs/implementation/phase46-payment-settlement-report-desktop.png`.
- Create runtime evidence `docs/implementation/phase46-payment-settlement-report-mobile.png`.
- Modify `progress.md`; do not rewrite mixed-encoding `task_plan.md`.

### Task 1: Lock The Backend Contract And SQL Metadata

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentSettlementReportContractTest.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PaymentSettlementReportQueryBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementReportRowVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementReportCurrencyTotalVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PaymentSettlementReportPageVo.java`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: backend message bundles listed in the file map

- [x] **Step 1: Write the failing persistence/API contract test**

Assert exact fields and types: query `startDate/endDate/providerCode/currencyCode`; row string money and ISO date strings; page `rows/total/currencyTotals/generatedAt`; SQL page ID `2034`, route `payment/payment-settlement-report/index`, menu order after `2033`, and permissions `payment:settlementReport:list|query|export`; all three message bundles contain `payment.settlementReport.date.invalid`, `.date.future`, `.provider.invalid`, `.currency.invalid`, `.export.tooLarge`, and `.group.notFound`.

```java
assertThat(PaymentSettlementReportRowVo.class.getMethod("getNetSettlement").getReturnType())
    .isEqualTo(String.class);
assertThat(walletSql).contains("payment:settlementReport:list", "payment:settlementReport:query",
    "payment:settlementReport:export");
```

- [x] **Step 2: Run RED**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlementReportContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: compilation/test failure because report contracts and metadata do not exist.

- [x] **Step 3: Add minimal contracts and idempotent metadata**

Use `LocalDate` for query dates and strings for all response money/IDs:

```java
@Data
public class PaymentSettlementReportQueryBo {
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private String providerCode;
    private String currencyCode;
}
```

Add SQL delete-before-insert rows for `2034,20341,20342,20343`, preserving page `2033`. Add stable localized messages to all bundles.

- [x] **Step 4: Run GREEN and commit**

Run the Task 1 command; expected `1` class with zero failures/errors/skips.

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/domain/PaymentSettlementReportContractTest.java backend/gameluck-admin/src/main/resources/i18n backend/script/sql/gameluck_wallet.sql
git commit -m "feat: add settlement report contracts"
```

### Task 2: Implement Tenant-Scoped Grouped Queries

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementReportMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementReportService.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementReportServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementReportServiceImplTest.java`

- [x] **Step 1: Write failing service tests**

Cover default normalization, inclusive 31-day UTC bounds, future/reversed rejection, uppercase Provider/currency, tenant forwarding, grouped-page projection, full-filter currency totals, empty results, exact drill-down, and absent groups. Capture mapper arguments and prove `periodEndExclusive = endDate.plusDays(1) at UTC midnight`.

```java
assertThat(capturedStart.toInstant()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
assertThat(capturedEnd.toInstant()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
verify(mapper).selectGroupedRows(any(), eq("000000"), any(), any(), eq("SIMULATED"), eq("USD"));
```

- [x] **Step 2: Run RED**

Run `PaymentSettlementReportServiceImplTest`; expected failure because mapper/service are absent.

- [x] **Step 3: Add grouped mapper and minimal service**

Use annotated MyBatis SQL over `gl_payment_settlement_batch` with these non-negotiable predicates:

```sql
tenant_id = #{tenantId}
and status = 'CLOSED'
and period_start >= #{periodStart}
and period_start < #{periodEndExclusive}
```

Group with `DATE(CONVERT_TZ(period_start, @@session.time_zone, '+00:00'))`, `provider_code`, and `currency_code`; sum every Phase 45 count/money field; order date descending then Provider/currency ascending. Apply MyBatis `Page` to grouped rows. Totals group only by currency and ignore page bounds. Drill-down repeats the exact source membership and returns `PaymentSettlementBatchVo` ordered by `period_start,id`.

- [x] **Step 4: Run GREEN and the Phase 45 service regression**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlementReportServiceImplTest,PaymentSettlementServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: zero failures/errors/skips.

- [x] **Step 5: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementReportMapper.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementReportServiceImplTest.java
git commit -m "feat: query settlement financial reports"
```

### Task 3: Add Safe Bounded CSV Export

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/report/SettlementReportCsvWriter.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/report/SettlementReportCsvWriterTest.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPaymentSettlementReportService.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementReportServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PaymentSettlementReportServiceImplTest.java`

- [x] **Step 1: Write CSV RED tests**

Assert UTF-8 BOM bytes `EF BB BF`, fixed 17-column header, CR/LF and quote escaping, stable row ordering, six-decimal money preservation, and apostrophe prefix for text beginning after whitespace with `=`, `+`, `-`, or `@`. Assert service rejects mapper count `2001` before writing and exports exactly the full non-paged filter when count is `2000`.

```java
assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
assertThat(csv).contains("'=@SUM(A1:A2)", "\"SIM,ULATED\"");
```

- [x] **Step 2: Run RED**

Run `SettlementReportCsvWriterTest,PaymentSettlementReportServiceImplTest`; expected missing writer/export failures.

- [x] **Step 3: Implement the writer with Hutool `CsvWriter`**

Write BOM directly, then pass headers and cells to `cn.hutool.core.text.csv.CsvWriter`. Keep `safeText` isolated:

```java
static String safeText(String value) {
    if (value == null) return "";
    int i = 0;
    while (i < value.length() && Character.isWhitespace(value.charAt(i))) i++;
    return i < value.length() && "=+-@".indexOf(value.charAt(i)) >= 0 ? "'" + value : value;
}
```

The service first calls `countGroupedRows`; reject above `2000`, then call one non-paged ordered export query and write once. Do not create temp files or DB rows.

- [x] **Step 4: Run GREEN and commit**

Expected: both test classes pass with zero failures/errors/skips.

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service
git commit -m "feat: export safe settlement report csv"
```

### Task 4: Expose Permission-Scoped Admin Endpoints

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementReportController.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentSettlementReportControllerContractTest.java`

- [x] **Step 1: Write controller RED contract**

Require `/payment/settlement-report`, `GET /list`, `GET /{date}/{providerCode}/{currencyCode}/batches`, and `GET /export`; exact permissions; `@Log(businessType = BusinessType.EXPORT, isSaveRequestData = false, isSaveResponseData = false)` on export; `text/csv` response; and no command-service dependency.

- [x] **Step 2: Run RED**

Run `PaymentSettlementReportControllerContractTest`; expected missing controller failure.

- [x] **Step 3: Add minimal controller**

```java
@SaCheckPermission("payment:settlementReport:export")
@Log(title = "Payment settlement report export", businessType = BusinessType.EXPORT,
    isSaveRequestData = false, isSaveResponseData = false)
@GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
public void export(@Validated PaymentSettlementReportQueryBo query, HttpServletResponse response) { ... }
```

Set RFC 5987-safe `Content-Disposition` to the deterministic filename and write only service bytes to the response stream.

- [x] **Step 4: Run GREEN plus Tasks 1-3 tests and commit**

Expected: all Phase 46 backend test classes pass.

```powershell
git add backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PaymentSettlementReportController.java backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/controller/PaymentSettlementReportControllerContractTest.java
git commit -m "feat: expose settlement report operations"
```

### Task 5: Add Typed Admin API And Contract Guard

**Files:**
- Create: `admin-ui/src/api/payment/paymentSettlementReport/types.ts`
- Create: `admin-ui/src/api/payment/paymentSettlementReport/index.ts`
- Create: `admin-ui/scripts/check-payment-settlement-report-contract.mjs`
- Modify: `admin-ui/package.json`

- [x] **Step 1: Write the failing Node contract**

Assert string money/IDs, `SettlementReportQuery`, row, currency total, page, and batch drill-down types; exact three endpoints; blob export; the three permission literals; and no `Number()` conversion of money.

- [x] **Step 2: Run RED**

```powershell
node admin-ui/scripts/check-payment-settlement-report-contract.mjs
```

Expected: failure because API/view files are absent.

- [x] **Step 3: Add types and API**

```ts
export interface SettlementReportQuery extends PageQuery {
  startDate: string;
  endDate: string;
  providerCode?: string;
  currencyCode?: string;
}
export const exportSettlementReport = (params: Omit<SettlementReportQuery, 'pageNum' | 'pageSize'>) =>
  request<Blob>({ url: `${base}/export`, method: 'get', params, responseType: 'blob' });
```

Add `check:payment-settlement-report` to `package.json`.

- [x] **Step 4: Run GREEN and TypeScript check**

```powershell
pnpm --dir admin-ui check:payment-settlement-report
pnpm --dir admin-ui exec vue-tsc --noEmit
```

Expected: both exit `0`.

- [x] **Step 5: Commit**

```powershell
git add admin-ui/src/api/payment/paymentSettlementReport admin-ui/scripts/check-payment-settlement-report-contract.mjs admin-ui/package.json
git commit -m "feat: add settlement report admin api"
```

### Task 6: Build The Admin Settlement Report Workbench

**Files:**
- Create: `admin-ui/src/views/payment/payment-settlement-report/index.vue`
- Modify: `admin-ui/src/lang/en_US.ts`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/scripts/check-payment-settlement-report-contract.mjs`

- [x] **Step 1: Extend the RED frontend contract**

Require default latest-seven-day UTC range, latest 7/31 segmented control, date validation, loading/empty/error/permission/export states, currency summary band, grouped table, explicit negative-net text, nested table scrolling, drill-down drawer, Phase 45 detail navigation, `file-saver` blob download, and all three permission directives.

- [x] **Step 2: Run RED**

Run `pnpm --dir admin-ui check:payment-settlement-report`; expected missing view/i18n assertions.

- [x] **Step 3: Implement the operational page**

Use one unframed page with a filter band, peer currency summaries, dense table, and drawer. Preserve money as strings; use a decimal sign helper that checks `/^-/.test(value)` rather than `Number(value)`.

```ts
const isNegative = (value: string) => /^-/.test(value.trim());
const canList = computed(() => permissions.value.includes('*:*:*') || permissions.value.includes('payment:settlementReport:list'));
```

Export strips pagination, locks the button, saves the server blob with the deterministic filename, and restores state in `finally`. Drill-down links to `/payment/payment-settlement?batchId=<id>`. Extend the Phase 45 view to read the string `batchId` query on mount, load that tenant-visible batch through its existing detail API, open the drawer only after a successful response, and leave the list usable when the ID is absent or rejected. Cover this behavior in both frontend contracts.

- [x] **Step 4: Run focused frontend checks**

```powershell
pnpm --dir admin-ui check:payment-settlement-report
pnpm --dir admin-ui check:payment-settlement
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui exec eslint src/views/payment/payment-settlement-report/index.vue src/api/payment/paymentSettlementReport/index.ts src/api/payment/paymentSettlementReport/types.ts
```

Expected: all commands exit `0`.

- [x] **Step 5: Commit**

```powershell
git add admin-ui/src/views/payment/payment-settlement-report admin-ui/src/lang admin-ui/scripts/check-payment-settlement-report-contract.mjs admin-ui/src/views/payment/payment-settlement/index.vue admin-ui/scripts/check-payment-settlement-contract.mjs
git commit -m "feat: add settlement report workbench"
```

### Task 7: Run Regression And Production-Equivalent Builds

**Files:**
- Modify only when a failing regression proves an in-scope defect.
- Modify: `progress.md`

- [ ] **Step 1: Run focused payment regression**

```powershell
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -Xmx768m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=PaymentSettlementReport*,PaymentSettlement*,PaymentReconciliation*,PaymentWebhook*,PurchasePaymentEvent*,PurchaseReversal*' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: zero failures, errors, and skips.

- [ ] **Step 2: Run split cross-module regression in fresh JVMs**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-wallet -am '-DskipTests=false' '-Dtest=WalletCoreServiceImplTest,WalletTurnoverTaskServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-member -am '-DskipTests=false' '-Dtest=MemberProfileServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -pl gameluck-modules/gameluck-payment -am '-DskipTests=false' '-Dtest=ClientPurchaseServiceTest,PurchaseReversalContractTest,PurchaseReversalReviewContractTest,PurchaseReversalReviewControllerContractTest,PurchaseOrderServiceImplTest,PurchasePaymentEventServiceImplTest,PurchaseReversalServiceImplTest,PurchaseReversalReviewServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test
```

Expected: wallet `19/19`, member `7/7`, and payment fulfillment/reversal `45/45`, with zero failures/errors/skips and no shared-JVM native-memory exhaustion.

- [ ] **Step 3: Build all deliverables with bounded memory**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -Plocal -DskipTests package
$env:NODE_OPTIONS='--max-old-space-size=2048'
$env:ROLLUP_MAX_PARALLEL_FILE_OPS='1'
pnpm --dir admin-ui build:dev
pnpm --dir h5 build
```

Expected: backend `BUILD SUCCESS`, Admin and H5 exit `0`; only the established large-chunk advisory is acceptable.

- [ ] **Step 4: Run safety and consistency scans**

```powershell
rg -n "IWallet|Wallet.*Mapper|Payment.*Command|rawBody|signature|update |insert |delete " backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PaymentSettlementReportServiceImpl.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PaymentSettlementReportMapper.java backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/report/SettlementReportCsvWriter.java
git diff --check
```

Expected: no wallet/command/sensitive-data dependency and no report mutation SQL; diff check exits `0` apart from existing line-ending warnings.

- [ ] **Step 5: Record exact evidence**

Append test counts, build module counts, warnings, and any bounded-memory retry to `progress.md`.

### Task 8: Verify SQL, Runtime, CSV, And Responsive UI

**Files:**
- Create: `admin-ui/scripts/phase46-payment-settlement-report-runtime.mjs`
- Create: `docs/implementation/phase46-payment-settlement-report-desktop.png`
- Create: `docs/implementation/phase46-payment-settlement-report-mobile.png`
- Modify: `progress.md`

- [ ] **Step 1: Import SQL twice and verify exact metadata**

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

Query `sys_menu`. Expected: exactly one page `2034`, exactly three permissions `20341`-`20343`, no duplicates, and Phase 45 page/permissions unchanged.

- [ ] **Step 2: Restart refreshed services**

Stop only verified project listeners on `8080/5173/5174`. Start the packaged backend with `--spring.profiles.active=local` and the established constrained JVM settings, then Admin and H5. Expected: all three return HTTP `200`.

- [ ] **Step 3: Establish deterministic closed-batch fixtures and source snapshots**

Create or reuse closed Phase 45 batches covering at least two UTC dates, two currencies, a negative net, and multiple Providers where available. Snapshot settlement batches and all payment/reversal/member-risk/turnover/wallet source tables before report calls.

- [ ] **Step 4: Verify report math, filters, drill-down, and tenant isolation**

Call the real authenticated endpoints. Recalculate every expected integer and six-decimal total directly from closed batches; confirm crossed-midnight membership follows UTC `period_start`, currency footers never combine currencies, paging does not change footers, empty filters return empty arrays, and another tenant sees no rows/group.

- [ ] **Step 5: Verify CSV parity and safety**

Download the real export, assert BOM/header/order/17 columns, compare every screen row to CSV, and seed a safely isolated Provider-code fixture beginning with `=` to prove the exported text is apostrophe-prefixed. Confirm a user without export permission receives authorization denial and no server-side file appears.

- [ ] **Step 6: Verify read-only source state**

Repeat source-table dumps after list, drill-down, and export. Expected: hashes are byte-identical; only the Admin operation log may gain the sanitized export entry.

- [ ] **Step 7: Capture desktop and mobile evidence**

Use the encrypted captcha login at `1440x900` and `390x844`. Verify summaries, filters, negative-net label, table scrolling, drill-down, export state, no console errors, no page-level overflow, and nonblank pixels. Save the two named PNG files.

- [ ] **Step 8: Final verification and recovery record**

Re-run Phase 46 backend tests, both frontend contracts, i18n, targeted ESLint, safety scan, and `git diff --check`. Mark every checkbox in this plan, append exact evidence and `Phase 46 completed` to `progress.md`, and leave mixed-encoding `task_plan.md` untouched with the reason recorded.

- [ ] **Step 9: Commit runtime evidence and completion record**

```powershell
git add admin-ui/scripts/phase46-payment-settlement-report-runtime.mjs docs/implementation/phase46-payment-settlement-report-desktop.png docs/implementation/phase46-payment-settlement-report-mobile.png progress.md docs/superpowers/plans/2026-07-30-payment-settlement-report-export.md
git commit -m "test: verify settlement report runtime"
```
