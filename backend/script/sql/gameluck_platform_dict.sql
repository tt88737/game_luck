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
  SELECT 21030, '000000', 5, '游戏派彩', 'GAME_PAYOUT', 'gl_wallet_biz_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, '游戏派彩账变' UNION ALL
  SELECT 21031, '000000', 6, '充值入账', 'DEPOSIT', 'gl_wallet_biz_type', '', 'primary', 'N', 103, 1, SYSDATE(), NULL, NULL, '充值入账账变' UNION ALL
  SELECT 21032, '000000', 7, '兑换处理', 'REDEMPTION', 'gl_wallet_biz_type', '', 'warning', 'N', 103, 1, SYSDATE(), NULL, NULL, '兑换处理账变' UNION ALL
  SELECT 21033, '000000', 8, '人工调账', 'ADJUSTMENT', 'gl_wallet_biz_type', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '人工调账账变' UNION ALL
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
