-- Daily login reward migration and default seed.
-- Import with:
-- .\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_daily_login_reward.sql

SET @db_name := DATABASE();

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_reward' AND COLUMN_NAME = 'promotion_type'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_promotion_reward ADD COLUMN promotion_type VARCHAR(64) NOT NULL DEFAULT ''GENERAL'' COMMENT ''Promotion type'' AFTER promotion_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_reward' AND COLUMN_NAME = 'claim_cycle'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_promotion_reward ADD COLUMN claim_cycle VARCHAR(32) NOT NULL DEFAULT ''ONCE'' COMMENT ''Claim cycle'' AFTER reward_amount',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_reward' AND COLUMN_NAME = 'daily_claim_limit'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_promotion_reward ADD COLUMN daily_claim_limit INT NOT NULL DEFAULT 1 COMMENT ''Daily claim limit'' AFTER claim_cycle',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_reward' AND COLUMN_NAME = 'reward_items'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_promotion_reward ADD COLUMN reward_items JSON DEFAULT NULL COMMENT ''Reward item snapshot'' AFTER daily_claim_limit',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_claim' AND COLUMN_NAME = 'promotion_type'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_promotion_claim ADD COLUMN promotion_type VARCHAR(64) DEFAULT NULL COMMENT ''Promotion type'' AFTER promotion_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_claim' AND COLUMN_NAME = 'claim_date'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_promotion_claim ADD COLUMN claim_date DATE NOT NULL DEFAULT ''1000-01-01'' COMMENT ''Claim date'' AFTER reward_amount',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE gl_promotion_claim
SET claim_date = '1000-01-01'
WHERE claim_date IS NULL;

SET @claim_date_nullable := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_claim' AND COLUMN_NAME = 'claim_date'
);
SET @sql := IF(@claim_date_nullable = 'YES',
  'ALTER TABLE gl_promotion_claim MODIFY COLUMN claim_date DATE NOT NULL DEFAULT ''1000-01-01'' COMMENT ''Claim date''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_claim' AND COLUMN_NAME = 'reward_snapshot'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_promotion_claim ADD COLUMN reward_snapshot JSON DEFAULT NULL COMMENT ''Reward snapshot'' AFTER claim_date',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @wallet_no_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_claim' AND COLUMN_NAME = 'wallet_transaction_no'
);
SET @sql := IF(@wallet_no_len IS NOT NULL AND @wallet_no_len < 512,
  'ALTER TABLE gl_promotion_claim MODIFY COLUMN wallet_transaction_no VARCHAR(512) DEFAULT NULL COMMENT ''Wallet transaction number''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @claim_idx_columns := (
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_claim' AND INDEX_NAME = 'uk_gl_promotion_claim_03'
);

SET @duplicate_claim_count := (
  SELECT COUNT(*)
  FROM (
    SELECT tenant_id, promotion_id, member_id, claim_date
    FROM gl_promotion_claim
    GROUP BY tenant_id, promotion_id, member_id, claim_date
    HAVING COUNT(*) > 1
  ) duplicate_claims
);
SET @sql := IF(@duplicate_claim_count > 0,
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Duplicate promotion claims block daily login reward index migration''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@claim_idx_columns IS NOT NULL AND @claim_idx_columns <> 'tenant_id,promotion_id,member_id,claim_date',
  'ALTER TABLE gl_promotion_claim DROP INDEX uk_gl_promotion_claim_03',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_promotion_claim' AND INDEX_NAME = 'uk_gl_promotion_claim_03'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE gl_promotion_claim ADD UNIQUE KEY uk_gl_promotion_claim_03 (tenant_id, promotion_id, member_id, claim_date)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO gl_wallet_rule
(id, tenant_id, currency_code, source_type, rule_name, credit_enabled, debit_enabled, withdraw_enabled, exchange_enabled, release_mode, turnover_required, default_required_turnover, status, sort_order, remark, create_time)
VALUES
(1900000000000000611, '000000', 'GC', 'DAILY_REWARD', 'GC daily login reward', '0', '1', '1', '1', 'IMMEDIATE', '1', 0, '0', 11, 'Daily login GC reward.', NOW()),
(1900000000000000612, '000000', 'SC', 'DAILY_REWARD', 'SC daily login reward', '0', '1', '0', '1', 'IMMEDIATE', '1', 0, '0', 12, 'Daily login SC reward.', NOW())
ON DUPLICATE KEY UPDATE
  rule_name = VALUES(rule_name),
  credit_enabled = VALUES(credit_enabled),
  debit_enabled = VALUES(debit_enabled),
  withdraw_enabled = VALUES(withdraw_enabled),
  exchange_enabled = VALUES(exchange_enabled),
  release_mode = VALUES(release_mode),
  turnover_required = VALUES(turnover_required),
  default_required_turnover = VALUES(default_required_turnover),
  status = VALUES(status),
  sort_order = VALUES(sort_order),
  remark = VALUES(remark),
  update_time = NOW();

INSERT INTO gl_promotion_reward
(id, tenant_id, promotion_no, promotion_name, promotion_type, currency_code, reward_amount, claim_cycle, daily_claim_limit, reward_items, status, start_time, end_time, remark, create_time)
VALUES
(1900000000000000901, '000000', 'PR-DAILY-LOGIN-DEFAULT', 'Daily Login Reward', 'DAILY_LOGIN', 'GC', 100.000000, 'DAILY', 1,
 JSON_ARRAY(JSON_OBJECT('currencyCode', 'GC', 'rewardAmount', '100.000000'), JSON_OBJECT('currencyCode', 'SC', 'rewardAmount', '1.000000')),
 'ACTIVE', NULL, NULL, 'Default configurable daily login reward.', NOW())
ON DUPLICATE KEY UPDATE
  promotion_name = VALUES(promotion_name),
  promotion_type = VALUES(promotion_type),
  currency_code = VALUES(currency_code),
  reward_amount = VALUES(reward_amount),
  claim_cycle = VALUES(claim_cycle),
  daily_claim_limit = VALUES(daily_claim_limit),
  reward_items = VALUES(reward_items),
  status = VALUES(status),
  remark = VALUES(remark),
  update_time = NOW();
