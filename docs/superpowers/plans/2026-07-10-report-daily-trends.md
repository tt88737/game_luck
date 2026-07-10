# Report Daily Trends Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a B-side `Report Center / Trends` page and API for recent 7-day and 30-day daily operating trends.

**Architecture:** Extend the existing read-only `gameluck-report` module with trend VOs, mapper SQL, service date-fill logic, and a permission-protected controller. Add a RuoYi admin page under the existing report menu that renders period totals and a dense daily trend table without adding chart dependencies.

**Tech Stack:** Java 17, Spring Boot, MyBatis, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, Vite.

---

## File Structure

Create or modify:

```text
backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/domain/bo/ReportDailyTrendQueryBo.java
backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/domain/vo/ReportDailyTrendVo.java
backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/mapper/ReportTrendMapper.java
backend/gameluck-modules/gameluck-report/src/main/resources/mapper/report/ReportTrendMapper.xml
backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/service/IReportTrendService.java
backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/service/impl/ReportTrendServiceImpl.java
backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/controller/ReportTrendController.java
backend/gameluck-modules/gameluck-report/src/test/java/com/gameluck/report/service/impl/ReportTrendServiceImplTest.java
backend/script/sql/gameluck_wallet.sql
admin-ui/src/api/report/trends/types.ts
admin-ui/src/api/report/trends/index.ts
admin-ui/src/views/report/trends/index.vue
admin-ui/src/lang/zh_CN.ts
admin-ui/src/lang/en_US.ts
admin-ui/src/utils/i18nTitle.ts
docs/superpowers/plans/2026-07-10-report-daily-trends.md
progress.md
task_plan.md
```

Do not modify the existing `Report Overview` API or page behavior.

### Task 1: Backend Red Test

**Files:**
- Create: `backend/gameluck-modules/gameluck-report/src/test/java/com/gameluck/report/service/impl/ReportTrendServiceImplTest.java`

- [ ] **Step 1: Write focused failing service tests**

Create `ReportTrendServiceImplTest.java`:

```java
package com.gameluck.report.service.impl;

import com.gameluck.report.domain.vo.ReportDailyTrendVo;
import com.gameluck.report.mapper.ReportTrendMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportTrendServiceImplTest {

    @Test
    @Tag("local")
    void dailyTrendsDefaultRangeReturnsSevenContinuousRowsInDescendingOrder() {
        ReportTrendMapper mapper = mock(ReportTrendMapper.class);
        ReportTrendServiceImpl service = new ReportTrendServiceImpl(mapper);
        LocalDate today = LocalDate.now();
        ReportDailyTrendVo aggregate = row(today.minusDays(1));
        aggregate.setMemberCount(2L);
        aggregate.setSuccessfulDepositAmount(new BigDecimal("30.000000"));
        when(mapper.selectDailyMembers("000000", today.minusDays(6), today)).thenReturn(List.of(aggregate));
        when(mapper.selectDailyDeposits("000000", today.minusDays(6), today)).thenReturn(List.of(aggregate));
        when(mapper.selectDailyGames("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyPromotions("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyRedemptions("000000", today.minusDays(6), today)).thenReturn(List.of());

        List<ReportDailyTrendVo> result = service.dailyTrends(null);

        assertEquals(7, result.size());
        assertEquals(today, result.get(0).getReportDate());
        assertEquals(today.minusDays(6), result.get(6).getReportDate());
        assertEquals(0L, result.get(0).getMemberCount());
        assertEquals(2L, result.get(1).getMemberCount());
        assertEquals(new BigDecimal("30.000000"), result.get(1).getSuccessfulDepositAmount());
        verify(mapper).selectDailyMembers("000000", today.minusDays(6), today);
    }

    @Test
    @Tag("local")
    void dailyTrendsSupportsThirtyDayRangeAndNormalizesNullValues() {
        ReportTrendMapper mapper = mock(ReportTrendMapper.class);
        ReportTrendServiceImpl service = new ReportTrendServiceImpl(mapper);
        LocalDate today = LocalDate.now();
        ReportDailyTrendVo aggregate = row(today);
        aggregate.setGameOrderCount(null);
        aggregate.setTotalBetAmount(null);
        when(mapper.selectDailyMembers("000000", today.minusDays(29), today)).thenReturn(List.of());
        when(mapper.selectDailyDeposits("000000", today.minusDays(29), today)).thenReturn(List.of());
        when(mapper.selectDailyGames("000000", today.minusDays(29), today)).thenReturn(List.of(aggregate));
        when(mapper.selectDailyPromotions("000000", today.minusDays(29), today)).thenReturn(List.of());
        when(mapper.selectDailyRedemptions("000000", today.minusDays(29), today)).thenReturn(List.of());

        List<ReportDailyTrendVo> result = service.dailyTrends(30);

        assertEquals(30, result.size());
        assertEquals(0L, result.get(0).getGameOrderCount());
        assertEquals(BigDecimal.ZERO, result.get(0).getTotalBetAmount());
    }

    @Test
    @Tag("local")
    void unsupportedRangeFallsBackToSevenDays() {
        ReportTrendMapper mapper = mock(ReportTrendMapper.class);
        ReportTrendServiceImpl service = new ReportTrendServiceImpl(mapper);
        LocalDate today = LocalDate.now();
        when(mapper.selectDailyMembers("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyDeposits("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyGames("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyPromotions("000000", today.minusDays(6), today)).thenReturn(List.of());
        when(mapper.selectDailyRedemptions("000000", today.minusDays(6), today)).thenReturn(List.of());

        List<ReportDailyTrendVo> result = service.dailyTrends(99);

        assertEquals(7, result.size());
        verify(mapper).selectDailyRedemptions("000000", today.minusDays(6), today);
    }

    private ReportDailyTrendVo row(LocalDate reportDate) {
        ReportDailyTrendVo vo = new ReportDailyTrendVo();
        vo.setReportDate(reportDate);
        return vo;
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-report -am -Plocal -DskipTests=false "-Dtest=ReportTrendServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because `ReportTrendServiceImpl`, `ReportTrendMapper`, and `ReportDailyTrendVo` do not exist yet.

- [ ] **Step 3: Commit the red test**

```powershell
git add backend/gameluck-modules/gameluck-report/src/test/java/com/gameluck/report/service/impl/ReportTrendServiceImplTest.java
git commit -m "test(report): cover daily trend service"
```

Expected: commit succeeds with only the new test file.

### Task 2: Backend Trend API

**Files:**
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/domain/bo/ReportDailyTrendQueryBo.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/domain/vo/ReportDailyTrendVo.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/mapper/ReportTrendMapper.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/resources/mapper/report/ReportTrendMapper.xml`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/service/IReportTrendService.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/service/impl/ReportTrendServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/controller/ReportTrendController.java`

- [ ] **Step 1: Add query BO and daily trend VO**

Create `ReportDailyTrendQueryBo.java`:

```java
package com.gameluck.report.domain.bo;

import lombok.Data;

@Data
public class ReportDailyTrendQueryBo {
    private Integer range;
}
```

Create `ReportDailyTrendVo.java`:

```java
package com.gameluck.report.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReportDailyTrendVo {
    private LocalDate reportDate;
    private Long memberCount;
    private Long depositOrderCount;
    private BigDecimal successfulDepositAmount;
    private Long gameOrderCount;
    private BigDecimal totalBetAmount;
    private BigDecimal totalPayoutAmount;
    private BigDecimal netGameAmount;
    private Long promotionClaimCount;
    private BigDecimal successfulRewardAmount;
    private Long redemptionOrderCount;
    private Long pendingRedemptionCount;
    private BigDecimal approvedRedemptionAmount;
}
```

- [ ] **Step 2: Add mapper interface**

Create `ReportTrendMapper.java`:

```java
package com.gameluck.report.mapper;

import com.gameluck.report.domain.vo.ReportDailyTrendVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportTrendMapper {
    List<ReportDailyTrendVo> selectDailyMembers(@Param("tenantId") String tenantId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    List<ReportDailyTrendVo> selectDailyDeposits(@Param("tenantId") String tenantId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    List<ReportDailyTrendVo> selectDailyGames(@Param("tenantId") String tenantId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    List<ReportDailyTrendVo> selectDailyPromotions(@Param("tenantId") String tenantId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    List<ReportDailyTrendVo> selectDailyRedemptions(@Param("tenantId") String tenantId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);
}
```

- [ ] **Step 3: Add mapper SQL**

Create `ReportTrendMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.gameluck.report.mapper.ReportTrendMapper">

    <select id="selectDailyMembers" resultType="com.gameluck.report.domain.vo.ReportDailyTrendVo">
        SELECT DATE(create_time) AS report_date,
               COUNT(1) AS member_count
        FROM gl_member_profile
        WHERE tenant_id = #{tenantId}
          AND del_flag = '0'
          AND DATE(create_time) BETWEEN #{startDate} AND #{endDate}
        GROUP BY DATE(create_time)
    </select>

    <select id="selectDailyDeposits" resultType="com.gameluck.report.domain.vo.ReportDailyTrendVo">
        SELECT DATE(create_time) AS report_date,
               COUNT(1) AS deposit_order_count,
               COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END), 0) AS successful_deposit_amount
        FROM gl_payment_deposit_order
        WHERE tenant_id = #{tenantId}
          AND del_flag = '0'
          AND DATE(create_time) BETWEEN #{startDate} AND #{endDate}
        GROUP BY DATE(create_time)
    </select>

    <select id="selectDailyGames" resultType="com.gameluck.report.domain.vo.ReportDailyTrendVo">
        SELECT DATE(create_time) AS report_date,
               COUNT(1) AS game_order_count,
               COALESCE(SUM(bet_amount), 0) AS total_bet_amount,
               COALESCE(SUM(payout_amount), 0) AS total_payout_amount,
               COALESCE(SUM(net_amount), 0) AS net_game_amount
        FROM gl_game_bet_order
        WHERE tenant_id = #{tenantId}
          AND del_flag = '0'
          AND DATE(create_time) BETWEEN #{startDate} AND #{endDate}
        GROUP BY DATE(create_time)
    </select>

    <select id="selectDailyPromotions" resultType="com.gameluck.report.domain.vo.ReportDailyTrendVo">
        SELECT DATE(create_time) AS report_date,
               COUNT(1) AS promotion_claim_count,
               COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN reward_amount ELSE 0 END), 0) AS successful_reward_amount
        FROM gl_promotion_claim
        WHERE tenant_id = #{tenantId}
          AND del_flag = '0'
          AND DATE(create_time) BETWEEN #{startDate} AND #{endDate}
        GROUP BY DATE(create_time)
    </select>

    <select id="selectDailyRedemptions" resultType="com.gameluck.report.domain.vo.ReportDailyTrendVo">
        SELECT DATE(create_time) AS report_date,
               COUNT(1) AS redemption_order_count,
               SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending_redemption_count,
               COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN amount ELSE 0 END), 0) AS approved_redemption_amount
        FROM gl_redemption_order
        WHERE tenant_id = #{tenantId}
          AND del_flag = '0'
          AND DATE(create_time) BETWEEN #{startDate} AND #{endDate}
        GROUP BY DATE(create_time)
    </select>

</mapper>
```

- [ ] **Step 4: Add service contract and implementation**

Create `IReportTrendService.java`:

```java
package com.gameluck.report.service;

import com.gameluck.report.domain.vo.ReportDailyTrendVo;

import java.util.List;

public interface IReportTrendService {
    List<ReportDailyTrendVo> dailyTrends(Integer range);
}
```

Create `ReportTrendServiceImpl.java`:

```java
package com.gameluck.report.service.impl;

import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.report.domain.vo.ReportDailyTrendVo;
import com.gameluck.report.mapper.ReportTrendMapper;
import com.gameluck.report.service.IReportTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ReportTrendServiceImpl implements IReportTrendService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final int DEFAULT_RANGE = 7;
    private static final int MAX_RANGE = 30;

    private final ReportTrendMapper reportTrendMapper;

    @Override
    public List<ReportDailyTrendVo> dailyTrends(Integer range) {
        int days = normalizeRange(range);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        Map<LocalDate, ReportDailyTrendVo> rows = zeroRows(startDate, endDate);
        String tenantId = currentTenantId();
        merge(rows, reportTrendMapper.selectDailyMembers(tenantId, startDate, endDate));
        merge(rows, reportTrendMapper.selectDailyDeposits(tenantId, startDate, endDate));
        merge(rows, reportTrendMapper.selectDailyGames(tenantId, startDate, endDate));
        merge(rows, reportTrendMapper.selectDailyPromotions(tenantId, startDate, endDate));
        merge(rows, reportTrendMapper.selectDailyRedemptions(tenantId, startDate, endDate));
        return rows.values().stream()
            .sorted((left, right) -> right.getReportDate().compareTo(left.getReportDate()))
            .peek(this::normalize)
            .toList();
    }

    private int normalizeRange(Integer range) {
        return range != null && range == MAX_RANGE ? MAX_RANGE : DEFAULT_RANGE;
    }

    private Map<LocalDate, ReportDailyTrendVo> zeroRows(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, ReportDailyTrendVo> rows = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            ReportDailyTrendVo row = new ReportDailyTrendVo();
            row.setReportDate(date);
            normalize(row);
            rows.put(date, row);
        }
        return rows;
    }

    private void merge(Map<LocalDate, ReportDailyTrendVo> target, List<ReportDailyTrendVo> source) {
        for (ReportDailyTrendVo incoming : source) {
            if (incoming == null || incoming.getReportDate() == null || !target.containsKey(incoming.getReportDate())) {
                continue;
            }
            ReportDailyTrendVo row = target.get(incoming.getReportDate());
            if (incoming.getMemberCount() != null) row.setMemberCount(incoming.getMemberCount());
            if (incoming.getDepositOrderCount() != null) row.setDepositOrderCount(incoming.getDepositOrderCount());
            if (incoming.getSuccessfulDepositAmount() != null) row.setSuccessfulDepositAmount(incoming.getSuccessfulDepositAmount());
            if (incoming.getGameOrderCount() != null) row.setGameOrderCount(incoming.getGameOrderCount());
            if (incoming.getTotalBetAmount() != null) row.setTotalBetAmount(incoming.getTotalBetAmount());
            if (incoming.getTotalPayoutAmount() != null) row.setTotalPayoutAmount(incoming.getTotalPayoutAmount());
            if (incoming.getNetGameAmount() != null) row.setNetGameAmount(incoming.getNetGameAmount());
            if (incoming.getPromotionClaimCount() != null) row.setPromotionClaimCount(incoming.getPromotionClaimCount());
            if (incoming.getSuccessfulRewardAmount() != null) row.setSuccessfulRewardAmount(incoming.getSuccessfulRewardAmount());
            if (incoming.getRedemptionOrderCount() != null) row.setRedemptionOrderCount(incoming.getRedemptionOrderCount());
            if (incoming.getPendingRedemptionCount() != null) row.setPendingRedemptionCount(incoming.getPendingRedemptionCount());
            if (incoming.getApprovedRedemptionAmount() != null) row.setApprovedRedemptionAmount(incoming.getApprovedRedemptionAmount());
        }
    }

    private void normalize(ReportDailyTrendVo row) {
        row.setMemberCount(defaultLong(row.getMemberCount()));
        row.setDepositOrderCount(defaultLong(row.getDepositOrderCount()));
        row.setSuccessfulDepositAmount(defaultDecimal(row.getSuccessfulDepositAmount()));
        row.setGameOrderCount(defaultLong(row.getGameOrderCount()));
        row.setTotalBetAmount(defaultDecimal(row.getTotalBetAmount()));
        row.setTotalPayoutAmount(defaultDecimal(row.getTotalPayoutAmount()));
        row.setNetGameAmount(defaultDecimal(row.getNetGameAmount()));
        row.setPromotionClaimCount(defaultLong(row.getPromotionClaimCount()));
        row.setSuccessfulRewardAmount(defaultDecimal(row.getSuccessfulRewardAmount()));
        row.setRedemptionOrderCount(defaultLong(row.getRedemptionOrderCount()));
        row.setPendingRedemptionCount(defaultLong(row.getPendingRedemptionCount()));
        row.setApprovedRedemptionAmount(defaultDecimal(row.getApprovedRedemptionAmount()));
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }
}
```

- [ ] **Step 5: Add controller**

Create `ReportTrendController.java`:

```java
package com.gameluck.report.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.report.domain.bo.ReportDailyTrendQueryBo;
import com.gameluck.report.domain.vo.ReportDailyTrendVo;
import com.gameluck.report.service.IReportTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/trends")
public class ReportTrendController extends BaseController {

    private final IReportTrendService reportTrendService;

    @SaCheckPermission("report:trends:query")
    @GetMapping("/daily")
    public R<List<ReportDailyTrendVo>> daily(@ModelAttribute ReportDailyTrendQueryBo bo) {
        return R.ok(reportTrendService.dailyTrends(bo.getRange()));
    }
}
```

- [ ] **Step 6: Run backend tests and compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-report -am -Plocal -DskipTests=false "-Dtest=ReportTrendServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: both commands pass.

- [ ] **Step 7: Commit backend trend API**

```powershell
git add backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/domain/bo/ReportDailyTrendQueryBo.java backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/domain/vo/ReportDailyTrendVo.java backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/mapper/ReportTrendMapper.java backend/gameluck-modules/gameluck-report/src/main/resources/mapper/report/ReportTrendMapper.xml backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/service/IReportTrendService.java backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/service/impl/ReportTrendServiceImpl.java backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/controller/ReportTrendController.java
git commit -m "feat(report): add daily trends api"
```

Expected: commit succeeds.

### Task 3: Menu And Route I18n

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`
- Modify: `admin-ui/src/utils/i18nTitle.ts`

- [ ] **Step 1: Add report trends menu SQL**

In `backend/script/sql/gameluck_wallet.sql`, extend the existing `INSERT INTO sys_menu ... VALUES` block around menu ids `2000`, `2001`, and `2011` so it contains:

```sql
(2000, 'Report Center', 0, 12, 'report', NULL, '', 1, 0, 'M', '0', '0', '', 'chart', 103, 1, NOW(), NULL, NULL, 'Report center directory'),
(2001, 'Overview', 2000, 1, 'overview', 'report/overview/index', '', 1, 0, 'C', '0', '0', 'report:overview:list', 'chart', 103, 1, NOW(), NULL, NULL, 'Report overview menu'),
(2002, 'Trends', 2000, 2, 'trends', 'report/trends/index', '', 1, 0, 'C', '0', '0', 'report:trends:list', 'chart', 103, 1, NOW(), NULL, NULL, 'Report trends menu'),
(2011, 'Report Overview Query', 2001, 1, '#', '', '', 1, 0, 'F', '0', '0', 'report:overview:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(2021, 'Report Trends Query', 2002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'report:trends:query', '#', 103, 1, NOW(), NULL, NULL, '')
```

Keep the existing `ON DUPLICATE KEY UPDATE` block unchanged.

- [ ] **Step 2: Add route i18n keys**

In `admin-ui/src/lang/en_US.ts`, inside `route`, add:

```ts
reportTrends: 'Trends',
reportTrendsQuery: 'Report Trends Query',
```

In `admin-ui/src/lang/zh_CN.ts`, inside `route`, add:

```ts
reportTrends: '趋势看板',
reportTrendsQuery: '趋势看板查询',
```

- [ ] **Step 3: Add title translation mapping**

In `admin-ui/src/utils/i18nTitle.ts`, add:

```ts
'Trends': 'route.reportTrends',
趋势看板: 'route.reportTrends',
'Report Trends Query': 'route.reportTrendsQuery',
趋势看板查询: 'route.reportTrendsQuery',
```

- [ ] **Step 4: Verify menu icons**

Run:

```powershell
pnpm --dir admin-ui check:menu-icons
```

Expected: command passes and accepts `chart` for page menu plus `#` for function menu.

- [ ] **Step 5: Commit menu and route i18n**

```powershell
git add backend/script/sql/gameluck_wallet.sql admin-ui/src/lang/zh_CN.ts admin-ui/src/lang/en_US.ts admin-ui/src/utils/i18nTitle.ts
git commit -m "feat(report): add trends menu"
```

Expected: commit succeeds.

### Task 4: Admin UI Trends Page

**Files:**
- Create: `admin-ui/src/api/report/trends/types.ts`
- Create: `admin-ui/src/api/report/trends/index.ts`
- Create: `admin-ui/src/views/report/trends/index.vue`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`

- [ ] **Step 1: Add API types**

Create `admin-ui/src/api/report/trends/types.ts`:

```ts
export interface ReportDailyTrendVO {
  reportDate: string;
  memberCount: number;
  depositOrderCount: number;
  successfulDepositAmount: string | number;
  gameOrderCount: number;
  totalBetAmount: string | number;
  totalPayoutAmount: string | number;
  netGameAmount: string | number;
  promotionClaimCount: number;
  successfulRewardAmount: string | number;
  redemptionOrderCount: number;
  pendingRedemptionCount: number;
  approvedRedemptionAmount: string | number;
}
```

- [ ] **Step 2: Add API wrapper**

Create `admin-ui/src/api/report/trends/index.ts`:

```ts
import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ReportDailyTrendVO } from './types';

export function listReportDailyTrends(range: number): AxiosPromise<ReportDailyTrendVO[]> {
  return request({
    url: '/report/trends/daily',
    method: 'get',
    params: { range }
  });
}
```

- [ ] **Step 3: Add frontend i18n keys**

In `admin-ui/src/lang/en_US.ts`, add root-level `reportTrends` block after the existing `reportOverview` block:

```ts
reportTrends: {
  title: 'Daily Trends',
  subtitle: 'Recent all-business operating trends by date.',
  refresh: 'Refresh',
  empty: 'No trend data returned',
  range: {
    seven: 'Last 7 days',
    thirty: 'Last 30 days'
  },
  cards: {
    depositAmount: 'Deposit Amount',
    gameNet: 'Game Net',
    rewards: 'Rewards',
    approvedRedeem: 'Approved Redeem',
    pendingRedeem: 'Pending Redeem'
  },
  columns: {
    date: 'Date',
    members: 'New Members',
    depositOrders: 'Deposit Orders',
    depositAmount: 'Deposit Amount',
    gameOrders: 'Game Orders',
    betAmount: 'Bet Amount',
    payoutAmount: 'Payout Amount',
    gameNet: 'Game Net',
    promotionClaims: 'Claims',
    rewardAmount: 'Reward Amount',
    redemptionOrders: 'Redeem Orders',
    pendingRedeem: 'Pending',
    approvedRedeemAmount: 'Approved Amount'
  },
  messages: {
    loadFailed: 'Failed to load daily trends'
  }
},
```

In `admin-ui/src/lang/zh_CN.ts`, add:

```ts
reportTrends: {
  title: '每日趋势',
  subtitle: '按日期查看近期全业务经营趋势。',
  refresh: '刷新',
  empty: '暂无趋势数据',
  range: {
    seven: '最近 7 天',
    thirty: '最近 30 天'
  },
  cards: {
    depositAmount: '充值金额',
    gameNet: '游戏净额',
    rewards: '奖励金额',
    approvedRedeem: '通过兑换',
    pendingRedeem: '待审兑换'
  },
  columns: {
    date: '日期',
    members: '新增会员',
    depositOrders: '充值订单',
    depositAmount: '充值金额',
    gameOrders: '投注订单',
    betAmount: '投注额',
    payoutAmount: '派彩额',
    gameNet: '游戏净额',
    promotionClaims: '领取次数',
    rewardAmount: '奖励金额',
    redemptionOrders: '兑换订单',
    pendingRedeem: '待审核',
    approvedRedeemAmount: '通过金额'
  },
  messages: {
    loadFailed: '每日趋势加载失败'
  }
},
```

- [ ] **Step 4: Add trends page**

Create `admin-ui/src/views/report/trends/index.vue`:

```vue
<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="trends-toolbar">
          <div>
            <div class="trends-title">{{ t('reportTrends.title') }}</div>
            <div class="trends-subtitle">{{ t('reportTrends.subtitle') }}</div>
          </div>
          <div class="trends-actions">
            <el-radio-group v-model="range" size="default" @change="getList">
              <el-radio-button :label="7">{{ t('reportTrends.range.seven') }}</el-radio-button>
              <el-radio-button :label="30">{{ t('reportTrends.range.thirty') }}</el-radio-button>
            </el-radio-group>
            <el-button v-hasPermi="['report:trends:query']" type="primary" icon="Refresh" :loading="loading" @click="getList">
              {{ t('reportTrends.refresh') }}
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!loading && !rows.length" :description="t('reportTrends.empty')" />

      <el-row v-else :gutter="10" class="metric-row" v-loading="loading">
        <el-col v-for="card in metricCards" :key="card.label" :xs="24" :sm="12" :md="8" :lg="4">
          <div class="metric-card">
            <div class="metric-label">{{ card.label }}</div>
            <div class="metric-value">{{ card.value }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" border :data="rows">
        <el-table-column :label="t('reportTrends.columns.date')" prop="reportDate" align="center" width="120" />
        <el-table-column :label="t('reportTrends.columns.members')" prop="memberCount" align="right" width="110" />
        <el-table-column :label="t('reportTrends.columns.depositOrders')" prop="depositOrderCount" align="right" width="120" />
        <el-table-column :label="t('reportTrends.columns.depositAmount')" align="right" width="150">
          <template #default="scope">{{ formatAmount(scope.row.successfulDepositAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.gameOrders')" prop="gameOrderCount" align="right" width="120" />
        <el-table-column :label="t('reportTrends.columns.betAmount')" align="right" width="140">
          <template #default="scope">{{ formatAmount(scope.row.totalBetAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.payoutAmount')" align="right" width="140">
          <template #default="scope">{{ formatAmount(scope.row.totalPayoutAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.gameNet')" align="right" width="140">
          <template #default="scope">{{ formatAmount(scope.row.netGameAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.promotionClaims')" prop="promotionClaimCount" align="right" width="120" />
        <el-table-column :label="t('reportTrends.columns.rewardAmount')" align="right" width="140">
          <template #default="scope">{{ formatAmount(scope.row.successfulRewardAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('reportTrends.columns.redemptionOrders')" prop="redemptionOrderCount" align="right" width="130" />
        <el-table-column :label="t('reportTrends.columns.pendingRedeem')" prop="pendingRedemptionCount" align="right" width="110" />
        <el-table-column :label="t('reportTrends.columns.approvedRedeemAmount')" align="right" width="150">
          <template #default="scope">{{ formatAmount(scope.row.approvedRedemptionAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="ReportTrends" lang="ts">
import { listReportDailyTrends } from '@/api/report/trends';
import { ReportDailyTrendVO } from '@/api/report/trends/types';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { t } = useI18n();

const loading = ref(false);
const range = ref(7);
const rows = ref<ReportDailyTrendVO[]>([]);

const amount = (value: string | number | undefined) => Number(value || 0);
const formatAmount = (value: string | number | undefined) => amount(value).toFixed(6);
const formatCount = (value: number | undefined) => Number(value || 0).toLocaleString();

const totals = computed(() =>
  rows.value.reduce(
    (acc, row) => {
      acc.depositAmount += amount(row.successfulDepositAmount);
      acc.gameNet += amount(row.netGameAmount);
      acc.rewards += amount(row.successfulRewardAmount);
      acc.approvedRedeem += amount(row.approvedRedemptionAmount);
      acc.pendingRedeem += Number(row.pendingRedemptionCount || 0);
      return acc;
    },
    { depositAmount: 0, gameNet: 0, rewards: 0, approvedRedeem: 0, pendingRedeem: 0 }
  )
);

const metricCards = computed(() => [
  { label: t('reportTrends.cards.depositAmount'), value: formatAmount(totals.value.depositAmount) },
  { label: t('reportTrends.cards.gameNet'), value: formatAmount(totals.value.gameNet) },
  { label: t('reportTrends.cards.rewards'), value: formatAmount(totals.value.rewards) },
  { label: t('reportTrends.cards.approvedRedeem'), value: formatAmount(totals.value.approvedRedeem) },
  { label: t('reportTrends.cards.pendingRedeem'), value: formatCount(totals.value.pendingRedeem) }
]);

const getList = async () => {
  loading.value = true;
  try {
    const res = await listReportDailyTrends(range.value);
    rows.value = res.data || [];
  } catch (error) {
    proxy?.$modal.msgError(t('reportTrends.messages.loadFailed'));
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.trends-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.trends-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.trends-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.trends-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-row {
  row-gap: 10px;
}

.metric-card {
  min-height: 86px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.metric-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.metric-value {
  margin-top: 10px;
  color: var(--el-text-color-primary);
  font-size: 22px;
  font-weight: 760;
}

@media (max-width: 768px) {
  .trends-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .trends-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
```

- [ ] **Step 5: Run frontend checks**

Run:

```powershell
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui build:dev
```

Expected: both commands pass.

- [ ] **Step 6: Commit frontend trends page**

```powershell
git add admin-ui/src/api/report/trends admin-ui/src/views/report/trends/index.vue admin-ui/src/lang/zh_CN.ts admin-ui/src/lang/en_US.ts
git commit -m "feat(admin): add report trends page"
```

Expected: commit succeeds.

### Task 5: SQL Import And Runtime Smoke

**Files:**
- Modify local database only through existing SQL import script.

- [ ] **Step 1: Import menu SQL**

Run:

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

Expected: command exits 0.

- [ ] **Step 2: Verify menu rows**

Run:

```powershell
cmd /c "mysql -uroot -proot -N -e \"SELECT menu_id, menu_name, parent_id, perms, icon FROM gameluck_vue.sys_menu WHERE menu_id IN (2000,2001,2002,2011,2021) ORDER BY menu_id;\""
```

Expected output includes:

```text
2002    Trends    2000    report:trends:list     chart
2021    Report Trends Query    2002    report:trends:query    #
```

- [ ] **Step 3: Package backend**

If a previous backend jar process is running, stop only the process whose command line contains `C:\codex\project\backend\gameluck-admin\target\gameluck-admin.jar`.

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Start or restart backend**

Run:

```powershell
$existing = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*C:\\codex\\project\\backend\\gameluck-admin\\target\\gameluck-admin.jar*' }
if ($existing) { $existing | ForEach-Object { Stop-Process -Id $_.ProcessId -Force } }
Start-Process -FilePath "java" -ArgumentList @("-jar", "gameluck-admin\target\gameluck-admin.jar", "--spring.profiles.active=local") -WorkingDirectory "C:\codex\project\backend" -WindowStyle Hidden
Start-Sleep -Seconds 20
curl.exe -I --max-time 15 http://localhost:8080/
```

Expected: backend responds with HTTP 200.

- [ ] **Step 5: Runtime API smoke through authenticated Admin UI**

Open `http://localhost:5173/`, log in with the local admin account, then open browser DevTools Network panel and visit the Trends page added in Task 4.

Expected Network results:

```text
GET http://localhost:8080/report/trends/daily?range=7 returns HTTP 200
Response data contains 7 rows
After switching to Last 30 days, GET http://localhost:8080/report/trends/daily?range=30 returns HTTP 200
Response data contains 30 rows
The first row has reportDate and numeric metric fields
```

- [ ] **Step 6: Browser smoke**

With Admin UI running at `http://localhost:5173/`, open the admin console and verify:

```text
Report Center / Trends appears in sidebar
The Trends page loads without console errors
Last 7 days shows 7 daily rows
Last 30 days shows 30 daily rows
Refresh reloads data
Amount columns are right-aligned
No text overlaps on desktop width
```

### Task 6: Final Verification And Closure

**Files:**
- Modify: `progress.md`
- Modify: `task_plan.md`
- Modify: `docs/superpowers/plans/2026-07-10-report-daily-trends.md`

- [ ] **Step 1: Run full targeted verification**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-report -am -Plocal -DskipTests=false "-Dtest=ReportTrendServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui check:menu-icons
pnpm --dir admin-ui build:dev
```

Expected: all commands pass.

- [ ] **Step 2: Update root plan**

Append this row to `task_plan.md` after Phase 15:

```markdown
| 16. Phase 5 B端每日趋势看板 | complete | 新增 Report Center / Trends，支持最近 7/30 天全业务经营趋势查询 | docs/superpowers/specs/2026-07-10-report-daily-trends-design.md、docs/superpowers/plans/2026-07-10-report-daily-trends.md |
```

- [ ] **Step 3: Update progress log**

Append to `progress.md`:

```markdown
## 2026-07-10 Phase 5 Report Daily Trends

- Completed Phase 5 B-side daily operating trends:
  - Added `/report/trends/daily?range=7|30`.
  - Added continuous date-fill service logic in `gameluck-report`.
  - Added `Report Center / Trends` menu and Admin UI page.
  - Verification passed:
    - `C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-report -am -Plocal -DskipTests=false "-Dtest=ReportTrendServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
    - `C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests`
    - `pnpm --dir admin-ui check:i18n`
    - `pnpm --dir admin-ui check:menu-icons`
    - `pnpm --dir admin-ui build:dev`
```

- [ ] **Step 4: Mark this plan complete**

Change every executable checkbox in this file from `- [ ]` to `- [x]` after the corresponding step has actually been completed and verified.

- [ ] **Step 5: Commit closure docs**

```powershell
git add docs/superpowers/plans/2026-07-10-report-daily-trends.md progress.md task_plan.md
git commit -m "docs: close report daily trends phase"
```

Expected: commit succeeds.

- [ ] **Step 6: Push and verify remote**

Run:

```powershell
git push origin main
git rev-parse main
git ls-remote https://github.com/tt88737/game_luck.git refs/heads/main
git status --short
```

Expected:

```text
remote hash matches local main
only pre-existing unrelated h5/package-lock.json remains, if still present
```

## Self-Review

Spec coverage:

- Daily `7|30` API is covered by Tasks 1 and 2.
- Continuous zero-filled dates are covered by Task 1 tests and Task 2 service logic.
- Report menu, route i18n, and permission are covered by Task 3.
- Admin UI API wrappers, KPI totals, range switch, loading, empty, and error states are covered by Task 4.
- SQL import and runtime checks are covered by Task 5.
- Final verification, progress, root plan closure, and push are covered by Task 6.

Scope controls:

- No chart dependency is introduced.
- No export is introduced.
- Existing `Report Overview` remains unchanged.
- No wallet or business table mutation is introduced.
