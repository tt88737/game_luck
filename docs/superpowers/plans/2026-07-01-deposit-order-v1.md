# Deposit Order v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal admin-side deposit order flow that creates simulated RC deposit orders, marks them paid, and credits wallet balances through wallet-center.

**Architecture:** Add a new `gameluck-payment` module that owns deposit order state and depends on `gameluck-wallet` for accounting. Payment services never update balances directly; successful simulated payment calls `IWalletCoreService.credit` with `sourceType = DEPOSIT` and wallet idempotency key `deposit:success:{depositOrderNo}`.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL, GameLuck Admin conventions, Vue 3, Element Plus, TypeScript.

---

## File Structure

Backend module:

- Create `backend/gameluck-modules/gameluck-payment/pom.xml`: payment module dependencies, including `gameluck-wallet`.
- Modify `backend/gameluck-modules/pom.xml`: register `gameluck-payment`.
- Modify `backend/gameluck-admin/pom.xml`: add `gameluck-payment` dependency.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/DepositOrder.java`: entity for `gl_payment_deposit_order`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/DepositOrderBo.java`: list query and create request.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/DepositOrderVo.java`: admin response object.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/DepositOrderStatus.java`: `PENDING`, `SUCCESS`, `CANCELLED`, `FAILED`.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/DepositOrderMapper.java`: MyBatis mapper and row lock method.
- Create `backend/gameluck-modules/gameluck-payment/src/main/resources/mapper/payment/DepositOrderMapper.xml`: `selectByIdForUpdate` SQL.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IDepositOrderService.java`: admin query and state transition contract.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/DepositOrderServiceImpl.java`: create, cancel, simulate success, wallet credit integration.
- Create `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/DepositOrderController.java`: admin endpoints under `/payment/deposit`.
- Modify `backend/script/sql/gameluck_wallet.sql`: append payment deposit table and menu seed.

Frontend:

- Create `admin-ui/src/api/payment/deposit/index.ts`: deposit API wrapper.
- Create `admin-ui/src/api/payment/deposit/types.ts`: deposit TypeScript types.
- Create `admin-ui/src/views/payment/deposit/index.vue`: admin list, create dialog, detail dialog, simulate success and cancel actions.

## Task 1: Database Schema And Menu Seed

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`

- [ ] **Step 1: Append deposit order table SQL**

Add this table after wallet tables or at the end of the script:

```sql
CREATE TABLE IF NOT EXISTS gl_payment_deposit_order (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  deposit_order_no VARCHAR(64) NOT NULL COMMENT 'Deposit order number',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  amount DECIMAL(20,6) NOT NULL COMMENT 'Deposit amount',
  pay_method VARCHAR(32) NOT NULL DEFAULT 'SIMULATED' COMMENT 'Pay method',
  pay_channel VARCHAR(64) NOT NULL DEFAULT 'SIMULATED' COMMENT 'Pay channel',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Order status',
  wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Wallet transaction number',
  wallet_idempotency_key VARCHAR(128) NOT NULL COMMENT 'Wallet idempotency key',
  pay_time DATETIME DEFAULT NULL COMMENT 'Pay time',
  fail_reason VARCHAR(500) DEFAULT NULL COMMENT 'Failure reason',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_deposit_order_01 (tenant_id, deposit_order_no),
  UNIQUE KEY uk_gl_payment_deposit_order_02 (tenant_id, wallet_idempotency_key),
  KEY idx_gl_payment_deposit_order_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_payment_deposit_order_02 (tenant_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment deposit order';
```

- [ ] **Step 2: Append payment menu SQL**

Add payment center menu and deposit permissions:

```sql
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1900, '支付中心', 0, 7, 'payment', NULL, '', 1, 0, 'M', '0', '0', '', 'pay', 103, 1, NOW(), NULL, NULL, '支付中心目录'),
(1901, '充值订单', 1900, 1, 'deposit', 'payment/deposit/index', '', 1, 0, 'C', '0', '0', 'payment:deposit:list', 'money', 103, 1, NOW(), NULL, NULL, '充值订单菜单'),
(1911, '充值订单查询', 1901, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:deposit:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1912, '充值订单新增', 1901, 2, '#', '', '', 1, 0, 'F', '0', '0', 'payment:deposit:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1913, '模拟支付成功', 1901, 3, '#', '', '', 1, 0, 'F', '0', '0', 'payment:deposit:simulate', '#', 103, 1, NOW(), NULL, NULL, ''),
(1914, '充值订单取消', 1901, 4, '#', '', '', 1, 0, 'F', '0', '0', 'payment:deposit:cancel', '#', 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  parent_id = VALUES(parent_id),
  order_num = VALUES(order_num),
  path = VALUES(path),
  component = VALUES(component),
  perms = VALUES(perms),
  icon = VALUES(icon),
  remark = VALUES(remark),
  update_time = NOW();
```

- [ ] **Step 3: Apply SQL using the UTF-8 import script**

Run:

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

Expected: command exits with status `0`. Do not use `Get-Content | mysql` for this script.

- [ ] **Step 4: Verify table and menu**

Run:

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "show tables like 'gl_payment_deposit_order'; select menu_id, menu_name, icon from sys_menu where menu_id between 1900 and 1914 order by menu_id;"
```

Expected: table exists, and menu names display as `支付中心`, `充值订单`, `模拟支付成功`.

## Task 2: Backend Payment Module Skeleton

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/pom.xml`
- Modify: `backend/gameluck-modules/pom.xml`
- Modify: `backend/gameluck-admin/pom.xml`

- [ ] **Step 1: Create payment module POM**

Create `backend/gameluck-modules/gameluck-payment/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.gameluck</groupId>
        <artifactId>gameluck-modules</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>gameluck-payment</artifactId>

    <description>
        payment center
    </description>

    <dependencies>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-mybatis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-log</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-tenant</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-wallet</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Register module in parent POM**

Modify `backend/gameluck-modules/pom.xml`:

```xml
<modules>
    <module>gameluck-generator</module>
    <module>gameluck-job</module>
    <module>gameluck-system</module>
    <module>gameluck-wallet</module>
    <module>gameluck-payment</module>
</modules>
```

- [ ] **Step 3: Add admin dependency**

Modify `backend/gameluck-admin/pom.xml`, after `gameluck-wallet`:

```xml
<dependency>
    <groupId>com.gameluck</groupId>
    <artifactId>gameluck-payment</artifactId>
</dependency>
```

- [ ] **Step 4: Compile module skeleton**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

## Task 3: Deposit Domain, Mapper, Service Contract

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/enums/DepositOrderStatus.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/DepositOrder.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/DepositOrderBo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/DepositOrderVo.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/DepositOrderMapper.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/resources/mapper/payment/DepositOrderMapper.xml`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IDepositOrderService.java`

- [ ] **Step 1: Create status enum**

Create `DepositOrderStatus.java`:

```java
package com.gameluck.payment.enums;

public enum DepositOrderStatus {
    PENDING,
    SUCCESS,
    CANCELLED,
    FAILED
}
```

- [ ] **Step 2: Create entity**

Create `DepositOrder.java`:

```java
package com.gameluck.payment.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gl_payment_deposit_order")
public class DepositOrder extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;
    private String tenantId;
    private String depositOrderNo;
    private Long memberId;
    private String currencyCode;
    private BigDecimal amount;
    private String payMethod;
    private String payChannel;
    private String status;
    private String walletTransactionNo;
    private String walletIdempotencyKey;
    private Date payTime;
    private String failReason;
    private String remark;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

- [ ] **Step 3: Create BO**

Create `DepositOrderBo.java`:

```java
package com.gameluck.payment.domain.bo;

import com.gameluck.payment.domain.DepositOrder;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = DepositOrder.class, reverseConvertGenerate = false)
public class DepositOrderBo {

    private Long id;
    private String tenantId;
    private String depositOrderNo;
    @NotNull(message = "会员ID不能为空")
    private Long memberId;
    private String currencyCode;
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.000001", message = "充值金额必须大于0")
    private BigDecimal amount;
    private String payMethod;
    private String payChannel;
    private String status;
    private String remark;
    private Date beginTime;
    private Date endTime;
}
```

- [ ] **Step 4: Create VO**

Create `DepositOrderVo.java`:

```java
package com.gameluck.payment.domain.vo;

import com.gameluck.payment.domain.DepositOrder;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = DepositOrder.class)
public class DepositOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tenantId;
    private String depositOrderNo;
    private Long memberId;
    private String currencyCode;
    private BigDecimal amount;
    private String payMethod;
    private String payChannel;
    private String status;
    private String walletTransactionNo;
    private String walletIdempotencyKey;
    private Date payTime;
    private String failReason;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
```

- [ ] **Step 5: Create mapper**

Create `DepositOrderMapper.java`:

```java
package com.gameluck.payment.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.payment.domain.DepositOrder;
import com.gameluck.payment.domain.vo.DepositOrderVo;
import org.apache.ibatis.annotations.Param;

public interface DepositOrderMapper extends BaseMapperPlus<DepositOrder, DepositOrderVo> {

    DepositOrder selectByIdForUpdate(@Param("id") Long id);
}
```

- [ ] **Step 6: Create mapper XML**

Create `DepositOrderMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.gameluck.payment.mapper.DepositOrderMapper">

    <select id="selectByIdForUpdate" resultType="com.gameluck.payment.domain.DepositOrder">
        SELECT *
        FROM gl_payment_deposit_order
        WHERE id = #{id}
          AND del_flag = '0'
        FOR UPDATE
    </select>

</mapper>
```

- [ ] **Step 7: Create service contract**

Create `IDepositOrderService.java`:

```java
package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.DepositOrderBo;
import com.gameluck.payment.domain.vo.DepositOrderVo;

import java.util.List;

public interface IDepositOrderService {

    TableDataInfo<DepositOrderVo> queryPageList(DepositOrderBo bo, PageQuery pageQuery);

    DepositOrderVo queryById(Long id);

    List<DepositOrderVo> queryList(DepositOrderBo bo);

    Boolean insertByBo(DepositOrderBo bo);

    DepositOrderVo simulateSuccess(Long id);

    Boolean cancel(Long id);
}
```

- [ ] **Step 8: Compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

## Task 4: Deposit Service And Controller

**Files:**
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/DepositOrderServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/DepositOrderController.java`

- [ ] **Step 1: Implement service**

Create `DepositOrderServiceImpl.java`:

```java
package com.gameluck.payment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.DepositOrder;
import com.gameluck.payment.domain.bo.DepositOrderBo;
import com.gameluck.payment.domain.vo.DepositOrderVo;
import com.gameluck.payment.enums.DepositOrderStatus;
import com.gameluck.payment.mapper.DepositOrderMapper;
import com.gameluck.payment.service.IDepositOrderService;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DepositOrderServiceImpl implements IDepositOrderService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_CURRENCY = "RC";
    private static final String SIMULATED = "SIMULATED";
    private static final int MONEY_SCALE = 6;

    private final DepositOrderMapper baseMapper;
    private final IWalletCoreService walletCoreService;

    @Override
    public TableDataInfo<DepositOrderVo> queryPageList(DepositOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DepositOrder> lqw = buildQueryWrapper(bo);
        Page<DepositOrderVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public DepositOrderVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<DepositOrderVo> queryList(DepositOrderBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(DepositOrderBo bo) {
        Date now = new Date();
        String orderNo = "DP" + IdUtil.getSnowflakeNextIdStr();
        String tenantId = currentTenantId();
        DepositOrder add = BeanUtil.toBean(bo, DepositOrder.class);
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(tenantId);
        add.setDepositOrderNo(orderNo);
        add.setCurrencyCode(StringUtils.blankToDefault(bo.getCurrencyCode(), DEFAULT_CURRENCY));
        add.setAmount(normalizeAmount(bo.getAmount()));
        add.setPayMethod(StringUtils.blankToDefault(bo.getPayMethod(), SIMULATED));
        add.setPayChannel(StringUtils.blankToDefault(bo.getPayChannel(), SIMULATED));
        add.setStatus(DepositOrderStatus.PENDING.name());
        add.setWalletIdempotencyKey(walletIdempotencyKey(orderNo));
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return baseMapper.insert(add) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositOrderVo simulateSuccess(Long id) {
        DepositOrder order = lockOrder(id);
        if (DepositOrderStatus.SUCCESS.name().equals(order.getStatus())) {
            return BeanUtil.toBean(order, DepositOrderVo.class);
        }
        requirePending(order);

        Date now = new Date();
        try {
            WalletCreditBo creditBo = new WalletCreditBo();
            creditBo.setMemberId(order.getMemberId());
            creditBo.setCurrencyCode(order.getCurrencyCode());
            creditBo.setAmount(order.getAmount());
            creditBo.setSourceType("DEPOSIT");
            creditBo.setBusinessNo(order.getDepositOrderNo());
            creditBo.setIdempotencyKey(order.getWalletIdempotencyKey());
            creditBo.setRemark("Simulated deposit success");
            WalletTransaction transaction = walletCoreService.credit(creditBo);
            if (!WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
                throw new ServiceException("钱包入账未成功");
            }
            order.setStatus(DepositOrderStatus.SUCCESS.name());
            order.setWalletTransactionNo(transaction.getTransactionNo());
            order.setPayTime(now);
            order.setFailReason(null);
            order.setUpdateTime(now);
            baseMapper.updateById(order);
            return BeanUtil.toBean(order, DepositOrderVo.class);
        } catch (RuntimeException ex) {
            order.setStatus(DepositOrderStatus.FAILED.name());
            order.setFailReason(StringUtils.substring(ex.getMessage(), 0, 500));
            order.setUpdateTime(now);
            baseMapper.updateById(order);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Long id) {
        DepositOrder order = lockOrder(id);
        requirePending(order);
        order.setStatus(DepositOrderStatus.CANCELLED.name());
        order.setUpdateTime(new Date());
        return baseMapper.updateById(order) > 0;
    }

    private LambdaQueryWrapper<DepositOrder> buildQueryWrapper(DepositOrderBo bo) {
        LambdaQueryWrapper<DepositOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), DepositOrder::getTenantId, bo.getTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getDepositOrderNo()), DepositOrder::getDepositOrderNo, bo.getDepositOrderNo());
        lqw.eq(bo.getMemberId() != null, DepositOrder::getMemberId, bo.getMemberId());
        lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), DepositOrder::getCurrencyCode, bo.getCurrencyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), DepositOrder::getStatus, bo.getStatus());
        lqw.ge(bo.getBeginTime() != null, DepositOrder::getCreateTime, bo.getBeginTime());
        lqw.le(bo.getEndTime() != null, DepositOrder::getCreateTime, bo.getEndTime());
        lqw.orderByDesc(DepositOrder::getCreateTime);
        return lqw;
    }

    private DepositOrder lockOrder(Long id) {
        DepositOrder order = baseMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new ServiceException("充值订单不存在");
        }
        return order;
    }

    private void requirePending(DepositOrder order) {
        if (!DepositOrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new ServiceException("只有待支付订单允许操作");
        }
    }

    private String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        return StringUtils.isBlank(tenantId) ? DEFAULT_TENANT_ID : tenantId;
    }

    private String walletIdempotencyKey(String orderNo) {
        return "deposit:success:" + orderNo;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("充值金额必须大于0");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 2: Implement controller**

Create `DepositOrderController.java`:

```java
package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.payment.domain.bo.DepositOrderBo;
import com.gameluck.payment.domain.vo.DepositOrderVo;
import com.gameluck.payment.service.IDepositOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/deposit")
public class DepositOrderController extends BaseController {

    private final IDepositOrderService depositOrderService;

    @SaCheckPermission("payment:deposit:list")
    @GetMapping("/list")
    public TableDataInfo<DepositOrderVo> list(DepositOrderBo bo, PageQuery pageQuery) {
        return depositOrderService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("payment:deposit:query")
    @GetMapping("/{id}")
    public R<DepositOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(depositOrderService.queryById(id));
    }

    @SaCheckPermission("payment:deposit:add")
    @Log(title = "充值订单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody DepositOrderBo bo) {
        return toAjax(depositOrderService.insertByBo(bo));
    }

    @SaCheckPermission("payment:deposit:simulate")
    @Log(title = "模拟支付成功", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/simulate-success")
    public R<DepositOrderVo> simulateSuccess(@PathVariable Long id) {
        return R.ok(depositOrderService.simulateSuccess(id));
    }

    @SaCheckPermission("payment:deposit:cancel")
    @Log(title = "充值订单取消", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        return toAjax(depositOrderService.cancel(id));
    }
}
```

- [ ] **Step 3: Compile admin**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

## Task 5: Admin UI Page

**Files:**
- Create: `admin-ui/src/api/payment/deposit/types.ts`
- Create: `admin-ui/src/api/payment/deposit/index.ts`
- Create: `admin-ui/src/views/payment/deposit/index.vue`

- [ ] **Step 1: Create TypeScript types**

Create `types.ts`:

```ts
export interface DepositOrderVO {
  id: string | number;
  tenantId: string;
  depositOrderNo: string;
  memberId: string | number;
  currencyCode: string;
  amount: number;
  payMethod: string;
  payChannel: string;
  status: string;
  walletTransactionNo: string;
  walletIdempotencyKey: string;
  payTime: string;
  failReason: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface DepositOrderForm {
  id?: string | number;
  memberId?: string | number;
  currencyCode?: string;
  amount?: number;
  payMethod?: string;
  payChannel?: string;
  remark?: string;
}

export interface DepositOrderQuery extends PageQuery {
  depositOrderNo?: string;
  memberId?: string | number;
  currencyCode?: string;
  status?: string;
  beginTime?: string;
  endTime?: string;
}
```

- [ ] **Step 2: Create API wrapper**

Create `index.ts`:

```ts
import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { DepositOrderForm, DepositOrderQuery, DepositOrderVO } from './types';

export function listDeposit(query: DepositOrderQuery): AxiosPromise<DepositOrderVO[]> {
  return request({
    url: '/payment/deposit/list',
    method: 'get',
    params: query
  });
}

export function getDeposit(id: string | number): AxiosPromise<DepositOrderVO> {
  return request({
    url: '/payment/deposit/' + id,
    method: 'get'
  });
}

export function addDeposit(data: DepositOrderForm) {
  return request({
    url: '/payment/deposit',
    method: 'post',
    data
  });
}

export function simulateDepositSuccess(id: string | number): AxiosPromise<DepositOrderVO> {
  return request({
    url: '/payment/deposit/' + id + '/simulate-success',
    method: 'post'
  });
}

export function cancelDeposit(id: string | number) {
  return request({
    url: '/payment/deposit/' + id + '/cancel',
    method: 'post'
  });
}
```

- [ ] **Step 3: Create Vue page**

Create `index.vue` with a dense B-side table:

```vue
<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="订单号" prop="depositOrderNo">
              <el-input v-model="queryParams.depositOrderNo" placeholder="请输入订单号" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="会员ID" prop="memberId">
              <el-input v-model="queryParams.memberId" placeholder="请输入会员ID" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="币种" prop="currencyCode">
              <el-select v-model="queryParams.currencyCode" placeholder="请选择币种" clearable class="!w-120px">
                <el-option label="RC" value="RC" />
                <el-option label="SC" value="SC" />
                <el-option label="GC" value="GC" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-140px">
                <el-option label="待支付" value="PENDING" />
                <el-option label="成功" value="SUCCESS" />
                <el-option label="已取消" value="CANCELLED" />
                <el-option label="失败" value="FAILED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="never">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['payment:deposit:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" :data="depositList">
        <el-table-column label="订单号" align="center" prop="depositOrderNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="会员ID" align="center" prop="memberId" width="120" />
        <el-table-column label="币种" align="center" prop="currencyCode" width="90" />
        <el-table-column label="金额" align="right" prop="amount" width="130" />
        <el-table-column label="支付方式" align="center" prop="payMethod" width="120" />
        <el-table-column label="状态" align="center" prop="status" width="110">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="钱包交易号" align="center" prop="walletTransactionNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="支付时间" align="center" prop="payTime" width="170" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" align="center" width="180" fixed="right">
          <template #default="scope">
            <el-button v-hasPermi="['payment:deposit:query']" link type="primary" icon="View" @click="handleDetail(scope.row)"></el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              v-hasPermi="['payment:deposit:simulate']"
              link
              type="primary"
              icon="CircleCheck"
              @click="handleSimulate(scope.row)"
            ></el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              v-hasPermi="['payment:deposit:cancel']"
              link
              type="danger"
              icon="Close"
              @click="handleCancel(scope.row)"
            ></el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="open" title="新增模拟充值订单" width="520px" append-to-body>
      <el-form ref="depositFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="会员ID" prop="memberId">
          <el-input v-model="form.memberId" placeholder="请输入会员ID" />
        </el-form-item>
        <el-form-item label="币种" prop="currencyCode">
          <el-select v-model="form.currencyCode" class="w-full">
            <el-option label="RC" value="RC" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :precision="6" :min="0.000001" class="w-full" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailOpen" title="充值订单详情" width="620px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.depositOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ detail.memberId }}</el-descriptions-item>
        <el-descriptions-item label="币种">{{ detail.currencyCode }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.payMethod }}</el-descriptions-item>
        <el-descriptions-item label="钱包交易号">{{ detail.walletTransactionNo }}</el-descriptions-item>
        <el-descriptions-item label="幂等键">{{ detail.walletIdempotencyKey }}</el-descriptions-item>
        <el-descriptions-item label="失败原因" :span="2">{{ detail.failReason }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="DepositOrder" lang="ts">
import { addDeposit, cancelDeposit, getDeposit, listDeposit, simulateDepositSuccess } from '@/api/payment/deposit';
import { DepositOrderForm, DepositOrderQuery, DepositOrderVO } from '@/api/payment/deposit/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const depositList = ref<DepositOrderVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const open = ref(false);
const detailOpen = ref(false);

const queryFormRef = ref<ElFormInstance>();
const depositFormRef = ref<ElFormInstance>();

const initForm: DepositOrderForm = {
  currencyCode: 'RC',
  payMethod: 'SIMULATED',
  payChannel: 'SIMULATED'
};

const data = reactive<PageData<DepositOrderForm, DepositOrderQuery>>({
  form: { ...initForm },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    depositOrderNo: undefined,
    memberId: undefined,
    currencyCode: undefined,
    status: undefined
  },
  rules: {
    memberId: [{ required: true, message: '会员ID不能为空', trigger: 'blur' }],
    currencyCode: [{ required: true, message: '币种不能为空', trigger: 'change' }],
    amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);
const detail = ref<Partial<DepositOrderVO>>({});

const getList = async () => {
  loading.value = true;
  const res = await listDeposit(queryParams.value);
  depositList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const statusLabel = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: '待支付',
    SUCCESS: '成功',
    CANCELLED: '已取消',
    FAILED: '失败'
  };
  return status ? map[status] || status : '';
};

const statusType = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    SUCCESS: 'success',
    CANCELLED: 'info',
    FAILED: 'danger'
  };
  return status ? map[status] || '' : '';
};

const reset = () => {
  form.value = { ...initForm };
  depositFormRef.value?.resetFields();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleAdd = () => {
  reset();
  open.value = true;
};

const submitForm = () => {
  depositFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await addDeposit(form.value);
      proxy?.$modal.msgSuccess('新增成功');
      open.value = false;
      await getList();
    }
  });
};

const cancel = () => {
  open.value = false;
  reset();
};

const handleDetail = async (row: DepositOrderVO) => {
  const res = await getDeposit(row.id);
  detail.value = res.data;
  detailOpen.value = true;
};

const handleSimulate = async (row: DepositOrderVO) => {
  await proxy?.$modal.confirm('确认将该充值订单标记为模拟支付成功并执行钱包入账？');
  await simulateDepositSuccess(row.id);
  proxy?.$modal.msgSuccess('模拟支付成功');
  await getList();
};

const handleCancel = async (row: DepositOrderVO) => {
  await proxy?.$modal.confirm('确认取消该充值订单？');
  await cancelDeposit(row.id);
  proxy?.$modal.msgSuccess('取消成功');
  await getList();
};

onMounted(() => {
  getList();
});
</script>
```

- [ ] **Step 4: Build frontend**

Run:

```powershell
pnpm --dir admin-ui build:prod
```

Expected: build exits with status `0`.

## Task 6: Local Verification

**Files:**
- No source changes expected unless verification exposes a defect.

- [ ] **Step 1: Backend compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Frontend build**

Run:

```powershell
pnpm --dir admin-ui build:prod
```

Expected: build exits with status `0`.

- [ ] **Step 3: Apply SQL**

Run:

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

Expected: command exits with status `0`.

- [ ] **Step 4: Verify simulated deposit through database and API**

After logging into the admin UI or using an authenticated session, create a deposit order from `支付中心 / 充值订单`, then click `模拟支付成功`.

Verify in MySQL:

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "select deposit_order_no, member_id, currency_code, amount, status, wallet_transaction_no from gl_payment_deposit_order order by create_time desc limit 5; select member_id, currency_code, available_balance from gl_wallet_account where currency_code='RC' order by update_time desc limit 5; select business_no, operation, source_type, amount, status from gl_wallet_transaction where source_type='DEPOSIT' order by create_time desc limit 5; select business_no, release_mode, release_status from gl_wallet_release where source_type='DEPOSIT' order by create_time desc limit 5;"
```

Expected:

- Deposit order status is `SUCCESS`.
- Wallet account `available_balance` increased.
- Wallet transaction has `CREDIT`, `DEPOSIT`, `SUCCESS`.
- Wallet release has `IMMEDIATE`, `RELEASED`.

## Self-Review

Spec coverage:

- Simulated deposit order creation: Task 4 service and Task 5 UI.
- `PENDING -> SUCCESS` and `PENDING -> CANCELLED`: Task 4 service methods.
- Wallet-only balance mutation: Task 4 calls `IWalletCoreService.credit`; no direct wallet table updates.
- Idempotency: Task 1 unique key and Task 4 `deposit:success:{orderNo}` wallet idempotency.
- Backend APIs and permissions: Task 1 menu permissions and Task 4 controller.
- Admin page states and high-risk confirmation: Task 5 confirm dialogs and conditional buttons.
- Verification: Task 6 database checks for order, account, transaction, and release.

Known limits:

- No real payment provider callback.
- No automatic retry for `FAILED` orders.
- No C-side recharge page.
