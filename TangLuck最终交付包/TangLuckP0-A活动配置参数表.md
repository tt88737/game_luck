# Tang Luck P0-A 活动配置参数表

## 1. 文档定位

本文用于把 P0-A 15 天版本可上线演示的活动、任务、奖励、SC 策略、预算、风控动作和合规注意事项细化到可配置级别。

重要说明：本文中的 SC 额度是产品建议值，用于演示和评审。真实运营前必须由美国律师确认开放州、额度、频率、规则文案和 AMOE 策略。

## 2. P0-A 活动总原则

| 原则 | 执行标准 |
| --- | --- |
| SC 不销售 | 所有 SC 只能来自活动赠送、免费路径或 AMOE |
| 默认低额 | P0-A 只允许注册、每日登录等低风险场景小额 SC |
| 风险降级 | 风险用户命中后只发 GC、延迟或人工审核 |
| 地区强校验 | 前端隐藏 + 后端按 `state_code` 拦截 |
| 活动可追溯 | 每次领取写 `promotion_claims`、`promotion_reward_grants`、`wallet_ledger` |
| 配置可阻断 | 缺地区、预算、规则版本、法务 ID 时不能发布 |

## 3. P0-A 默认活动配置

| 活动 | 阶段 | 默认奖励 | SC 策略 | 建议频率 | 建议预算 | 风控动作 | 是否默认可发 SC | 法务确认 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 注册赠送 | P0-A | `10,000 GC + 0.50 SC` | `default_small_sc` | 每用户一次 | `500 SC/day` | 命中风险只发 GC | 可，但受地区/额度限制 | 额度、州级需确认 |
| 每日登录 | P0-A | `1,000 GC + 0.05 SC` | `default_small_sc` | 每日一次 | `200 SC/day` | 命中风险只发 GC | 可，但额度低 | 频率、额度需确认 |
| 查看 Rules 任务 | P0-A | `500 GC` | `gc_only` | 每日一次 | `1,000,000 GC/day` | 正常发 GC | 否 | 文案需确认 |
| 完成邮箱验证 | P0-A | `1,000 GC` | `gc_only` | 每用户一次 | `1,000,000 GC/day` | 重复设备限制 | 否 | 不涉及 SC |
| 浏览钱包说明 | P0-A | `500 GC` | `gc_only` | 每日一次 | `500,000 GC/day` | 正常发 GC | 否 | 不涉及 SC |
| Coupon Code | P0-A | `500-5,000 GC` | `gc_only` | 按 code 配置 | 按批次配置 | 撞库拦截 | 否 | 发 SC 必须确认 |
| AMOE 静态入口 | P0-A | 不直接发奖 | `legal_required` | 不开放表单 | 无 | 展示规则 | 否 | 必须确认 |

## 4. P1/P2 预留活动策略

| 活动 | 默认奖励 | SC 策略 | 建议上线阶段 | 合规注意事项 |
| --- | --- | --- | --- | --- |
| 邀请奖励 | GC、经验、Coupon | `gc_only` | P1 | 需防多账号；披露激励关系；发 SC 必须法务确认 |
| 排行榜 | GC、徽章、经验 | `gc_only` | P1 | 榜单冻结后审核；高价值奖品必须确认 |
| 转盘 | GC、Coupon、小额 SC | `legal_required` | P1 | 概率、免费次数、奖池、库存、SC 发放必须确认 |
| 会员/VIP | GC、权益、客服优先 | `gc_only` | P1/P2 | 不得绕过 KYC、风控、兑换审核 |
| 运营日历 | GC、Coupon、低额 SC | 按活动审批 | P2 | 每场活动独立预算、规则版本、审批 ID |

## 5. 活动配置字段

| 字段 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- |
| `campaign_code` | 是 | `register_bonus_v1` | 活动唯一 code |
| `campaign_type` | 是 | `register` | 活动类型 |
| `eligible_regions` | 是 | `["CA","TX","NJ"]` | 开放州 |
| `blocked_regions` | 否 | `["WA"]` | 禁止州 |
| `reward_policy` | 是 | `GC 10000 + SC 0.50` | 奖励配置 |
| `sc_strategy` | 是 | `default_small_sc` | SC 策略 |
| `daily_budget_cap` | 是 | `SC 500/day` | 日预算 |
| `user_period_cap` | 是 | `1` | 用户周期上限 |
| `period_type` | 是 | `once`、`daily` | 周期 |
| `rules_version` | 是 | `rules-v1` | 规则版本 |
| `legal_approval_id` | 条件必填 | `LEGAL-2026-0617-CA` | 发 SC 或高风险活动必填 |
| `risk_action` | 是 | `gc_only` | 风险命中动作 |
| `start_at` / `end_at` | 是/否 | UTC 时间 | 活动时间 |

## 6. SC 策略定义

| 策略 | 含义 | 可用场景 | 发布要求 |
| --- | --- | --- | --- |
| `gc_only` | 只发 GC，不发 SC | 任务、Coupon、邀请、排行榜、会员 | 不需要 SC 审批 |
| `default_small_sc` | 默认低额 SC | 注册赠送、每日登录 | 需要地区、额度、预算、法务确认 |
| `legal_required` | 必须法务确认后才可发 SC | AMOE、转盘、商品包 SC bonus、高价值奖品 | `legal_approval_id` 必填 |
| `sc_blocked` | 明确禁止发 SC | 禁止州、风险用户、未确认活动 | 只发 GC 或拒绝 |

## 7. 风控动作

| 动作 | 说明 | 用户感知 |
| --- | --- | --- |
| `pass` | 正常发奖 | 正常到账 |
| `gc_only` | 只发 GC，不发 SC | 显示“部分奖励受地区或风险限制” |
| `delay` | 延迟发奖，进入队列 | 显示“奖励审核中” |
| `manual_review` | 人工审核 | 显示“需要人工确认” |
| `block` | 拦截领取 | 显示不可领取原因 |

## 8. 后台发布阻断

| 阻断项 | 处理 |
| --- | --- |
| 未配置地区 | 不允许发布 |
| 未配置预算 | 不允许发布 |
| 未配置规则版本 | 不允许发布 |
| 发 SC 但无 `legal_approval_id` | 不允许发布 |
| `legal_required` 未通过审批 | 不允许发布 |
| 文案包含“买 SC / 充值返现 / 保证提现” | 不允许发布 |
| 预算低于已领取量 | 不允许保存或要求先暂停 |

## 9. 运营建议

| 目标 | 建议 |
| --- | --- |
| 15 天竞标演示 | 注册赠送 + 每日登录 + 每日任务 + Coupon 足够展示活动能力 |
| 用户吸引力 | P0-A 用小额 SC 建立获得感，用 GC 提高任务频次 |
| 合规安全 | P0-A 不开放转盘、排行榜 SC 奖池、真实兑换 |
| 长期运营 | P1 后再上邀请、排行榜、转盘、会员，先补风控和审批 |

## 10. QA 验收

```gherkin
Feature: P0-A 活动配置
  Scenario: 正常用户领取注册奖励
    Given 用户在允许州且完成条款确认
    When 用户领取注册赠送
    Then 用户获得 10000 GC
    And 用户获得 0.50 SC
    And 系统写入 wallet_ledger

  Scenario: 风险用户领取每日登录
    Given 用户命中 risk_action=gc_only
    When 用户领取每日登录
    Then 用户只获得 GC
    And 不生成 SC 入账流水

  Scenario: 活动缺少法务审批
    Given 活动 sc_strategy=default_small_sc
    And legal_approval_id 为空
    When 运营发布活动
    Then 系统阻断发布
```
