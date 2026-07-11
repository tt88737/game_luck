-- Normalize GameLuck business menu names for existing databases.
-- Run this once after importing older seeds that used English menu names.

UPDATE sys_menu
SET menu_name = CASE menu_id
  WHEN 1940 THEN '兑换中心'
  WHEN 1941 THEN '兑换订单'
  WHEN 1951 THEN '兑换查询'
  WHEN 1952 THEN '兑换新增'
  WHEN 1953 THEN '兑换通过'
  WHEN 1954 THEN '兑换拒绝'
  WHEN 1960 THEN '促销中心'
  WHEN 1961 THEN '促销奖励'
  WHEN 1971 THEN '促销查询'
  WHEN 1972 THEN '促销新增'
  WHEN 1973 THEN '促销编辑'
  WHEN 1974 THEN '促销删除'
  WHEN 1975 THEN '促销领取'
  WHEN 1980 THEN '会员中心'
  WHEN 1981 THEN '会员资料'
  WHEN 1991 THEN '会员查询'
  WHEN 1992 THEN '会员新增'
  WHEN 1993 THEN '会员编辑'
  WHEN 1994 THEN '会员删除'
  WHEN 2000 THEN '报表中心'
  WHEN 2001 THEN '数据总览'
  WHEN 2002 THEN '趋势看板'
  WHEN 2011 THEN '报表总览查询'
  WHEN 2021 THEN '趋势看板查询'
  ELSE menu_name
END,
update_time = NOW()
WHERE menu_id IN (
  1940, 1941, 1951, 1952, 1953, 1954,
  1960, 1961, 1971, 1972, 1973, 1974, 1975,
  1980, 1981, 1991, 1992, 1993, 1994,
  2000, 2001, 2002, 2011, 2021
);
