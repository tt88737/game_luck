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
