-- ============================================================
-- P1-USER-LOCATION-001 （S1：用户/员工归属列改名 + 关联 + 兼容视图）
-- 依据：docs/design/user-store-assignment-design-002.md §5 修订一 + §5 兼容视图
--       docs/design/user-store-assignment-design-001.md §1.4 / §4.1
-- 前置：V20260927_001（locations / location_id_map 已建并完成 stores_new/warehouses 映射）
--
-- 内容：
--   1. employees.user_id 新增 + 按 employee_code 回填（-001 §1.4 前置）
--   2. users.store_id     -> users.location_id（值经 location_id_map 由 stores_new.store_id 重映射）
--   3. employees.store_id -> employees.location_id（同上）
--   4. 兼容视图 v_users_store_compat（观察期暴露旧列形态；禁止新代码读取；与 Location 模型 M7 同窗 DROP）
--
-- 安全守卫：映射前后校验「store_id 非空但 location_id 仍空」的行数，非 0 即 RAISE EXCEPTION 中止，
--           防止归属静默丢失（本卡在无 DB 环境开发，故用守卫替代活体验证）。
-- 回滚：DROP VIEW v_users_store_compat → ADD COLUMN store_id BIGINT → 由 location_id_map 反查回填
--       → ADD CONSTRAINT fk_users_store_id / fk_employees_store_id REFERENCES stores_new(store_id)
--       → DROP COLUMN location_id。见实施记录 -001 §6.3。
-- ============================================================

-- ---------- 1) employees.user_id（员工档案 ↔ 用户账号强关联；可空） ----------
ALTER TABLE employees ADD COLUMN IF NOT EXISTS user_id BIGINT;
COMMENT ON COLUMN employees.user_id IS 'P1-USER-LOCATION-001：员工档案↔用户账号强关联（可空：admin/finqa 等无档案账号）';

UPDATE employees e
   SET user_id = u.user_id
  FROM users u
 WHERE e.user_id IS NULL
   AND e.deleted = 0
   AND u.deleted = 0
   AND e.employee_code IS NOT NULL
   AND u.employee_code IS NOT NULL
   AND u.employee_code = e.employee_code;

CREATE INDEX IF NOT EXISTS idx_employees_user_id ON employees(user_id);

-- ---------- 2) users.store_id -> users.location_id ----------
ALTER TABLE users ADD COLUMN IF NOT EXISTS location_id BIGINT;
COMMENT ON COLUMN users.location_id IS 'P1-USER-LOCATION-001：用户归属位置（经 location_id_map 映射；STORE/CENTRAL/DEPOT 型）';

-- 2a) 由 stores_new.store_id 经映射桥重映射（无 store_id 的行保持 NULL）
UPDATE users u
   SET location_id = m.location_id
  FROM location_id_map m
 WHERE u.location_id IS NULL
   AND m.src_table = 'stores_new'
   AND m.src_id = u.store_id;

-- 2b) 守卫：不允许出现「旧列非空、新列仍空」
DO $$
DECLARE
    v_orphan INT;
BEGIN
    SELECT COUNT(*) INTO v_orphan
      FROM users
     WHERE deleted = 0
       AND store_id IS NOT NULL
       AND location_id IS NULL;
    IF v_orphan > 0 THEN
        RAISE EXCEPTION 'P1-USER-LOCATION-001 S1: users 有 % 行 store_id 非空但无法经 location_id_map 映射为 location_id（中止，防静默丢归属）', v_orphan;
    END IF;
END $$;

-- 2c) 旧列退役（旧 FK/索引随列一并移除），新列建 FK/索引
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_store_id;
DROP INDEX IF EXISTS idx_users_store_id;
ALTER TABLE users DROP COLUMN IF EXISTS store_id;
ALTER TABLE users ADD CONSTRAINT fk_users_location_id FOREIGN KEY (location_id) REFERENCES locations(location_id);
CREATE INDEX IF NOT EXISTS idx_users_location_id ON users(location_id);

-- ---------- 3) employees.store_id -> employees.location_id ----------
ALTER TABLE employees ADD COLUMN IF NOT EXISTS location_id BIGINT;
COMMENT ON COLUMN employees.location_id IS 'P1-USER-LOCATION-001：员工归属位置（与 users.location_id 同口径）';

UPDATE employees e
   SET location_id = m.location_id
  FROM location_id_map m
 WHERE e.location_id IS NULL
   AND m.src_table = 'stores_new'
   AND m.src_id = e.store_id;

DO $$
DECLARE
    v_orphan INT;
BEGIN
    SELECT COUNT(*) INTO v_orphan
      FROM employees
     WHERE deleted = 0
       AND store_id IS NOT NULL
       AND location_id IS NULL;
    IF v_orphan > 0 THEN
        RAISE EXCEPTION 'P1-USER-LOCATION-001 S1: employees 有 % 行 store_id 非空但无法经 location_id_map 映射为 location_id（中止，防静默丢归属）', v_orphan;
    END IF;
END $$;

ALTER TABLE employees DROP CONSTRAINT IF EXISTS fk_employees_store_id;
DROP INDEX IF EXISTS idx_employees_store_id;
ALTER TABLE employees DROP COLUMN IF EXISTS store_id;
ALTER TABLE employees ADD CONSTRAINT fk_employees_location_id FOREIGN KEY (location_id) REFERENCES locations(location_id);
CREATE INDEX IF NOT EXISTS idx_employees_location_id ON employees(location_id);

-- ---------- 4) 兼容视图（观察期；禁止新代码读取） ----------
CREATE OR REPLACE VIEW v_users_store_compat AS
SELECT u.user_id,
       u.username,
       u.name,
       u.department_id,
       u.employee_code,
       u.status,
       u.deleted,
       m.src_id AS store_id
  FROM users u
  LEFT JOIN location_id_map m
    ON m.src_table = 'stores_new'
   AND m.location_id = u.location_id;

COMMENT ON VIEW v_users_store_compat IS 'P1-USER-LOCATION-001 观察期兼容视图：暴露旧 users.store_id 形态（location_id 经 location_id_map 反查 stores_new.store_id）；禁止新代码读取，与 Location 模型 M7 同窗 DROP';
