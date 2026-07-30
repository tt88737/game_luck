-- GameLuck platform business dictionaries.
-- Import with UTF-8, for example:
-- .\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_platform_dict.sql

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20001, '000000', 'GameLuck通用启停状态', 'gl_common_status', 103, 1, SYSDATE(), NULL, NULL, '平台业务通用启停状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_common_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20002, '000000', 'GameLuck业务是否', 'gl_yes_no', 103, 1, SYSDATE(), NULL, NULL, '平台业务是否选项'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_yes_no');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20003, '000000', 'GameLuck会员状态', 'gl_member_status', 103, 1, SYSDATE(), NULL, NULL, '玩家会员状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_member_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20004, '000000', 'GameLuck KYC状态', 'gl_kyc_status', 103, 1, SYSDATE(), NULL, NULL, '玩家KYC状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_kyc_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20005, '000000', 'GameLuck地区检查状态', 'gl_geo_status', 103, 1, SYSDATE(), NULL, NULL, '地区合规检查状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_geo_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20006, '000000', 'GameLuck风控决策', 'gl_risk_decision', 103, 1, SYSDATE(), NULL, NULL, '风控决策结果'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_risk_decision');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20007, '000000', 'GameLuck币种类型', 'gl_currency_type', 103, 1, SYSDATE(), NULL, NULL, '钱包币种类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_currency_type');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20008, '000000', 'GameLuck钱包账户状态', 'gl_wallet_account_status', 103, 1, SYSDATE(), NULL, NULL, '会员钱包账户状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_wallet_account_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20009, '000000', 'GameLuck钱包业务类型', 'gl_wallet_biz_type', 103, 1, SYSDATE(), NULL, NULL, '钱包账变业务类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_wallet_biz_type');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20010, '000000', 'GameLuck钱包冻结状态', 'gl_wallet_freeze_status', 103, 1, SYSDATE(), NULL, NULL, '钱包冻结记录状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_wallet_freeze_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20011, '000000', 'GameLuck活动类型', 'gl_promotion_type', 103, 1, SYSDATE(), NULL, NULL, '活动中心活动类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_promotion_type');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20012, '000000', 'GameLuck活动状态', 'gl_promotion_status', 103, 1, SYSDATE(), NULL, NULL, '活动中心活动状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_promotion_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20013, '000000', 'GameLuck奖励领取状态', 'gl_reward_claim_status', 103, 1, SYSDATE(), NULL, NULL, '奖励领取记录状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_reward_claim_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20014, '000000', 'GameLuck游戏状态', 'gl_game_status', 103, 1, SYSDATE(), NULL, NULL, '游戏状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_game_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20015, '000000', 'GameLuck游戏会话状态', 'gl_game_session_status', 103, 1, SYSDATE(), NULL, NULL, '游戏会话状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_game_session_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20016, '000000', 'GameLuck充值订单状态', 'gl_deposit_status', 103, 1, SYSDATE(), NULL, NULL, '充值订单状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_deposit_status');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 20017, '000000', 'GameLuck兑换订单状态', 'gl_redemption_status', 103, 1, SYSDATE(), NULL, NULL, '兑换订单状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type = 'gl_redemption_status');

INSERT INTO sys_dict_data
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark
FROM (
  SELECT 21001 dict_code, '000000' tenant_id, 1 dict_sort, '启用' dict_label, 'ENABLED' dict_value, 'gl_common_status' dict_type, '' css_class, 'success' list_class, 'Y' is_default, 103 create_dept, 1 create_by, SYSDATE() create_time, NULL update_by, NULL update_time, '启用' remark UNION ALL
  SELECT 21002, '000000', 2, '停用', 'DISABLED', 'gl_common_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '停用' UNION ALL
  SELECT 21003, '000000', 1, '是', 'Y', 'gl_yes_no', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '是' UNION ALL
  SELECT 21004, '000000', 2, '否', 'N', 'gl_yes_no', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '否' UNION ALL
  SELECT 21005, '000000', 1, '正常', 'ACTIVE', 'gl_member_status', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '正常会员' UNION ALL
  SELECT 21006, '000000', 2, '冻结', 'FROZEN', 'gl_member_status', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '冻结会员' UNION ALL
  SELECT 21007, '000000', 3, '封禁', 'BANNED', 'gl_member_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '封禁会员' UNION ALL
  SELECT 21008, '000000', 4, '关闭', 'CLOSED', 'gl_member_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '关闭账号' UNION ALL
  SELECT 21009, '000000', 1, '未开始', 'NOT_STARTED', 'gl_kyc_status', '', 'info', 'Y', 103, 1, SYSDATE(), NULL, NULL, 'KYC未开始' UNION ALL
  SELECT 21010, '000000', 2, '审核中', 'PENDING', 'gl_kyc_status', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, 'KYC审核中' UNION ALL
  SELECT 21011, '000000', 3, '已通过', 'APPROVED', 'gl_kyc_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, 'KYC已通过' UNION ALL
  SELECT 21012, '000000', 4, '已拒绝', 'REJECTED', 'gl_kyc_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, 'KYC已拒绝' UNION ALL
  SELECT 21013, '000000', 1, '通过', 'PASS', 'gl_geo_status', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '地区检查通过' UNION ALL
  SELECT 21014, '000000', 2, '阻断', 'BLOCKED', 'gl_geo_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '地区检查阻断' UNION ALL
  SELECT 21015, '000000', 3, '未知', 'UNKNOWN', 'gl_geo_status', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '地区检查未知' UNION ALL
  SELECT 21016, '000000', 1, '通过', 'PASS', 'gl_risk_decision', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '风控通过' UNION ALL
  SELECT 21017, '000000', 2, '加强验证', 'CHALLENGE', 'gl_risk_decision', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '需要加强验证' UNION ALL
  SELECT 21018, '000000', 3, '人工复核', 'REVIEW', 'gl_risk_decision', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '需要人工复核' UNION ALL
  SELECT 21019, '000000', 4, '阻断', 'BLOCK', 'gl_risk_decision', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '风控阻断' UNION ALL
  SELECT 21020, '000000', 1, '虚拟币', 'VIRTUAL', 'gl_currency_type', '', 'primary', 'Y', 103, 1, SYSDATE(), NULL, NULL, '虚拟币' UNION ALL
  SELECT 21021, '000000', 2, 'Sweepstakes币', 'SWEEPSTAKES', 'gl_currency_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, 'Sweepstakes币' UNION ALL
  SELECT 21022, '000000', 3, '现金币', 'CASH', 'gl_currency_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '现金币' UNION ALL
  SELECT 21023, '000000', 4, '奖励币', 'BONUS', 'gl_currency_type', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '奖励币' UNION ALL
  SELECT 21024, '000000', 1, '正常', 'NORMAL', 'gl_wallet_account_status', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '钱包正常' UNION ALL
  SELECT 21025, '000000', 2, '冻结', 'FROZEN', 'gl_wallet_account_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '钱包冻结' UNION ALL
  SELECT 21026, '000000', 1, '注册赠送', 'REGISTER_BONUS', 'gl_wallet_biz_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '注册赠送账变' UNION ALL
  SELECT 21027, '000000', 2, '每日奖励', 'DAILY_REWARD', 'gl_wallet_biz_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '每日奖励账变' UNION ALL
  SELECT 21028, '000000', 3, '任务奖励', 'TASK_REWARD', 'gl_wallet_biz_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '任务奖励账变' UNION ALL
  SELECT 21029, '000000', 4, '游戏投注', 'GAME_BET', 'gl_wallet_biz_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏投注账变' UNION ALL
  SELECT 21031, '000000', 5, '充值入账', 'DEPOSIT', 'gl_wallet_biz_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '充值入账账变' UNION ALL
  SELECT 21032, '000000', 6, '兑换处理', 'REDEMPTION', 'gl_wallet_biz_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换处理账变' UNION ALL
  SELECT 21066, '000000', 7, '游戏收益', 'GAME_PROFIT', 'gl_wallet_biz_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏收益账变' UNION ALL
  SELECT 21067, '000000', 8, '人工调账', 'MANUAL_ADJUST', 'gl_wallet_biz_type', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '人工调账账变' UNION ALL
  SELECT 21068, '000000', 9, '游戏退款', 'GAME_REFUND', 'gl_wallet_biz_type', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏退款账变' UNION ALL
  SELECT 21069, '000000', 10, '活动奖励', 'PROMOTION', 'gl_wallet_biz_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '活动奖励账变' UNION ALL
  SELECT 21070, '000000', 11, '流水释放', 'TURNOVER', 'gl_wallet_biz_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '流水释放账变' UNION ALL
  SELECT 21034, '000000', 1, '已冻结', 'FROZEN', 'gl_wallet_freeze_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '已冻结' UNION ALL
  SELECT 21035, '000000', 2, '已结算', 'SETTLED', 'gl_wallet_freeze_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '已结算' UNION ALL
  SELECT 21036, '000000', 3, '已释放', 'RELEASED', 'gl_wallet_freeze_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '已释放' UNION ALL
  SELECT 21037, '000000', 1, '注册赠送', 'REGISTER_BONUS', 'gl_promotion_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '注册赠送活动' UNION ALL
  SELECT 21038, '000000', 2, '每日登录', 'DAILY_LOGIN', 'gl_promotion_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '每日登录活动' UNION ALL
  SELECT 21039, '000000', 3, '每日任务', 'DAILY_MISSION', 'gl_promotion_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '每日任务活动' UNION ALL
  SELECT 21040, '000000', 4, '在线奖励', 'ONLINE_REWARD', 'gl_promotion_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '在线奖励活动' UNION ALL
  SELECT 21041, '000000', 5, 'Welcome Offer', 'WELCOME_OFFER', 'gl_promotion_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, 'Welcome Offer活动' UNION ALL
  SELECT 21042, '000000', 6, 'Refill', 'REFILL', 'gl_promotion_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, 'Refill活动' UNION ALL
  SELECT 21043, '000000', 1, '草稿', 'DRAFT', 'gl_promotion_status', '', 'info', 'Y', 103, 1, SYSDATE(), NULL, NULL, '活动草稿' UNION ALL
  SELECT 21044, '000000', 2, '启用', 'ENABLED', 'gl_promotion_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '活动启用' UNION ALL
  SELECT 21045, '000000', 3, '停用', 'DISABLED', 'gl_promotion_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '活动停用' UNION ALL
  SELECT 21046, '000000', 4, '已过期', 'EXPIRED', 'gl_promotion_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '活动已过期' UNION ALL
  SELECT 21047, '000000', 1, '待处理', 'PENDING', 'gl_reward_claim_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '奖励待处理' UNION ALL
  SELECT 21048, '000000', 2, '成功', 'SUCCESS', 'gl_reward_claim_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '奖励领取成功' UNION ALL
  SELECT 21049, '000000', 3, '失败', 'FAILED', 'gl_reward_claim_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '奖励领取失败' UNION ALL
  SELECT 21050, '000000', 4, '重复', 'DUPLICATE', 'gl_reward_claim_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '重复领取' UNION ALL
  SELECT 21051, '000000', 1, '启用', 'ENABLED', 'gl_game_status', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '游戏启用' UNION ALL
  SELECT 21052, '000000', 2, '维护中', 'MAINTENANCE', 'gl_game_status', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏维护中' UNION ALL
  SELECT 21053, '000000', 3, '停用', 'DISABLED', 'gl_game_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏停用' UNION ALL
  SELECT 21054, '000000', 1, '已开始', 'STARTED', 'gl_game_session_status', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '游戏会话已开始' UNION ALL
  SELECT 21055, '000000', 2, '已结束', 'ENDED', 'gl_game_session_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏会话已结束' UNION ALL
  SELECT 21056, '000000', 3, '失败', 'FAILED', 'gl_game_session_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏会话失败' UNION ALL
  SELECT 21057, '000000', 1, '待支付', 'PENDING', 'gl_deposit_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '充值待支付' UNION ALL
  SELECT 21058, '000000', 2, '成功', 'SUCCESS', 'gl_deposit_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '充值成功' UNION ALL
  SELECT 21059, '000000', 3, '失败', 'FAILED', 'gl_deposit_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '充值失败' UNION ALL
  SELECT 21060, '000000', 4, '已取消', 'CANCELLED', 'gl_deposit_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '充值已取消' UNION ALL
  SELECT 21061, '000000', 1, '待审核', 'PENDING', 'gl_redemption_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '兑换待审核' UNION ALL
  SELECT 21062, '000000', 2, '审核通过', 'APPROVED', 'gl_redemption_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换审核通过' UNION ALL
  SELECT 21063, '000000', 3, '已拒绝', 'REJECTED', 'gl_redemption_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换已拒绝' UNION ALL
  SELECT 21064, '000000', 4, '已打款', 'PAID', 'gl_redemption_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换已打款' UNION ALL
  SELECT 21065, '000000', 5, '失败', 'FAILED', 'gl_redemption_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换失败'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
    AND existing.dict_value = seed.dict_value
);

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark
FROM (
  SELECT 20030 dict_id, '000000' tenant_id, '购买订单状态' dict_name, 'gl_purchase_order_status' dict_type, 103 create_dept, 1 create_by, SYSDATE() create_time, NULL update_by, NULL update_time, '购买订单生命周期状态' remark UNION ALL
  SELECT 20031, '000000', '购买支付事件类型', 'gl_purchase_payment_event_type', 103, 1, SYSDATE(), NULL, NULL, '购买支付回调和人工事件类型' UNION ALL
  SELECT 20032, '000000', '购买支付事件状态', 'gl_purchase_payment_event_status', 103, 1, SYSDATE(), NULL, NULL, '购买支付事件处理状态' UNION ALL
  SELECT 20033, '000000', '购买追偿类型', 'gl_purchase_reversal_type', 103, 1, SYSDATE(), NULL, NULL, '退款和拒付资产追偿类型' UNION ALL
  SELECT 20034, '000000', '购买追偿状态', 'gl_purchase_reversal_status', 103, 1, SYSDATE(), NULL, NULL, '购买资产追偿处理状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
);

-- Phase 45 payment settlement dictionaries. Wallet SQL may seed the same values first.
INSERT INTO sys_dict_type
(dict_id,tenant_id,dict_name,dict_type,create_dept,create_by,create_time,remark)
SELECT 20045,'000000','Payment Settlement Batch Status','gl_payment_settlement_batch_status',103,1,SYSDATE(),''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id='000000' AND dict_type='gl_payment_settlement_batch_status');
INSERT INTO sys_dict_type
(dict_id,tenant_id,dict_name,dict_type,create_dept,create_by,create_time,remark)
SELECT 20046,'000000','Payment Settlement Action Type','gl_payment_settlement_action_type',103,1,SYSDATE(),''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id='000000' AND dict_type='gl_payment_settlement_action_type');

INSERT INTO sys_dict_data
(dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,create_dept,create_by,create_time,remark)
SELECT * FROM (
 SELECT 21317 dict_code,'000000' tenant_id,1 dict_sort,'Created' dict_label,'CREATED' dict_value,
 'gl_payment_settlement_batch_status' dict_type,'' css_class,'info' list_class,'N' is_default,
 103 create_dept,1 create_by,SYSDATE() create_time,'' remark UNION ALL
 SELECT 21318,'000000',2,'Calculating','CALCULATING','gl_payment_settlement_batch_status','','warning','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21319,'000000',3,'Calculated','CALCULATED','gl_payment_settlement_batch_status','','primary','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21320,'000000',4,'Closed','CLOSED','gl_payment_settlement_batch_status','','success','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21321,'000000',5,'Failed','FAILED','gl_payment_settlement_batch_status','','danger','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21322,'000000',1,'Create','CREATE','gl_payment_settlement_action_type','','info','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21323,'000000',2,'Calculate','CALCULATE','gl_payment_settlement_action_type','','primary','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21324,'000000',3,'Calculation Failed','CALCULATION_FAILED','gl_payment_settlement_action_type','','danger','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21325,'000000',4,'Close Rejected','CLOSE_REJECTED','gl_payment_settlement_action_type','','warning','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21326,'000000',5,'Close','CLOSE','gl_payment_settlement_action_type','','success','N',103,1,SYSDATE(),''
) settlement_seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data existing WHERE existing.tenant_id=settlement_seed.tenant_id
 AND existing.dict_type=settlement_seed.dict_type AND existing.dict_value=settlement_seed.dict_value);

-- Phase 47 settlement payout approval dictionaries.
INSERT INTO sys_dict_type
(dict_id,tenant_id,dict_name,dict_type,create_dept,create_by,create_time,remark)
SELECT 20047,'000000','Payment Settlement Payout Status','gl_payment_settlement_payout_status',103,1,SYSDATE(),''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id='000000' AND dict_type='gl_payment_settlement_payout_status');
INSERT INTO sys_dict_type
(dict_id,tenant_id,dict_name,dict_type,create_dept,create_by,create_time,remark)
SELECT 20048,'000000','Payment Settlement Payout Action Type','gl_payment_settlement_payout_action_type',103,1,SYSDATE(),''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id='000000' AND dict_type='gl_payment_settlement_payout_action_type');

INSERT INTO sys_dict_data
(dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,create_dept,create_by,create_time,remark)
SELECT * FROM (
 SELECT 21327 dict_code,'000000' tenant_id,1 dict_sort,'Draft' dict_label,'DRAFT' dict_value,
 'gl_payment_settlement_payout_status' dict_type,'' css_class,'info' list_class,'N' is_default,
 103 create_dept,1 create_by,SYSDATE() create_time,'' remark UNION ALL
 SELECT 21328,'000000',2,'Pending Approval','PENDING_APPROVAL','gl_payment_settlement_payout_status','','warning','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21329,'000000',3,'Approved','APPROVED','gl_payment_settlement_payout_status','','success','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21330,'000000',4,'Rejected','REJECTED','gl_payment_settlement_payout_status','','danger','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21331,'000000',5,'Cancelled','CANCELLED','gl_payment_settlement_payout_status','','info','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21332,'000000',1,'Create','CREATE','gl_payment_settlement_payout_action_type','','info','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21333,'000000',2,'Edit','EDIT','gl_payment_settlement_payout_action_type','','primary','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21334,'000000',3,'Submit','SUBMIT','gl_payment_settlement_payout_action_type','','warning','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21335,'000000',4,'Approve','APPROVE','gl_payment_settlement_payout_action_type','','success','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21336,'000000',5,'Reject','REJECT','gl_payment_settlement_payout_action_type','','danger','N',103,1,SYSDATE(),'' UNION ALL
 SELECT 21337,'000000',6,'Cancel','CANCEL','gl_payment_settlement_payout_action_type','','info','N',103,1,SYSDATE(),''
) payout_seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data existing WHERE existing.tenant_id=payout_seed.tenant_id
 AND existing.dict_type=payout_seed.dict_type AND existing.dict_value=payout_seed.dict_value);

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark FROM (
  SELECT 20035 dict_id, '000000' tenant_id, '追偿处置状态' dict_name, 'gl_purchase_reversal_disposition_status' dict_type, 103 create_dept, 1 create_by, SYSDATE() create_time, NULL update_by, NULL update_time, '购买追偿人工处置状态' remark UNION ALL
  SELECT 20036, '000000', '追偿审核操作', 'gl_purchase_reversal_review_operation_type', 103, 1, SYSDATE(), NULL, NULL, '购买追偿审核操作类型') seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type existing WHERE existing.tenant_id=seed.tenant_id AND existing.dict_type=seed.dict_type);

INSERT INTO sys_dict_data
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark
FROM (
  SELECT 21270 dict_code, '000000' tenant_id, 1 dict_sort, '待复核' dict_label, 'PENDING_REVIEW' dict_value, 'gl_purchase_reversal_disposition_status' dict_type, '' css_class, 'warning' list_class, 'Y' is_default, 103 create_dept, 1 create_by, SYSDATE() create_time, NULL update_by, NULL update_time, '追偿待人工复核' remark UNION ALL
  SELECT 21271, '000000', 2, '已追回', 'RECOVERY_COMPLETED', 'gl_purchase_reversal_disposition_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '人工再次全额追回' UNION ALL
  SELECT 21272, '000000', 3, '已确认损失', 'LOSS_ACCEPTED', 'gl_purchase_reversal_disposition_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '确认损失结案' UNION ALL
  SELECT 21273, '000000', 1, '重试仍不足', 'RETRY_INSUFFICIENT', 'gl_purchase_reversal_review_operation_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '再次追偿余额仍不足' UNION ALL
  SELECT 21274, '000000', 2, '重试已追回', 'RETRY_COMPLETED', 'gl_purchase_reversal_review_operation_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '再次追偿成功' UNION ALL
  SELECT 21275, '000000', 3, '确认损失', 'LOSS_ACCEPTED', 'gl_purchase_reversal_review_operation_type', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '确认损失结案') seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data existing WHERE existing.tenant_id=seed.tenant_id AND existing.dict_type=seed.dict_type AND existing.dict_value=seed.dict_value);

INSERT INTO sys_dict_data
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark
FROM (
  SELECT 21230 dict_code, '000000' tenant_id, 1 dict_sort, '已创建' dict_label, 'CREATED' dict_value, 'gl_purchase_order_status' dict_type, '' css_class, 'warning' list_class, 'N' is_default, 103 create_dept, 1 create_by, SYSDATE() create_time, NULL update_by, NULL update_time, '购买订单已创建' remark UNION ALL
  SELECT 21231, '000000', 2, '待支付', 'PENDING', 'gl_purchase_order_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '购买订单待支付' UNION ALL
  SELECT 21232, '000000', 3, '已支付', 'PAID', 'gl_purchase_order_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '购买订单已支付' UNION ALL
  SELECT 21233, '000000', 4, '已入账', 'CREDITED', 'gl_purchase_order_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '购买奖励已入账' UNION ALL
  SELECT 21234, '000000', 5, '失败', 'FAILED', 'gl_purchase_order_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '购买订单失败' UNION ALL
  SELECT 21235, '000000', 6, '已取消', 'CANCELLED', 'gl_purchase_order_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '购买订单已取消' UNION ALL
  SELECT 21236, '000000', 7, '已退款', 'REFUNDED', 'gl_purchase_order_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '购买订单已退款' UNION ALL
  SELECT 21237, '000000', 8, '拒付', 'CHARGEBACK', 'gl_purchase_order_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '购买订单拒付' UNION ALL
  SELECT 21238, '000000', 9, '退款待复核', 'REFUND_REVIEW', 'gl_purchase_order_status', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '退款资产追偿需要人工复核' UNION ALL
  SELECT 21239, '000000', 10, '拒付待复核', 'CHARGEBACK_REVIEW', 'gl_purchase_order_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '拒付资产追偿需要人工复核' UNION ALL
  SELECT 21240, '000000', 1, '支付成功', 'PAY_SUCCESS', 'gl_purchase_payment_event_type', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '支付成功事件' UNION ALL
  SELECT 21241, '000000', 2, '支付失败', 'PAY_FAILED', 'gl_purchase_payment_event_type', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '支付失败事件' UNION ALL
  SELECT 21242, '000000', 3, '已取消', 'CANCELLED', 'gl_purchase_payment_event_type', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '取消事件' UNION ALL
  SELECT 21243, '000000', 4, '已退款', 'REFUNDED', 'gl_purchase_payment_event_type', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '退款事件' UNION ALL
  SELECT 21244, '000000', 5, '拒付', 'CHARGEBACK', 'gl_purchase_payment_event_type', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '拒付事件' UNION ALL
  SELECT 21250, '000000', 1, '已接收', 'RECEIVED', 'gl_purchase_payment_event_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '事件已接收' UNION ALL
  SELECT 21251, '000000', 2, '已处理', 'PROCESSED', 'gl_purchase_payment_event_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '事件已处理' UNION ALL
  SELECT 21252, '000000', 3, '已忽略', 'IGNORED', 'gl_purchase_payment_event_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '事件已忽略' UNION ALL
  SELECT 21253, '000000', 4, '失败', 'FAILED', 'gl_purchase_payment_event_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '事件处理失败' UNION ALL
  SELECT 21260, '000000', 1, '退款追偿', 'REFUND', 'gl_purchase_reversal_type', '', 'info', 'Y', 103, 1, SYSDATE(), NULL, NULL, '退款资产追偿' UNION ALL
  SELECT 21261, '000000', 2, '拒付追偿', 'CHARGEBACK', 'gl_purchase_reversal_type', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '拒付资产追偿' UNION ALL
  SELECT 21262, '000000', 1, '处理中', 'PROCESSING', 'gl_purchase_reversal_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '资产追偿处理中' UNION ALL
  SELECT 21263, '000000', 2, '已完成', 'COMPLETED', 'gl_purchase_reversal_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '资产追偿已完成' UNION ALL
  SELECT 21264, '000000', 3, '需人工复核', 'REVIEW_REQUIRED', 'gl_purchase_reversal_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '资产追偿需要人工复核'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
    AND existing.dict_value = seed.dict_value
);

INSERT INTO sys_dict_data
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 21225, '000000', 5, '已过期', 'EXPIRED', 'gl_kyc_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, 'KYC已过期'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = '000000'
    AND dict_type = 'gl_kyc_status'
    AND dict_value = 'EXPIRED'
);

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark
FROM (
  SELECT 20018 dict_id, '000000' tenant_id, '钱包资金属性' dict_name, 'gl_wallet_fund_property' dict_type, 103 create_dept, 1 create_by, SYSDATE() create_time, NULL update_by, NULL update_time, '钱包入账资金属性' remark UNION ALL
  SELECT 20019, '000000', '钱包游戏范围', 'gl_wallet_game_scope_type', 103, 1, SYSDATE(), NULL, NULL, '流水可核销游戏范围' UNION ALL
  SELECT 20020, '000000', '钱包流水任务状态', 'gl_wallet_turnover_task_status', 103, 1, SYSDATE(), NULL, NULL, '流水任务生命周期状态' UNION ALL
  SELECT 20021, '000000', '钱包兑换汇率类型', 'gl_wallet_exchange_rate_type', 103, 1, SYSDATE(), NULL, NULL, '币种兑换汇率类型' UNION ALL
  SELECT 20022, '000000', '钱包兑换手续费类型', 'gl_wallet_exchange_fee_type', 103, 1, SYSDATE(), NULL, NULL, '币种兑换手续费类型' UNION ALL
  SELECT 20023, '000000', '钱包兑换订单状态', 'gl_wallet_exchange_order_status', 103, 1, SYSDATE(), NULL, NULL, '币种兑换订单生命周期状态' UNION ALL
  SELECT 20024, '000000', '钱包币种可见渠道', 'gl_wallet_policy_channel', 103, 1, SYSDATE(), NULL, NULL, '币种可见策略渠道条件' UNION ALL
  SELECT 20025, '000000', '购买产品类型', 'gl_purchase_offer_type', 103, 1, SYSDATE(), NULL, NULL, '购买产品和购买活动类型' UNION ALL
  SELECT 20026, '000000', '购买产品状态', 'gl_purchase_offer_status', 103, 1, SYSDATE(), NULL, NULL, '购买产品启停状态' UNION ALL
  SELECT 20027, '000000', '购买发放类型', 'gl_purchase_grant_type', 103, 1, SYSDATE(), NULL, NULL, '购买成功后发放项类型' UNION ALL
  SELECT 20028, '000000', '购买流水模式', 'gl_purchase_wagering_mode', 103, 1, SYSDATE(), NULL, NULL, '购买发放项流水要求模式' UNION ALL
  SELECT 20029, '000000', '购买限购类型', 'gl_purchase_limit_type', 103, 1, SYSDATE(), NULL, NULL, '购买产品限购规则'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark
FROM (
  SELECT 21100 dict_code, '000000' tenant_id, 1 dict_sort, '充值本金' dict_label, 'DEPOSIT_PRINCIPAL' dict_value, 'gl_wallet_fund_property' dict_type, '' css_class, 'primary' list_class, 'Y' is_default, 103 create_dept, 1 create_by, SYSDATE() create_time, NULL update_by, NULL update_time, '用户充值到账的本金' remark UNION ALL
  SELECT 21101, '000000', 2, '充值赠送', 'DEPOSIT_BONUS', 'gl_wallet_fund_property', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '充值活动赠送金额' UNION ALL
  SELECT 21102, '000000', 3, '活动奖励', 'ACTIVITY_REWARD', 'gl_wallet_fund_property', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '通用活动奖励' UNION ALL
  SELECT 21103, '000000', 4, '每日奖励', 'DAILY_REWARD', 'gl_wallet_fund_property', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '每日登录等奖励' UNION ALL
  SELECT 21104, '000000', 5, '返佣奖励', 'COMMISSION', 'gl_wallet_fund_property', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '代理或邀请返佣奖励' UNION ALL
  SELECT 21105, '000000', 6, '游戏盈利', 'GAME_PROFIT', 'gl_wallet_fund_property', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏结算产生的收益' UNION ALL
  SELECT 21106, '000000', 7, '游戏退款', 'GAME_REFUND', 'gl_wallet_fund_property', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏取消、失败或回滚退款' UNION ALL
  SELECT 21107, '000000', 8, '人工调账', 'MANUAL_ADJUST', 'gl_wallet_fund_property', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '人工调账金额' UNION ALL
  SELECT 21108, '000000', 9, '兑换入账', 'EXCHANGE_IN', 'gl_wallet_fund_property', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '币种兑换目标币种入账' UNION ALL
  SELECT 21120, '000000', 1, '全部游戏', 'ALL', 'gl_wallet_game_scope_type', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '全部可核销游戏' UNION ALL
  SELECT 21121, '000000', 2, '指定分类', 'CATEGORY', 'gl_wallet_game_scope_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '指定游戏分类' UNION ALL
  SELECT 21122, '000000', 3, '指定厂商', 'PROVIDER', 'gl_wallet_game_scope_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '指定游戏厂商' UNION ALL
  SELECT 21123, '000000', 4, '指定游戏', 'GAME', 'gl_wallet_game_scope_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '指定游戏ID' UNION ALL
  SELECT 21130, '000000', 1, '待完成', 'PENDING', 'gl_wallet_turnover_task_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '流水任务待完成' UNION ALL
  SELECT 21131, '000000', 2, '已完成', 'COMPLETED', 'gl_wallet_turnover_task_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '流水任务已完成' UNION ALL
  SELECT 21132, '000000', 3, '已过期', 'EXPIRED', 'gl_wallet_turnover_task_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '流水任务已过期' UNION ALL
  SELECT 21133, '000000', 4, '已取消', 'CANCELLED', 'gl_wallet_turnover_task_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '流水任务已取消' UNION ALL
  SELECT 21140, '000000', 1, '固定汇率', 'FIXED', 'gl_wallet_exchange_rate_type', '', 'primary', 'Y', 103, 1, SYSDATE(), NULL, NULL, '固定兑换汇率' UNION ALL
  SELECT 21141, '000000', 2, '阶梯汇率', 'TIERED', 'gl_wallet_exchange_rate_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '按金额阶梯配置汇率' UNION ALL
  SELECT 21142, '000000', 3, '活动汇率', 'ACTIVITY', 'gl_wallet_exchange_rate_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '活动专属兑换汇率' UNION ALL
  SELECT 21150, '000000', 1, '无手续费', 'NONE', 'gl_wallet_exchange_fee_type', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '不收取兑换手续费' UNION ALL
  SELECT 21151, '000000', 2, '固定手续费', 'FIXED', 'gl_wallet_exchange_fee_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '固定兑换手续费' UNION ALL
  SELECT 21152, '000000', 3, '按比例手续费', 'PERCENT', 'gl_wallet_exchange_fee_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '按兑换金额比例收取手续费' UNION ALL
  SELECT 21160, '000000', 1, '待处理', 'PENDING', 'gl_wallet_exchange_order_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, '兑换订单待处理' UNION ALL
  SELECT 21161, '000000', 2, '成功', 'SUCCESS', 'gl_wallet_exchange_order_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换订单成功' UNION ALL
  SELECT 21162, '000000', 3, '失败', 'FAILED', 'gl_wallet_exchange_order_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换订单失败' UNION ALL
  SELECT 21163, '000000', 4, '已取消', 'CANCELLED', 'gl_wallet_exchange_order_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换订单已取消' UNION ALL
  SELECT 21170, '000000', 1, 'H5', 'H5', 'gl_wallet_policy_channel', '', 'primary', 'Y', 103, 1, SYSDATE(), NULL, NULL, 'H5 client' UNION ALL
  SELECT 21171, '000000', 2, 'APP', 'APP', 'gl_wallet_policy_channel', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, 'Native app client' UNION ALL
  SELECT 21172, '000000', 3, '后台', 'ADMIN', 'gl_wallet_policy_channel', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '后台运营渠道' UNION ALL
  SELECT 21180, '000000', 1, '普通购买', 'STANDARD', 'gl_purchase_offer_type', '', 'primary', 'Y', 103, 1, SYSDATE(), NULL, NULL, '常规购买产品' UNION ALL
  SELECT 21181, '000000', 2, '首充购买', 'FIRST_PURCHASE', 'gl_purchase_offer_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '用户首次购买产品' UNION ALL
  SELECT 21182, '000000', 3, '充值活动', 'CAMPAIGN', 'gl_purchase_offer_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '运营配置的购买活动' UNION ALL
  SELECT 21183, '000000', 4, '折扣购买', 'DISCOUNT', 'gl_purchase_offer_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '折扣购买产品' UNION ALL
  SELECT 21184, '000000', 5, '召回购买', 'RECALL', 'gl_purchase_offer_type', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '召回用户购买产品' UNION ALL
  SELECT 21190, '000000', 1, '启用', '0', 'gl_purchase_offer_status', '', 'success', 'Y', 103, 1, SYSDATE(), NULL, NULL, '产品可见可购买' UNION ALL
  SELECT 21191, '000000', 2, '停用', '1', 'gl_purchase_offer_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, '产品不可购买' UNION ALL
  SELECT 21200, '000000', 1, '购买获得', 'PURCHASE_GRANT', 'gl_purchase_grant_type', '', 'primary', 'Y', 103, 1, SYSDATE(), NULL, NULL, '购买后获得的基础额度' UNION ALL
  SELECT 21201, '000000', 2, '购买赠送', 'PURCHASE_BONUS', 'gl_purchase_grant_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '购买活动赠送额度' UNION ALL
  SELECT 21202, '000000', 3, '入金本金', 'DEPOSIT_PRINCIPAL', 'gl_purchase_grant_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '未来真金模式入金本金' UNION ALL
  SELECT 21203, '000000', 4, '入金赠送', 'DEPOSIT_BONUS', 'gl_purchase_grant_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '未来真金模式入金赠送' UNION ALL
  SELECT 21210, '000000', 1, '不需要流水', 'NONE', 'gl_purchase_wagering_mode', '', 'info', 'Y', 103, 1, SYSDATE(), NULL, NULL, '发放后不生成流水义务' UNION ALL
  SELECT 21211, '000000', 2, '固定流水金额', 'FIXED', 'gl_purchase_wagering_mode', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '按固定金额要求流水' UNION ALL
  SELECT 21212, '000000', 3, '流水倍数', 'MULTIPLIER', 'gl_purchase_wagering_mode', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '按发放金额乘以倍数要求流水' UNION ALL
  SELECT 21213, '000000', 4, '组合倍数', 'COMBINED_MULTIPLIER', 'gl_purchase_wagering_mode', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '按订单发放组合金额计算流水，首期暂不启用' UNION ALL
  SELECT 21220, '000000', 1, '不限购', 'NONE', 'gl_purchase_limit_type', '', 'info', 'Y', 103, 1, SYSDATE(), NULL, NULL, '不限制购买次数' UNION ALL
  SELECT 21221, '000000', 2, '仅首购', 'FIRST_ONLY', 'gl_purchase_limit_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '每个用户仅首次可购买' UNION ALL
  SELECT 21222, '000000', 3, '每日一次', 'DAILY_ONCE', 'gl_purchase_limit_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '每个用户每日最多购买一次' UNION ALL
  SELECT 21223, '000000', 4, '总计一次', 'TOTAL_ONCE', 'gl_purchase_limit_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '每个用户总计最多购买一次' UNION ALL
  SELECT 21224, '000000', 5, '周期限购', 'PERIOD_LIMIT', 'gl_purchase_limit_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '按周期配置购买次数'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
    AND existing.dict_value = seed.dict_value
);
