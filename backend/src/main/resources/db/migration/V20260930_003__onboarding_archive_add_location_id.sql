-- ============================================================
-- P1-USER-LOCATION-001 （S6：入职链路 location_id）
-- onboarding_archive 新增 location_id —— 注册回填 users.location_id 的依据
-- 依据：docs/design/user-store-assignment-design-001.md §1.1（archive 加 storeId）
--       + user-store-assignment-design-002.md §5 修订一（store_id → location_id 全链改名）
--
-- 语义：入职记录 OnboardingRecord.storeId（stores_new.store_id 空间）经 location_id_map
--       换算为 location_id 后写入本列；注册时 AuthServiceImpl 读本列回填 users.location_id。
-- 存量：按 employee_code 对齐已注册用户的归属做**尽力回填**；无匹配或未分配者保持 NULL
--       （不猜门店，符合 Owner"人工分配"裁定）。
-- 回滚：ALTER TABLE onboarding_archive DROP COLUMN IF EXISTS location_id;
-- ============================================================

ALTER TABLE onboarding_archive ADD COLUMN IF NOT EXISTS location_id BIGINT;
COMMENT ON COLUMN onboarding_archive.location_id IS 'P1-USER-LOCATION-001：归属位置ID（由入职记录 storeId 经 location_id_map 换算）；注册回填 users.location_id 的依据';

UPDATE onboarding_archive a
   SET location_id = u.location_id
  FROM users u
 WHERE a.location_id IS NULL
   AND a.deleted = 0
   AND u.deleted = 0
   AND a.employee_code IS NOT NULL
   AND u.employee_code IS NOT NULL
   AND u.employee_code = a.employee_code;
