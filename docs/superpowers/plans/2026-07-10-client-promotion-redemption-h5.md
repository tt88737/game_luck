# Phase 3 Client Promotion And Redemption H5 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build backend-backed H5 promotion reward claiming and redemption request flows for the demo player client.

**Architecture:** Add narrow `/api/client/**` promotion and redemption APIs that reuse existing promotion, redemption, wallet, and client-token services. H5 replaces static promotion/redemption demo data with typed API calls and session-aware page states.

**Tech Stack:** Spring Boot, Java, MyBatis Plus, JUnit 5, Mockito, Vue 3, TypeScript, Vite, Playwright smoke testing.

---

## File Structure

Create or modify these files during execution:

```text
backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/
backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/client/
backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper/PromotionRewardMapper.java
backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper/PromotionClaimMapper.java
backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion/PromotionRewardMapper.xml
backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion/PromotionClaimMapper.xml
backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/
backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client/
backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/mapper/RedemptionOrderMapper.java
backend/gameluck-modules/gameluck-redemption/src/main/resources/mapper/redemption/RedemptionOrderMapper.xml
backend/gameluck-admin/src/main/resources/i18n/messages.properties
backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties
backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties
backend/script/sql/gameluck_client_demo.sql
h5/src/types/client.ts
h5/src/api/client.ts
h5/src/views/PromotionsView.vue
h5/src/views/RedemptionsView.vue
progress.md
task_plan.md
```

Do not modify existing admin controllers except when a compile error proves a shared API conflict. Client APIs must use dedicated client BO/VO types so admin fields and permissions do not leak to H5.

### Task 1: Client Promotion API

**Files:**
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/domain/bo/ClientPromotionClaimBo.java`
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/domain/vo/ClientPromotionVo.java`
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/service/ClientPromotionService.java`
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/controller/ClientPromotionController.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper/PromotionRewardMapper.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper/PromotionClaimMapper.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion/PromotionRewardMapper.xml`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion/PromotionClaimMapper.xml`
- Test: `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/client/service/ClientPromotionServiceTest.java`

- [x] **Step 1: Add mapper method declarations**

Modify `PromotionRewardMapper.java`:

```java
List<PromotionReward> selectClientActiveRewards(@Param("tenantId") String tenantId);
```

Modify `PromotionClaimMapper.java`:

```java
List<PromotionClaim> selectClientClaimsByMember(@Param("tenantId") String tenantId,
                                                @Param("memberId") Long memberId);
```

- [x] **Step 2: Add mapper SQL**

Add to `PromotionRewardMapper.xml`:

```xml
<select id="selectClientActiveRewards" resultType="com.gameluck.promotion.domain.PromotionReward">
    SELECT *
    FROM gl_promotion_reward
    WHERE tenant_id = #{tenantId}
      AND status = 'ACTIVE'
      AND del_flag = '0'
      AND (start_time IS NULL OR start_time <= NOW())
      AND (end_time IS NULL OR end_time >= NOW())
    ORDER BY create_time DESC
</select>
```

Add to `PromotionClaimMapper.xml`:

```xml
<select id="selectClientClaimsByMember" resultType="com.gameluck.promotion.domain.PromotionClaim">
    SELECT *
    FROM gl_promotion_claim
    WHERE tenant_id = #{tenantId}
      AND member_id = #{memberId}
      AND del_flag = '0'
    ORDER BY create_time DESC
</select>
```

- [x] **Step 3: Write promotion service tests**

Create `ClientPromotionServiceTest.java`:

```java
package com.gameluck.promotion.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.promotion.client.domain.bo.ClientPromotionClaimBo;
import com.gameluck.promotion.client.domain.vo.ClientPromotionVo;
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.PromotionReward;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.mapper.PromotionClaimMapper;
import com.gameluck.promotion.mapper.PromotionRewardMapper;
import com.gameluck.promotion.service.IPromotionRewardService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientPromotionServiceTest {

    @Test
    @Tag("local")
    void listMarksClaimedRewardsForCurrentMember() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IPromotionRewardService rewardService = mock(IPromotionRewardService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientPromotionService service = new ClientPromotionService(rewardMapper, claimMapper, rewardService, tokenService);
        PromotionReward reward = reward();
        PromotionClaim claim = claim();
        when(rewardMapper.selectClientActiveRewards("000000")).thenReturn(List.of(reward));
        when(claimMapper.selectClientClaimsByMember("000000", 1001L)).thenReturn(List.of(claim));

        List<ClientPromotionVo> result = service.promotions("Bearer " + tokenService.issue(1001L));

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getPromotionId());
        assertEquals("SUCCESS", result.get(0).getClaimStatus());
        assertFalse(result.get(0).getCanClaim());
    }

    @Test
    @Tag("local")
    void claimUsesCurrentMemberAndExistingRewardService() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IPromotionRewardService rewardService = mock(IPromotionRewardService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientPromotionService service = new ClientPromotionService(rewardMapper, claimMapper, rewardService, tokenService);
        PromotionClaimVo claimVo = new PromotionClaimVo();
        claimVo.setPromotionId(10L);
        claimVo.setMemberId(1001L);
        claimVo.setStatus("SUCCESS");
        claimVo.setWalletTransactionNo("WT_PROMO_1");
        when(rewardService.claim(any(PromotionClaimBo.class))).thenReturn(claimVo);
        ClientPromotionClaimBo bo = new ClientPromotionClaimBo();
        bo.setPromotionId(10L);

        ClientPromotionVo result = service.claim("Bearer " + tokenService.issue(1001L), bo);

        assertEquals(10L, result.getPromotionId());
        assertEquals("SUCCESS", result.getClaimStatus());
        assertEquals("WT_PROMO_1", result.getWalletTransactionNo());
        assertFalse(result.getCanClaim());
        verify(rewardService).claim(any(PromotionClaimBo.class));
    }

    private PromotionReward reward() {
        PromotionReward reward = new PromotionReward();
        reward.setId(10L);
        reward.setPromotionNo("PR-DEMO-DAILY-SC");
        reward.setPromotionName("每日 SC 奖励");
        reward.setCurrencyCode("SC");
        reward.setRewardAmount(new BigDecimal("8.000000"));
        reward.setStatus("ACTIVE");
        return reward;
    }

    private PromotionClaim claim() {
        PromotionClaim claim = new PromotionClaim();
        claim.setPromotionId(10L);
        claim.setClaimNo("PC1001");
        claim.setStatus("SUCCESS");
        claim.setWalletTransactionNo("WT_PROMO_1");
        return claim;
    }
}
```

- [x] **Step 4: Run promotion tests and verify failure**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests=false "-Dtest=ClientPromotionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because client promotion classes and mapper methods do not exist.

- [x] **Step 5: Implement promotion client BO and VO**

Create `ClientPromotionClaimBo.java`:

```java
package com.gameluck.promotion.client.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientPromotionClaimBo {
    @NotNull(message = "{client.promotion.id.required}")
    private Long promotionId;
}
```

Create `ClientPromotionVo.java`:

```java
package com.gameluck.promotion.client.domain.vo;

import lombok.Data;

@Data
public class ClientPromotionVo {
    private Long promotionId;
    private String promotionNo;
    private String promotionName;
    private String currencyCode;
    private String rewardAmount;
    private String status;
    private String claimStatus;
    private String claimNo;
    private String walletTransactionNo;
    private Boolean canClaim;
}
```

- [x] **Step 6: Implement promotion client service**

Create `ClientPromotionService.java`:

```java
package com.gameluck.promotion.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.promotion.client.domain.bo.ClientPromotionClaimBo;
import com.gameluck.promotion.client.domain.vo.ClientPromotionVo;
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.PromotionReward;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.mapper.PromotionClaimMapper;
import com.gameluck.promotion.mapper.PromotionRewardMapper;
import com.gameluck.promotion.service.IPromotionRewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ClientPromotionService {

    private static final String TENANT_ID = "000000";

    private final PromotionRewardMapper promotionRewardMapper;
    private final PromotionClaimMapper promotionClaimMapper;
    private final IPromotionRewardService promotionRewardService;
    private final ClientTokenService clientTokenService;

    public List<ClientPromotionVo> promotions(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        Map<Long, PromotionClaim> claims = promotionClaimMapper.selectClientClaimsByMember(TENANT_ID, memberId).stream()
            .collect(Collectors.toMap(PromotionClaim::getPromotionId, Function.identity(), (left, right) -> left));
        return promotionRewardMapper.selectClientActiveRewards(TENANT_ID).stream()
            .map(reward -> toClientPromotion(reward, claims.get(reward.getId())))
            .toList();
    }

    public ClientPromotionVo claim(String authorization, ClientPromotionClaimBo bo) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        PromotionClaimBo claimBo = new PromotionClaimBo();
        claimBo.setPromotionId(bo.getPromotionId());
        claimBo.setMemberId(memberId);
        claimBo.setRemark("H5 client claim");
        PromotionClaimVo claim = promotionRewardService.claim(claimBo);
        return toClientPromotion(claim);
    }

    private ClientPromotionVo toClientPromotion(PromotionReward reward, PromotionClaim claim) {
        ClientPromotionVo vo = new ClientPromotionVo();
        vo.setPromotionId(reward.getId());
        vo.setPromotionNo(reward.getPromotionNo());
        vo.setPromotionName(reward.getPromotionName());
        vo.setCurrencyCode(reward.getCurrencyCode());
        vo.setRewardAmount(reward.getRewardAmount().setScale(2).toPlainString());
        vo.setStatus(reward.getStatus());
        if (claim != null) {
            vo.setClaimStatus(claim.getStatus());
            vo.setClaimNo(claim.getClaimNo());
            vo.setWalletTransactionNo(claim.getWalletTransactionNo());
            vo.setCanClaim(false);
        } else {
            vo.setClaimStatus("UNCLAIMED");
            vo.setCanClaim(true);
        }
        return vo;
    }

    private ClientPromotionVo toClientPromotion(PromotionClaimVo claim) {
        ClientPromotionVo vo = new ClientPromotionVo();
        vo.setPromotionId(claim.getPromotionId());
        vo.setPromotionNo(claim.getPromotionNo());
        vo.setPromotionName(claim.getPromotionName());
        vo.setCurrencyCode(claim.getCurrencyCode());
        vo.setRewardAmount(claim.getRewardAmount().setScale(2).toPlainString());
        vo.setStatus("ACTIVE");
        vo.setClaimStatus(claim.getStatus());
        vo.setClaimNo(claim.getClaimNo());
        vo.setWalletTransactionNo(claim.getWalletTransactionNo());
        vo.setCanClaim(false);
        return vo;
    }
}
```

- [x] **Step 7: Implement promotion client controller**

Create `ClientPromotionController.java`:

```java
package com.gameluck.promotion.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.promotion.client.domain.bo.ClientPromotionClaimBo;
import com.gameluck.promotion.client.domain.vo.ClientPromotionVo;
import com.gameluck.promotion.client.service.ClientPromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/api/client/promotions")
public class ClientPromotionController {

    private final ClientPromotionService clientPromotionService;

    @GetMapping
    public R<List<ClientPromotionVo>> promotions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientPromotionService.promotions(authorization));
    }

    @PostMapping("/claim")
    public R<ClientPromotionVo> claim(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @Valid @RequestBody ClientPromotionClaimBo bo) {
        return R.ok(clientPromotionService.claim(authorization, bo));
    }
}
```

- [x] **Step 8: Add promotion i18n keys**

Add to `messages.properties` and `messages_zh_CN.properties`:

```properties
client.promotion.id.required=\u6d3b\u52a8ID\u4e0d\u80fd\u4e3a\u7a7a
```

Add to `messages_en_US.properties`:

```properties
client.promotion.id.required=Promotion id is required.
```

- [x] **Step 9: Run promotion tests and compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests=false "-Dtest=ClientPromotionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: both commands pass.

- [x] **Step 10: Commit promotion client API**

Run:

```powershell
git add backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/client backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion backend/gameluck-admin/src/main/resources/i18n
git commit -m "feat(client): add promotion claim api"
```

Expected: commit succeeds.

### Task 2: Client Redemption API

**Files:**
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/domain/bo/ClientRedemptionRequestBo.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/domain/vo/ClientRedemptionVo.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/service/ClientRedemptionService.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/controller/ClientRedemptionController.java`
- Modify: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/mapper/RedemptionOrderMapper.java`
- Modify: `backend/gameluck-modules/gameluck-redemption/src/main/resources/mapper/redemption/RedemptionOrderMapper.xml`
- Test: `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client/service/ClientRedemptionServiceTest.java`

- [x] **Step 1: Add mapper method declaration**

Modify `RedemptionOrderMapper.java`:

```java
List<RedemptionOrder> selectClientOrders(@Param("tenantId") String tenantId,
                                         @Param("memberId") Long memberId,
                                         @Param("offset") Integer offset,
                                         @Param("pageSize") Integer pageSize);
```

- [x] **Step 2: Add mapper SQL**

Add to `RedemptionOrderMapper.xml`:

```xml
<select id="selectClientOrders" resultType="com.gameluck.redemption.domain.RedemptionOrder">
    SELECT *
    FROM gl_redemption_order
    WHERE tenant_id = #{tenantId}
      AND member_id = #{memberId}
      AND del_flag = '0'
    ORDER BY create_time DESC
    LIMIT #{offset}, #{pageSize}
</select>
```

- [x] **Step 3: Write redemption service tests**

Create `ClientRedemptionServiceTest.java`:

```java
package com.gameluck.redemption.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.redemption.client.domain.bo.ClientRedemptionRequestBo;
import com.gameluck.redemption.client.domain.vo.ClientRedemptionVo;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.domain.bo.RedemptionOrderBo;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.redemption.service.IRedemptionOrderService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientRedemptionServiceTest {

    @Test
    @Tag("local")
    void listReturnsCurrentMemberOrdersOnly() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, tokenService);
        when(mapper.selectClientOrders("000000", 1001L, 0, 20)).thenReturn(List.of(order()));

        List<ClientRedemptionVo> result = service.redemptions("Bearer " + tokenService.issue(1001L));

        assertEquals(1, result.size());
        assertEquals("RD1001", result.get(0).getOrderNo());
        assertEquals("PENDING", result.get(0).getStatus());
    }

    @Test
    @Tag("local")
    void requestCreatesScRedemptionForCurrentMember() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, tokenService);
        when(orderService.insertByBo(any(RedemptionOrderBo.class))).thenReturn(true);
        ClientRedemptionRequestBo bo = new ClientRedemptionRequestBo();
        bo.setCurrencyCode("SC");
        bo.setAmount(new BigDecimal("1.00"));

        ClientRedemptionVo result = service.request("Bearer " + tokenService.issue(1001L), bo);

        assertEquals("SC", result.getCurrencyCode());
        assertEquals("1.00", result.getAmount());
        assertEquals("PENDING", result.getStatus());
        verify(orderService).insertByBo(any(RedemptionOrderBo.class));
    }

    @Test
    @Tag("local")
    void requestRejectsUnsupportedCurrency() {
        ClientRedemptionService service = new ClientRedemptionService(
            mock(RedemptionOrderMapper.class), mock(IRedemptionOrderService.class), new ClientTokenService());
        ClientRedemptionRequestBo bo = new ClientRedemptionRequestBo();
        bo.setCurrencyCode("GC");
        bo.setAmount(new BigDecimal("1.00"));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.request("Bearer " + new ClientTokenService().issue(1001L), bo));

        assertEquals("client.redemption.currency.unsupported", exception.getMessage());
    }

    private RedemptionOrder order() {
        RedemptionOrder order = new RedemptionOrder();
        order.setId(1L);
        order.setRedemptionOrderNo("RD1001");
        order.setCurrencyCode("SC");
        order.setAmount(new BigDecimal("1.00"));
        order.setStatus("PENDING");
        order.setFreezeNo("WF1001");
        order.setCreateTime(new Date());
        return order;
    }
}
```

- [x] **Step 4: Run redemption tests and verify failure**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=ClientRedemptionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because client redemption classes and mapper method do not exist.

- [x] **Step 5: Implement redemption client BO and VO**

Create `ClientRedemptionRequestBo.java`:

```java
package com.gameluck.redemption.client.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClientRedemptionRequestBo {
    @NotNull(message = "{client.redemption.currency.required}")
    private String currencyCode;

    @NotNull(message = "{client.redemption.amount.required}")
    @DecimalMin(value = "0.000001", message = "{client.redemption.amount.positive}")
    private BigDecimal amount;
}
```

Create `ClientRedemptionVo.java`:

```java
package com.gameluck.redemption.client.domain.vo;

import lombok.Data;

@Data
public class ClientRedemptionVo {
    private Long orderId;
    private String orderNo;
    private String currencyCode;
    private String amount;
    private String status;
    private String walletFreezeNo;
    private String reviewRemark;
    private String createdAt;
}
```

- [x] **Step 6: Implement redemption client service**

Create `ClientRedemptionService.java`:

```java
package com.gameluck.redemption.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.redemption.client.domain.bo.ClientRedemptionRequestBo;
import com.gameluck.redemption.client.domain.vo.ClientRedemptionVo;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.domain.bo.RedemptionOrderBo;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.redemption.service.IRedemptionOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientRedemptionService {

    private static final String TENANT_ID = "000000";
    private static final String SUPPORTED_CURRENCY = "SC";

    private final RedemptionOrderMapper redemptionOrderMapper;
    private final IRedemptionOrderService redemptionOrderService;
    private final ClientTokenService clientTokenService;

    public List<ClientRedemptionVo> redemptions(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        return redemptionOrderMapper.selectClientOrders(TENANT_ID, memberId, 0, 20).stream()
            .map(this::toClientRedemption)
            .toList();
    }

    public ClientRedemptionVo request(String authorization, ClientRedemptionRequestBo bo) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        if (!SUPPORTED_CURRENCY.equals(bo.getCurrencyCode())) {
            throw new ServiceException(MessageUtils.message("client.redemption.currency.unsupported"));
        }
        RedemptionOrderBo orderBo = new RedemptionOrderBo();
        orderBo.setMemberId(memberId);
        orderBo.setCurrencyCode(bo.getCurrencyCode());
        orderBo.setAmount(bo.getAmount());
        orderBo.setRedemptionMethod("SIMULATED");
        orderBo.setAccountRef("H5_DEMO");
        orderBo.setRemark(MessageUtils.message("client.redemption.request.remark"));
        redemptionOrderService.insertByBo(orderBo);

        ClientRedemptionVo vo = new ClientRedemptionVo();
        vo.setCurrencyCode(bo.getCurrencyCode());
        vo.setAmount(bo.getAmount().setScale(2).toPlainString());
        vo.setStatus("PENDING");
        return vo;
    }

    private ClientRedemptionVo toClientRedemption(RedemptionOrder order) {
        ClientRedemptionVo vo = new ClientRedemptionVo();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getRedemptionOrderNo());
        vo.setCurrencyCode(order.getCurrencyCode());
        vo.setAmount(order.getAmount().setScale(2).toPlainString());
        vo.setStatus(order.getStatus());
        vo.setWalletFreezeNo(order.getFreezeNo());
        vo.setReviewRemark(order.getAuditReason());
        if (order.getCreateTime() != null) {
            vo.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(order.getCreateTime()));
        }
        return vo;
    }
}
```

- [x] **Step 7: Implement redemption client controller**

Create `ClientRedemptionController.java`:

```java
package com.gameluck.redemption.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.redemption.client.domain.bo.ClientRedemptionRequestBo;
import com.gameluck.redemption.client.domain.vo.ClientRedemptionVo;
import com.gameluck.redemption.client.service.ClientRedemptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/api/client/redemptions")
public class ClientRedemptionController {

    private final ClientRedemptionService clientRedemptionService;

    @GetMapping
    public R<List<ClientRedemptionVo>> redemptions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientRedemptionService.redemptions(authorization));
    }

    @PostMapping("/request")
    public R<ClientRedemptionVo> request(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @Valid @RequestBody ClientRedemptionRequestBo bo) {
        return R.ok(clientRedemptionService.request(authorization, bo));
    }
}
```

- [x] **Step 8: Add redemption i18n keys**

Add to `messages.properties` and `messages_zh_CN.properties`:

```properties
client.redemption.currency.required=\u5e01\u79cd\u4e0d\u80fd\u4e3a\u7a7a
client.redemption.currency.unsupported=\u5f53\u524d\u4ec5\u652f\u6301 SC \u5151\u6362
client.redemption.amount.required=\u5151\u6362\u91d1\u989d\u4e0d\u80fd\u4e3a\u7a7a
client.redemption.amount.positive=\u5151\u6362\u91d1\u989d\u5fc5\u987b\u5927\u4e8e 0
client.redemption.request.remark=H5 \u73a9\u5bb6\u7aef\u5151\u6362\u7533\u8bf7
```

Add to `messages_en_US.properties`:

```properties
client.redemption.currency.required=Currency is required.
client.redemption.currency.unsupported=Only SC redemption is supported.
client.redemption.amount.required=Redemption amount is required.
client.redemption.amount.positive=Redemption amount must be greater than 0.
client.redemption.request.remark=H5 client redemption request
```

- [x] **Step 9: Run redemption tests and compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=ClientRedemptionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: both commands pass.

- [x] **Step 10: Commit redemption client API**

Run:

```powershell
git add backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/mapper backend/gameluck-modules/gameluck-redemption/src/main/resources/mapper/redemption backend/gameluck-admin/src/main/resources/i18n
git commit -m "feat(client): add redemption request api"
```

Expected: commit succeeds.

### Task 3: Demo Seed Data

**Files:**
- Modify: `backend/script/sql/gameluck_client_demo.sql`

- [x] **Step 1: Add demo promotion cleanup and seed**

Modify the top of `gameluck_client_demo.sql` so demo claim rows are removed before demo reward rows:

```sql
DELETE FROM gl_promotion_claim WHERE tenant_id = '000000' AND promotion_no = 'PR-DEMO-DAILY-SC' AND member_id = 1001;
DELETE FROM gl_promotion_reward WHERE tenant_id = '000000' AND promotion_no = 'PR-DEMO-DAILY-SC';
DELETE FROM gl_redemption_order WHERE tenant_id = '000000' AND member_id = 1001 AND account_ref = 'H5_DEMO';
```

Append the demo reward insert:

```sql
INSERT INTO gl_promotion_reward (
  id, tenant_id, promotion_no, promotion_name, currency_code, reward_amount, status,
  start_time, end_time, remark, version, del_flag, create_time, update_time
) VALUES (
  13001, '000000', 'PR-DEMO-DAILY-SC', '每日 SC 奖励', 'SC', 8.000000, 'ACTIVE',
  NULL, NULL, 'H5 demo promotion reward', 0, '0', NOW(), NOW()
);
```

- [x] **Step 2: Import demo seed locally**

Run:

```powershell
cmd /c "mysql -uroot -proot gameluck_vue < backend\script\sql\gameluck_client_demo.sql"
```

Expected: command exits 0.

- [x] **Step 3: Verify demo reward and wallet reset**

Run:

```powershell
cmd /c "mysql -uroot -proot -N -e \"SELECT promotion_no, promotion_name, currency_code, reward_amount, status FROM gameluck_vue.gl_promotion_reward WHERE tenant_id='000000' AND promotion_no='PR-DEMO-DAILY-SC'; SELECT currency_code, available_balance, frozen_balance FROM gameluck_vue.gl_wallet_account WHERE tenant_id='000000' AND member_id=1001 ORDER BY currency_code;\""
```

Expected output includes:

```text
PR-DEMO-DAILY-SC
GC 1000.000000 0.000000
SC 25.000000 0.000000
```

- [x] **Step 4: Commit demo seed**

Run:

```powershell
git add backend/script/sql/gameluck_client_demo.sql
git commit -m "chore(client): seed promotion demo data"
```

Expected: commit succeeds.

### Task 4: H5 Client Types And API Methods

**Files:**
- Modify: `h5/src/types/client.ts`
- Modify: `h5/src/api/client.ts`

- [x] **Step 1: Add H5 types**

Add to `h5/src/types/client.ts`:

```ts
export interface ClientPromotion {
  promotionId: number
  promotionNo: string
  promotionName: string
  currencyCode: string
  rewardAmount: string
  status: string
  claimStatus: string
  claimNo: string
  walletTransactionNo: string
  canClaim: boolean
}

export interface ClientRedemption {
  orderId: number
  orderNo: string
  currencyCode: string
  amount: string
  status: string
  walletFreezeNo: string
  reviewRemark: string
  createdAt: string
}
```

- [x] **Step 2: Add H5 API client methods**

Modify the import block in `h5/src/api/client.ts`:

```ts
import type {
  ApiResponse,
  ClientBootstrap,
  ClientGame,
  ClientGameLaunch,
  ClientLoginResponse,
  ClientMember,
  ClientPage,
  ClientPromotion,
  ClientRedemption,
  WalletAccount,
  WalletLedger,
} from '../types/client'
```

Add methods inside `clientApi`:

```ts
  promotions: () => request<ClientPromotion[]>('/api/client/promotions'),
  claimPromotion: (promotionId: number) =>
    request<ClientPromotion>('/api/client/promotions/claim', {
      method: 'POST',
      body: JSON.stringify({ promotionId }),
    }),
  redemptions: () => request<ClientRedemption[]>('/api/client/redemptions'),
  requestRedemption: (currencyCode: string, amount: string) =>
    request<ClientRedemption>('/api/client/redemptions/request', {
      method: 'POST',
      body: JSON.stringify({ currencyCode, amount }),
    }),
```

- [x] **Step 3: Run H5 build**

Run:

```powershell
npm run build --prefix h5
```

Expected: build succeeds.

- [x] **Step 4: Commit H5 API foundation**

Run:

```powershell
git add h5/src/types/client.ts h5/src/api/client.ts
git commit -m "feat(h5): add promotion redemption client api"
```

Expected: commit succeeds.

### Task 5: H5 Promotion Page Integration

**Files:**
- Modify: `h5/src/views/PromotionsView.vue`

- [ ] **Step 1: Replace static promotion page with backend state**

Replace `PromotionsView.vue` with:

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { ClientPromotion } from '../types/client'

const promotions = ref<ClientPromotion[]>([])
const loading = ref(false)
const claimingId = ref<number | null>(null)
const error = ref('')
const success = ref('')

async function loadPromotions() {
  if (!sessionState.member) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    promotions.value = await clientApi.promotions()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '奖励加载失败'
  } finally {
    loading.value = false
  }
}

async function claim(promotion: ClientPromotion) {
  claimingId.value = promotion.promotionId
  error.value = ''
  success.value = ''
  try {
    const result = await clientApi.claimPromotion(promotion.promotionId)
    success.value = `${result.promotionName} 已领取`
    await loadPromotions()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '领取失败'
  } finally {
    claimingId.value = null
  }
}

onMounted(loadPromotions)
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">奖励</p>
    <h1>活动奖励领取</h1>
    <p class="muted">奖励数据来自后端玩家端活动 API。</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>请先登录</strong>
    <RouterLink class="btn primary" to="/login">登录</RouterLink>
  </section>

  <template v-else>
    <p v-if="loading" class="muted">正在加载奖励...</p>
    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="success" class="success-text">{{ success }}</p>

    <section v-if="promotions.length" class="item-list">
      <article v-for="promotion in promotions" :key="promotion.promotionId" class="list-card">
        <div>
          <small>{{ promotion.rewardAmount }} {{ promotion.currencyCode }}</small>
          <h2>{{ promotion.promotionName }}</h2>
          <p>{{ promotion.canClaim ? '可领取' : '已领取' }}</p>
        </div>
        <button class="btn compact" :disabled="!promotion.canClaim || claimingId === promotion.promotionId" @click="claim(promotion)">
          {{ claimingId === promotion.promotionId ? '领取中' : promotion.canClaim ? '领取' : '已领取' }}
        </button>
      </article>
    </section>

    <section v-else-if="!loading" class="empty-state">
      <strong>暂无可领取奖励</strong>
    </section>
  </template>
</template>
```

- [ ] **Step 2: Run H5 build**

Run:

```powershell
npm run build --prefix h5
```

Expected: build succeeds.

- [ ] **Step 3: Commit H5 promotion page**

Run:

```powershell
git add h5/src/views/PromotionsView.vue
git commit -m "feat(h5): connect promotions page"
```

Expected: commit succeeds.

### Task 6: H5 Redemption Page Integration

**Files:**
- Modify: `h5/src/views/RedemptionsView.vue`

- [ ] **Step 1: Replace static redemption page with backend state**

Replace `RedemptionsView.vue` with:

```vue
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { ClientRedemption, WalletAccount } from '../types/client'

const accounts = ref<WalletAccount[]>([])
const redemptions = ref<ClientRedemption[]>([])
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const form = reactive({
  amount: '1.00',
})

const scAccount = computed(() => accounts.value.find((account) => account.currencyCode === 'SC'))
const canSubmit = computed(() => Number(form.amount) > 0 && !submitting.value)

async function loadRedemptions() {
  if (!sessionState.member) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [walletAccounts, rows] = await Promise.all([
      clientApi.walletAccounts(),
      clientApi.redemptions(),
    ])
    accounts.value = walletAccounts
    redemptions.value = rows
  } catch (err) {
    error.value = err instanceof Error ? err.message : '兑换加载失败'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!canSubmit.value) {
    return
  }
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    await clientApi.requestRedemption('SC', form.amount)
    success.value = '兑换申请已提交'
    await loadRedemptions()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '兑换申请失败'
  } finally {
    submitting.value = false
  }
}

onMounted(loadRedemptions)
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">兑换</p>
    <h1>提交审核后冻结余额</h1>
    <p class="muted">当前演示流程仅支持提交 SC 兑换申请。</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>请先登录</strong>
    <RouterLink class="btn primary" to="/login">登录</RouterLink>
  </section>

  <template v-else>
    <p v-if="loading" class="muted">正在加载兑换数据...</p>
    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="success" class="success-text">{{ success }}</p>

    <section class="redeem-panel">
      <div>
        <span>可用 SC</span>
        <strong>{{ scAccount?.availableBalance || '0.00' }}</strong>
        <small>冻结：{{ scAccount?.frozenBalance || '0.00' }}</small>
      </div>
      <form class="inline-form" @submit.prevent="submit">
        <input v-model="form.amount" inputmode="decimal" aria-label="兑换金额" />
        <button class="btn primary" type="submit" :disabled="!canSubmit">
          {{ submitting ? '提交中' : '提交兑换' }}
        </button>
      </form>
    </section>

    <section class="table-panel">
      <h2>最近兑换</h2>
      <div v-if="redemptions.length" class="table-list">
        <div v-for="item in redemptions" :key="item.orderNo || item.orderId" class="table-row">
          <span>{{ item.createdAt || item.orderNo }}</span>
          <strong>{{ item.amount }} {{ item.currencyCode }}</strong>
          <em>{{ item.status }}</em>
        </div>
      </div>
      <p v-else class="muted">暂无兑换记录。</p>
    </section>
  </template>
</template>
```

- [ ] **Step 2: Add inline form CSS if needed**

If `inline-form` is not already styled in `h5/src/style.css`, add:

```css
.inline-form {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.inline-form input {
  min-width: 120px;
  flex: 1;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px;
  font: inherit;
}
```

- [ ] **Step 3: Run H5 build**

Run:

```powershell
npm run build --prefix h5
```

Expected: build succeeds.

- [ ] **Step 4: Commit H5 redemption page**

Run:

```powershell
git add h5/src/views/RedemptionsView.vue h5/src/style.css
git commit -m "feat(h5): connect redemptions page"
```

Expected: commit succeeds.

### Task 7: Final Verification And Runtime Smoke

**Files:**
- Modify: `progress.md`
- Modify: `task_plan.md`
- Modify: this plan file by checking completed steps during execution

- [ ] **Step 1: Run static verification**

Run:

```powershell
pnpm --dir admin-ui check:i18n
npm run build --prefix h5
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: all commands pass.

- [ ] **Step 2: Package backend**

If a previous backend jar process is running, stop only the process whose command line contains `C:\codex\project\backend\gameluck-admin\target\gameluck-admin.jar`.

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: package succeeds and writes `backend/gameluck-admin/target/gameluck-admin.jar`.

- [ ] **Step 3: Start backend and H5**

Start backend:

```powershell
$backend = Start-Process -FilePath "java" -ArgumentList @("-jar", "gameluck-admin\target\gameluck-admin.jar", "--spring.profiles.active=local") -WorkingDirectory "C:\codex\project\backend" -WindowStyle Hidden -PassThru
```

Start H5 if not already running:

```powershell
$h5 = Start-Process -FilePath "powershell" -ArgumentList @("-NoProfile", "-Command", "npm run dev --prefix h5 -- --host 127.0.0.1 --port 5174") -WorkingDirectory "C:\codex\project" -WindowStyle Hidden -PassThru
```

Expected:

```text
http://localhost:8080/api/client/bootstrap returns 200
http://127.0.0.1:5174/ returns 200
```

- [ ] **Step 4: Run API smoke**

Run:

```powershell
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/client/auth/login" -ContentType "application/json" -Body '{"username":"demo_player","password":"Demo123456"}'
$token = $login.data.accessToken
$promotions = Invoke-RestMethod -Uri "http://localhost:8080/api/client/promotions" -Headers @{ Authorization = "Bearer $token" }
$claim = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/client/promotions/claim" -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body '{"promotionId":13001}'
$redemption = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/client/redemptions/request" -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body '{"currencyCode":"SC","amount":"1.00"}'
$rows = Invoke-RestMethod -Uri "http://localhost:8080/api/client/redemptions" -Headers @{ Authorization = "Bearer $token" }
```

Expected:

```text
$promotions.data contains PR-DEMO-DAILY-SC
$claim.data.claimStatus is SUCCESS
$redemption.data.status is PENDING
$rows.data contains at least one row
```

- [ ] **Step 5: Run browser smoke**

Use Playwright from a temp directory as in previous smoke tests. The smoke must:

```text
Open http://127.0.0.1:5174/
Login with demo_player / Demo123456
Open 奖励
Click 领取
See 已领取 or success message
Open 钱包
See SC balance reflects promotion credit
Open 兑换
Submit 1.00
See 兑换申请已提交
See recent redemption row
```

Expected: Playwright exits 0.

- [ ] **Step 6: Update progress and root plan**

Append to `progress.md`:

```markdown
- Completed Phase 3 client promotion and redemption H5 flow:
  - Added `/api/client/promotions`, `/api/client/promotions/claim`, `/api/client/redemptions`, and `/api/client/redemptions/request`.
  - Seeded the demo SC promotion reward.
  - Wired H5 rewards and redemption pages to backend client APIs.
  - Verification passed: i18n guard, H5 build, backend compile/package, API smoke, and H5 browser smoke.
```

Update `task_plan.md` Phase 13 status from `in_progress` to `complete` if Phase 2 and Phase 3 client acceptance are both satisfied, or add a new Phase 14 row for the completed Phase 3 scope if preserving Phase 13 as the broader player-client epic.

- [ ] **Step 7: Commit final docs**

Run:

```powershell
git add docs/superpowers/plans/2026-07-10-client-promotion-redemption-h5.md progress.md task_plan.md
git commit -m "docs: mark phase 3 client flow progress"
```

Expected: commit succeeds.

- [ ] **Step 8: Push and verify remote**

Run:

```powershell
git push origin main
git rev-parse main
git ls-remote https://github.com/tt88737/game_luck.git refs/heads/main
```

Expected: remote hash matches local `main`.

## Self-Review

Spec coverage:

- Promotion list and claim APIs are covered by Task 1.
- Redemption list and request APIs are covered by Task 2.
- Demo reward seed is covered by Task 3.
- H5 typed API methods are covered by Task 4.
- H5 rewards page integration is covered by Task 5.
- H5 redemption page integration is covered by Task 6.
- Static verification, runtime smoke, docs, push, and remote verification are covered by Task 7.

Scope controls:

- Real KYC is not included.
- Real payout/provider integration is not included.
- Admin approval UI is not changed.
- Flutter/App integration is not included.
- Promotion targeting/rules engine is not included.
