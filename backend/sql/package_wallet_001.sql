CREATE TABLE wallet_currency_config (
  id BIGINT PRIMARY KEY,
  currency_code VARCHAR(32) NOT NULL UNIQUE,
  currency_name VARCHAR(64) NOT NULL,
  currency_type VARCHAR(32) NOT NULL,
  decimal_scale INT NOT NULL DEFAULT 2,
  platform_enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE tenant_currency_config (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  currency_code VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  rechargeable TINYINT NOT NULL DEFAULT 0,
  withdrawable TINYINT NOT NULL DEFAULT 0,
  playable TINYINT NOT NULL DEFAULT 1,
  bonus TINYINT NOT NULL DEFAULT 0,
  min_withdraw_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
  max_withdraw_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
  config_json JSON NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_tenant_currency (tenant_id, currency_code)
);

CREATE TABLE member_wallet_account (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  currency_code VARCHAR(32) NOT NULL,
  available_balance DECIMAL(24,8) NOT NULL DEFAULT 0,
  frozen_balance DECIMAL(24,8) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'normal',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_member_currency (tenant_id, member_id, currency_code),
  KEY idx_member_wallet_account_member (tenant_id, member_id)
);

CREATE TABLE member_wallet_ledger (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  currency_code VARCHAR(32) NOT NULL,
  account_id BIGINT NOT NULL,
  direction VARCHAR(16) NOT NULL,
  amount DECIMAL(24,8) NOT NULL,
  before_available DECIMAL(24,8) NOT NULL,
  after_available DECIMAL(24,8) NOT NULL,
  before_frozen DECIMAL(24,8) NOT NULL,
  after_frozen DECIMAL(24,8) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_no VARCHAR(128) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  remark VARCHAR(512) NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_wallet_idempotency (tenant_id, idempotency_key),
  KEY idx_member_wallet_ledger_biz (tenant_id, biz_type, biz_no),
  KEY idx_member_wallet_ledger_member (tenant_id, member_id, currency_code, created_at)
);

CREATE TABLE wallet_transaction (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  transaction_no VARCHAR(128) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  member_id BIGINT NOT NULL,
  currency_code VARCHAR(32) NOT NULL,
  operation VARCHAR(32) NOT NULL,
  amount DECIMAL(24,8) NOT NULL,
  status VARCHAR(32) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_no VARCHAR(128) NOT NULL,
  origin_transaction_no VARCHAR(128) NULL,
  request_hash VARCHAR(128) NULL,
  fail_code VARCHAR(64) NULL,
  fail_reason VARCHAR(512) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_wallet_transaction_no (tenant_id, transaction_no),
  UNIQUE KEY uk_wallet_transaction_idempotency (tenant_id, idempotency_key),
  KEY idx_wallet_transaction_member (tenant_id, member_id, currency_code, created_at),
  KEY idx_wallet_transaction_biz (tenant_id, biz_type, biz_no)
);

CREATE TABLE wallet_freeze_record (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  currency_code VARCHAR(32) NOT NULL,
  freeze_no VARCHAR(128) NOT NULL,
  amount DECIMAL(24,8) NOT NULL,
  status VARCHAR(32) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  source_no VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_wallet_freeze_record_no (tenant_id, freeze_no),
  KEY idx_wallet_freeze_record_member (tenant_id, member_id, currency_code, created_at),
  KEY idx_wallet_freeze_record_source (tenant_id, source_type, source_no)
);

CREATE TABLE wallet_manual_review (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  review_no VARCHAR(128) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  source_no VARCHAR(128) NOT NULL,
  reason VARCHAR(512) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_wallet_manual_review_no (tenant_id, review_no),
  KEY idx_wallet_manual_review_source (tenant_id, source_type, source_no),
  KEY idx_wallet_manual_review_status (tenant_id, status, created_at)
);
