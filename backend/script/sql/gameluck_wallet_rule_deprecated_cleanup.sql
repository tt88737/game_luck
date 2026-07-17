-- Deprecated wallet rule cleanup.
-- Wallet credit policy is now provided by each business request, not by gl_wallet_rule.
-- Keep the table for compatibility, but remove misleading seed data and admin menu entries.

DELETE FROM sys_role_menu
WHERE menu_id IN (1806, 1817, 1818, 1819, 1820);

DELETE FROM sys_menu
WHERE menu_id IN (1806, 1817, 1818, 1819, 1820)
   OR perms LIKE 'wallet:rule:%'
   OR component = 'wallet/rule/index';

DELETE FROM gl_wallet_rule;
