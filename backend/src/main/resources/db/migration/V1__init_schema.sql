create table users (
  id bigint auto_increment primary key,
  email varchar(255) not null,
  password_hash varchar(255) not null,
  birth_date date not null,
  country_code char(2) not null,
  state_code varchar(8) not null,
  status varchar(32) not null,
  risk_level varchar(32) not null,
  device_id varchar(128),
  register_ip varchar(64),
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint uk_users_email unique (email)
);
create index idx_users_region on users(country_code, state_code);
create index idx_users_risk on users(risk_level, status);

create table user_consent_logs (
  id bigint auto_increment primary key,
  user_id bigint not null,
  document_type varchar(64) not null,
  version varchar(64) not null,
  accepted_at timestamp not null,
  ip varchar(64),
  device_id varchar(128),
  constraint uk_user_consent unique (user_id, document_type, version)
);
create index idx_user_consent_user on user_consent_logs(user_id);

create table compliance_regions (
  id bigint auto_increment primary key,
  country_code char(2) not null,
  state_code varchar(8) not null,
  registration_allowed boolean not null,
  game_allowed boolean not null,
  purchase_allowed boolean not null,
  sc_grant_allowed boolean not null,
  redemption_allowed boolean not null,
  amoe_allowed boolean not null,
  requires_legal_review boolean not null,
  status varchar(32) not null,
  legal_approval_id varchar(128),
  updated_at timestamp not null,
  constraint uk_compliance_region unique (country_code, state_code)
);

create table compliance_documents (
  id bigint auto_increment primary key,
  document_type varchar(64) not null,
  version varchar(64) not null,
  title varchar(255) not null,
  content_url varchar(512) not null,
  effective_at timestamp not null,
  status varchar(32) not null,
  legal_approval_id varchar(128),
  constraint uk_compliance_document unique (document_type, version)
);

create table wallet_accounts (
  id bigint auto_increment primary key,
  user_id bigint not null,
  currency varchar(8) not null,
  balance decimal(20,4) not null,
  frozen_balance decimal(20,4) not null,
  status varchar(32) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint uk_wallet_user_currency unique (user_id, currency)
);

create table wallet_ledger (
  id bigint auto_increment primary key,
  user_id bigint not null,
  wallet_account_id bigint not null,
  currency varchar(8) not null,
  direction varchar(16) not null,
  amount decimal(20,4) not null,
  balance_after decimal(20,4) not null,
  frozen_after decimal(20,4) not null,
  business_type varchar(64) not null,
  business_id varchar(128) not null,
  idempotency_key varchar(255) not null,
  status varchar(32) not null,
  metadata_json json,
  created_at timestamp not null,
  constraint uk_ledger_idempotency unique (idempotency_key)
);
create index idx_ledger_user_time on wallet_ledger(user_id, created_at);
create index idx_ledger_business on wallet_ledger(business_type, business_id);

create table promotion_campaigns (
  id bigint auto_increment primary key,
  campaign_code varchar(64) not null,
  name varchar(255) not null,
  campaign_type varchar(64) not null,
  status varchar(32) not null,
  eligible_regions_json json not null,
  blocked_regions_json json,
  reward_policy_json json not null,
  sc_strategy varchar(32) not null,
  daily_budget_cap_json json not null,
  user_period_cap int not null,
  period_type varchar(32) not null,
  rules_version varchar(64) not null,
  legal_approval_id varchar(128),
  risk_action varchar(32) not null,
  starts_at timestamp not null,
  ends_at timestamp,
  created_by bigint not null,
  updated_by bigint not null,
  constraint uk_campaign_code unique (campaign_code)
);
create index idx_campaign_type_status on promotion_campaigns(campaign_type, status);

create table promotion_claims (
  id bigint auto_increment primary key,
  user_id bigint not null,
  campaign_id bigint not null,
  period_key varchar(64) not null,
  status varchar(32) not null,
  risk_action varchar(32) not null,
  idempotency_key varchar(255) not null,
  created_at timestamp not null,
  constraint uk_claim_user_campaign_period unique (user_id, campaign_id, period_key),
  constraint uk_claim_idempotency unique (idempotency_key)
);

create table promotion_reward_grants (
  id bigint auto_increment primary key,
  claim_id bigint not null,
  user_id bigint not null,
  currency varchar(8) not null,
  amount decimal(20,4) not null,
  ledger_id bigint,
  status varchar(32) not null,
  reject_reason varchar(255),
  created_at timestamp not null
);
create index idx_reward_user on promotion_reward_grants(user_id, created_at);

create table daily_task_progress (
  id bigint auto_increment primary key,
  user_id bigint not null,
  task_code varchar(64) not null,
  period_key varchar(64) not null,
  progress int not null,
  target int not null,
  status varchar(32) not null,
  claimed_at timestamp,
  constraint uk_daily_task_progress unique (user_id, task_code, period_key)
);

create table coupon_codes (
  id bigint auto_increment primary key,
  code varchar(64) not null,
  batch_id varchar(64) not null,
  reward_policy_json json not null,
  max_total_claims int not null,
  max_user_claims int not null,
  eligible_regions_json json not null,
  sc_strategy varchar(32) not null,
  status varchar(32) not null,
  starts_at timestamp not null,
  ends_at timestamp not null,
  constraint uk_coupon_code unique (code)
);

create table risk_events (
  id bigint auto_increment primary key,
  user_id bigint not null,
  event_type varchar(64) not null,
  risk_level varchar(32) not null,
  action varchar(32) not null,
  related_business_type varchar(64),
  related_business_id varchar(128),
  metadata_json json,
  created_at timestamp not null
);

create table audit_logs (
  id bigint auto_increment primary key,
  operator_id bigint not null,
  operator_role varchar(64) not null,
  action varchar(64) not null,
  target_type varchar(64) not null,
  target_id varchar(128) not null,
  before_json json,
  after_json json,
  reason varchar(512),
  ip varchar(64),
  created_at timestamp not null
);
create index idx_audit_operator_time on audit_logs(operator_id, created_at);
create index idx_audit_target on audit_logs(target_type, target_id);
