-- Wallet Center v1 schema and default currency seed.

CREATE TABLE IF NOT EXISTS gl_wallet_currency (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  currency_name VARCHAR(64) NOT NULL COMMENT 'Currency name',
  scale_num TINYINT NOT NULL DEFAULT 6 COMMENT 'Decimal scale',
  enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Enabled status: 0 enabled, 1 disabled',
  credit_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Credit allowed: 0 yes, 1 no',
  debit_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Debit allowed: 0 yes, 1 no',
  freeze_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Freeze allowed: 0 yes, 1 no',
  deposit_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Deposit capable: 0 yes, 1 no',
  withdraw_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Withdraw capable: 0 yes, 1 no',
  exchange_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange capable: 0 yes, 1 no',
  exchange_in_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange-in capable: 0 yes, 1 no',
  exchange_out_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange-out capable: 0 yes, 1 no',
  play_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Play capable: 0 yes, 1 no',
  negative_allowed CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Negative balance allowed: 0 yes, 1 no',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_currency_01 (tenant_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet currency config';

CREATE TABLE IF NOT EXISTS gl_wallet_account (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL COMMENT 'Tenant id',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  available_balance DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Available balance',
  frozen_balance DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Frozen balance',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Account status: 0 normal, 1 frozen, 2 disabled',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_account_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_wallet_account_01 (tenant_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Member wallet account';

CREATE TABLE IF NOT EXISTS gl_wallet_rule (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  source_type VARCHAR(64) NOT NULL COMMENT 'Source type',
  rule_name VARCHAR(128) NOT NULL COMMENT 'Rule name',
  credit_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Credit allowed: 0 yes, 1 no',
  debit_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Debit allowed: 0 yes, 1 no',
  withdraw_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Withdraw capable: 0 yes, 1 no',
  exchange_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange capable: 0 yes, 1 no',
  release_mode VARCHAR(32) NOT NULL DEFAULT 'NEVER' COMMENT 'Release mode',
  turnover_required CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Business turnover required: 0 yes, 1 no',
  default_required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Default required turnover',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 enabled, 1 disabled',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_rule_01 (tenant_id, currency_code, source_type),
  KEY idx_gl_wallet_rule_01 (tenant_id, currency_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet source rule';

CREATE TABLE IF NOT EXISTS gl_wallet_transaction (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL COMMENT 'Tenant id',
  transaction_no VARCHAR(64) NOT NULL COMMENT 'Wallet transaction no',
  idempotency_key VARCHAR(128) NOT NULL COMMENT 'Idempotency key',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  operation VARCHAR(32) NOT NULL COMMENT 'Operation type',
  source_type VARCHAR(64) NOT NULL COMMENT 'Source type',
  business_no VARCHAR(128) NOT NULL COMMENT 'Business no',
  amount DECIMAL(20,6) NOT NULL COMMENT 'Amount',
  balance_before DECIMAL(20,6) NOT NULL COMMENT 'Available balance before change',
  balance_after DECIMAL(20,6) NOT NULL COMMENT 'Available balance after change',
  frozen_before DECIMAL(20,6) NOT NULL COMMENT 'Frozen balance before change',
  frozen_after DECIMAL(20,6) NOT NULL COMMENT 'Frozen balance after change',
  release_mode VARCHAR(32) DEFAULT NULL COMMENT 'Release mode',
  required_turnover DECIMAL(20,6) DEFAULT NULL COMMENT 'Required turnover',
  request_hash VARCHAR(128) NOT NULL COMMENT 'Request parameter hash',
  status VARCHAR(32) NOT NULL COMMENT 'Transaction status',
  fail_code VARCHAR(64) DEFAULT NULL COMMENT 'Failure code',
  fail_reason VARCHAR(500) DEFAULT NULL COMMENT 'Failure reason',
  operator_id BIGINT DEFAULT NULL COMMENT 'Operator id',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_transaction_01 (tenant_id, transaction_no),
  UNIQUE KEY uk_gl_wallet_transaction_02 (tenant_id, idempotency_key),
  KEY idx_gl_wallet_transaction_01 (tenant_id, member_id, currency_code, create_time),
  KEY idx_gl_wallet_transaction_02 (tenant_id, business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet transaction ledger';

CREATE TABLE IF NOT EXISTS gl_wallet_release (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL COMMENT 'Tenant id',
  release_no VARCHAR(64) NOT NULL COMMENT 'Release record no',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  source_type VARCHAR(64) NOT NULL COMMENT 'Source type',
  business_no VARCHAR(128) NOT NULL COMMENT 'Business no',
  amount DECIMAL(20,6) NOT NULL COMMENT 'Credited amount',
  released_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Released amount',
  consumed_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Consumed released amount',
  required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Required turnover',
  completed_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Completed turnover',
  release_mode VARCHAR(32) NOT NULL COMMENT 'Release mode',
  release_status VARCHAR(32) NOT NULL COMMENT 'Release status',
  metadata JSON DEFAULT NULL COMMENT 'Extended metadata',
  operator_id BIGINT DEFAULT NULL COMMENT 'Operator id',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_release_01 (tenant_id, release_no),
  KEY idx_gl_wallet_release_01 (tenant_id, member_id, currency_code, release_status, create_time),
  KEY idx_gl_wallet_release_02 (tenant_id, business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet release record';

CREATE TABLE IF NOT EXISTS gl_wallet_freeze (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL COMMENT 'Tenant id',
  freeze_no VARCHAR(64) NOT NULL COMMENT 'Freeze order no',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  amount DECIMAL(20,6) NOT NULL COMMENT 'Frozen amount',
  source_type VARCHAR(64) NOT NULL COMMENT 'Source type',
  business_no VARCHAR(128) NOT NULL COMMENT 'Business no',
  status VARCHAR(32) NOT NULL COMMENT 'Freeze status',
  operator_id BIGINT DEFAULT NULL COMMENT 'Operator id',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_freeze_01 (tenant_id, freeze_no),
  KEY idx_gl_wallet_freeze_01 (tenant_id, member_id, currency_code, create_time),
  KEY idx_gl_wallet_freeze_02 (tenant_id, business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet freeze record';

SET @db_name := DATABASE();

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency' AND COLUMN_NAME = 'deposit_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency ADD COLUMN deposit_enabled CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''Deposit capable: 0 yes, 1 no'' AFTER freeze_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency' AND COLUMN_NAME = 'exchange_in_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency ADD COLUMN exchange_in_enabled CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''Exchange-in capable: 0 yes, 1 no'' AFTER exchange_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency' AND COLUMN_NAME = 'exchange_out_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency ADD COLUMN exchange_out_enabled CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''Exchange-out capable: 0 yes, 1 no'' AFTER exchange_in_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency' AND COLUMN_NAME = 'play_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency ADD COLUMN play_enabled CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''Play capable: 0 yes, 1 no'' AFTER exchange_out_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS gl_wallet_fund_property_template (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  property_code VARCHAR(64) NOT NULL COMMENT 'Fund property code',
  property_name VARCHAR(128) NOT NULL COMMENT 'Fund property name',
  default_source_type VARCHAR(64) NOT NULL COMMENT 'Default wallet source type',
  default_turnover_mode VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE, FIXED, MULTIPLIER',
  default_turnover_required_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Default fixed turnover amount',
  default_turnover_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'Default turnover multiplier',
  default_game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL, CATEGORY, PROVIDER, GAME',
  default_game_scope_value VARCHAR(1000) DEFAULT NULL COMMENT 'Default game scope value',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 enabled, 1 disabled',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_fund_property_template_01 (tenant_id, property_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet fund property template';

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_fund_property_template' AND COLUMN_NAME = 'default_turnover_mode'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_fund_property_template ADD COLUMN default_turnover_mode VARCHAR(32) NOT NULL DEFAULT ''NONE'' COMMENT ''NONE, FIXED, MULTIPLIER'' AFTER default_source_type',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_fund_property_template' AND COLUMN_NAME = 'withdraw_enabled'
);
SET @sql := IF(@col_exists > 0,
  'ALTER TABLE gl_wallet_fund_property_template DROP COLUMN withdraw_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_fund_property_template' AND COLUMN_NAME = 'exchange_enabled'
);
SET @sql := IF(@col_exists > 0,
  'ALTER TABLE gl_wallet_fund_property_template DROP COLUMN exchange_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_fund_property_template' AND COLUMN_NAME = 'default_release_mode'
);
SET @sql := IF(@col_exists > 0,
  'ALTER TABLE gl_wallet_fund_property_template DROP COLUMN default_release_mode',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_fund_property_template' AND COLUMN_NAME = 'default_turnover_required_amount'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_fund_property_template ADD COLUMN default_turnover_required_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT ''Default fixed turnover amount'' AFTER default_turnover_mode',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS gl_wallet_currency_policy (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  policy_name VARCHAR(128) NOT NULL COMMENT 'Policy name',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  member_tag VARCHAR(64) DEFAULT NULL COMMENT 'Member tag condition',
  vip_level VARCHAR(32) DEFAULT NULL COMMENT 'VIP level condition',
  country_code VARCHAR(16) DEFAULT NULL COMMENT 'Country condition',
  state_code VARCHAR(32) DEFAULT NULL COMMENT 'State condition',
  channel VARCHAR(32) DEFAULT NULL COMMENT 'Channel condition',
  visible_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Visible: 0 yes, 1 no',
  deposit_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Deposit: 0 yes, 1 no',
  withdraw_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Withdraw: 0 yes, 1 no',
  exchange_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange: 0 yes, 1 no',
  play_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Play: 0 yes, 1 no',
  priority INT NOT NULL DEFAULT 0 COMMENT 'Higher priority wins; deny remains strict',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 enabled, 1 disabled',
  start_time DATETIME DEFAULT NULL COMMENT 'Start time',
  end_time DATETIME DEFAULT NULL COMMENT 'End time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  KEY idx_gl_wallet_currency_policy_01 (tenant_id, currency_code, status, priority),
  KEY idx_gl_wallet_currency_policy_02 (tenant_id, country_code, state_code, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet currency visibility and action policy';

CREATE TABLE IF NOT EXISTS gl_wallet_turnover_task (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  turnover_task_no VARCHAR(64) NOT NULL COMMENT 'Turnover task no',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  fund_property_code VARCHAR(64) NOT NULL COMMENT 'Fund property code',
  source_type VARCHAR(64) NOT NULL COMMENT 'Wallet source type',
  source_id VARCHAR(128) DEFAULT NULL COMMENT 'Source id',
  business_no VARCHAR(128) NOT NULL COMMENT 'Business no',
  wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Wallet transaction no',
  reward_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Reward amount',
  required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Required turnover',
  completed_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Completed turnover',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL, CATEGORY, PROVIDER, GAME',
  game_scope_value VARCHAR(1000) DEFAULT NULL COMMENT 'Allowed game scope',
  rule_snapshot JSON DEFAULT NULL COMMENT 'Immutable rule snapshot',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, COMPLETED, EXPIRED, CANCELLED',
  expire_time DATETIME DEFAULT NULL COMMENT 'Expire time',
  complete_time DATETIME DEFAULT NULL COMMENT 'Complete time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_turnover_task_01 (tenant_id, turnover_task_no),
  KEY idx_gl_wallet_turnover_task_01 (tenant_id, member_id, currency_code, status),
  KEY idx_gl_wallet_turnover_task_02 (tenant_id, business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet turnover task snapshot';

CREATE TABLE IF NOT EXISTS gl_wallet_exchange_rule (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  rule_name VARCHAR(128) NOT NULL COMMENT 'Exchange rule name',
  from_currency_code VARCHAR(32) NOT NULL COMMENT 'Source currency code',
  to_currency_code VARCHAR(32) NOT NULL COMMENT 'Target currency code',
  rate_type VARCHAR(32) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED, TIERED, ACTIVITY',
  rate_value DECIMAL(20,8) NOT NULL COMMENT 'Target amount per one source unit',
  min_from_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Minimum source amount',
  max_from_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Maximum source amount, 0 unlimited',
  daily_from_limit DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Daily source amount limit, 0 unlimited',
  fee_type VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE, FIXED, PERCENT',
  fee_value DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Fee value',
  turnover_required CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Target turnover required: 0 yes, 1 no',
  turnover_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'Target turnover multiplier',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL, CATEGORY, PROVIDER, GAME',
  game_scope_value VARCHAR(1000) DEFAULT NULL COMMENT 'Allowed game scope',
  country_code VARCHAR(16) DEFAULT NULL COMMENT 'Country condition',
  state_code VARCHAR(32) DEFAULT NULL COMMENT 'State condition',
  member_tag VARCHAR(64) DEFAULT NULL COMMENT 'Member tag condition',
  channel VARCHAR(32) DEFAULT NULL COMMENT 'Channel condition',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 enabled, 1 disabled',
  start_time DATETIME DEFAULT NULL COMMENT 'Start time',
  end_time DATETIME DEFAULT NULL COMMENT 'End time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  KEY idx_gl_wallet_exchange_rule_01 (tenant_id, from_currency_code, to_currency_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet currency exchange rule';

CREATE TABLE IF NOT EXISTS gl_wallet_exchange_order (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  exchange_order_no VARCHAR(64) NOT NULL COMMENT 'Exchange order no',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  exchange_rule_id BIGINT NOT NULL COMMENT 'Exchange rule id',
  from_currency_code VARCHAR(32) NOT NULL COMMENT 'Source currency code',
  from_amount DECIMAL(20,6) NOT NULL COMMENT 'Source amount',
  to_currency_code VARCHAR(32) NOT NULL COMMENT 'Target currency code',
  to_amount DECIMAL(20,6) NOT NULL COMMENT 'Target amount',
  fee_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Fee amount',
  debit_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Debit wallet transaction no',
  credit_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Credit wallet transaction no',
  turnover_task_no VARCHAR(64) DEFAULT NULL COMMENT 'Generated turnover task no',
  rule_snapshot JSON DEFAULT NULL COMMENT 'Immutable rule snapshot',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SUCCESS, FAILED, CANCELLED',
  fail_reason VARCHAR(500) DEFAULT NULL COMMENT 'Fail reason',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_exchange_order_01 (tenant_id, exchange_order_no),
  KEY idx_gl_wallet_exchange_order_01 (tenant_id, member_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet currency exchange order';

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_fund_property_template' AND COLUMN_NAME = 'create_dept'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_fund_property_template ADD COLUMN create_dept BIGINT DEFAULT NULL COMMENT ''Create department'' AFTER remark, ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT ''Created by'' AFTER create_dept, ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT ''Updated by'' AFTER create_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency_policy' AND COLUMN_NAME = 'create_dept'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency_policy ADD COLUMN create_dept BIGINT DEFAULT NULL COMMENT ''Create department'' AFTER remark, ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT ''Created by'' AFTER create_dept, ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT ''Updated by'' AFTER create_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_exchange_rule' AND COLUMN_NAME = 'create_dept'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_exchange_rule ADD COLUMN create_dept BIGINT DEFAULT NULL COMMENT ''Create department'' AFTER remark, ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT ''Created by'' AFTER create_dept, ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT ''Updated by'' AFTER create_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_exchange_order' AND COLUMN_NAME = 'create_dept'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_exchange_order ADD COLUMN create_dept BIGINT DEFAULT NULL COMMENT ''Create department'' AFTER fail_reason, ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT ''Created by'' AFTER create_dept, ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT ''Updated by'' AFTER create_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO gl_wallet_currency
(id, tenant_id, currency_code, currency_name, scale_num, enabled, credit_enabled, debit_enabled, freeze_enabled, deposit_enabled, withdraw_enabled, exchange_enabled, exchange_in_enabled, exchange_out_enabled, play_enabled, negative_allowed, sort_order, remark, create_time)
VALUES
(1900000000000000001, '000000', 'GC', 'Gold Coin', 6, '0', '0', '0', '0', '1', '1', '1', '1', '1', '0', '1', 1, 'Default play currency. Withdraw, deposit, and exchange disabled.', NOW()),
(1900000000000000002, '000000', 'SC', 'Sweep Coin', 6, '0', '0', '0', '0', '1', '0', '0', '0', '0', '0', '1', 2, 'Default sweep currency. Withdraw and exchange capability enabled.', NOW()),
(1900000000000000003, '000000', 'RC', 'Real Cash', 6, '0', '0', '0', '0', '0', '0', '1', '1', '1', '1', '1', 3, 'Default cash currency. Deposit and withdraw enabled; exchange execution disabled by default.', NOW())
ON DUPLICATE KEY UPDATE
  currency_name = VALUES(currency_name),
  scale_num = VALUES(scale_num),
  enabled = VALUES(enabled),
  credit_enabled = VALUES(credit_enabled),
  debit_enabled = VALUES(debit_enabled),
  freeze_enabled = VALUES(freeze_enabled),
  deposit_enabled = VALUES(deposit_enabled),
  withdraw_enabled = VALUES(withdraw_enabled),
  exchange_enabled = VALUES(exchange_enabled),
  exchange_in_enabled = VALUES(exchange_in_enabled),
  exchange_out_enabled = VALUES(exchange_out_enabled),
  play_enabled = VALUES(play_enabled),
  negative_allowed = VALUES(negative_allowed),
  sort_order = VALUES(sort_order),
  remark = VALUES(remark),
  update_time = NOW();

INSERT INTO gl_wallet_fund_property_template
(id, tenant_id, property_code, property_name, default_source_type, default_turnover_mode, default_turnover_required_amount, default_turnover_multiplier, default_game_scope_type, default_game_scope_value, status, sort_order, remark, create_time)
VALUES
(1900000000000001001, '000000', 'DEPOSIT_PRINCIPAL', '充值本金', 'DEPOSIT', 'MULTIPLIER', 0, 1.0000, 'ALL', NULL, '0', 10, '用户充值到账的本金，默认完成1倍流水后可提现。', NOW()),
(1900000000000001002, '000000', 'DEPOSIT_BONUS', '充值赠送', 'PROMOTION', 'MULTIPLIER', 0, 10.0000, 'ALL', NULL, '0', 20, '充值活动赠送金额，通常需要更高流水。', NOW()),
(1900000000000001003, '000000', 'ACTIVITY_REWARD', '活动奖励', 'PROMOTION', 'NONE', 0, 0.0000, 'ALL', NULL, '0', 30, '通用活动奖励，具体活动可覆盖流水和可核销游戏范围。', NOW()),
(1900000000000001004, '000000', 'DAILY_REWARD', '每日奖励', 'DAILY_REWARD', 'NONE', 0, 0.0000, 'ALL', NULL, '0', 40, '每日登录等奖励的默认资金属性。', NOW()),
(1900000000000001005, '000000', 'COMMISSION', '返佣奖励', 'COMMISSION', 'MULTIPLIER', 0, 1.0000, 'ALL', NULL, '0', 50, '代理或邀请返佣奖励。', NOW()),
(1900000000000001006, '000000', 'GAME_PROFIT', '游戏盈利', 'GAME_PROFIT', 'MULTIPLIER', 0, 1.0000, 'ALL', NULL, '0', 60, '游戏结算产生的收益。', NOW()),
(1900000000000001007, '000000', 'GAME_REFUND', '游戏退款', 'GAME_REFUND', 'NONE', 0, 0.0000, 'ALL', NULL, '0', 70, '游戏取消、失败或回滚产生的退款。', NOW()),
(1900000000000001008, '000000', 'MANUAL_ADJUST', '人工调账', 'MANUAL_ADJUST', 'NONE', 0, 0.0000, 'ALL', NULL, '0', 80, '人工调账默认不强制流水，操作员可按场景选择。', NOW()),
(1900000000000001009, '000000', 'EXCHANGE_IN', '兑换入账', 'EXCHANGE', 'NONE', 0, 0.0000, 'ALL', NULL, '0', 90, '币种兑换目标币种的入账金额。', NOW()),
(1900000000000001010, '000000', 'PURCHASE_GRANT_GC', '购买获得GC', 'PURCHASE', 'NONE', 0, 0.0000, 'ALL', NULL, '0', 100, '购买产品发放的GC，当前不可提不可兑，默认不需要流水。', NOW()),
(1900000000000001011, '000000', 'PURCHASE_BONUS_SC', '购买赠送SC', 'PURCHASE', 'MULTIPLIER', 0, 10.0000, 'ALL', NULL, '0', 110, '购买产品赠送的SC，默认需要10倍流水。', NOW())
ON DUPLICATE KEY UPDATE
  property_name = VALUES(property_name),
  default_source_type = VALUES(default_source_type),
  default_turnover_mode = VALUES(default_turnover_mode),
  default_turnover_required_amount = VALUES(default_turnover_required_amount),
  default_turnover_multiplier = VALUES(default_turnover_multiplier),
  default_game_scope_type = VALUES(default_game_scope_type),
  default_game_scope_value = VALUES(default_game_scope_value),
  status = VALUES(status),
  sort_order = VALUES(sort_order),
  remark = VALUES(remark),
  update_time = NOW();

CREATE TABLE IF NOT EXISTS gl_member_profile (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  member_no VARCHAR(64) NOT NULL COMMENT 'Member number',
  username VARCHAR(64) NOT NULL COMMENT 'Username',
  nickname VARCHAR(128) DEFAULT NULL COMMENT 'Nickname',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Member status',
  risk_level VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'Risk level',
  register_channel VARCHAR(64) NOT NULL DEFAULT 'ADMIN' COMMENT 'Register channel',
  last_login_time DATETIME DEFAULT NULL COMMENT 'Last login time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_member_profile_01 (tenant_id, member_no),
  UNIQUE KEY uk_gl_member_profile_02 (tenant_id, username),
  KEY idx_gl_member_profile_01 (tenant_id, status, create_time),
  KEY idx_gl_member_profile_02 (tenant_id, risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Member profile';

INSERT INTO gl_member_profile
(id, tenant_id, member_no, username, nickname, status, risk_level, register_channel, remark, create_time)
VALUES
(1900000000000000401, '000000', 'MB-SEED-1001', 'member1001', 'Seed Member 1001', 'ACTIVE', 'NORMAL', 'ADMIN', 'Default local member profile.', NOW())
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  status = VALUES(status),
  risk_level = VALUES(risk_level),
  register_channel = VALUES(register_channel),
  remark = VALUES(remark),
  update_time = NOW();

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(2000, '报表中心', 0, 7, 'report', NULL, '', 1, 0, 'M', '0', '0', '', 'chart', 103, 1, NOW(), NULL, NULL, 'Report center directory'),
(2001, '数据总览', 2000, 1, 'overview', 'report/overview/index', '', 1, 0, 'C', '0', '0', 'report:overview:list', 'chart', 103, 1, NOW(), NULL, NULL, 'Report overview menu'),
(2002, '趋势看板', 2000, 2, 'trends', 'report/trends/index', '', 1, 0, 'C', '0', '0', 'report:trends:list', 'chart', 103, 1, NOW(), NULL, NULL, 'Report trends menu'),
(2011, '报表总览查询', 2001, 1, '#', '', '', 1, 0, 'F', '0', '0', 'report:overview:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(2021, '趋势看板查询', 2002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'report:trends:query', '#', 103, 1, NOW(), NULL, NULL, '')
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

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1980, '会员中心', 0, 1, 'member', NULL, '', 1, 0, 'M', '0', '0', '', 'user', 103, 1, NOW(), NULL, NULL, 'Member center directory'),
(1981, '会员资料', 1980, 1, 'profile', 'member/profile/index', '', 1, 0, 'C', '0', '0', 'member:profile:list', 'user', 103, 1, NOW(), NULL, NULL, 'Member profile menu'),
(1991, '会员查询', 1981, 1, '#', '', '', 1, 0, 'F', '0', '0', 'member:profile:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1992, '会员新增', 1981, 2, '#', '', '', 1, 0, 'F', '0', '0', 'member:profile:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1993, '会员编辑', 1981, 3, '#', '', '', 1, 0, 'F', '0', '0', 'member:profile:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
(1994, '会员删除', 1981, 4, '#', '', '', 1, 0, 'F', '0', '0', 'member:profile:remove', '#', 103, 1, NOW(), NULL, NULL, '')
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

CREATE TABLE IF NOT EXISTS gl_promotion_reward (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  promotion_no VARCHAR(64) NOT NULL COMMENT 'Promotion number',
  promotion_name VARCHAR(128) NOT NULL COMMENT 'Promotion name',
  promotion_type VARCHAR(64) NOT NULL DEFAULT 'GENERAL' COMMENT 'Promotion type',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  reward_amount DECIMAL(20,6) NOT NULL COMMENT 'Reward amount',
  claim_cycle VARCHAR(32) NOT NULL DEFAULT 'ONCE' COMMENT 'Claim cycle',
  daily_claim_limit INT NOT NULL DEFAULT 1 COMMENT 'Daily claim limit',
  reward_items JSON DEFAULT NULL COMMENT 'Reward item snapshot',
  status VARCHAR(32) NOT NULL DEFAULT 'INACTIVE' COMMENT 'Reward status',
  start_time DATETIME DEFAULT NULL COMMENT 'Start time',
  end_time DATETIME DEFAULT NULL COMMENT 'End time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_promotion_reward_01 (tenant_id, promotion_no),
  KEY idx_gl_promotion_reward_01 (tenant_id, status, create_time),
  KEY idx_gl_promotion_reward_02 (tenant_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Promotion reward';

CREATE TABLE IF NOT EXISTS gl_promotion_claim (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  claim_no VARCHAR(64) NOT NULL COMMENT 'Claim number',
  promotion_id BIGINT NOT NULL COMMENT 'Promotion id',
  promotion_no VARCHAR(64) NOT NULL COMMENT 'Promotion number',
  promotion_name VARCHAR(128) NOT NULL COMMENT 'Promotion name',
  promotion_type VARCHAR(64) DEFAULT NULL COMMENT 'Promotion type',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  reward_amount DECIMAL(20,6) NOT NULL COMMENT 'Reward amount',
  claim_date DATE NOT NULL DEFAULT '1000-01-01' COMMENT 'Claim date',
  reward_snapshot JSON DEFAULT NULL COMMENT 'Reward snapshot',
  status VARCHAR(32) NOT NULL COMMENT 'Claim status',
  wallet_transaction_no VARCHAR(512) DEFAULT NULL COMMENT 'Wallet transaction number',
  idempotency_key VARCHAR(160) NOT NULL COMMENT 'Claim idempotency key',
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
  UNIQUE KEY uk_gl_promotion_claim_01 (tenant_id, claim_no),
  UNIQUE KEY uk_gl_promotion_claim_02 (tenant_id, idempotency_key),
  UNIQUE KEY uk_gl_promotion_claim_03 (tenant_id, promotion_id, member_id, claim_date),
  KEY idx_gl_promotion_claim_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_promotion_claim_02 (tenant_id, promotion_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Promotion claim';

INSERT INTO gl_promotion_reward
(id, tenant_id, promotion_no, promotion_name, promotion_type, currency_code, reward_amount, claim_cycle, daily_claim_limit, reward_items, status, start_time, end_time, remark, create_time)
VALUES
(1900000000000000301, '000000', 'PR-SEED-SC-001', 'Seed SC Reward', 'GENERAL', 'SC', 3.000000, 'ONCE', 1,
 JSON_ARRAY(JSON_OBJECT('currencyCode', 'SC', 'rewardAmount', '3.000000', 'fundPropertyCode', 'ACTIVITY_REWARD', 'turnoverMode', 'NONE', 'gameScopeType', 'ALL')),
 'ACTIVE', NULL, NULL, 'Default simulated promotion reward.', NOW())
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

INSERT INTO gl_promotion_reward
(id, tenant_id, promotion_no, promotion_name, promotion_type, currency_code, reward_amount, claim_cycle, daily_claim_limit, reward_items, status, start_time, end_time, remark, create_time)
VALUES
(1900000000000000901, '000000', 'PR-DAILY-LOGIN-DEFAULT', 'Daily Login Reward', 'DAILY_LOGIN', 'GC', 100.000000, 'DAILY', 1,
 JSON_ARRAY(
   JSON_OBJECT('currencyCode', 'GC', 'rewardAmount', '100.000000', 'fundPropertyCode', 'DAILY_REWARD', 'turnoverMode', 'NONE', 'gameScopeType', 'ALL'),
   JSON_OBJECT('currencyCode', 'SC', 'rewardAmount', '1.000000', 'fundPropertyCode', 'DAILY_REWARD', 'turnoverMode', 'NONE', 'gameScopeType', 'ALL')
 ),
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

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1960, '促销中心', 0, 6, 'promotion', NULL, '', 1, 0, 'M', '0', '0', '', 'skill', 103, 1, NOW(), NULL, NULL, 'Promotion center directory'),
(1961, '促销奖励', 1960, 1, 'reward', 'promotion/reward/index', '', 1, 0, 'C', '0', '0', 'promotion:reward:list', 'skill', 103, 1, NOW(), NULL, NULL, 'Promotion reward menu'),
(1971, '促销查询', 1961, 1, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1972, '促销新增', 1961, 2, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1973, '促销编辑', 1961, 3, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
(1974, '促销删除', 1961, 4, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:remove', '#', 103, 1, NOW(), NULL, NULL, ''),
(1975, '促销领取', 1961, 5, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:claim', '#', 103, 1, NOW(), NULL, NULL, '')
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

CREATE TABLE IF NOT EXISTS gl_redemption_order (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  redemption_order_no VARCHAR(64) NOT NULL COMMENT 'Redemption order number',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  amount DECIMAL(20,6) NOT NULL COMMENT 'Redemption amount',
  redemption_method VARCHAR(32) NOT NULL DEFAULT 'SIMULATED' COMMENT 'Redemption method',
  account_ref VARCHAR(256) DEFAULT NULL COMMENT 'Masked account reference',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Order status',
  freeze_no VARCHAR(64) NOT NULL COMMENT 'Wallet freeze number',
  freeze_wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Freeze wallet transaction number',
  settle_wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Settle wallet transaction number',
  release_wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Release wallet transaction number',
  freeze_idempotency_key VARCHAR(128) NOT NULL COMMENT 'Freeze idempotency key',
  settle_idempotency_key VARCHAR(128) NOT NULL COMMENT 'Settle idempotency key',
  release_idempotency_key VARCHAR(128) NOT NULL COMMENT 'Release idempotency key',
  audit_by BIGINT DEFAULT NULL COMMENT 'Audit user id',
  audit_time DATETIME DEFAULT NULL COMMENT 'Audit time',
  audit_reason VARCHAR(500) DEFAULT NULL COMMENT 'Audit reason',
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
  UNIQUE KEY uk_gl_redemption_order_01 (tenant_id, redemption_order_no),
  UNIQUE KEY uk_gl_redemption_order_02 (tenant_id, freeze_idempotency_key),
  UNIQUE KEY uk_gl_redemption_order_03 (tenant_id, settle_idempotency_key),
  UNIQUE KEY uk_gl_redemption_order_04 (tenant_id, release_idempotency_key),
  UNIQUE KEY uk_gl_redemption_order_05 (tenant_id, freeze_no),
  KEY idx_gl_redemption_order_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_redemption_order_02 (tenant_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Redemption order';

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1940, '兑换中心', 0, 5, 'redemption', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 103, 1, NOW(), NULL, NULL, 'Redemption center directory'),
(1941, '兑换订单', 1940, 1, 'order', 'redemption/order/index', '', 1, 0, 'C', '0', '0', 'redemption:order:list', 'money', 103, 1, NOW(), NULL, NULL, 'Redemption order menu'),
(1951, '兑换查询', 1941, 1, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1952, '兑换新增', 1941, 2, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1953, '兑换通过', 1941, 3, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:approve', '#', 103, 1, NOW(), NULL, NULL, ''),
(1954, '兑换拒绝', 1941, 4, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:reject', '#', 103, 1, NOW(), NULL, NULL, '')
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

-- Wallet Center admin menu and permissions.
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1800, '钱包中心', 0, 2, 'wallet', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 103, 1, NOW(), NULL, NULL, '钱包中心目录'),
(1801, '币种配置', 1800, 1, 'currency', 'wallet/currency/index', '', 1, 0, 'C', '0', '0', 'wallet:currency:list', 'switch', 103, 1, NOW(), NULL, NULL, '钱包币种配置菜单'),
(1802, '钱包账户', 1800, 2, 'account', 'wallet/account/index', '', 1, 0, 'C', '0', '0', 'wallet:account:list', 'user', 103, 1, NOW(), NULL, NULL, '会员钱包账户菜单'),
(1803, '账变流水', 1800, 3, 'transaction', 'wallet/transaction/index', '', 1, 0, 'C', '0', '0', 'wallet:transaction:list', 'list', 103, 1, NOW(), NULL, NULL, '钱包账变流水菜单'),
(1804, '释放记录', 1800, 4, 'release', 'wallet/release/index', '', 1, 0, 'C', '0', '0', 'wallet:release:list', 'validCode', 103, 1, NOW(), NULL, NULL, '钱包释放记录菜单'),
(1805, '冻结记录', 1800, 5, 'freeze', 'wallet/freeze/index', '', 1, 0, 'C', '0', '0', 'wallet:freeze:list', 'lock', 103, 1, NOW(), NULL, NULL, '钱包冻结记录菜单'),
(1811, '币种查询', 1801, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:currency:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1812, '币种编辑', 1801, 2, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:currency:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
(1813, '账户查询', 1802, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:account:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1814, '流水查询', 1803, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:transaction:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1815, '释放查询', 1804, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:release:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1816, '冻结查询', 1805, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:freeze:query', '#', 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  parent_id = VALUES(parent_id),
  order_num = VALUES(order_num),
  path = VALUES(path),
  component = VALUES(component),
  query_param = VALUES(query_param),
  is_frame = VALUES(is_frame),
  is_cache = VALUES(is_cache),
  menu_type = VALUES(menu_type),
  visible = VALUES(visible),
  status = VALUES(status),
  perms = VALUES(perms),
  icon = VALUES(icon),
  remark = VALUES(remark),
  update_time = NOW();

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

CREATE TABLE IF NOT EXISTS gl_purchase_offer (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  offer_no VARCHAR(64) NOT NULL COMMENT 'Offer no',
  offer_name VARCHAR(128) NOT NULL COMMENT 'Offer name',
  offer_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD,FIRST_PURCHASE,CAMPAIGN,DISCOUNT,RECALL',
  pay_currency_code VARCHAR(32) NOT NULL DEFAULT 'USD' COMMENT 'Payment currency',
  pay_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Payment amount',
  user_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL,NEW_USER,RECALL,TAG',
  user_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'User scope value',
  region_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL,COUNTRY,STATE',
  region_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'Region scope value',
  purchase_limit_type VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE,FIRST_ONLY,DAILY_ONCE,TOTAL_ONCE,PERIOD_LIMIT',
  stackable CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Stackable: 0 yes, 1 no',
  status CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Status: 0 enabled, 1 disabled',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  start_time DATETIME DEFAULT NULL COMMENT 'Start time',
  end_time DATETIME DEFAULT NULL COMMENT 'End time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  version INT NOT NULL DEFAULT 0 COMMENT 'Version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_offer_01 (tenant_id, offer_no),
  KEY idx_gl_purchase_offer_01 (tenant_id, offer_type, status, sort_order),
  KEY idx_gl_purchase_offer_02 (tenant_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase offer';

CREATE TABLE IF NOT EXISTS gl_purchase_offer_grant_item (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  offer_id BIGINT NOT NULL COMMENT 'Offer id',
  grant_type VARCHAR(32) NOT NULL COMMENT 'PURCHASE_GRANT,PURCHASE_BONUS,DEPOSIT_PRINCIPAL,DEPOSIT_BONUS',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Grant currency',
  grant_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Grant amount',
  fund_property_code VARCHAR(64) NOT NULL COMMENT 'System fund property code',
  wagering_mode VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE,FIXED,MULTIPLIER,COMBINED_MULTIPLIER',
  wagering_required_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Fixed wagering amount',
  wagering_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'Wagering multiplier',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL,CATEGORY,PROVIDER,GAME',
  game_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'Game scope value',
  wagering_expire_days INT NOT NULL DEFAULT 0 COMMENT 'Wagering expiry days',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  KEY idx_gl_purchase_offer_grant_item_01 (tenant_id, offer_id, sort_order),
  KEY idx_gl_purchase_offer_grant_item_02 (tenant_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase offer grant item';

CREATE TABLE IF NOT EXISTS gl_purchase_order (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order no',
  offer_id BIGINT DEFAULT NULL COMMENT 'Offer id',
  offer_no VARCHAR(64) DEFAULT NULL COMMENT 'Offer no snapshot',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  pay_currency_code VARCHAR(32) NOT NULL COMMENT 'Payment currency',
  pay_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Payment amount',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,PAID,CREDITED,FAILED,CANCELLED',
  idempotency_key VARCHAR(128) NOT NULL COMMENT 'Idempotency key',
  fail_reason VARCHAR(500) DEFAULT NULL COMMENT 'Failure reason',
  paid_time DATETIME DEFAULT NULL COMMENT 'Paid time',
  credited_time DATETIME DEFAULT NULL COMMENT 'Credited time',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_order_01 (tenant_id, purchase_order_no),
  UNIQUE KEY uk_gl_purchase_order_02 (tenant_id, idempotency_key),
  KEY idx_gl_purchase_order_01 (tenant_id, member_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase order';

CREATE TABLE IF NOT EXISTS gl_purchase_order_grant_snapshot (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  purchase_order_id BIGINT NOT NULL COMMENT 'Purchase order id',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order no',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  grant_type VARCHAR(32) NOT NULL COMMENT 'Grant type',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Grant currency',
  grant_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Grant amount',
  fund_property_code VARCHAR(64) NOT NULL COMMENT 'Fund property code',
  wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Wallet transaction no',
  turnover_task_no VARCHAR(64) DEFAULT NULL COMMENT 'Turnover task no',
  wagering_mode VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'Wagering mode snapshot',
  required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Required turnover snapshot',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'Game scope type',
  game_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'Game scope value',
  rule_snapshot JSON DEFAULT NULL COMMENT 'Rule snapshot',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  KEY idx_gl_purchase_order_grant_snapshot_01 (tenant_id, purchase_order_no),
  KEY idx_gl_purchase_order_grant_snapshot_02 (tenant_id, member_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase order grant snapshot';

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1900, '支付中心', 0, 3, 'payment', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 103, 1, NOW(), NULL, NULL, '支付中心目录'),
(1901, '充值订单', 1900, 1, 'deposit', 'payment/deposit/index', '', 1, 0, 'C', '0', '0', 'payment:deposit:list', 'money', 103, 1, NOW(), NULL, NULL, '充值订单菜单'),
(1910, '购买产品', 1900, 2, 'purchase-offer', 'payment/purchase-offer/index', '', 1, 0, 'C', '0', '0', 'payment:purchaseOffer:list', 'shopping', 103, 1, NOW(), NULL, NULL, '购买产品配置菜单'),
(1911, '充值订单查询', 1901, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:deposit:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1912, '充值订单新增', 1901, 2, '#', '', '', 1, 0, 'F', '0', '0', 'payment:deposit:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1913, '模拟支付成功', 1901, 3, '#', '', '', 1, 0, 'F', '0', '0', 'payment:deposit:simulate', '#', 103, 1, NOW(), NULL, NULL, ''),
(1914, '充值订单取消', 1901, 4, '#', '', '', 1, 0, 'F', '0', '0', 'payment:deposit:cancel', '#', 103, 1, NOW(), NULL, NULL, ''),
(1916, '购买产品查询', 1910, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOffer:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1917, '购买产品新增', 1910, 2, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOffer:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1918, '购买产品编辑', 1910, 3, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOffer:edit', '#', 103, 1, NOW(), NULL, NULL, '')
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

CREATE TABLE IF NOT EXISTS gl_game_bet_order (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  bet_order_no VARCHAR(64) NOT NULL COMMENT 'Bet order number',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  game_code VARCHAR(64) NOT NULL DEFAULT 'SIMULATED' COMMENT 'Game code',
  round_no VARCHAR(64) NOT NULL COMMENT 'Game round number',
  bet_amount DECIMAL(20,6) NOT NULL COMMENT 'Bet amount',
  payout_amount DECIMAL(20,6) NOT NULL COMMENT 'Payout amount',
  net_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Payout minus bet amount',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Order status',
  bet_wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Bet wallet transaction number',
  settle_wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Settle wallet transaction number',
  refund_wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Refund wallet transaction number',
  refund_idempotency_key VARCHAR(128) DEFAULT NULL COMMENT 'Refund idempotency key',
  cancel_time DATETIME DEFAULT NULL COMMENT 'Cancel time',
  bet_idempotency_key VARCHAR(128) NOT NULL COMMENT 'Bet idempotency key',
  settle_idempotency_key VARCHAR(128) NOT NULL COMMENT 'Settle idempotency key',
  bet_time DATETIME DEFAULT NULL COMMENT 'Bet time',
  settle_time DATETIME DEFAULT NULL COMMENT 'Settle time',
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
  UNIQUE KEY uk_gl_game_bet_order_01 (tenant_id, bet_order_no),
  UNIQUE KEY uk_gl_game_bet_order_02 (tenant_id, bet_idempotency_key),
  UNIQUE KEY uk_gl_game_bet_order_03 (tenant_id, settle_idempotency_key),
  UNIQUE KEY uk_gl_game_bet_order_04 (tenant_id, refund_idempotency_key),
  KEY idx_gl_game_bet_order_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_game_bet_order_02 (tenant_id, status, create_time),
  KEY idx_gl_game_bet_order_03 (tenant_id, game_code, round_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Game simulated bet order';

SET @db_name := DATABASE();
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_game_bet_order' AND COLUMN_NAME = 'refund_wallet_transaction_no'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_game_bet_order ADD COLUMN refund_wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT ''Refund wallet transaction number'' AFTER settle_wallet_transaction_no',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_game_bet_order' AND COLUMN_NAME = 'refund_idempotency_key'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_game_bet_order ADD COLUMN refund_idempotency_key VARCHAR(128) DEFAULT NULL COMMENT ''Refund idempotency key'' AFTER refund_wallet_transaction_no',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_game_bet_order' AND COLUMN_NAME = 'cancel_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_game_bet_order ADD COLUMN cancel_time DATETIME DEFAULT NULL COMMENT ''Cancel time'' AFTER refund_idempotency_key',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_game_bet_order' AND INDEX_NAME = 'uk_gl_game_bet_order_04'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE gl_game_bet_order ADD UNIQUE KEY uk_gl_game_bet_order_04 (tenant_id, refund_idempotency_key)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1920, '游戏交易', 0, 4, 'game', NULL, '', 1, 0, 'M', '0', '0', '', 'shopping', 103, 1, NOW(), NULL, NULL, '游戏交易目录'),
(1921, '模拟下注订单', 1920, 1, 'bet', 'game/bet/index', '', 1, 0, 'C', '0', '0', 'game:bet:list', 'list', 103, 1, NOW(), NULL, NULL, '模拟下注订单菜单'),
(1931, '模拟下注订单查询', 1921, 1, '#', '', '', 1, 0, 'F', '0', '0', 'game:bet:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1932, '模拟下注订单新增', 1921, 2, '#', '', '', 1, 0, 'F', '0', '0', 'game:bet:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1933, '模拟下注扣款', 1921, 3, '#', '', '', 1, 0, 'F', '0', '0', 'game:bet:place', '#', 103, 1, NOW(), NULL, NULL, ''),
(1934, '模拟结算派彩', 1921, 4, '#', '', '', 1, 0, 'F', '0', '0', 'game:bet:settle', '#', 103, 1, NOW(), NULL, NULL, '')
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

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1935, '模拟下注取消退款', 1921, 5, '#', '', '', 1, 0, 'F', '0', '0', 'game:bet:cancel', '#', 103, 1, NOW(), NULL, NULL, '模拟下注取消退款')
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

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1822, '资金属性', 1800, 7, 'fund-property', 'wallet/fund-property/index', '', 1, 0, 'C', '0', '0', 'wallet:fundProperty:list', 'category', 103, 1, NOW(), NULL, NULL, '钱包资金属性模板菜单'),
(1823, '资金属性查询', 1822, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:fundProperty:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1832, '资金属性新增', 1822, 2, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:fundProperty:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1833, '资金属性编辑', 1822, 3, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:fundProperty:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
(1824, '币种策略', 1800, 8, 'currency-policy', 'wallet/currency-policy/index', '', 1, 0, 'C', '0', '0', 'wallet:currencyPolicy:list', 'guide', 103, 1, NOW(), NULL, NULL, '钱包币种可见和操作策略菜单'),
(1825, '币种策略查询', 1824, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:currencyPolicy:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1826, '币种策略新增', 1824, 2, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:currencyPolicy:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1827, '币种策略编辑', 1824, 3, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:currencyPolicy:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
(1828, '币种兑换规则', 1800, 9, 'exchange-rule', 'wallet/exchange-rule/index', '', 1, 0, 'C', '0', '0', 'wallet:exchangeRule:list', 'sort', 103, 1, NOW(), NULL, NULL, '钱包币种兑换规则菜单'),
(1829, '币种兑换规则查询', 1828, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:exchangeRule:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1830, '币种兑换规则新增', 1828, 2, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:exchangeRule:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1831, '币种兑换规则编辑', 1828, 3, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:exchangeRule:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
(1807, '人工调账', 1800, 10, 'manual-adjust', 'wallet/manual-adjust/index', '', 1, 0, 'C', '0', '0', 'wallet:manualAdjust:list', 'edit', 103, 1, NOW(), NULL, NULL, '后台人工调账菜单'),
(1821, '人工调账操作', 1807, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:manualAdjust:add', '#', 103, 1, NOW(), NULL, NULL, '')
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

UPDATE sys_menu
SET order_num = CASE menu_id
  WHEN 1980 THEN 1
  WHEN 1800 THEN 2
  WHEN 1900 THEN 3
  WHEN 1920 THEN 4
  WHEN 1940 THEN 5
  WHEN 1960 THEN 6
  WHEN 2000 THEN 7
  WHEN 6 THEN 90
  WHEN 1 THEN 91
  WHEN 3 THEN 92
  WHEN 2 THEN 93
  ELSE order_num
END,
update_time = NOW()
WHERE parent_id = 0
  AND menu_id IN (1980, 1800, 1900, 1920, 1940, 1960, 2000, 6, 1, 3, 2);
