insert into compliance_regions(country_code, state_code, registration_allowed, game_allowed, purchase_allowed, sc_grant_allowed, redemption_allowed, amoe_allowed, requires_legal_review, status, legal_approval_id, updated_at)
values
('US','CA', true, true, false, true, false, true, false, 'active', 'LEGAL-2026-0617-CA', timestamp '2026-06-17 08:00:00'),
('US','TX', true, true, false, true, false, true, false, 'active', 'LEGAL-2026-0617-TX', timestamp '2026-06-17 08:00:00'),
('US','NJ', true, true, false, true, false, true, false, 'active', 'LEGAL-2026-0617-NJ', timestamp '2026-06-17 08:00:00'),
('US','WA', false, false, false, false, false, false, true, 'blocked', null, timestamp '2026-06-17 08:00:00');

insert into compliance_documents(document_type, version, title, content_url, effective_at, status, legal_approval_id)
values
('terms', 'terms-v1', 'Terms of Use', '/legal/terms-v1', timestamp '2026-06-17 08:00:00', 'active', 'LEGAL-2026-0617-DOC'),
('sweepstakes_rules', 'rules-v1', 'Sweepstakes Rules', '/legal/rules-v1', timestamp '2026-06-17 08:00:00', 'active', 'LEGAL-2026-0617-DOC'),
('privacy', 'privacy-v1', 'Privacy Policy', '/legal/privacy-v1', timestamp '2026-06-17 08:00:00', 'active', 'LEGAL-2026-0617-DOC'),
('no_purchase', 'no-purchase-v1', 'No Purchase Necessary', '/legal/no-purchase-v1', timestamp '2026-06-17 08:00:00', 'active', 'LEGAL-2026-0617-DOC'),
('amoe', 'amoe-v1', 'AMOE / No Purchase Necessary', '/legal/amoe-v1', timestamp '2026-06-17 08:00:00', 'active', 'LEGAL-2026-0617-DOC');

insert into promotion_campaigns(campaign_code, name, campaign_type, status, eligible_regions_json, blocked_regions_json, reward_policy_json, sc_strategy, daily_budget_cap_json, user_period_cap, period_type, rules_version, legal_approval_id, risk_action, starts_at, ends_at, created_by, updated_by)
values
('register_bonus_v1', 'Welcome Bonus', 'register', 'active', '["CA","TX","NJ"]', '["WA"]', '[{"currency":"GC","amount":10000},{"currency":"SC","amount":"0.50"}]', 'default_small_sc', '{"GC":100000000,"SC":"500.00"}', 1, 'once', 'rules-v1', 'LEGAL-2026-0617-CA', 'gc_only', timestamp '2026-06-17 08:00:00', null, 1, 1),
('daily_login_v1', 'Daily Login', 'daily_login', 'active', '["CA","TX","NJ"]', '["WA"]', '[{"currency":"GC","amount":1000},{"currency":"SC","amount":"0.05"}]', 'default_small_sc', '{"GC":2000000,"SC":"200.00"}', 1, 'daily', 'rules-v1', 'LEGAL-2026-0617-CA', 'gc_only', timestamp '2026-06-17 08:00:00', null, 1, 1),
('view_rules_task_v1', 'View Sweeps Rules', 'daily_task', 'active', '["CA","TX","NJ"]', '["WA"]', '[{"currency":"GC","amount":500}]', 'gc_only', '{"GC":1000000}', 1, 'daily', 'rules-v1', null, 'pass', timestamp '2026-06-17 08:00:00', null, 1, 1);

insert into coupon_codes(code, batch_id, reward_policy_json, max_total_claims, max_user_claims, eligible_regions_json, sc_strategy, status, starts_at, ends_at)
values
('WELCOME500', 'welcome-demo', '[{"currency":"GC","amount":500}]', 10000, 1, '["CA","TX","NJ"]', 'gc_only', 'active', timestamp '2026-06-17 08:00:00', timestamp '2027-06-17 08:00:00');
