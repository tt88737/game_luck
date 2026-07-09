# Phase 2 Player Client API and H5 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a backend-backed player H5 closed loop for bootstrap, demo login, member profile, wallet balances, wallet ledger, game lobby, and game launch stub.

**Architecture:** Add a narrow `/api/client/**` API layer on top of existing member, wallet, and game modules without changing admin endpoints. H5 gets a small typed API client plus reactive session state and replaces static demo data on home, login, wallet, and games pages. Real payment, KYC, third-party game launch, Flutter integration, and Cocos runtime remain outside this plan.

**Tech Stack:** Spring Boot, Java, MyBatis Plus, JUnit 5, Mockito, Vue3, Vue Router, TypeScript, Vite.

---

## File Structure

Create or modify these files during execution:

```text
backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/
backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/client/
backend/gameluck-common/gameluck-common-core/src/main/java/com/gameluck/common/core/client/
backend/gameluck-common/gameluck-common-core/src/test/java/com/gameluck/common/core/client/
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/
backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/client/
backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/client/
backend/gameluck-modules/gameluck-game/src/test/java/com/gameluck/game/client/
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/mapper/WalletAccountMapper.java
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/mapper/WalletTransactionMapper.java
backend/gameluck-modules/gameluck-wallet/src/main/resources/mapper/wallet/WalletAccountMapper.xml
backend/gameluck-modules/gameluck-wallet/src/main/resources/mapper/wallet/WalletTransactionMapper.xml
backend/gameluck-admin/src/main/resources/i18n/messages.properties
backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties
backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties
backend/script/sql/gameluck_client_demo.sql
h5/src/api/client.ts
h5/src/stores/session.ts
h5/src/types/client.ts
h5/src/App.vue
h5/src/views/HomeView.vue
h5/src/views/LoginView.vue
h5/src/views/WalletView.vue
h5/src/views/GamesView.vue
progress.md
```

Do not modify admin controllers for member, wallet, or game unless a compile error proves a shared type conflict. Client APIs use dedicated VO/BO classes so admin fields are not exposed to H5.

### Task 1: Client Bootstrap API

**Files:**
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/domain/vo/ClientBootstrapVo.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/service/ClientBootstrapService.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/controller/ClientBootstrapController.java`
- Test: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/client/service/ClientBootstrapServiceTest.java`

- [x] **Step 1: Write bootstrap service test**

Create `ClientBootstrapServiceTest.java`:

```java
package com.gameluck.member.client.service;

import com.gameluck.member.client.domain.vo.ClientBootstrapVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientBootstrapServiceTest {

    @Test
    @Tag("local")
    void defaultBootstrapReturnsDemoBrandCurrenciesAndFeatureFlags() {
        ClientBootstrapService service = new ClientBootstrapService();

        ClientBootstrapVo result = service.getBootstrap(null, null);

        assertEquals("000000", result.getTenantId());
        assertEquals("demo", result.getBrandCode());
        assertEquals("h5", result.getChannelCode());
        assertEquals("GameLuck", result.getBrandName());
        assertTrue(result.getFeatures().getWalletEnabled());
        assertTrue(result.getFeatures().getGameEnabled());
        assertFalse(result.getFeatures().getPaymentEnabled());
        assertEquals(2, result.getCurrencies().size());
        assertEquals("GC", result.getCurrencies().get(0).getCurrencyCode());
        assertEquals("SC", result.getCurrencies().get(1).getCurrencyCode());
    }
}
```

- [x] **Step 2: Run test and verify it fails**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=ClientBootstrapServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because `ClientBootstrapService` and `ClientBootstrapVo` do not exist.

- [x] **Step 3: Implement bootstrap VO**

Create `ClientBootstrapVo.java`:

```java
package com.gameluck.member.client.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ClientBootstrapVo {

    private String tenantId;
    private String brandCode;
    private String channelCode;
    private String brandName;
    private Theme theme;
    private Features features;
    private List<Currency> currencies;

    @Data
    public static class Theme {
        private String logoText;
        private String primaryColor;
    }

    @Data
    public static class Features {
        private Boolean walletEnabled;
        private Boolean gameEnabled;
        private Boolean promotionEnabled;
        private Boolean redemptionEnabled;
        private Boolean paymentEnabled;
        private Boolean kycEnabled;
    }

    @Data
    public static class Currency {
        private String currencyCode;
        private String currencyName;
        private Integer decimalScale;
        private Boolean playable;
        private Boolean rechargeable;
        private Boolean withdrawable;
    }
}
```

- [x] **Step 4: Implement bootstrap service**

Create `ClientBootstrapService.java`:

```java
package com.gameluck.member.client.service;

import cn.hutool.core.util.StrUtil;
import com.gameluck.member.client.domain.vo.ClientBootstrapVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientBootstrapService {

    public ClientBootstrapVo getBootstrap(String brandCode, String channelCode) {
        ClientBootstrapVo vo = new ClientBootstrapVo();
        vo.setTenantId("000000");
        vo.setBrandCode(StrUtil.blankToDefault(brandCode, "demo"));
        vo.setChannelCode(StrUtil.blankToDefault(channelCode, "h5"));
        vo.setBrandName("GameLuck");
        vo.setTheme(theme());
        vo.setFeatures(features());
        vo.setCurrencies(List.of(currency("GC", "Gold Coin"), currency("SC", "Sweep Coin")));
        return vo;
    }

    private ClientBootstrapVo.Theme theme() {
        ClientBootstrapVo.Theme theme = new ClientBootstrapVo.Theme();
        theme.setLogoText("GameLuck");
        theme.setPrimaryColor("#1f7a4d");
        return theme;
    }

    private ClientBootstrapVo.Features features() {
        ClientBootstrapVo.Features features = new ClientBootstrapVo.Features();
        features.setWalletEnabled(true);
        features.setGameEnabled(true);
        features.setPromotionEnabled(true);
        features.setRedemptionEnabled(false);
        features.setPaymentEnabled(false);
        features.setKycEnabled(false);
        return features;
    }

    private ClientBootstrapVo.Currency currency(String code, String name) {
        ClientBootstrapVo.Currency currency = new ClientBootstrapVo.Currency();
        currency.setCurrencyCode(code);
        currency.setCurrencyName(name);
        currency.setDecimalScale(2);
        currency.setPlayable(true);
        currency.setRechargeable(false);
        currency.setWithdrawable(false);
        return currency;
    }
}
```

- [x] **Step 5: Implement bootstrap controller**

Create `ClientBootstrapController.java`:

```java
package com.gameluck.member.client.controller;

import com.gameluck.common.core.domain.R;
import com.gameluck.member.client.domain.vo.ClientBootstrapVo;
import com.gameluck.member.client.service.ClientBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/client")
public class ClientBootstrapController {

    private final ClientBootstrapService clientBootstrapService;

    @GetMapping("/bootstrap")
    public R<ClientBootstrapVo> bootstrap(@RequestHeader(value = "X-Brand-Code", required = false) String brandCode,
                                          @RequestHeader(value = "X-Channel-Code", required = false) String channelCode) {
        return R.ok(clientBootstrapService.getBootstrap(brandCode, channelCode));
    }
}
```

- [x] **Step 6: Run bootstrap test and compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=ClientBootstrapServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: both commands pass.

- [x] **Step 7: Commit bootstrap API**

Run:

```powershell
git add backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/client
git commit -m "feat(client): add bootstrap api"
```

Expected: commit succeeds.

### Task 2: Client Demo Auth and Session Context

**Files:**
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/domain/bo/ClientLoginBo.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/domain/vo/ClientMemberVo.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/domain/vo/ClientLoginVo.java`
- Create: `backend/gameluck-common/gameluck-common-core/src/main/java/com/gameluck/common/core/client/ClientTokenService.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/service/ClientAuthService.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/controller/ClientAuthController.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/mapper/MemberProfileMapper.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/resources/mapper/member/MemberProfileMapper.xml`
- Modify: backend message bundles under `backend/gameluck-admin/src/main/resources/i18n/messages*.properties`
- Test: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/client/service/ClientAuthServiceTest.java`

- [ ] **Step 1: Add mapper method declarations**

Modify `MemberProfileMapper.java`:

```java
MemberProfile selectByUsername(@Param("tenantId") String tenantId, @Param("username") String username);

MemberProfile selectClientMember(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);
```

Keep the existing `selectByUsername` method and add only `selectClientMember`.

- [ ] **Step 2: Add mapper SQL**

Modify `MemberProfileMapper.xml`:

```xml
<select id="selectClientMember" resultType="com.gameluck.member.domain.MemberProfile">
    SELECT *
    FROM gl_member_profile
    WHERE tenant_id = #{tenantId}
      AND id = #{memberId}
      AND del_flag = '0'
    LIMIT 1
</select>
```

- [ ] **Step 3: Write auth service tests**

Create `ClientAuthServiceTest.java`:

```java
package com.gameluck.member.client.service;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.member.client.domain.bo.ClientLoginBo;
import com.gameluck.member.client.domain.vo.ClientLoginVo;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAuthServiceTest {

    @Test
    @Tag("local")
    void loginReturnsTokenForDemoMember() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientAuthService service = new ClientAuthService(mapper, tokenService);
        MemberProfile member = member();
        when(mapper.selectByUsername("000000", "demo_player")).thenReturn(member);

        ClientLoginBo bo = new ClientLoginBo();
        bo.setUsername("demo_player");
        bo.setPassword("Demo123456");
        ClientLoginVo result = service.login(bo);

        assertNotNull(result.getAccessToken());
        assertEquals(7200L, result.getExpiresIn());
        assertEquals(1001L, result.getMember().getMemberId());
        assertEquals("demo_player", result.getMember().getUsername());
    }

    @Test
    @Tag("local")
    void loginRejectsWrongPassword() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        ClientAuthService service = new ClientAuthService(mapper, new ClientTokenService());
        when(mapper.selectByUsername("000000", "demo_player")).thenReturn(member());
        ClientLoginBo bo = new ClientLoginBo();
        bo.setUsername("demo_player");
        bo.setPassword("bad");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.login(bo));

        assertEquals("client.auth.invalid.credentials", exception.getMessage());
    }

    @Test
    @Tag("local")
    void tokenCanResolveCurrentMemberId() {
        ClientTokenService tokenService = new ClientTokenService();
        String token = tokenService.issue(1001L);

        assertEquals(1001L, tokenService.requireMemberId("Bearer " + token));
    }

    private MemberProfile member() {
        MemberProfile member = new MemberProfile();
        member.setId(1001L);
        member.setTenantId("000000");
        member.setMemberNo("M1001");
        member.setUsername("demo_player");
        member.setNickname("Demo Player");
        member.setStatus("ACTIVE");
        return member;
    }
}
```

- [ ] **Step 4: Run auth tests and verify failure**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=ClientAuthServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because client auth classes do not exist.

- [ ] **Step 5: Implement auth BO and VO classes**

Create `ClientLoginBo.java`:

```java
package com.gameluck.member.client.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientLoginBo {

    @NotBlank(message = "{client.auth.username.required}")
    private String username;

    @NotBlank(message = "{client.auth.password.required}")
    private String password;
}
```

Create `ClientMemberVo.java`:

```java
package com.gameluck.member.client.domain.vo;

import lombok.Data;

@Data
public class ClientMemberVo {
    private Long memberId;
    private String memberNo;
    private String username;
    private String nickname;
    private String status;
    private String kycStatus;
}
```

Create `ClientLoginVo.java`:

```java
package com.gameluck.member.client.domain.vo;

import lombok.Data;

@Data
public class ClientLoginVo {
    private String accessToken;
    private Long expiresIn;
    private ClientMemberVo member;
}
```

- [ ] **Step 6: Implement token and auth services**

Create `backend/gameluck-common/gameluck-common-core/src/main/java/com/gameluck/common/core/client/ClientTokenService.java`:

```java
package com.gameluck.common.core.client;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class ClientTokenService {

    private static final String PREFIX = "client:";
    private static final long EXPIRES_IN = 7200L;

    public String issue(Long memberId) {
        String raw = PREFIX + memberId + ":" + (Instant.now().getEpochSecond() + EXPIRES_IN);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Long requireMemberId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new ServiceException(MessageUtils.message("client.auth.required"));
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != 3 || !PREFIX.substring(0, PREFIX.length() - 1).equals(parts[0])) {
                throw new IllegalArgumentException("invalid token");
            }
            long expiresAt = Long.parseLong(parts[2]);
            if (expiresAt < Instant.now().getEpochSecond()) {
                throw new ServiceException(MessageUtils.message("client.auth.expired"));
            }
            return Long.parseLong(parts[1]);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(MessageUtils.message("client.auth.required"));
        }
    }

    public Long expiresIn() {
        return EXPIRES_IN;
    }
}
```

Create `ClientAuthService.java`:

```java
package com.gameluck.member.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.member.client.domain.bo.ClientLoginBo;
import com.gameluck.member.client.domain.vo.ClientLoginVo;
import com.gameluck.member.client.domain.vo.ClientMemberVo;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ClientAuthService {

    private static final String TENANT_ID = "000000";
    private static final String DEMO_PASSWORD = "Demo123456";

    private final MemberProfileMapper memberProfileMapper;
    private final ClientTokenService clientTokenService;

    public ClientLoginVo login(ClientLoginBo bo) {
        MemberProfile member = memberProfileMapper.selectByUsername(TENANT_ID, bo.getUsername());
        if (member == null || !DEMO_PASSWORD.equals(bo.getPassword())) {
            throw new ServiceException(MessageUtils.message("client.auth.invalid.credentials"));
        }
        ClientLoginVo vo = new ClientLoginVo();
        vo.setAccessToken(clientTokenService.issue(member.getId()));
        vo.setExpiresIn(clientTokenService.expiresIn());
        vo.setMember(toClientMember(member));
        return vo;
    }

    public ClientMemberVo currentMember(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        MemberProfile member = memberProfileMapper.selectClientMember(TENANT_ID, memberId);
        if (member == null) {
            throw new ServiceException(MessageUtils.message("client.auth.member.not.exists"));
        }
        return toClientMember(member);
    }

    private ClientMemberVo toClientMember(MemberProfile member) {
        ClientMemberVo vo = new ClientMemberVo();
        vo.setMemberId(member.getId());
        vo.setMemberNo(member.getMemberNo());
        vo.setUsername(member.getUsername());
        vo.setNickname(member.getNickname());
        vo.setStatus(member.getStatus());
        vo.setKycStatus("not_required");
        return vo;
    }
}
```

- [ ] **Step 7: Implement auth controller**

Create `ClientAuthController.java`:

```java
package com.gameluck.member.client.controller;

import com.gameluck.common.core.domain.R;
import com.gameluck.member.client.domain.bo.ClientLoginBo;
import com.gameluck.member.client.domain.vo.ClientLoginVo;
import com.gameluck.member.client.domain.vo.ClientMemberVo;
import com.gameluck.member.client.service.ClientAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/client")
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    @PostMapping("/auth/login")
    public R<ClientLoginVo> login(@Valid @RequestBody ClientLoginBo bo) {
        return R.ok(clientAuthService.login(bo));
    }

    @GetMapping("/member/me")
    public R<ClientMemberVo> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientAuthService.currentMember(authorization));
    }
}
```

- [ ] **Step 8: Add backend i18n keys**

Add these keys to `backend/gameluck-admin/src/main/resources/i18n/messages.properties`:

```properties
client.auth.username.required=用户名不能为空
client.auth.password.required=密码不能为空
client.auth.invalid.credentials=用户名或密码错误
client.auth.required=请先登录
client.auth.expired=登录已过期
client.auth.member.not.exists=会员不存在
game.provider.required=游戏供应商不能为空
game.code.required=游戏编号不能为空
```

Add these keys to `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`:

```properties
client.auth.username.required=用户名不能为空
client.auth.password.required=密码不能为空
client.auth.invalid.credentials=用户名或密码错误
client.auth.required=请先登录
client.auth.expired=登录已过期
client.auth.member.not.exists=会员不存在
game.provider.required=游戏供应商不能为空
game.code.required=游戏编号不能为空
```

Add these keys to `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`:

```properties
client.auth.username.required=Username is required.
client.auth.password.required=Password is required.
client.auth.invalid.credentials=Invalid username or password.
client.auth.required=Login is required.
client.auth.expired=Login has expired.
client.auth.member.not.exists=Member does not exist.
game.provider.required=Game provider is required.
game.code.required=Game code is required.
```

- [ ] **Step 9: Run auth verification**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=ClientAuthServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir admin-ui check:i18n
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: all commands pass.

- [ ] **Step 10: Commit auth API**

Run:

```powershell
git add backend/gameluck-common/gameluck-common-core/src/main/java/com/gameluck/common/core/client backend/gameluck-modules/gameluck-member backend/gameluck-admin/src/main/resources/i18n/messages.properties backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties
git commit -m "feat(client): add demo member auth"
```

Expected: commit succeeds.

### Task 3: Client Wallet Read APIs

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/domain/vo/ClientWalletAccountVo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/domain/vo/ClientWalletLedgerVo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/domain/vo/ClientPageVo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/service/ClientWalletService.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/controller/ClientWalletController.java`
- Modify: `WalletAccountMapper.java`, `WalletTransactionMapper.java`, `WalletAccountMapper.xml`, `WalletTransactionMapper.xml`
- Test: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/client/service/ClientWalletServiceTest.java`

- [ ] **Step 1: Add wallet mapper methods**

Modify `WalletAccountMapper.java`:

```java
List<WalletAccount> selectClientAccounts(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);
```

Modify `WalletTransactionMapper.java`:

```java
List<WalletTransaction> selectClientLedgers(@Param("tenantId") String tenantId,
                                            @Param("memberId") Long memberId,
                                            @Param("currencyCode") String currencyCode,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

Long countClientLedgers(@Param("tenantId") String tenantId,
                        @Param("memberId") Long memberId,
                        @Param("currencyCode") String currencyCode);
```

- [ ] **Step 2: Add wallet mapper SQL**

Add to `WalletAccountMapper.xml`:

```xml
<select id="selectClientAccounts" resultType="com.gameluck.wallet.domain.WalletAccount">
    select *
    from gl_wallet_account
    where tenant_id = #{tenantId}
      and member_id = #{memberId}
      and del_flag = '0'
    order by currency_code asc
</select>
```

Add to `WalletTransactionMapper.xml`:

```xml
<select id="selectClientLedgers" resultType="com.gameluck.wallet.domain.WalletTransaction">
    select *
    from gl_wallet_transaction
    where tenant_id = #{tenantId}
      and member_id = #{memberId}
      <if test="currencyCode != null and currencyCode != ''">
        and currency_code = #{currencyCode}
      </if>
    order by create_time desc, id desc
    limit #{offset}, #{pageSize}
</select>

<select id="countClientLedgers" resultType="java.lang.Long">
    select count(1)
    from gl_wallet_transaction
    where tenant_id = #{tenantId}
      and member_id = #{memberId}
      <if test="currencyCode != null and currencyCode != ''">
        and currency_code = #{currencyCode}
      </if>
</select>
```

- [ ] **Step 3: Write wallet service tests**

Create `ClientWalletServiceTest.java`:

```java
package com.gameluck.wallet.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientWalletServiceTest {

    @Test
    @Tag("local")
    void accountsAreReadForCurrentMemberOnly() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, tokenService);
        WalletAccount account = new WalletAccount();
        account.setCurrencyCode("GC");
        account.setAvailableBalance(new BigDecimal("1000.00"));
        account.setFrozenBalance(BigDecimal.ZERO);
        account.setStatus("NORMAL");
        when(accountMapper.selectClientAccounts("000000", 1001L)).thenReturn(List.of(account));

        List<ClientWalletAccountVo> result = service.accounts("Bearer " + tokenService.issue(1001L));

        assertEquals(1, result.size());
        assertEquals("GC", result.get(0).getCurrencyCode());
        assertEquals("1000.00", result.get(0).getAvailableBalance());
    }

    @Test
    @Tag("local")
    void ledgerPageSizeIsCappedAtFifty() {
        WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
        WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientWalletService service = new ClientWalletService(accountMapper, transactionMapper, tokenService);
        WalletTransaction tx = new WalletTransaction();
        tx.setId(9001L);
        tx.setCurrencyCode("GC");
        tx.setOperation("credit");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setBalanceAfter(new BigDecimal("1010.00"));
        tx.setSourceType("demo_seed");
        tx.setCreateTime(new Date());
        when(transactionMapper.selectClientLedgers("000000", 1001L, "GC", 0, 50)).thenReturn(List.of(tx));
        when(transactionMapper.countClientLedgers("000000", 1001L, "GC")).thenReturn(1L);

        ClientPageVo<ClientWalletLedgerVo> result = service.ledgers("Bearer " + tokenService.issue(1001L), "GC", 1, 500);

        assertEquals(1L, result.getTotal());
        assertEquals("credit", result.getRecords().get(0).getDirection());
    }
}
```

- [ ] **Step 4: Run wallet tests and verify failure**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am -Plocal -DskipTests=false "-Dtest=ClientWalletServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because client wallet classes and mapper methods do not exist.

- [ ] **Step 5: Implement wallet client VO classes**

Create `ClientWalletAccountVo.java`:

```java
package com.gameluck.wallet.client.domain.vo;

import lombok.Data;

@Data
public class ClientWalletAccountVo {
    private String currencyCode;
    private String currencyName;
    private String availableBalance;
    private String frozenBalance;
    private Integer decimalScale;
    private Boolean playable;
    private Boolean withdrawable;
}
```

Create `ClientWalletLedgerVo.java`:

```java
package com.gameluck.wallet.client.domain.vo;

import lombok.Data;

@Data
public class ClientWalletLedgerVo {
    private Long ledgerId;
    private String currencyCode;
    private String direction;
    private String amount;
    private String afterAvailable;
    private String bizType;
    private String createdAt;
}
```

Create `ClientPageVo.java`:

```java
package com.gameluck.wallet.client.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ClientPageVo<T> {
    private List<T> records;
    private Long total;
}
```

- [ ] **Step 6: Implement wallet service and controller**

Create `ClientWalletService.java`:

```java
package com.gameluck.wallet.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientWalletService {

    private static final String TENANT_ID = "000000";

    private final WalletAccountMapper walletAccountMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final ClientTokenService clientTokenService;

    public List<ClientWalletAccountVo> accounts(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        return walletAccountMapper.selectClientAccounts(TENANT_ID, memberId).stream().map(this::toAccount).toList();
    }

    public ClientPageVo<ClientWalletLedgerVo> ledgers(String authorization, String currencyCode, Integer pageNum, Integer pageSize) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        int offset = (safePageNum - 1) * safePageSize;
        ClientPageVo<ClientWalletLedgerVo> page = new ClientPageVo<>();
        page.setRecords(walletTransactionMapper.selectClientLedgers(TENANT_ID, memberId, currencyCode, offset, safePageSize).stream().map(this::toLedger).toList());
        page.setTotal(walletTransactionMapper.countClientLedgers(TENANT_ID, memberId, currencyCode));
        return page;
    }

    private ClientWalletAccountVo toAccount(WalletAccount account) {
        ClientWalletAccountVo vo = new ClientWalletAccountVo();
        vo.setCurrencyCode(account.getCurrencyCode());
        vo.setCurrencyName("GC".equals(account.getCurrencyCode()) ? "Gold Coin" : "Sweep Coin");
        vo.setAvailableBalance(account.getAvailableBalance().setScale(2).toPlainString());
        vo.setFrozenBalance(account.getFrozenBalance().setScale(2).toPlainString());
        vo.setDecimalScale(2);
        vo.setPlayable(true);
        vo.setWithdrawable(false);
        return vo;
    }

    private ClientWalletLedgerVo toLedger(WalletTransaction transaction) {
        ClientWalletLedgerVo vo = new ClientWalletLedgerVo();
        vo.setLedgerId(transaction.getId());
        vo.setCurrencyCode(transaction.getCurrencyCode());
        vo.setDirection(transaction.getOperation());
        vo.setAmount(transaction.getAmount().setScale(2).toPlainString());
        vo.setAfterAvailable(transaction.getBalanceAfter().setScale(2).toPlainString());
        vo.setBizType(transaction.getSourceType());
        vo.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(transaction.getCreateTime()));
        return vo;
    }
}
```

Create `ClientWalletController.java`:

```java
package com.gameluck.wallet.client.controller;

import com.gameluck.common.core.domain.R;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.client.service.ClientWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/client/wallet")
public class ClientWalletController {

    private final ClientWalletService clientWalletService;

    @GetMapping("/accounts")
    public R<List<ClientWalletAccountVo>> accounts(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientWalletService.accounts(authorization));
    }

    @GetMapping("/ledgers")
    public R<ClientPageVo<ClientWalletLedgerVo>> ledgers(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestParam(required = false) String currencyCode,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(clientWalletService.ledgers(authorization, currencyCode, pageNum, pageSize));
    }
}
```

- [ ] **Step 7: Run wallet verification**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am -Plocal -DskipTests=false "-Dtest=ClientWalletServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: both commands pass.

- [ ] **Step 8: Commit wallet APIs**

Run:

```powershell
git add backend/gameluck-modules/gameluck-wallet
git commit -m "feat(client): add wallet read apis"
```

Expected: commit succeeds.

### Task 4: Client Game Lobby and Launch Stub

**Files:**
- Create: `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/client/domain/bo/ClientGameLaunchBo.java`
- Create: `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/client/domain/vo/ClientGameVo.java`
- Create: `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/client/domain/vo/ClientGameLaunchVo.java`
- Create: `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/client/service/ClientGameService.java`
- Create: `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/client/controller/ClientGameController.java`
- Test: `backend/gameluck-modules/gameluck-game/src/test/java/com/gameluck/game/client/service/ClientGameServiceTest.java`

- [ ] **Step 1: Write game client tests**

Create `ClientGameServiceTest.java`:

```java
package com.gameluck.game.client.service;

import com.gameluck.game.client.domain.bo.ClientGameLaunchBo;
import com.gameluck.game.client.domain.vo.ClientGameLaunchVo;
import com.gameluck.game.client.domain.vo.ClientGameVo;
import com.gameluck.common.core.client.ClientTokenService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientGameServiceTest {

    @Test
    @Tag("local")
    void gamesReturnsMockGameForPlayableCurrency() {
        ClientGameService service = new ClientGameService(new ClientTokenService());

        List<ClientGameVo> result = service.games("GC");

        assertEquals(1, result.size());
        assertEquals("mock", result.get(0).getProviderCode());
        assertTrue(result.get(0).getSupportedCurrencies().contains("GC"));
    }

    @Test
    @Tag("local")
    void launchReturnsStubWithoutWalletDebit() {
        ClientTokenService tokenService = new ClientTokenService();
        ClientGameService service = new ClientGameService(tokenService);
        ClientGameLaunchBo bo = new ClientGameLaunchBo();
        bo.setProviderCode("mock");
        bo.setGameCode("mock-slot-001");
        bo.setCurrencyCode("GC");

        ClientGameLaunchVo result = service.launch("Bearer " + tokenService.issue(1001L), bo);

        assertEquals("stub", result.getLaunchMode());
        assertEquals("", result.getLaunchUrl());
        assertTrue(result.getSessionNo().startsWith("GS"));
    }
}
```

- [ ] **Step 2: Run game tests and verify failure**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-game -am -Plocal -DskipTests=false "-Dtest=ClientGameServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because client game classes do not exist.

- [ ] **Step 3: Implement game BO and VO classes**

Create `ClientGameLaunchBo.java`:

```java
package com.gameluck.game.client.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientGameLaunchBo {
    @NotBlank(message = "{game.provider.required}")
    private String providerCode;

    @NotBlank(message = "{game.code.required}")
    private String gameCode;

    @NotBlank(message = "{wallet.currency.required}")
    private String currencyCode;
}
```

Create `ClientGameVo.java`:

```java
package com.gameluck.game.client.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ClientGameVo {
    private String providerCode;
    private String gameCode;
    private String gameName;
    private String status;
    private List<String> supportedCurrencies;
    private String thumbnailUrl;
    private Boolean maintenance;
}
```

Create `ClientGameLaunchVo.java`:

```java
package com.gameluck.game.client.domain.vo;

import lombok.Data;

@Data
public class ClientGameLaunchVo {
    private String sessionNo;
    private String launchMode;
    private String launchUrl;
    private String message;
}
```

- [ ] **Step 4: Implement game service and controller**

Create `ClientGameService.java`:

```java
package com.gameluck.game.client.service;

import com.gameluck.game.client.domain.bo.ClientGameLaunchBo;
import com.gameluck.game.client.domain.vo.ClientGameLaunchVo;
import com.gameluck.game.client.domain.vo.ClientGameVo;
import com.gameluck.common.core.client.ClientTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientGameService {

    private final ClientTokenService clientTokenService;

    public List<ClientGameVo> games(String currencyCode) {
        if (currencyCode != null && !currencyCode.isBlank() && !"GC".equals(currencyCode) && !"SC".equals(currencyCode)) {
            return List.of();
        }
        ClientGameVo game = new ClientGameVo();
        game.setProviderCode("mock");
        game.setGameCode("mock-slot-001");
        game.setGameName("Mock Slot");
        game.setStatus("enabled");
        game.setSupportedCurrencies(List.of("GC", "SC"));
        game.setThumbnailUrl("");
        game.setMaintenance(false);
        return List.of(game);
    }

    public ClientGameLaunchVo launch(String authorization, ClientGameLaunchBo bo) {
        clientTokenService.requireMemberId(authorization);
        ClientGameLaunchVo vo = new ClientGameLaunchVo();
        vo.setSessionNo("GS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        vo.setLaunchMode("stub");
        vo.setLaunchUrl("");
        vo.setMessage("Game launch is not live yet.");
        return vo;
    }
}
```

Create `ClientGameController.java`:

```java
package com.gameluck.game.client.controller;

import com.gameluck.common.core.domain.R;
import com.gameluck.game.client.domain.bo.ClientGameLaunchBo;
import com.gameluck.game.client.domain.vo.ClientGameLaunchVo;
import com.gameluck.game.client.domain.vo.ClientGameVo;
import com.gameluck.game.client.service.ClientGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/client/games")
public class ClientGameController {

    private final ClientGameService clientGameService;

    @GetMapping
    public R<List<ClientGameVo>> games(@RequestParam(required = false) String currencyCode) {
        return R.ok(clientGameService.games(currencyCode));
    }

    @PostMapping("/launch")
    public R<ClientGameLaunchVo> launch(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @Valid @RequestBody ClientGameLaunchBo bo) {
        return R.ok(clientGameService.launch(authorization, bo));
    }
}
```

- [ ] **Step 5: Run game verification**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-game -am -Plocal -DskipTests=false "-Dtest=ClientGameServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: both commands pass.

- [ ] **Step 6: Commit game APIs**

Run:

```powershell
git add backend/gameluck-modules/gameluck-game
git commit -m "feat(client): add game lobby apis"
```

Expected: commit succeeds.

### Task 5: Demo Client Seed SQL

**Files:**
- Create: `backend/script/sql/gameluck_client_demo.sql`
- Modify: `progress.md`

- [ ] **Step 1: Create idempotent demo seed SQL**

Create `gameluck_client_demo.sql`:

```sql
DELETE FROM gl_wallet_transaction WHERE tenant_id = '000000' AND member_id = 1001 AND source_type = 'demo_seed';
DELETE FROM gl_wallet_account WHERE tenant_id = '000000' AND member_id = 1001;
DELETE FROM gl_member_profile WHERE tenant_id = '000000' AND id = 1001;

INSERT INTO gl_member_profile (
  id, tenant_id, member_no, username, nickname, status, risk_level, register_channel,
  version, del_flag, create_time, update_time
) VALUES (
  1001, '000000', 'M1001', 'demo_player', 'Demo Player', 'ACTIVE', 'NORMAL', 'h5',
  0, '0', NOW(), NOW()
);

INSERT INTO gl_wallet_account (
  id, tenant_id, member_id, currency_code, available_balance, frozen_balance, status,
  version, del_flag, create_time, update_time
) VALUES
  (11001, '000000', 1001, 'GC', 1000.000000, 0.000000, 'NORMAL', 0, '0', NOW(), NOW()),
  (11002, '000000', 1001, 'SC', 25.000000, 0.000000, 'NORMAL', 0, '0', NOW(), NOW());

INSERT INTO gl_wallet_transaction (
  id, tenant_id, transaction_no, idempotency_key, member_id, currency_code, operation,
  source_type, business_no, amount, balance_before, balance_after, frozen_before,
  frozen_after, status, remark, create_time, update_time
) VALUES
  (12001, '000000', 'WT-DEMO-GC-INIT', 'demo:1001:gc:init', 1001, 'GC', 'credit',
   'demo_seed', 'DEMO-GC-INIT', 1000.000000, 0.000000, 1000.000000, 0.000000,
   0.000000, 'SUCCESS', 'Demo GC seed', NOW(), NOW()),
  (12002, '000000', 'WT-DEMO-SC-INIT', 'demo:1001:sc:init', 1001, 'SC', 'credit',
   'demo_seed', 'DEMO-SC-INIT', 25.000000, 0.000000, 25.000000, 0.000000,
   0.000000, 'SUCCESS', 'Demo SC seed', NOW(), NOW());
```

- [ ] **Step 2: Verify SQL references existing tables**

Run:

```powershell
Select-String -Path backend\script\sql\gameluck_wallet.sql -Pattern "gl_member_profile|gl_wallet_account|gl_wallet_transaction"
Get-Content backend\script\sql\gameluck_client_demo.sql
```

Expected: existing schema references are visible and seed SQL is readable.

- [ ] **Step 3: Import demo seed locally**

Run:

```powershell
mysql -uroot -proot gameluck_vue < backend\script\sql\gameluck_client_demo.sql
```

Expected: import succeeds. If local MySQL password differs, use the password documented in `docs/implementation/backend-local-startup.md`.

- [ ] **Step 4: Verify seed data**

Run:

```powershell
mysql -uroot -proot gameluck_vue -e "SELECT id, username, status FROM gl_member_profile WHERE id = 1001; SELECT member_id, currency_code, available_balance FROM gl_wallet_account WHERE member_id = 1001;"
```

Expected: `demo_player` exists and has GC plus SC wallet accounts.

- [ ] **Step 5: Commit demo seed**

Run:

```powershell
git add backend/script/sql/gameluck_client_demo.sql progress.md
git commit -m "chore(client): add demo seed data"
```

Expected: commit succeeds.

### Task 6: H5 API Client and Session Store

**Files:**
- Create: `h5/src/types/client.ts`
- Create: `h5/src/api/client.ts`
- Create: `h5/src/stores/session.ts`
- Modify: `h5/src/App.vue`

- [ ] **Step 1: Create client API types**

Create `h5/src/types/client.ts`:

```ts
export interface ApiResponse<T> {
  code: number
  msg?: string
  message?: string
  data: T
}

export interface ClientCurrency {
  currencyCode: string
  currencyName: string
  decimalScale: number
  playable: boolean
  rechargeable: boolean
  withdrawable: boolean
}

export interface ClientBootstrap {
  tenantId: string
  brandCode: string
  channelCode: string
  brandName: string
  theme: {
    logoText: string
    primaryColor: string
  }
  features: {
    walletEnabled: boolean
    gameEnabled: boolean
    promotionEnabled: boolean
    redemptionEnabled: boolean
    paymentEnabled: boolean
    kycEnabled: boolean
  }
  currencies: ClientCurrency[]
}

export interface ClientMember {
  memberId: number
  memberNo: string
  username: string
  nickname: string
  status: string
  kycStatus: string
}

export interface ClientLoginResponse {
  accessToken: string
  expiresIn: number
  member: ClientMember
}

export interface WalletAccount {
  currencyCode: string
  currencyName: string
  availableBalance: string
  frozenBalance: string
  decimalScale: number
  playable: boolean
  withdrawable: boolean
}

export interface WalletLedger {
  ledgerId: number
  currencyCode: string
  direction: string
  amount: string
  afterAvailable: string
  bizType: string
  createdAt: string
}

export interface ClientPage<T> {
  records: T[]
  total: number
}

export interface ClientGame {
  providerCode: string
  gameCode: string
  gameName: string
  status: string
  supportedCurrencies: string[]
  thumbnailUrl: string
  maintenance: boolean
}

export interface ClientGameLaunch {
  sessionNo: string
  launchMode: string
  launchUrl: string
  message: string
}
```

- [ ] **Step 2: Create API client**

Create `h5/src/api/client.ts`:

```ts
import type {
  ApiResponse,
  ClientBootstrap,
  ClientGame,
  ClientGameLaunch,
  ClientLoginResponse,
  ClientMember,
  ClientPage,
  WalletAccount,
  WalletLedger,
} from '../types/client'

const API_BASE = import.meta.env.VITE_API_BASE || ''
const TOKEN_KEY = 'gameluck.client.token'

export function getClientToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setClientToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearClientToken() {
  localStorage.removeItem(TOKEN_KEY)
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getClientToken()
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('X-Channel-Code', 'h5')
  headers.set('X-Brand-Code', 'demo')
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (response.status === 401) {
    clearClientToken()
    throw new Error('Login required')
  }
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }
  const payload = (await response.json()) as ApiResponse<T>
  if (payload.code !== 200 && payload.code !== 0) {
    throw new Error(payload.msg || payload.message || 'Request failed')
  }
  return payload.data
}

export const clientApi = {
  bootstrap: () => request<ClientBootstrap>('/api/client/bootstrap'),
  login: (username: string, password: string) =>
    request<ClientLoginResponse>('/api/client/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  me: () => request<ClientMember>('/api/client/member/me'),
  walletAccounts: () => request<WalletAccount[]>('/api/client/wallet/accounts'),
  walletLedgers: (currencyCode = 'GC') =>
    request<ClientPage<WalletLedger>>(`/api/client/wallet/ledgers?currencyCode=${encodeURIComponent(currencyCode)}&pageNum=1&pageSize=20`),
  games: (currencyCode = 'GC') => request<ClientGame[]>(`/api/client/games?currencyCode=${encodeURIComponent(currencyCode)}`),
  launchGame: (providerCode: string, gameCode: string, currencyCode: string) =>
    request<ClientGameLaunch>('/api/client/games/launch', {
      method: 'POST',
      body: JSON.stringify({ providerCode, gameCode, currencyCode }),
    }),
}
```

- [ ] **Step 3: Create session store**

Create `h5/src/stores/session.ts`:

```ts
import { reactive } from 'vue'
import { clientApi, clearClientToken, getClientToken, setClientToken } from '../api/client'
import type { ClientBootstrap, ClientMember } from '../types/client'

export const sessionState = reactive({
  bootstrap: null as ClientBootstrap | null,
  member: null as ClientMember | null,
  loading: false,
  error: '',
})

export const isLoggedIn = () => Boolean(sessionState.member)

export async function loadBootstrap() {
  sessionState.bootstrap = await clientApi.bootstrap()
}

export async function restoreSession() {
  if (!getClientToken()) {
    return
  }
  try {
    sessionState.member = await clientApi.me()
  } catch (error) {
    sessionState.member = null
    clearClientToken()
  }
}

export async function login(username: string, password: string) {
  sessionState.loading = true
  sessionState.error = ''
  try {
    const result = await clientApi.login(username, password)
    setClientToken(result.accessToken)
    sessionState.member = result.member
    return result.member
  } catch (error) {
    sessionState.error = error instanceof Error ? error.message : 'Login failed'
    throw error
  } finally {
    sessionState.loading = false
  }
}

export function logout() {
  clearClientToken()
  sessionState.member = null
}
```

- [ ] **Step 4: Update app shell for session-aware navigation**

Modify `h5/src/App.vue` so it imports session state and loads bootstrap/session on mount:

```vue
<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { loadBootstrap, logout, restoreSession, sessionState } from './stores/session'

onMounted(async () => {
  await loadBootstrap()
  await restoreSession()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">GL</span>
        <span>{{ sessionState.bootstrap?.brandName || 'GameLuck' }}</span>
      </RouterLink>
      <nav class="topnav" aria-label="Primary">
        <RouterLink to="/wallet">Wallet</RouterLink>
        <RouterLink to="/games">Games</RouterLink>
        <RouterLink to="/promotions">Rewards</RouterLink>
        <RouterLink to="/redemptions">Redeem</RouterLink>
      </nav>
      <div v-if="sessionState.member" class="login-link session-chip">
        <span>{{ sessionState.member.nickname || sessionState.member.username }}</span>
        <button type="button" @click="logout">Logout</button>
      </div>
      <RouterLink v-else class="login-link" to="/login">Login</RouterLink>
    </header>

    <main class="page-wrap">
      <RouterView />
    </main>

    <footer class="tabbar" aria-label="Mobile navigation">
      <RouterLink to="/">Home</RouterLink>
      <RouterLink to="/wallet">Wallet</RouterLink>
      <RouterLink to="/games">Games</RouterLink>
      <RouterLink to="/help">Help</RouterLink>
    </footer>
  </div>
</template>
```

Add CSS for `.session-chip button` in `h5/src/style.css`:

```css
.session-chip {
  gap: 8px;
}

.session-chip button {
  border: 0;
  background: transparent;
  color: var(--brand-strong);
  cursor: pointer;
  font-weight: 760;
}
```

- [ ] **Step 5: Run H5 build**

Run:

```powershell
npm run build --prefix h5
```

Expected: build succeeds.

- [ ] **Step 6: Commit H5 API foundation**

Run:

```powershell
git add h5/src/api h5/src/stores h5/src/types h5/src/App.vue h5/src/style.css
git commit -m "feat(h5): add client api session shell"
```

Expected: commit succeeds.

### Task 7: H5 Page Integration

**Files:**
- Modify: `h5/src/views/HomeView.vue`
- Modify: `h5/src/views/LoginView.vue`
- Modify: `h5/src/views/WalletView.vue`
- Modify: `h5/src/views/GamesView.vue`
- Modify: `h5/src/style.css`

- [ ] **Step 1: Wire home page to bootstrap and session state**

Replace demo imports in `HomeView.vue` with `sessionState` and render bootstrap currencies/features:

```vue
<script setup lang="ts">
import { sessionState } from '../stores/session'
</script>

<template>
  <section class="hero-panel">
    <div class="hero-copy">
      <p class="eyebrow">Player account</p>
      <h1>Wallet, games, rewards, and redemption in one entry.</h1>
      <p class="hero-text">
        {{ sessionState.bootstrap?.brandName || 'GameLuck' }} is running with backend client API data.
      </p>
      <div class="hero-actions">
        <RouterLink class="btn primary" :to="sessionState.member ? '/wallet' : '/login'">
          {{ sessionState.member ? 'Open wallet' : 'Login' }}
        </RouterLink>
        <RouterLink class="btn secondary" to="/games">Games</RouterLink>
      </div>
    </div>
    <div class="balance-board" aria-label="Client bootstrap summary">
      <div class="state-row">
        <span>Account</span>
        <strong>{{ sessionState.member ? 'Logged in' : 'Logged out' }}</strong>
      </div>
      <div v-for="currency in sessionState.bootstrap?.currencies || []" :key="currency.currencyCode" class="balance-row">
        <span>{{ currency.currencyCode }}</span>
        <strong>{{ currency.playable ? 'Playable' : 'Disabled' }}</strong>
      </div>
    </div>
  </section>

  <section class="quick-grid">
    <RouterLink class="feature-tile" to="/wallet">
      <span class="tile-icon">W</span>
      <strong>Wallet</strong>
      <small>{{ sessionState.bootstrap?.features.walletEnabled ? 'Balances available' : 'Wallet disabled' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/games">
      <span class="tile-icon">G</span>
      <strong>Games</strong>
      <small>{{ sessionState.bootstrap?.features.gameEnabled ? 'Mock lobby available' : 'Games disabled' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/promotions">
      <span class="tile-icon">R</span>
      <strong>Rewards</strong>
      <small>{{ sessionState.bootstrap?.features.promotionEnabled ? 'Reward entry visible' : 'Rewards disabled' }}</small>
    </RouterLink>
    <RouterLink class="feature-tile" to="/redemptions">
      <span class="tile-icon">D</span>
      <strong>Redeem</strong>
      <small>{{ sessionState.bootstrap?.features.redemptionEnabled ? 'Redeem enabled' : 'Redeem not live' }}</small>
    </RouterLink>
  </section>
</template>
```

- [ ] **Step 2: Wire login page**

Replace `LoginView.vue` with:

```vue
<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, sessionState } from '../stores/session'

const router = useRouter()
const form = reactive({
  username: 'demo_player',
  password: 'Demo123456',
})

async function submit() {
  await login(form.username, form.password)
  await router.push('/wallet')
}
</script>

<template>
  <section class="form-screen">
    <div>
      <p class="eyebrow">Player login</p>
      <h1>Sign in before wallet changes.</h1>
      <p class="muted">Use the seeded demo player to verify the client API loop.</p>
    </div>

    <form class="panel-form" @submit.prevent="submit">
      <label>
        Username
        <input v-model="form.username" autocomplete="username" />
      </label>
      <label>
        Password
        <input v-model="form.password" type="password" autocomplete="current-password" />
      </label>
      <p v-if="sessionState.error" class="error-text">{{ sessionState.error }}</p>
      <button class="btn primary" type="submit" :disabled="sessionState.loading">
        {{ sessionState.loading ? 'Signing in' : 'Login' }}
      </button>
      <RouterLink class="text-link" to="/register">Create player account</RouterLink>
    </form>
  </section>
</template>
```

- [ ] **Step 3: Wire wallet page**

Replace `WalletView.vue` with:

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { WalletAccount, WalletLedger } from '../types/client'

const accounts = ref<WalletAccount[]>([])
const ledgers = ref<WalletLedger[]>([])
const loading = ref(false)
const error = ref('')

async function loadWallet() {
  if (!sessionState.member) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    accounts.value = await clientApi.walletAccounts()
    const currency = accounts.value[0]?.currencyCode || 'GC'
    ledgers.value = (await clientApi.walletLedgers(currency)).records
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Wallet load failed'
  } finally {
    loading.value = false
  }
}

onMounted(loadWallet)
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">Wallet</p>
    <h1>Balances and recent ledger</h1>
    <p class="muted">Balances are loaded from the backend client wallet API.</p>
  </section>

  <section v-if="!sessionState.member" class="empty-state">
    <strong>Login required</strong>
    <RouterLink class="btn primary" to="/login">Login</RouterLink>
  </section>

  <template v-else>
    <p v-if="loading" class="muted">Loading wallet...</p>
    <p v-if="error" class="error-text">{{ error }}</p>

    <section class="data-grid">
      <article v-for="account in accounts" :key="account.currencyCode" class="metric-card">
        <span>{{ account.currencyName }}</span>
        <strong>{{ account.availableBalance }}</strong>
        <small>Locked: {{ account.frozenBalance }} | {{ account.playable ? 'Playable' : 'Disabled' }}</small>
      </article>
    </section>

    <section class="table-panel">
      <h2>Recent ledger</h2>
      <div v-if="ledgers.length" class="table-list">
        <div v-for="row in ledgers" :key="row.ledgerId" class="table-row">
          <span>{{ row.createdAt }}</span>
          <strong>{{ row.direction }}</strong>
          <span>{{ row.amount }}</span>
          <em>{{ row.bizType }}</em>
        </div>
      </div>
      <p v-else class="muted">No ledger records.</p>
    </section>
  </template>
</template>
```

- [ ] **Step 4: Wire games page**

Replace `GamesView.vue` with:

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { clientApi } from '../api/client'
import { sessionState } from '../stores/session'
import type { ClientGame } from '../types/client'

const games = ref<ClientGame[]>([])
const launchMessage = ref('')
const error = ref('')

async function loadGames() {
  try {
    games.value = await clientApi.games('GC')
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Game lobby load failed'
  }
}

async function launch(game: ClientGame) {
  if (!sessionState.member) {
    error.value = 'Login required'
    return
  }
  const result = await clientApi.launchGame(game.providerCode, game.gameCode, 'GC')
  launchMessage.value = `${result.sessionNo}: ${result.message}`
}

onMounted(loadGames)
</script>

<template>
  <section class="page-heading">
    <p class="eyebrow">Games</p>
    <h1>Simulated game lobby</h1>
    <p class="muted">The lobby is loaded from the backend client game API.</p>
  </section>

  <p v-if="error" class="error-text">{{ error }}</p>
  <p v-if="launchMessage" class="success-text">{{ launchMessage }}</p>

  <section class="item-list">
    <article v-for="game in games" :key="game.gameCode" class="list-card">
      <div>
        <small>{{ game.providerCode }} | {{ game.supportedCurrencies.join(', ') }}</small>
        <h2>{{ game.gameName }}</h2>
        <p>{{ game.maintenance ? 'Maintenance' : 'Available' }}</p>
      </div>
      <button class="btn compact" :disabled="game.maintenance" @click="launch(game)">Launch</button>
    </article>
  </section>
</template>
```

- [ ] **Step 5: Add missing state styles**

Add to `h5/src/style.css`:

```css
.empty-state {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

.error-text {
  color: #a83232;
  font-weight: 700;
}

.success-text {
  color: var(--brand-strong);
  font-weight: 700;
}
```

- [ ] **Step 6: Run H5 build**

Run:

```powershell
npm run build --prefix h5
```

Expected: build succeeds.

- [ ] **Step 7: Commit H5 page integration**

Run:

```powershell
git add h5/src/views h5/src/style.css
git commit -m "feat(h5): connect pages to client api"
```

Expected: commit succeeds.

### Task 8: Runtime Smoke, Final Verification, and Push

**Files:**
- Modify: `progress.md`
- Modify: this plan file by checking off completed steps during execution

- [ ] **Step 1: Run backend and frontend static verification**

Run:

```powershell
pnpm --dir admin-ui check:i18n
npm run build --prefix h5
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: all commands pass.

- [ ] **Step 2: Start backend locally**

Run from `backend`:

```powershell
java -jar gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local
```

Expected: backend listens on `http://localhost:8080`.

- [ ] **Step 3: Smoke bootstrap and login API**

Run:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/client/bootstrap" -Headers @{ "X-Brand-Code" = "demo"; "X-Channel-Code" = "h5" }
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/client/auth/login" -ContentType "application/json" -Body '{"username":"demo_player","password":"Demo123456"}'
```

Expected: bootstrap returns currencies and login returns `accessToken`.

- [ ] **Step 4: Smoke wallet and game APIs**

Store the token from login, then run:

```powershell
$token = "<accessToken from login response>"
Invoke-RestMethod -Uri "http://localhost:8080/api/client/wallet/accounts" -Headers @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/client/games?currencyCode=GC"
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/client/games/launch" -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body '{"providerCode":"mock","gameCode":"mock-slot-001","currencyCode":"GC"}'
```

Expected: wallet returns GC/SC accounts, games returns one mock game, launch returns `launchMode=stub`.

- [ ] **Step 5: Start H5 dev server**

Run:

```powershell
npm run dev --prefix h5 -- --host 127.0.0.1 --port 5174
```

Expected: H5 is available at `http://127.0.0.1:5174/`.

- [ ] **Step 6: Browser smoke**

Open `http://127.0.0.1:5174/` and verify:

```text
Home renders backend brand and currencies.
Login succeeds with demo_player / Demo123456.
Wallet renders backend balances.
Games renders mock game.
Launch shows launch stub message.
Logout returns shell to logged-out state.
```

- [ ] **Step 7: Update progress**

Append to `progress.md`:

```markdown
- Completed Phase 2 player client API and H5 integration:
  - Added `/api/client/bootstrap`, `/api/client/auth/login`, `/api/client/member/me`, `/api/client/wallet/accounts`, `/api/client/wallet/ledgers`, `/api/client/games`, and `/api/client/games/launch`.
  - Added demo player seed SQL under `backend/script/sql/gameluck_client_demo.sql`.
  - Wired H5 home, login, wallet, and games pages to backend client APIs.
  - Verification passed: i18n guard, H5 build, backend compile, and runtime smoke.
```

- [ ] **Step 8: Commit final plan/progress updates**

Run:

```powershell
git add docs/superpowers/plans/2026-07-09-player-client-api-h5.md progress.md
git commit -m "docs: mark phase 2 client api progress"
```

Expected: commit succeeds.

- [ ] **Step 9: Push and verify remote**

Run:

```powershell
git push origin main
git rev-parse main
git ls-remote https://github.com/tt88737/game_luck.git refs/heads/main
```

Expected: remote hash matches local `main`.

## Self-Review

Spec coverage:

- Bootstrap API is covered by Task 1.
- Demo login, token, and current member are covered by Task 2.
- Wallet balances and ledger are covered by Task 3.
- Game lobby and launch stub are covered by Task 4.
- Demo seed data is covered by Task 5.
- H5 API client and shell session state are covered by Task 6.
- H5 home, login, wallet, and games pages are covered by Task 7.
- Verification and runtime smoke are covered by Task 8.

Scope controls:

- Real payment is not included.
- KYC provider integration is not included.
- Third-party game provider integration is not included.
- Flutter App integration is not included.
- Cocos runtime is not included.
