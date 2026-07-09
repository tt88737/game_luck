DELETE FROM gl_wallet_transaction WHERE tenant_id = '000000' AND member_id = 1001 AND source_type = 'demo_seed';
DELETE FROM gl_wallet_account WHERE tenant_id = '000000' AND member_id = 1001;
DELETE FROM gl_member_profile WHERE tenant_id = '000000' AND id = 1001;

INSERT INTO gl_member_profile (
  id, tenant_id, member_no, username, nickname, status, risk_level, register_channel,
  version, del_flag, create_time, update_time
) VALUES (
  1001, '000000', 'M1001', 'demo_player', 'Demo Player', 'ACTIVE', 'NORMAL', 'h5',
  0, '0', NOW(), NOW()
);

INSERT INTO gl_wallet_account (
  id, tenant_id, member_id, currency_code, available_balance, frozen_balance, status,
  version, del_flag, create_time, update_time
) VALUES
  (11001, '000000', 1001, 'GC', 1000.000000, 0.000000, '0', 0, '0', NOW(), NOW()),
  (11002, '000000', 1001, 'SC', 25.000000, 0.000000, '0', 0, '0', NOW(), NOW());

INSERT INTO gl_wallet_transaction (
  id, tenant_id, transaction_no, idempotency_key, member_id, currency_code, operation,
  source_type, business_no, amount, balance_before, balance_after, frozen_before,
  frozen_after, request_hash, status, remark, create_time, update_time
) VALUES
  (12001, '000000', 'WT-DEMO-GC-INIT', 'demo:1001:gc:init', 1001, 'GC', 'credit',
   'demo_seed', 'DEMO-GC-INIT', 1000.000000, 0.000000, 1000.000000, 0.000000,
   0.000000, 'demo-seed-gc', 'SUCCESS', 'Demo GC seed', NOW(), NOW()),
  (12002, '000000', 'WT-DEMO-SC-INIT', 'demo:1001:sc:init', 1001, 'SC', 'credit',
   'demo_seed', 'DEMO-SC-INIT', 25.000000, 0.000000, 25.000000, 0.000000,
   0.000000, 'demo-seed-sc', 'SUCCESS', 'Demo SC seed', NOW(), NOW());
