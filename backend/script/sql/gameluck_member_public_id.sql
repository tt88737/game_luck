-- Normalize public member IDs stored in gl_member_profile.member_no.
-- Internal primary keys and business-table member_id foreign keys are not changed.

SELECT tenant_id, member_no, COUNT(*) AS duplicate_count
FROM gl_member_profile
WHERE del_flag = '0'
GROUP BY tenant_id, member_no
HAVING COUNT(*) > 1;

UPDATE gl_member_profile
SET member_no = CONCAT('GL',
    CASE
      WHEN CHAR_LENGTH(CAST(id AS CHAR)) < 6 THEN LPAD(CAST(id AS CHAR), 6, '0')
      ELSE CAST(id AS CHAR)
    END),
    update_time = NOW()
WHERE del_flag = '0'
  AND (
    member_no IS NULL
    OR member_no = ''
    OR member_no REGEXP '^M[0-9]+$'
    OR member_no REGEXP '^MB-SEED-[0-9]+$'
    OR member_no REGEXP '^MB[0-9]{10,}$'
    OR member_no = CONCAT('GL', LEFT(CAST(id AS CHAR), 6))
  );

SELECT tenant_id, member_no, COUNT(*) AS duplicate_count
FROM gl_member_profile
WHERE del_flag = '0'
GROUP BY tenant_id, member_no
HAVING COUNT(*) > 1;
