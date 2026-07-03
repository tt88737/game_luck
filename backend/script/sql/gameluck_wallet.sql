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
  withdraw_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Withdraw capable: 0 yes, 1 no',
  exchange_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange capable: 0 yes, 1 no',
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

INSERT INTO gl_wallet_currency
(id, tenant_id, currency_code, currency_name, scale_num, enabled, credit_enabled, debit_enabled, freeze_enabled, withdraw_enabled, exchange_enabled, negative_allowed, sort_order, remark, create_time)
VALUES
(1900000000000000001, '000000', 'GC', 'Gold Coin', 6, '0', '0', '0', '0', '1', '1', '1', 1, 'Default play currency. Withdraw and exchange disabled.', NOW()),
(1900000000000000002, '000000', 'SC', 'Sweep Coin', 6, '0', '0', '0', '0', '1', '0', '1', 2, 'Default sweep currency. Exchange enabled.', NOW()),
(1900000000000000003, '000000', 'RC', 'Real Cash', 6, '0', '0', '0', '0', '0', '1', '1', 3, 'Default cash currency. Withdraw enabled.', NOW())
ON DUPLICATE KEY UPDATE
  currency_name = VALUES(currency_name),
  scale_num = VALUES(scale_num),
  enabled = VALUES(enabled),
  credit_enabled = VALUES(credit_enabled),
  debit_enabled = VALUES(debit_enabled),
  freeze_enabled = VALUES(freeze_enabled),
  withdraw_enabled = VALUES(withdraw_enabled),
  exchange_enabled = VALUES(exchange_enabled),
  negative_allowed = VALUES(negative_allowed),
  sort_order = VALUES(sort_order),
  remark = VALUES(remark),
  update_time = NOW();

CREATE TABLE IF NOT EXISTS gl_promotion_reward (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  promotion_no VARCHAR(64) NOT NULL COMMENT 'Promotion number',
  promotion_name VARCHAR(128) NOT NULL COMMENT 'Promotion name',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  reward_amount DECIMAL(20,6) NOT NULL COMMENT 'Reward amount',
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
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  reward_amount DECIMAL(20,6) NOT NULL COMMENT 'Reward amount',
  status VARCHAR(32) NOT NULL COMMENT 'Claim status',
  wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Wallet transaction number',
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
  UNIQUE KEY uk_gl_promotion_claim_03 (tenant_id, promotion_id, member_id),
  KEY idx_gl_promotion_claim_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_promotion_claim_02 (tenant_id, promotion_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Promotion claim';

INSERT INTO gl_promotion_reward
(id, tenant_id, promotion_no, promotion_name, currency_code, reward_amount, status, start_time, end_time, remark, create_time)
VALUES
(1900000000000000301, '000000', 'PR-SEED-SC-001', 'Seed SC Reward', 'SC', 3.000000, 'ACTIVE', NULL, NULL, 'Default simulated promotion reward.', NOW())
ON DUPLICATE KEY UPDATE
  promotion_name = VALUES(promotion_name),
  currency_code = VALUES(currency_code),
  reward_amount = VALUES(reward_amount),
  status = VALUES(status),
  remark = VALUES(remark),
  update_time = NOW();

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1960, 'Promotion Center', 0, 10, 'promotion', NULL, '', 1, 0, 'M', '0', '0', '', 'skill', 103, 1, NOW(), NULL, NULL, 'Promotion center directory'),
(1961, 'Promotion Rewards', 1960, 1, 'reward', 'promotion/reward/index', '', 1, 0, 'C', '0', '0', 'promotion:reward:list', 'skill', 103, 1, NOW(), NULL, NULL, 'Promotion reward menu'),
(1971, 'Promotion Query', 1961, 1, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1972, 'Promotion Add', 1961, 2, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1973, 'Promotion Edit', 1961, 3, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
(1974, 'Promotion Remove', 1961, 4, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:remove', '#', 103, 1, NOW(), NULL, NULL, ''),
(1975, 'Promotion Claim', 1961, 5, '#', '', '', 1, 0, 'F', '0', '0', 'promotion:reward:claim', '#', 103, 1, NOW(), NULL, NULL, '')
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
(1940, 'Redemption Center', 0, 9, 'redemption', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 103, 1, NOW(), NULL, NULL, 'Redemption center directory'),
(1941, 'Redemption Orders', 1940, 1, 'order', 'redemption/order/index', '', 1, 0, 'C', '0', '0', 'redemption:order:list', 'money', 103, 1, NOW(), NULL, NULL, 'Redemption order menu'),
(1951, 'Redemption Query', 1941, 1, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1952, 'Redemption Add', 1941, 2, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1953, 'Redemption Approve', 1941, 3, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:approve', '#', 103, 1, NOW(), NULL, NULL, ''),
(1954, 'Redemption Reject', 1941, 4, '#', '', '', 1, 0, 'F', '0', '0', 'redemption:order:reject', '#', 103, 1, NOW(), NULL, NULL, '')
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

INSERT INTO gl_wallet_rule
(id, tenant_id, currency_code, source_type, rule_name, credit_enabled, debit_enabled, withdraw_enabled, exchange_enabled, release_mode, turnover_required, default_required_turnover, status, sort_order, remark, create_time)
VALUES
(1900000000000000101, '000000', 'GC', 'GAME_PROFIT', 'GC game profit', '0', '0', '1', '1', 'NEVER', '1', 0, '0', 1, 'GC is a play currency and is not withdrawable or exchangeable.', NOW()),
(1900000000000000102, '000000', 'SC', 'GAME_PROFIT', 'SC game profit', '0', '0', '1', '0', 'AFTER_TURNOVER', '0', 0, '0', 2, 'SC is exchangeable only for game profit source after configured conditions.', NOW()),
(1900000000000000103, '000000', 'SC', 'PROMOTION', 'SC promotion', '0', '0', '1', '1', 'MANUAL_REVIEW', '0', 0, '0', 3, 'Promotional SC requires review by default.', NOW()),
(1900000000000000104, '000000', 'RC', 'DEPOSIT', 'RC deposit', '0', '0', '0', '1', 'IMMEDIATE', '1', 0, '0', 4, 'RC deposit can be withdrawable immediately unless tenant changes the rule.', NOW()),
(1900000000000000105, '000000', 'RC', 'MANUAL_ADJUST', 'RC manual adjustment', '0', '0', '0', '1', 'MANUAL_REVIEW', '1', 0, '0', 5, 'Manual RC adjustment requires review by default.', NOW()),
(1900000000000000106, '000000', 'SC', 'GAME_REFUND', 'SC game refund', '0', '0', '1', '0', 'IMMEDIATE', '0', 0, '0', 6, 'SC refund returns original stake immediately.', NOW())
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

-- Wallet Center admin menu and permissions.
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1800, '钱包中心', 0, 6, 'wallet', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 103, 1, NOW(), NULL, NULL, '钱包中心目录'),
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

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1900, '支付中心', 0, 7, 'payment', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 103, 1, NOW(), NULL, NULL, '支付中心目录'),
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
(1920, '游戏交易', 0, 8, 'game', NULL, '', 1, 0, 'M', '0', '0', '', 'shopping', 103, 1, NOW(), NULL, NULL, '游戏交易目录'),
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
(1806, '钱包规则', 1800, 6, 'rule', 'wallet/rule/index', '', 1, 0, 'C', '0', '0', 'wallet:rule:list', 'slider', 103, 1, NOW(), NULL, NULL, '钱包来源规则菜单'),
(1817, '规则查询', 1806, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:rule:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1818, '规则新增', 1806, 2, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:rule:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1819, '规则编辑', 1806, 3, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:rule:edit', '#', 103, 1, NOW(), NULL, NULL, '')
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
