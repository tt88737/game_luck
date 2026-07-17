-- H5 client registration schema and wallet rules.

SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'password_hash') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN password_hash VARCHAR(128) DEFAULT NULL COMMENT ''Client password hash'' AFTER nickname',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'country_code') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN country_code VARCHAR(16) DEFAULT NULL COMMENT ''Registration country code'' AFTER register_channel',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'state_code') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN state_code VARCHAR(32) DEFAULT NULL COMMENT ''Registration state code'' AFTER country_code',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'age_confirmed') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN age_confirmed TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Age confirmation'' AFTER state_code',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'terms_accepted') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN terms_accepted TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Terms accepted'' AFTER age_confirmed',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'privacy_accepted') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN privacy_accepted TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Privacy policy accepted'' AFTER terms_accepted',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'sweepstakes_rules_accepted') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN sweepstakes_rules_accepted TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Sweepstakes rules accepted'' AFTER privacy_accepted',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
