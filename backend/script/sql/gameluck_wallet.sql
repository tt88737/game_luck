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
  risk_reason VARCHAR(500) DEFAULT NULL COMMENT 'Risk audit reason',
  risk_source VARCHAR(255) DEFAULT NULL COMMENT 'Risk audit source',
  risk_updated_time DATETIME DEFAULT NULL COMMENT 'Risk audit update time',
  kyc_status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'KYC status',
  kyc_review_reason VARCHAR(512) DEFAULT NULL COMMENT 'KYC review reason',
  kyc_reviewed_by VARCHAR(64) DEFAULT NULL COMMENT 'KYC reviewed by',
  kyc_review_time DATETIME DEFAULT NULL COMMENT 'KYC review time',
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
  KEY idx_gl_member_profile_02 (tenant_id, risk_level),
  KEY idx_gl_member_profile_03 (tenant_id, kyc_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Member profile';

SET @db_name := DATABASE();

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_member_profile' AND COLUMN_NAME = 'risk_reason'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN risk_reason VARCHAR(500) DEFAULT NULL COMMENT ''Risk audit reason'' AFTER risk_level',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_member_profile' AND COLUMN_NAME = 'risk_source'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN risk_source VARCHAR(255) DEFAULT NULL COMMENT ''Risk audit source'' AFTER risk_reason',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_member_profile' AND COLUMN_NAME = 'risk_updated_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN risk_updated_time DATETIME DEFAULT NULL COMMENT ''Risk audit update time'' AFTER risk_source',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_member_profile' AND COLUMN_NAME = 'kyc_status'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN kyc_status VARCHAR(32) NOT NULL DEFAULT ''NOT_STARTED'' COMMENT ''KYC status'' AFTER risk_level',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_member_profile' AND COLUMN_NAME = 'kyc_review_reason'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN kyc_review_reason VARCHAR(512) DEFAULT NULL COMMENT ''KYC review reason'' AFTER kyc_status',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_member_profile' AND COLUMN_NAME = 'kyc_reviewed_by'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN kyc_reviewed_by VARCHAR(64) DEFAULT NULL COMMENT ''KYC reviewed by'' AFTER kyc_review_reason',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_member_profile' AND COLUMN_NAME = 'kyc_review_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN kyc_review_time DATETIME DEFAULT NULL COMMENT ''KYC review time'' AFTER kyc_reviewed_by',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_member_profile' AND INDEX_NAME = 'idx_gl_member_profile_03'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE gl_member_profile ADD KEY idx_gl_member_profile_03 (tenant_id, kyc_status)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE gl_member_profile
SET kyc_status = 'NOT_STARTED'
WHERE kyc_status IS NULL OR kyc_status = '';

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

CREATE TABLE IF NOT EXISTS gl_redemption_eligibility_policy (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  policy_name VARCHAR(128) NOT NULL COMMENT 'Policy name',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Redemption currency code',
  country_code VARCHAR(16) DEFAULT NULL COMMENT 'Country condition',
  state_code VARCHAR(32) DEFAULT NULL COMMENT 'State or province condition',
  channel VARCHAR(32) DEFAULT NULL COMMENT 'Channel condition',
  effect VARCHAR(16) NOT NULL DEFAULT 'DENY' COMMENT 'ALLOW or DENY',
  priority INT NOT NULL DEFAULT 0 COMMENT 'Higher priority wins; DENY wins ties',
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
  KEY idx_gl_redemption_eligibility_policy_01 (tenant_id, currency_code, status, priority),
  KEY idx_gl_redemption_eligibility_policy_02 (tenant_id, country_code, state_code, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Redemption eligibility policy';

INSERT INTO gl_redemption_eligibility_policy
(id, tenant_id, policy_name, currency_code, country_code, state_code, channel, effect, priority, status, remark, create_time, update_time, version, del_flag)
VALUES
(19000000000002901, '000000', 'US WA redemption denied', 'SC', 'US', 'WA', 'H5', 'DENY', 100, '0', 'Seeded denied redemption region.', NOW(), NOW(), 0, '0'),
(19000000000002902, '000000', 'US ID redemption denied', 'SC', 'US', 'ID', 'H5', 'DENY', 100, '0', 'Seeded denied redemption region.', NOW(), NOW(), 0, '0'),
(19000000000002903, '000000', 'US NV redemption denied', 'SC', 'US', 'NV', 'H5', 'DENY', 100, '0', 'Seeded denied redemption region.', NOW(), NOW(), 0, '0'),
(19000000000002904, '000000', 'US MI redemption denied', 'SC', 'US', 'MI', 'H5', 'DENY', 100, '0', 'Seeded denied redemption region.', NOW(), NOW(), 0, '0')
ON DUPLICATE KEY UPDATE
  policy_name = VALUES(policy_name),
  currency_code = VALUES(currency_code),
  country_code = VALUES(country_code),
  state_code = VALUES(state_code),
  channel = VALUES(channel),
  effect = VALUES(effect),
  priority = VALUES(priority),
  status = VALUES(status),
  remark = VALUES(remark),
  update_time = NOW();

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1940, '兑换中心', 0, 5, 'redemption', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 103, 1, NOW(), NULL, NULL, 'Redemption center directory'),
(1941, '兑换订单', 1940, 1, 'order', 'redemption/order/index', '', 1, 0, 'C', '0', '0', 'redemption:order:list', 'money', 103, 1, NOW(), NULL, NULL, 'Redemption order menu'),
(1955, '兑换资格策略', 1940, 2, 'eligibility-policy', 'redemption/eligibility-policy/index', '', 1, 0, 'C', '0', '0', 'redemption:eligibilityPolicy:list', 'guide', 103, 1, NOW(), NULL, NULL, 'Redemption eligibility policy menu'),
(1951, '兑换查询', 1941, 1, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1952, '兑换新增', 1941, 2, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1953, '兑换通过', 1941, 3, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:approve', '#', 103, 1, NOW(), NULL, NULL, ''),
(1954, '兑换拒绝', 1941, 4, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:reject', '#', 103, 1, NOW(), NULL, NULL, ''),
(1956, '兑换资格策略查询', 1955, 1, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:eligibilityPolicy:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1957, '兑换资格策略新增', 1955, 2, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:eligibilityPolicy:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1958, '兑换资格策略编辑', 1955, 3, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:eligibilityPolicy:edit', '#', 103, 1, NOW(), NULL, NULL, '')
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
  offer_name_snapshot VARCHAR(128) DEFAULT NULL COMMENT 'Offer name snapshot',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  pay_currency_code VARCHAR(32) NOT NULL COMMENT 'Payment currency',
  pay_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Payment amount',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'CREATED,PENDING,PAID,CREDITED,FAILED,CANCELLED,REFUNDED,CHARGEBACK',
  idempotency_key VARCHAR(128) NOT NULL COMMENT 'Idempotency key',
  provider_code VARCHAR(64) DEFAULT NULL COMMENT 'Payment provider code',
  provider_order_no VARCHAR(128) DEFAULT NULL COMMENT 'Provider order no',
  payment_session_no VARCHAR(128) DEFAULT NULL COMMENT 'Internal payment session no',
  callback_event_key VARCHAR(128) DEFAULT NULL COMMENT 'Last callback event key',
  fail_reason VARCHAR(500) DEFAULT NULL COMMENT 'Failure reason',
  paid_time DATETIME DEFAULT NULL COMMENT 'Paid time',
  credited_time DATETIME DEFAULT NULL COMMENT 'Credited time',
  cancel_time DATETIME DEFAULT NULL COMMENT 'Cancel time',
  refund_time DATETIME DEFAULT NULL COMMENT 'Refund time',
  chargeback_time DATETIME DEFAULT NULL COMMENT 'Chargeback time',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_order_01 (tenant_id, purchase_order_no),
  UNIQUE KEY uk_gl_purchase_order_02 (tenant_id, idempotency_key),
  KEY idx_gl_purchase_order_01 (tenant_id, member_id, status, create_time),
  KEY idx_gl_purchase_order_02 (tenant_id, provider_code, provider_order_no),
  KEY idx_gl_purchase_order_03 (tenant_id, payment_session_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase order';

SET @db_name := DATABASE();

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND COLUMN_NAME = 'offer_name_snapshot'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order ADD COLUMN offer_name_snapshot VARCHAR(128) DEFAULT NULL COMMENT ''Offer name snapshot'' AFTER offer_no',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND COLUMN_NAME = 'provider_code'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order ADD COLUMN provider_code VARCHAR(64) DEFAULT NULL COMMENT ''Payment provider code'' AFTER idempotency_key',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND COLUMN_NAME = 'provider_order_no'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order ADD COLUMN provider_order_no VARCHAR(128) DEFAULT NULL COMMENT ''Provider order no'' AFTER provider_code',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND COLUMN_NAME = 'payment_session_no'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order ADD COLUMN payment_session_no VARCHAR(128) DEFAULT NULL COMMENT ''Internal payment session no'' AFTER provider_order_no',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND COLUMN_NAME = 'callback_event_key'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order ADD COLUMN callback_event_key VARCHAR(128) DEFAULT NULL COMMENT ''Last callback event key'' AFTER payment_session_no',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND COLUMN_NAME = 'cancel_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order ADD COLUMN cancel_time DATETIME DEFAULT NULL COMMENT ''Cancel time'' AFTER credited_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND COLUMN_NAME = 'refund_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order ADD COLUMN refund_time DATETIME DEFAULT NULL COMMENT ''Refund time'' AFTER cancel_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND COLUMN_NAME = 'chargeback_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order ADD COLUMN chargeback_time DATETIME DEFAULT NULL COMMENT ''Chargeback time'' AFTER refund_time',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND INDEX_NAME = 'idx_gl_purchase_order_02'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE gl_purchase_order ADD KEY idx_gl_purchase_order_02 (tenant_id, provider_code, provider_order_no)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_order' AND INDEX_NAME = 'idx_gl_purchase_order_03'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE gl_purchase_order ADD KEY idx_gl_purchase_order_03 (tenant_id, payment_session_no)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS gl_purchase_payment_event (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  event_key VARCHAR(128) NOT NULL COMMENT 'Payment event idempotency key',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order no',
  provider_code VARCHAR(64) NOT NULL COMMENT 'Provider code',
  provider_order_no VARCHAR(128) DEFAULT NULL COMMENT 'Provider order no',
  event_type VARCHAR(32) NOT NULL COMMENT 'PAY_SUCCESS,PAY_FAILED,CANCELLED,REFUNDED,CHARGEBACK',
  event_status VARCHAR(32) NOT NULL COMMENT 'RECEIVED,PROCESSED,IGNORED,FAILED',
  request_hash VARCHAR(128) NOT NULL COMMENT 'Normalized request hash',
  request_body TEXT DEFAULT NULL COMMENT 'Raw or normalized request body',
  process_result VARCHAR(500) DEFAULT NULL COMMENT 'Process result',
  process_time DATETIME DEFAULT NULL COMMENT 'Process time',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_payment_event_01 (tenant_id, event_key),
  KEY idx_gl_purchase_payment_event_01 (tenant_id, purchase_order_no),
  KEY idx_gl_purchase_payment_event_02 (tenant_id, provider_code, provider_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase payment event';

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
  wagering_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'Wagering multiplier snapshot',
  wagering_expire_days INT NOT NULL DEFAULT 0 COMMENT 'Wagering expiry days snapshot',
  required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Required turnover snapshot',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'Game scope type',
  game_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'Game scope value',
  rule_snapshot JSON DEFAULT NULL COMMENT 'Rule snapshot',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  KEY idx_gl_purchase_order_grant_snapshot_01 (tenant_id, purchase_order_no),
  KEY idx_gl_purchase_order_grant_snapshot_02 (tenant_id, member_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase order grant snapshot';

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gl_purchase_order_grant_snapshot' AND COLUMN_NAME = 'wagering_multiplier'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order_grant_snapshot ADD COLUMN wagering_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT ''Wagering multiplier snapshot'' AFTER wagering_mode',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gl_purchase_order_grant_snapshot' AND COLUMN_NAME = 'wagering_expire_days'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_purchase_order_grant_snapshot ADD COLUMN wagering_expire_days INT NOT NULL DEFAULT 0 COMMENT ''Wagering expiry days snapshot'' AFTER wagering_multiplier',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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
(1918, '购买产品编辑', 1910, 3, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOffer:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
(1919, '购买订单', 1900, 3, 'purchase-order', 'payment/purchase-order/index', '', 1, 0, 'C', '0', '0', 'payment:purchaseOrder:list', 'list', 103, 1, NOW(), NULL, NULL, '购买订单菜单'),
(19191, '购买订单查询', 1919, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOrder:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(19192, '购买订单人工处理', 1919, 2, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOrder:manual', '#', 103, 1, NOW(), NULL, NULL, '')
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

DELETE FROM sys_menu WHERE menu_id = 1922 AND parent_id = 1919 AND perms = 'payment:purchaseOrder:manual';

DELETE FROM sys_menu WHERE menu_id IN (20301, 20311, 20312, 2030, 2031);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(2030, '支付会话', 1900, 4, 'payment-session', 'payment/payment-session/index', '', 1, 0, 'C', '0', '0', 'payment:paymentSession:list', 'link', 103, 1, NOW(), NULL, NULL, '支付通道会话'),
(20301, '支付会话查询', 2030, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:paymentSession:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(2031, '支付回调事件', 1900, 5, 'payment-webhook-event', 'payment/payment-webhook-event/index', '', 1, 0, 'C', '0', '0', 'payment:webhookEvent:list', 'webhook', 103, 1, NOW(), NULL, NULL, '支付通道回调事件'),
(20311, '支付回调事件查询', 2031, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:webhookEvent:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(20312, '支付回调事件重试', 2031, 2, '#', '', '', 1, 0, 'F', '0', '0', 'payment:webhookEvent:retry', '#', 103, 1, NOW(), NULL, NULL, '');

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(19195, '拒付审核', 1900, 6, 'purchase-reversal-review', 'payment/purchase-reversal-review/index', '', 1, 0, 'C', '0', '0', 'payment:reversalReview:list', 'audit', 103, 1, NOW(), NULL, NULL, '退款和拒付追偿审核工作台'),
(19196, '拒付审核查询', 19195, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:reversalReview:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(19197, '再次全额追偿', 19195, 2, '#', '', '', 1, 0, 'F', '0', '0', 'payment:reversalReview:retry', '#', 103, 1, NOW(), NULL, NULL, ''),
(19198, '确认损失结案', 19195, 3, '#', '', '', 1, 0, 'F', '0', '0', 'payment:reversalReview:acceptLoss', '#', 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), order_num=VALUES(order_num), path=VALUES(path), component=VALUES(component), perms=VALUES(perms), icon=VALUES(icon), remark=VALUES(remark), update_time=NOW();

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
(1834, '币种兑换订单', 1800, 10, 'exchange-order', 'wallet/exchange-order/index', '', 1, 0, 'C', '0', '0', 'wallet:exchangeOrder:list', 'list', 103, 1, NOW(), NULL, NULL, '钱包币种兑换订单菜单'),
(1835, '币种兑换订单查询', 1834, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:exchangeOrder:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1807, '人工调账', 1800, 11, 'manual-adjust', 'wallet/manual-adjust/index', '', 1, 0, 'C', '0', '0', 'wallet:manualAdjust:list', 'edit', 103, 1, NOW(), NULL, NULL, '后台人工调账菜单'),
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

CREATE TABLE IF NOT EXISTS gl_purchase_reversal (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  reversal_no VARCHAR(64) NOT NULL COMMENT 'Purchase reversal number',
  purchase_order_id BIGINT NOT NULL COMMENT 'Purchase order id',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order number',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  event_key VARCHAR(128) NOT NULL COMMENT 'Payment event idempotency key',
  reversal_type VARCHAR(32) NOT NULL COMMENT 'REFUND or CHARGEBACK',
  status VARCHAR(32) NOT NULL COMMENT 'PROCESSING, COMPLETED, or REVIEW_REQUIRED',
  reason VARCHAR(500) DEFAULT NULL COMMENT 'Source reason',
  review_reason VARCHAR(500) DEFAULT NULL COMMENT 'Manual review reason',
  disposition_status VARCHAR(32) DEFAULT NULL COMMENT 'PENDING_REVIEW, RECOVERY_COMPLETED, or LOSS_ACCEPTED',
  reviewed_by BIGINT DEFAULT NULL COMMENT 'Final review operator user id',
  reviewed_name VARCHAR(100) DEFAULT NULL COMMENT 'Final review operator name snapshot',
  review_note VARCHAR(500) DEFAULT NULL COMMENT 'Final review note',
  resolved_time DATETIME DEFAULT NULL COMMENT 'Final disposition time',
  retry_count INT NOT NULL DEFAULT 0 COMMENT 'Manual retry count',
  last_retry_time DATETIME DEFAULT NULL COMMENT 'Latest manual retry time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  completed_time DATETIME DEFAULT NULL COMMENT 'Completion time',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_reversal_01 (tenant_id, reversal_no),
  UNIQUE KEY uk_gl_purchase_reversal_02 (tenant_id, event_key),
  KEY idx_gl_purchase_reversal_01 (tenant_id, purchase_order_no),
  KEY idx_gl_purchase_reversal_03 (tenant_id, purchase_order_no, create_time, id),
  KEY idx_gl_purchase_reversal_02 (tenant_id, member_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase asset reversal case';

CREATE TABLE IF NOT EXISTS gl_purchase_reversal_item (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  reversal_id BIGINT NOT NULL COMMENT 'Purchase reversal id',
  reversal_no VARCHAR(64) NOT NULL COMMENT 'Purchase reversal number',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order number',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Aggregated grant currency',
  required_amount DECIMAL(20,8) NOT NULL COMMENT 'Amount required for full recovery',
  available_amount DECIMAL(20,8) NOT NULL DEFAULT 0 COMMENT 'Available balance observed during preflight',
  recovered_amount DECIMAL(20,8) NOT NULL DEFAULT 0 COMMENT 'Recovered amount',
  shortfall_amount DECIMAL(20,8) NOT NULL DEFAULT 0 COMMENT 'Recovery shortfall',
  wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Recovery debit transaction number',
  status VARCHAR(32) NOT NULL COMMENT 'COMPLETED or REVIEW_REQUIRED',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_reversal_item_01 (tenant_id, reversal_no, currency_code),
  KEY idx_gl_purchase_reversal_item_01 (tenant_id, purchase_order_no),
  KEY idx_gl_purchase_reversal_item_02 (tenant_id, member_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase asset reversal currency item';

SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_reversal' AND COLUMN_NAME = 'disposition_status') = 0,
  'ALTER TABLE gl_purchase_reversal ADD COLUMN disposition_status VARCHAR(32) DEFAULT NULL COMMENT ''PENDING_REVIEW, RECOVERY_COMPLETED, or LOSS_ACCEPTED'' AFTER review_reason', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_reversal' AND COLUMN_NAME = 'reviewed_by') = 0,
  'ALTER TABLE gl_purchase_reversal ADD COLUMN reviewed_by BIGINT DEFAULT NULL COMMENT ''Final review operator user id'' AFTER disposition_status', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_reversal' AND COLUMN_NAME = 'reviewed_name') = 0,
  'ALTER TABLE gl_purchase_reversal ADD COLUMN reviewed_name VARCHAR(100) DEFAULT NULL COMMENT ''Final review operator name snapshot'' AFTER reviewed_by', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_reversal' AND COLUMN_NAME = 'review_note') = 0,
  'ALTER TABLE gl_purchase_reversal ADD COLUMN review_note VARCHAR(500) DEFAULT NULL COMMENT ''Final review note'' AFTER reviewed_name', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_reversal' AND COLUMN_NAME = 'resolved_time') = 0,
  'ALTER TABLE gl_purchase_reversal ADD COLUMN resolved_time DATETIME DEFAULT NULL COMMENT ''Final disposition time'' AFTER review_note', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_reversal' AND COLUMN_NAME = 'retry_count') = 0,
  'ALTER TABLE gl_purchase_reversal ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT ''Manual retry count'' AFTER resolved_time', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_reversal' AND COLUMN_NAME = 'last_retry_time') = 0,
  'ALTER TABLE gl_purchase_reversal ADD COLUMN last_retry_time DATETIME DEFAULT NULL COMMENT ''Latest manual retry time'' AFTER retry_count', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_purchase_reversal' AND COLUMN_NAME = 'version') = 0,
  'ALTER TABLE gl_purchase_reversal ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT ''Optimistic lock version'' AFTER last_retry_time', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE gl_purchase_reversal
SET disposition_status = 'PENDING_REVIEW', retry_count = COALESCE(retry_count, 0), version = COALESCE(version, 0)
WHERE status = 'REVIEW_REQUIRED' AND disposition_status IS NULL;

CREATE TABLE IF NOT EXISTS gl_purchase_reversal_review_log (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  operation_no VARCHAR(64) NOT NULL COMMENT 'Review operation number',
  reversal_id BIGINT NOT NULL COMMENT 'Purchase reversal id',
  reversal_no VARCHAR(64) NOT NULL COMMENT 'Purchase reversal number',
  request_key VARCHAR(128) NOT NULL COMMENT 'Admin request idempotency key',
  operation_type VARCHAR(32) NOT NULL COMMENT 'RETRY_INSUFFICIENT, RETRY_COMPLETED, or LOSS_ACCEPTED',
  before_status VARCHAR(32) NOT NULL COMMENT 'Disposition before operation',
  after_status VARCHAR(32) NOT NULL COMMENT 'Disposition after operation',
  operator_id BIGINT NOT NULL COMMENT 'Operator user id',
  operator_name VARCHAR(100) NOT NULL COMMENT 'Operator name snapshot',
  review_note VARCHAR(500) DEFAULT NULL COMMENT 'Operation review note',
  snapshot_json LONGTEXT NOT NULL COMMENT 'Per-currency recovery snapshot JSON',
  create_time DATETIME NOT NULL COMMENT 'Operation time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_reversal_review_log_01 (tenant_id, request_key),
  UNIQUE KEY uk_gl_purchase_reversal_review_log_02 (tenant_id, operation_no),
  KEY idx_gl_purchase_reversal_review_log_01 (tenant_id, reversal_no, create_time),
  KEY idx_gl_purchase_reversal_review_log_02 (tenant_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase reversal manual review operation log';

CREATE TABLE IF NOT EXISTS gl_payment_session (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  session_no VARCHAR(64) NOT NULL COMMENT 'Internal payment session number',
  purchase_order_id BIGINT NOT NULL COMMENT 'Purchase order id',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order number',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  provider_code VARCHAR(64) NOT NULL COMMENT 'Payment provider code',
  provider_session_no VARCHAR(128) DEFAULT NULL COMMENT 'Provider session number',
  pay_currency_code VARCHAR(32) NOT NULL COMMENT 'Payment currency',
  pay_amount DECIMAL(20,6) NOT NULL COMMENT 'Payment amount',
  checkout_url VARCHAR(1000) DEFAULT NULL COMMENT 'Provider checkout URL',
  status VARCHAR(32) NOT NULL COMMENT 'CREATED,PENDING,SUCCEEDED,FAILED,CANCELLED,EXPIRED',
  request_key VARCHAR(128) NOT NULL COMMENT 'Session creation idempotency key',
  expire_time DATETIME NOT NULL COMMENT 'Session expiry time',
  completed_time DATETIME DEFAULT NULL COMMENT 'Session completion time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_session_01 (tenant_id, session_no),
  UNIQUE KEY uk_gl_payment_session_02 (tenant_id, request_key),
  UNIQUE KEY uk_gl_payment_session_03 (tenant_id, provider_code, provider_session_no),
  KEY idx_gl_payment_session_01 (tenant_id, purchase_order_no),
  KEY idx_gl_payment_session_02 (tenant_id, member_id, status, create_time),
  KEY idx_gl_payment_session_03 (tenant_id, status, expire_time),
  KEY idx_gl_payment_session_04 (tenant_id, create_time, id),
  KEY idx_gl_payment_session_05 (tenant_id, provider_session_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Provider-neutral payment session';

CREATE TABLE IF NOT EXISTS gl_payment_webhook_event (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  provider_code VARCHAR(64) NOT NULL COMMENT 'Payment provider code',
  provider_event_id VARCHAR(128) NOT NULL COMMENT 'Provider event id',
  event_type VARCHAR(32) NOT NULL COMMENT 'Normalized provider event type',
  provider_session_no VARCHAR(128) DEFAULT NULL COMMENT 'Provider session number',
  session_no VARCHAR(64) DEFAULT NULL COMMENT 'Internal payment session number',
  purchase_order_no VARCHAR(64) DEFAULT NULL COMMENT 'Purchase order number',
  raw_body LONGTEXT NOT NULL COMMENT 'Raw webhook request body',
  signature_digest VARCHAR(128) DEFAULT NULL COMMENT 'Webhook signature digest',
  received_time DATETIME NOT NULL COMMENT 'Webhook receipt time',
  status VARCHAR(32) NOT NULL COMMENT 'RECEIVED,PROCESSED,FAILED,IGNORED',
  failure_reason VARCHAR(500) DEFAULT NULL COMMENT 'Processing failure reason',
  processing_count INT NOT NULL DEFAULT 0 COMMENT 'Processing attempt count',
  last_processing_time DATETIME DEFAULT NULL COMMENT 'Latest processing time',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_webhook_event_01 (tenant_id, provider_code, provider_event_id),
  KEY idx_gl_payment_webhook_event_01 (tenant_id, session_no),
  KEY idx_gl_payment_webhook_event_02 (tenant_id, purchase_order_no),
  KEY idx_gl_payment_webhook_event_03 (tenant_id, status, received_time),
  KEY idx_gl_payment_webhook_event_04 (tenant_id, provider_code, provider_session_no),
  KEY idx_gl_payment_webhook_event_05 (tenant_id, received_time, id),
  KEY idx_gl_payment_webhook_event_06 (tenant_id, provider_event_id),
  KEY idx_gl_payment_webhook_event_07 (tenant_id, provider_session_no),
  KEY idx_gl_payment_webhook_event_08 (tenant_id, provider_code, received_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment provider webhook event inbox';

SET @db_name := DATABASE();
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_payment_session' AND INDEX_NAME = 'idx_gl_payment_session_04');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE gl_payment_session ADD KEY idx_gl_payment_session_04 (tenant_id, create_time, id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_payment_session' AND INDEX_NAME = 'idx_gl_payment_session_05');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE gl_payment_session ADD KEY idx_gl_payment_session_05 (tenant_id, provider_session_no)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_payment_webhook_event' AND INDEX_NAME = 'idx_gl_payment_webhook_event_05');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE gl_payment_webhook_event ADD KEY idx_gl_payment_webhook_event_05 (tenant_id, received_time, id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_payment_webhook_event' AND INDEX_NAME = 'idx_gl_payment_webhook_event_06');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE gl_payment_webhook_event ADD KEY idx_gl_payment_webhook_event_06 (tenant_id, provider_event_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_payment_webhook_event' AND INDEX_NAME = 'idx_gl_payment_webhook_event_07');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE gl_payment_webhook_event ADD KEY idx_gl_payment_webhook_event_07 (tenant_id, provider_session_no)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_payment_webhook_event' AND INDEX_NAME = 'idx_gl_payment_webhook_event_08');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE gl_payment_webhook_event ADD KEY idx_gl_payment_webhook_event_08 (tenant_id, provider_code, received_time, id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DELETE FROM sys_dict_data WHERE tenant_id = '000000' AND dict_type IN
  ('gl_payment_session_status', 'gl_payment_webhook_status', 'gl_payment_provider_event_type');
DELETE FROM sys_dict_type WHERE tenant_id = '000000' AND dict_type IN
  ('gl_payment_session_status', 'gl_payment_webhook_status', 'gl_payment_provider_event_type');

INSERT INTO sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(20037, '000000', '支付会话状态', 'gl_payment_session_status', 103, 1, SYSDATE(), NULL, NULL, '支付通道会话生命周期状态'),
(20038, '000000', '支付回调状态', 'gl_payment_webhook_status', 103, 1, SYSDATE(), NULL, NULL, '支付回调处理状态'),
(20039, '000000', '支付通道事件类型', 'gl_payment_provider_event_type', 103, 1, SYSDATE(), NULL, NULL, '支付通道回调事件类型');

INSERT INTO sys_dict_data
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(21276, '000000', 1, '已创建', 'CREATED', 'gl_payment_session_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21277, '000000', 2, '待支付', 'PENDING', 'gl_payment_session_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, ''),
(21278, '000000', 3, '成功', 'SUCCEEDED', 'gl_payment_session_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21279, '000000', 4, '失败', 'FAILED', 'gl_payment_session_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21280, '000000', 5, '已取消', 'CANCELLED', 'gl_payment_session_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21281, '000000', 6, '已过期', 'EXPIRED', 'gl_payment_session_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21282, '000000', 1, '已接收', 'RECEIVED', 'gl_payment_webhook_status', '', 'warning', 'Y', 103, 1, SYSDATE(), NULL, NULL, ''),
(21283, '000000', 2, '已处理', 'PROCESSED', 'gl_payment_webhook_status', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21284, '000000', 3, '失败', 'FAILED', 'gl_payment_webhook_status', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21285, '000000', 4, '已忽略', 'IGNORED', 'gl_payment_webhook_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21286, '000000', 1, '支付成功', 'PAYMENT_SUCCEEDED', 'gl_payment_provider_event_type', '', 'success', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21287, '000000', 2, '支付失败', 'PAYMENT_FAILED', 'gl_payment_provider_event_type', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21288, '000000', 3, '支付取消', 'PAYMENT_CANCELLED', 'gl_payment_provider_event_type', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21289, '000000', 4, '退款成功', 'REFUND_SUCCEEDED', 'gl_payment_provider_event_type', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, ''),
(21290, '000000', 5, '拒付创建', 'CHARGEBACK_CREATED', 'gl_payment_provider_event_type', '', 'danger', 'N', 103, 1, SYSDATE(), NULL, NULL, '');

CREATE TABLE IF NOT EXISTS gl_simulated_payment_dispatch (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  provider_session_no VARCHAR(128) NOT NULL COMMENT 'Provider session number',
  provider_event_id VARCHAR(128) NOT NULL COMMENT 'Provider event id',
  action VARCHAR(32) NOT NULL COMMENT 'Hosted checkout action',
  occurred_time DATETIME NOT NULL COMMENT 'Signed event occurrence time',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_simulated_payment_dispatch_01 (tenant_id, provider_event_id),
  KEY idx_gl_simulated_payment_dispatch_01 (tenant_id, provider_session_no, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hosted simulated checkout dispatch marker';

CREATE TABLE IF NOT EXISTS gl_payment_reconciliation_batch (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  provider_code VARCHAR(64) NOT NULL COMMENT 'Payment provider code',
  statement_date DATE NOT NULL COMMENT 'Provider statement date in UTC',
  original_file_name VARCHAR(255) NOT NULL COMMENT 'Original uploaded file name',
  file_digest VARCHAR(64) NOT NULL COMMENT 'SHA-256 digest of uploaded bytes',
  total_count INT NOT NULL DEFAULT 0 COMMENT 'Total parsed record count',
  valid_count INT NOT NULL DEFAULT 0 COMMENT 'Valid record count',
  invalid_count INT NOT NULL DEFAULT 0 COMMENT 'Invalid record count',
  matched_count INT NOT NULL DEFAULT 0 COMMENT 'Matched record count',
  discrepancy_count INT NOT NULL DEFAULT 0 COMMENT 'Discrepancy count',
  status VARCHAR(32) NOT NULL COMMENT 'Batch lifecycle status',
  failure_reason VARCHAR(500) DEFAULT NULL COMMENT 'Infrastructure failure reason',
  creator_id BIGINT NOT NULL COMMENT 'Creator user id',
  creator_name VARCHAR(100) NOT NULL COMMENT 'Creator name snapshot',
  version INT NOT NULL DEFAULT 0 COMMENT 'Guarded transition version',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_reconciliation_batch_01 (tenant_id, provider_code, file_digest),
  KEY idx_gl_payment_reconciliation_batch_01 (tenant_id, status, statement_date),
  KEY idx_gl_payment_reconciliation_batch_02 (tenant_id, provider_code, statement_date, id),
  KEY idx_gl_payment_reconciliation_batch_03 (tenant_id, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment reconciliation import batch';

CREATE TABLE IF NOT EXISTS gl_payment_reconciliation_line (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  batch_id BIGINT NOT NULL COMMENT 'Reconciliation batch id',
  source_row_number BIGINT NOT NULL COMMENT 'CSV parser source line number',
  provider_record_id VARCHAR(128) DEFAULT NULL COMMENT 'Provider statement record id',
  event_type VARCHAR(32) DEFAULT NULL COMMENT 'Normalized provider event type',
  provider_session_no VARCHAR(128) DEFAULT NULL COMMENT 'Provider session number',
  purchase_order_no VARCHAR(64) DEFAULT NULL COMMENT 'Internal purchase order number',
  currency_code VARCHAR(32) DEFAULT NULL COMMENT 'Payment currency',
  amount DECIMAL(20,6) DEFAULT NULL COMMENT 'Provider payment amount',
  occurred_time DATETIME DEFAULT NULL COMMENT 'Provider event occurrence time',
  status VARCHAR(32) NOT NULL COMMENT 'VALID, INVALID, MATCHED, or ISSUE',
  parse_error VARCHAR(500) DEFAULT NULL COMMENT 'Validation error',
  raw_fields_json LONGTEXT NOT NULL COMMENT 'Canonical JSON array from parsed CSV fields',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_reconciliation_line_01 (tenant_id, batch_id, source_row_number),
  KEY idx_gl_payment_reconciliation_line_02 (tenant_id, provider_record_id),
  KEY idx_gl_payment_reconciliation_line_03 (tenant_id, purchase_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable normalized reconciliation statement line';

CREATE TABLE IF NOT EXISTS gl_payment_reconciliation_issue (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  batch_id BIGINT NOT NULL COMMENT 'Reconciliation batch id',
  line_id BIGINT DEFAULT NULL COMMENT 'Related statement line id',
  issue_type VARCHAR(64) NOT NULL COMMENT 'Primary discrepancy type',
  status VARCHAR(32) NOT NULL COMMENT 'OPEN, RESOLVED, or IGNORED',
  payment_session_id BIGINT DEFAULT NULL COMMENT 'Related payment session id',
  session_no VARCHAR(64) DEFAULT NULL COMMENT 'Related payment session number',
  purchase_order_id BIGINT DEFAULT NULL COMMENT 'Related purchase order id',
  purchase_order_no VARCHAR(64) DEFAULT NULL COMMENT 'Related purchase order number',
  webhook_event_id BIGINT DEFAULT NULL COMMENT 'Related webhook event id',
  reversal_id BIGINT DEFAULT NULL COMMENT 'Related reversal id',
  provider_event_type VARCHAR(32) DEFAULT NULL COMMENT 'Provider event comparison value',
  platform_event_type VARCHAR(32) DEFAULT NULL COMMENT 'Platform event comparison value',
  provider_currency_code VARCHAR(32) DEFAULT NULL COMMENT 'Provider currency comparison value',
  platform_currency_code VARCHAR(32) DEFAULT NULL COMMENT 'Platform currency comparison value',
  provider_amount DECIMAL(20,6) DEFAULT NULL COMMENT 'Provider amount comparison value',
  platform_amount DECIMAL(20,6) DEFAULT NULL COMMENT 'Platform amount comparison value',
  provider_status VARCHAR(32) DEFAULT NULL COMMENT 'Provider status comparison value',
  platform_status VARCHAR(32) DEFAULT NULL COMMENT 'Platform status comparison value',
  diagnostic_snapshot_json LONGTEXT NOT NULL COMMENT 'Immutable complete discrepancy diagnostic JSON',
  resolution_type VARCHAR(32) DEFAULT NULL COMMENT 'Manual resolution classification',
  resolution_remark VARCHAR(500) DEFAULT NULL COMMENT 'Required terminal resolution remark',
  resolved_by BIGINT DEFAULT NULL COMMENT 'Resolver user id',
  resolved_time DATETIME DEFAULT NULL COMMENT 'Resolution time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_reconciliation_issue_01 (tenant_id, batch_id, line_id),
  KEY idx_gl_payment_reconciliation_issue_01 (tenant_id, batch_id, status, create_time),
  KEY idx_gl_payment_reconciliation_issue_02 (tenant_id, line_id),
  KEY idx_gl_payment_reconciliation_issue_03 (tenant_id, purchase_order_no),
  KEY idx_gl_payment_reconciliation_issue_04 (tenant_id, session_no),
  KEY idx_gl_payment_reconciliation_issue_05 (tenant_id, batch_id, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment reconciliation discrepancy';

CREATE TABLE IF NOT EXISTS gl_payment_reconciliation_action_log (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  batch_id BIGINT NOT NULL COMMENT 'Reconciliation batch id',
  issue_id BIGINT DEFAULT NULL COMMENT 'Related discrepancy id',
  action_type VARCHAR(32) NOT NULL COMMENT 'Business reconciliation action',
  before_status VARCHAR(32) DEFAULT NULL COMMENT 'Business state before action',
  after_status VARCHAR(32) DEFAULT NULL COMMENT 'Business state after action',
  operator_id BIGINT NOT NULL COMMENT 'Operator user id',
  operator_name VARCHAR(100) NOT NULL COMMENT 'Operator name snapshot',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Action or mandatory resolution remark',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Action time',
  PRIMARY KEY (id),
  KEY idx_gl_payment_reconciliation_action_log_01 (tenant_id, batch_id, create_time, id),
  KEY idx_gl_payment_reconciliation_action_log_02 (tenant_id, issue_id, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Append-only payment reconciliation action log';

SET @reversal_reconciliation_idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gl_purchase_reversal'
    AND INDEX_NAME = 'idx_gl_purchase_reversal_03'
);
SET @reversal_reconciliation_idx_sql := IF(@reversal_reconciliation_idx_exists = 0,
  'ALTER TABLE gl_purchase_reversal ADD INDEX idx_gl_purchase_reversal_03 (tenant_id, purchase_order_no, create_time, id)',
  'SELECT 1'
);
PREPARE stmt FROM @reversal_reconciliation_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS gl_payment_settlement_batch (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  settlement_no VARCHAR(64) NOT NULL,
  provider_code VARCHAR(64) NOT NULL,
  currency_code VARCHAR(32) NOT NULL,
  period_start DATETIME NOT NULL,
  period_end DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL,
  payment_fee_rate DECIMAL(12,8) NOT NULL,
  payment_fixed_fee DECIMAL(20,6) NOT NULL,
  chargeback_fixed_fee DECIMAL(20,6) NOT NULL,
  event_count INT NOT NULL DEFAULT 0,
  payment_count INT NOT NULL DEFAULT 0,
  refund_count INT NOT NULL DEFAULT 0,
  chargeback_count INT NOT NULL DEFAULT 0,
  gross_payment DECIMAL(20,6) NOT NULL DEFAULT 0,
  refund_amount DECIMAL(20,6) NOT NULL DEFAULT 0,
  chargeback_amount DECIMAL(20,6) NOT NULL DEFAULT 0,
  total_fee DECIMAL(20,6) NOT NULL DEFAULT 0,
  net_settlement DECIMAL(20,6) NOT NULL DEFAULT 0,
  reconciliation_coverage_count INT NOT NULL DEFAULT 0,
  open_issue_count INT NOT NULL DEFAULT 0,
  evidence_snapshot_json LONGTEXT DEFAULT NULL,
  failure_reason VARCHAR(500) DEFAULT NULL,
  creator_id BIGINT NOT NULL,
  creator_name VARCHAR(100) NOT NULL,
  calculator_id BIGINT DEFAULT NULL,
  calculator_name VARCHAR(100) DEFAULT NULL,
  closer_id BIGINT DEFAULT NULL,
  closer_name VARCHAR(100) DEFAULT NULL,
  close_remark VARCHAR(500) DEFAULT NULL,
  calculated_time DATETIME DEFAULT NULL,
  closed_time DATETIME DEFAULT NULL,
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_settlement_batch_01 (tenant_id, settlement_no),
  UNIQUE KEY uk_gl_payment_settlement_batch_02 (tenant_id, provider_code, currency_code, period_start, period_end),
  KEY idx_gl_payment_settlement_batch_01 (tenant_id, status, period_start, id),
  KEY idx_gl_payment_settlement_batch_02 (tenant_id, provider_code, currency_code, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment settlement financial snapshot batch';

CREATE TABLE IF NOT EXISTS gl_payment_settlement_item (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  batch_id BIGINT NOT NULL,
  webhook_event_id BIGINT NOT NULL,
  provider_event_id VARCHAR(128) NOT NULL,
  payment_session_id BIGINT NOT NULL,
  session_no VARCHAR(64) NOT NULL,
  provider_session_no VARCHAR(128) NOT NULL,
  purchase_order_id BIGINT NOT NULL,
  purchase_order_no VARCHAR(64) NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  received_time DATETIME NOT NULL,
  currency_code VARCHAR(32) NOT NULL,
  source_amount DECIMAL(20,6) NOT NULL,
  gross_payment DECIMAL(20,6) NOT NULL,
  refund_amount DECIMAL(20,6) NOT NULL,
  chargeback_amount DECIMAL(20,6) NOT NULL,
  fee_amount DECIMAL(20,6) NOT NULL,
  net_contribution DECIMAL(20,6) NOT NULL,
  source_snapshot_json LONGTEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_settlement_item_01 (tenant_id, webhook_event_id),
  KEY idx_gl_payment_settlement_item_01 (tenant_id, batch_id, received_time, id),
  KEY idx_gl_payment_settlement_item_02 (tenant_id, purchase_order_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable payment settlement event item';

CREATE TABLE IF NOT EXISTS gl_payment_settlement_action_log (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  batch_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  before_status VARCHAR(32) DEFAULT NULL,
  after_status VARCHAR(32) DEFAULT NULL,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(100) NOT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  evidence_snapshot_json LONGTEXT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_gl_payment_settlement_action_log_01 (tenant_id, batch_id, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Append-only payment settlement action log';

-- Phase 44 payment reconciliation admin menu and dictionaries (idempotent delete + insert).
DELETE FROM sys_menu WHERE menu_id IN (20321, 20322, 20323, 20324, 2032);
UPDATE sys_menu SET order_num = 7 WHERE menu_id = 19195 AND parent_id = 1900;
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(2032, '支付对账', 1900, 6, 'payment-reconciliation', 'payment/payment-reconciliation/index', '', 1, 0, 'C', '0', '0', 'payment:reconciliation:list', 'audit', 103, 1, NOW(), NULL, NULL, '支付对账与差异处理工作台'),
(20321, '支付对账查询', 2032, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:reconciliation:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(20322, '支付对账上传', 2032, 2, '#', '', '', 1, 0, 'F', '0', '0', 'payment:reconciliation:upload', '#', 103, 1, NOW(), NULL, NULL, ''),
(20323, '支付对账执行', 2032, 3, '#', '', '', 1, 0, 'F', '0', '0', 'payment:reconciliation:execute', '#', 103, 1, NOW(), NULL, NULL, ''),
(20324, '支付对账处理', 2032, 4, '#', '', '', 1, 0, 'F', '0', '0', 'payment:reconciliation:resolve', '#', 103, 1, NOW(), NULL, NULL, '');

DELETE FROM sys_dict_data WHERE tenant_id='000000' AND dict_type IN
('gl_payment_reconciliation_batch_status','gl_payment_reconciliation_line_status','gl_payment_reconciliation_issue_type','gl_payment_reconciliation_issue_status','gl_payment_reconciliation_resolution_type');
DELETE FROM sys_dict_type WHERE tenant_id='000000' AND dict_type IN
('gl_payment_reconciliation_batch_status','gl_payment_reconciliation_line_status','gl_payment_reconciliation_issue_type','gl_payment_reconciliation_issue_status','gl_payment_reconciliation_resolution_type');
INSERT INTO sys_dict_type
(dict_id,tenant_id,dict_name,dict_type,create_dept,create_by,create_time,remark) VALUES
(20040,'000000','支付对账批次状态','gl_payment_reconciliation_batch_status',103,1,SYSDATE(),''),
(20041,'000000','支付对账行状态','gl_payment_reconciliation_line_status',103,1,SYSDATE(),''),
(20042,'000000','支付对账差异类型','gl_payment_reconciliation_issue_type',103,1,SYSDATE(),''),
(20043,'000000','支付对账差异状态','gl_payment_reconciliation_issue_status',103,1,SYSDATE(),''),
(20044,'000000','支付对账处理类型','gl_payment_reconciliation_resolution_type',103,1,SYSDATE(),'');
INSERT INTO sys_dict_data
(dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,create_dept,create_by,create_time,remark) VALUES
(21291,'000000',1,'已上传','UPLOADED','gl_payment_reconciliation_batch_status','','info','N',103,1,SYSDATE(),''),(21292,'000000',2,'已校验','VALIDATED','gl_payment_reconciliation_batch_status','','warning','N',103,1,SYSDATE(),''),(21293,'000000',3,'对账中','RECONCILING','gl_payment_reconciliation_batch_status','','warning','N',103,1,SYSDATE(),''),(21294,'000000',4,'已完成','COMPLETED','gl_payment_reconciliation_batch_status','','success','N',103,1,SYSDATE(),''),(21295,'000000',5,'失败','FAILED','gl_payment_reconciliation_batch_status','','danger','N',103,1,SYSDATE(),''),
(21296,'000000',1,'有效','VALID','gl_payment_reconciliation_line_status','','info','N',103,1,SYSDATE(),''),(21297,'000000',2,'无效','INVALID','gl_payment_reconciliation_line_status','','danger','N',103,1,SYSDATE(),''),(21298,'000000',3,'已匹配','MATCHED','gl_payment_reconciliation_line_status','','success','N',103,1,SYSDATE(),''),(21299,'000000',4,'有差异','ISSUE','gl_payment_reconciliation_line_status','','warning','N',103,1,SYSDATE(),''),
(21300,'000000',1,'平台记录缺失','PLATFORM_RECORD_MISSING','gl_payment_reconciliation_issue_type','','danger','N',103,1,SYSDATE(),''),(21301,'000000',2,'通道记录缺失','PROVIDER_RECORD_MISSING','gl_payment_reconciliation_issue_type','','danger','N',103,1,SYSDATE(),''),(21302,'000000',3,'订单身份不符','ORDER_IDENTITY_MISMATCH','gl_payment_reconciliation_issue_type','','warning','N',103,1,SYSDATE(),''),(21303,'000000',4,'金额不符','AMOUNT_MISMATCH','gl_payment_reconciliation_issue_type','','warning','N',103,1,SYSDATE(),''),(21304,'000000',5,'币种不符','CURRENCY_MISMATCH','gl_payment_reconciliation_issue_type','','warning','N',103,1,SYSDATE(),''),(21305,'000000',6,'事件缺失','EVENT_MISSING','gl_payment_reconciliation_issue_type','','warning','N',103,1,SYSDATE(),''),(21306,'000000',7,'状态不符','STATUS_MISMATCH','gl_payment_reconciliation_issue_type','','warning','N',103,1,SYSDATE(),''),(21307,'000000',8,'通道记录重复','DUPLICATE_PROVIDER_RECORD','gl_payment_reconciliation_issue_type','','warning','N',103,1,SYSDATE(),''),(21308,'000000',9,'不支持的记录','UNSUPPORTED_RECORD','gl_payment_reconciliation_issue_type','','info','N',103,1,SYSDATE(),''),
(21309,'000000',1,'待处理','OPEN','gl_payment_reconciliation_issue_status','','warning','N',103,1,SYSDATE(),''),(21310,'000000',2,'已解决','RESOLVED','gl_payment_reconciliation_issue_status','','success','N',103,1,SYSDATE(),''),(21311,'000000',3,'已忽略','IGNORED','gl_payment_reconciliation_issue_status','','info','N',103,1,SYSDATE(),''),
(21312,'000000',1,'平台数据确认','PLATFORM_CONFIRMED','gl_payment_reconciliation_resolution_type','','success','N',103,1,SYSDATE(),''),(21313,'000000',2,'通道数据确认','PROVIDER_CONFIRMED','gl_payment_reconciliation_resolution_type','','success','N',103,1,SYSDATE(),''),(21314,'000000',3,'预期差异','EXPECTED_DIFFERENCE','gl_payment_reconciliation_resolution_type','','info','N',103,1,SYSDATE(),''),(21315,'000000',4,'重复记录确认','DUPLICATE_CONFIRMED','gl_payment_reconciliation_resolution_type','','info','N',103,1,SYSDATE(),''),(21316,'000000',5,'其他','OTHER','gl_payment_reconciliation_resolution_type','','info','N',103,1,SYSDATE(),'');

-- Phase 45 payment settlement admin menu and dictionaries (idempotent delete + insert).
DELETE FROM sys_menu WHERE menu_id IN (20331,20332,20333,20334,2033);
UPDATE sys_menu SET order_num=8 WHERE menu_id=19195 AND parent_id=1900;
INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
(2033,'支付结算',1900,7,'payment-settlement','payment/payment-settlement/index','',1,0,'C','0','0','payment:settlement:list','receipt',103,1,NOW(),NULL,NULL,'支付结算批次与财务汇总工作台'),
(20331,'支付结算查询',2033,1,'#','','',1,0,'F','0','0','payment:settlement:query','#',103,1,NOW(),NULL,NULL,''),
(20332,'支付结算创建',2033,2,'#','','',1,0,'F','0','0','payment:settlement:create','#',103,1,NOW(),NULL,NULL,''),
(20333,'支付结算计算',2033,3,'#','','',1,0,'F','0','0','payment:settlement:calculate','#',103,1,NOW(),NULL,NULL,''),
(20334,'支付结算关闭',2033,4,'#','','',1,0,'F','0','0','payment:settlement:close','#',103,1,NOW(),NULL,NULL,'');

-- Phase 46 payment settlement report admin menu (idempotent delete + insert).
DELETE FROM sys_menu WHERE menu_id IN (20341,20342,20343,2034);
UPDATE sys_menu SET order_num=9 WHERE menu_id=19195 AND parent_id=1900;
INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
(2034,'支付结算报表',1900,8,'payment-settlement-report','payment/payment-settlement-report/index','',1,0,'C','0','0','payment:settlementReport:list','chart',103,1,NOW(),NULL,NULL,'支付结算日报与导出工作台'),
(20341,'支付结算报表列表',2034,1,'#','','',1,0,'F','0','0','payment:settlementReport:list','#',103,1,NOW(),NULL,NULL,''),
(20342,'支付结算报表查询',2034,2,'#','','',1,0,'F','0','0','payment:settlementReport:query','#',103,1,NOW(),NULL,NULL,''),
(20343,'支付结算报表导出',2034,3,'#','','',1,0,'F','0','0','payment:settlementReport:export','#',103,1,NOW(),NULL,NULL,'');

DELETE FROM sys_dict_data WHERE tenant_id='000000' AND dict_type IN
('gl_payment_settlement_batch_status','gl_payment_settlement_action_type');
DELETE FROM sys_dict_type WHERE tenant_id='000000' AND dict_type IN
('gl_payment_settlement_batch_status','gl_payment_settlement_action_type');
INSERT INTO sys_dict_type
(dict_id,tenant_id,dict_name,dict_type,create_dept,create_by,create_time,remark) VALUES
(20045,'000000','支付结算批次状态','gl_payment_settlement_batch_status',103,1,SYSDATE(),''),
(20046,'000000','支付结算操作类型','gl_payment_settlement_action_type',103,1,SYSDATE(),'');
INSERT INTO sys_dict_data
(dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,create_dept,create_by,create_time,remark) VALUES
(21317,'000000',1,'已创建','CREATED','gl_payment_settlement_batch_status','','info','N',103,1,SYSDATE(),''),
(21318,'000000',2,'计算中','CALCULATING','gl_payment_settlement_batch_status','','warning','N',103,1,SYSDATE(),''),
(21319,'000000',3,'已计算','CALCULATED','gl_payment_settlement_batch_status','','primary','N',103,1,SYSDATE(),''),
(21320,'000000',4,'已关闭','CLOSED','gl_payment_settlement_batch_status','','success','N',103,1,SYSDATE(),''),
(21321,'000000',5,'失败','FAILED','gl_payment_settlement_batch_status','','danger','N',103,1,SYSDATE(),''),
(21322,'000000',1,'创建','CREATE','gl_payment_settlement_action_type','','info','N',103,1,SYSDATE(),''),
(21323,'000000',2,'计算','CALCULATE','gl_payment_settlement_action_type','','primary','N',103,1,SYSDATE(),''),
(21324,'000000',3,'计算失败','CALCULATION_FAILED','gl_payment_settlement_action_type','','danger','N',103,1,SYSDATE(),''),
(21325,'000000',4,'关闭拒绝','CLOSE_REJECTED','gl_payment_settlement_action_type','','warning','N',103,1,SYSDATE(),''),
(21326,'000000',5,'关闭','CLOSE','gl_payment_settlement_action_type','','success','N',103,1,SYSDATE(),'');

CREATE TABLE IF NOT EXISTS gl_payment_settlement_payout (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  payout_no VARCHAR(64) NOT NULL,
  settlement_batch_id BIGINT NOT NULL,
  settlement_no VARCHAR(64) NOT NULL,
  provider_code VARCHAR(64) NOT NULL,
  currency_code VARCHAR(32) NOT NULL,
  payout_amount DECIMAL(20,6) NOT NULL,
  settlement_evidence_json LONGTEXT DEFAULT NULL,
  payout_purpose VARCHAR(500) NOT NULL,
  payee_reference VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  maker_id BIGINT NOT NULL,
  maker_name VARCHAR(100) NOT NULL,
  submitter_id BIGINT DEFAULT NULL,
  submitter_name VARCHAR(100) DEFAULT NULL,
  reviewer_id BIGINT DEFAULT NULL,
  reviewer_name VARCHAR(100) DEFAULT NULL,
  decision_reason VARCHAR(500) DEFAULT NULL,
  version INT NOT NULL DEFAULT 0,
  submitted_time DATETIME DEFAULT NULL,
  reviewed_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_payment_settlement_payout_01 (tenant_id, payout_no),
  UNIQUE KEY uk_gl_payment_settlement_payout_02 (tenant_id, settlement_batch_id),
  KEY idx_gl_payment_settlement_payout_01 (tenant_id, status, create_time, id),
  KEY idx_gl_payment_settlement_payout_02 (tenant_id, settlement_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Settlement payout instruction and approval state';

CREATE TABLE IF NOT EXISTS gl_payment_settlement_payout_action_log (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  payout_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  before_status VARCHAR(32) DEFAULT NULL,
  after_status VARCHAR(32) NOT NULL,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(100) NOT NULL,
  reason VARCHAR(500) DEFAULT NULL,
  evidence_snapshot_json LONGTEXT DEFAULT NULL,
  expected_version INT DEFAULT NULL,
  result_version INT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_gl_payment_settlement_payout_action_log_01 (tenant_id, payout_id, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Append-only settlement payout action log';

-- Phase 47 settlement payout approval menu and dictionaries (idempotent delete + insert).
DELETE FROM sys_menu WHERE menu_id IN (20351,20352,20353,20354,20355,20356,2035);
UPDATE sys_menu SET order_num=10 WHERE menu_id=19195 AND parent_id=1900;
INSERT INTO sys_menu
(menu_id,menu_name,parent_id,order_num,path,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
(2035,'结算付款审批',1900,9,'payment-settlement-payout','payment/payment-settlement-payout/index','',1,0,'C','0','0','payment:settlementPayout:list','money',103,1,NOW(),NULL,NULL,'结算付款指令与双人审批工作台'),
(20351,'结算付款列表',2035,1,'#','','',1,0,'F','0','0','payment:settlementPayout:list','#',103,1,NOW(),NULL,NULL,''),
(20352,'结算付款查询',2035,2,'#','','',1,0,'F','0','0','payment:settlementPayout:query','#',103,1,NOW(),NULL,NULL,''),
(20353,'结算付款创建',2035,3,'#','','',1,0,'F','0','0','payment:settlementPayout:create','#',103,1,NOW(),NULL,NULL,''),
(20354,'结算付款提交',2035,4,'#','','',1,0,'F','0','0','payment:settlementPayout:submit','#',103,1,NOW(),NULL,NULL,''),
(20355,'结算付款审批',2035,5,'#','','',1,0,'F','0','0','payment:settlementPayout:approve','#',103,1,NOW(),NULL,NULL,''),
(20356,'结算付款取消',2035,6,'#','','',1,0,'F','0','0','payment:settlementPayout:cancel','#',103,1,NOW(),NULL,NULL,'');

DELETE FROM sys_dict_data WHERE tenant_id='000000' AND dict_type IN
('gl_payment_settlement_payout_status','gl_payment_settlement_payout_action_type');
DELETE FROM sys_dict_type WHERE tenant_id='000000' AND dict_type IN
('gl_payment_settlement_payout_status','gl_payment_settlement_payout_action_type');
INSERT INTO sys_dict_type
(dict_id,tenant_id,dict_name,dict_type,create_dept,create_by,create_time,remark) VALUES
(20047,'000000','结算付款状态','gl_payment_settlement_payout_status',103,1,SYSDATE(),''),
(20048,'000000','结算付款操作类型','gl_payment_settlement_payout_action_type',103,1,SYSDATE(),'');
INSERT INTO sys_dict_data
(dict_code,tenant_id,dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,create_dept,create_by,create_time,remark) VALUES
(21327,'000000',1,'草稿','DRAFT','gl_payment_settlement_payout_status','','info','N',103,1,SYSDATE(),''),
(21328,'000000',2,'待审批','PENDING_APPROVAL','gl_payment_settlement_payout_status','','warning','N',103,1,SYSDATE(),''),
(21329,'000000',3,'已批准','APPROVED','gl_payment_settlement_payout_status','','success','N',103,1,SYSDATE(),''),
(21330,'000000',4,'已拒绝','REJECTED','gl_payment_settlement_payout_status','','danger','N',103,1,SYSDATE(),''),
(21331,'000000',5,'已取消','CANCELLED','gl_payment_settlement_payout_status','','info','N',103,1,SYSDATE(),''),
(21332,'000000',1,'创建','CREATE','gl_payment_settlement_payout_action_type','','info','N',103,1,SYSDATE(),''),
(21333,'000000',2,'编辑','EDIT','gl_payment_settlement_payout_action_type','','primary','N',103,1,SYSDATE(),''),
(21334,'000000',3,'提交','SUBMIT','gl_payment_settlement_payout_action_type','','warning','N',103,1,SYSDATE(),''),
(21335,'000000',4,'批准','APPROVE','gl_payment_settlement_payout_action_type','','success','N',103,1,SYSDATE(),''),
(21336,'000000',5,'拒绝','REJECT','gl_payment_settlement_payout_action_type','','danger','N',103,1,SYSDATE(),''),
(21337,'000000',6,'取消','CANCEL','gl_payment_settlement_payout_action_type','','info','N',103,1,SYSDATE(),'');
