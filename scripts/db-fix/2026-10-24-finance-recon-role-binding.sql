-- 2026-10-24 财务对账 P0 权限按 §7 矩阵完整绑定（含新建 ROLE_TELLER 出纳）
-- 依据：docs/design/finance-reconciliation-design.md §7 权限点与角色矩阵；Owner 2026-10-24 裁决。
-- 事实（执行前只读核查）：
--   · roles 表 23 行，**无 ROLE_TELLER**；ROLE_CASHIER 的 role_name = '收银员'（不是出纳）
--     → 按裁决 (a)「不存在出纳角色且 CASHIER 明确是收银员」→ **新建 ROLE_TELLER（出纳）**，不复用 CASHIER。
--   · 权限码已存在：finance:bank:view/import/recon + 7 个 finance:deposit-slip:*
--   · role_permissions 的 (role_id, permission_id) 均存 *_str 字符串（varchar）。
-- 幂等：全部 INSERT 带 NOT EXISTS 守卫，重复执行 0 行。
BEGIN;

-- 0) 前置校验：权限码必须齐（缺一个就报错，不静默跳过）
DO $$
DECLARE missing text;
BEGIN
  SELECT string_agg(c, ',') INTO missing FROM (VALUES
    ('finance:bank:view'),('finance:bank:import'),('finance:bank:recon'),
    ('finance:deposit-slip:view'),('finance:deposit-slip:create'),('finance:deposit-slip:submit'),
    ('finance:deposit-slip:receive'),('finance:deposit-slip:deposit'),('finance:deposit-slip:verify'),
    ('finance:deposit-slip:close'),('finance:deposit-slip:reverse')
  ) v(c) WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_code = v.c AND p.deleted = 0);
  IF missing IS NOT NULL THEN
    RAISE EXCEPTION '缺少权限码，禁止继续绑定: %', missing;
  END IF;
END $$;

-- 1) 角色存在性校验（除 ROLE_TELLER 待建外，其余必须已在库）
DO $$
DECLARE missing text;
BEGIN
  SELECT string_agg(r, ',') INTO missing FROM (VALUES
    ('ROLE_FINANCE_DIRECTOR'),('ROLE_FINANCE_MANAGER'),('ROLE_CASHIER'),('ROLE_STORE_MANAGER'),('ROLE_AUDITOR')
  ) v(r) WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_id_str = v.r AND deleted = 0);
  IF missing IS NOT NULL THEN
    RAISE EXCEPTION '缺少角色，禁止继续绑定: %', missing;
  END IF;
END $$;

-- 2) 新建 ROLE_TELLER（出纳）——已存在则跳过
-- role_id 是 GENERATED ALWAYS 标识列，不能显式给值（否则 psql 报 cannot insert a non-DEFAULT value）
INSERT INTO roles (role_id_str, role_name, role_code, description, status, data_scope, org_level, create_time, update_time, deleted, version)
SELECT 'ROLE_TELLER', '出纳', 'teller',
       '资金对账：现金收妥、存行（P0 权限矩阵裁决新建）', 'active', 'store', 'STORE_STAFF', now(), now(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_id_str = 'ROLE_TELLER');

-- 3) 权限绑定矩阵（role_id_str, permission_code）
CREATE TEMP TABLE _bind(role_id_str varchar, permission_code varchar) ON COMMIT DROP;
INSERT INTO _bind(role_id_str, permission_code) VALUES
  -- deposit-slip:view：出纳 + 财务总监 + 财务经理 + 店长 + 稽查（只读）
  ('ROLE_TELLER','finance:deposit-slip:view'),
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:view'),
  ('ROLE_FINANCE_MANAGER','finance:deposit-slip:view'),
  ('ROLE_STORE_MANAGER','finance:deposit-slip:view'),
  ('ROLE_AUDITOR','finance:deposit-slip:view'),
  -- create：出纳 + 财务总监
  ('ROLE_TELLER','finance:deposit-slip:create'),
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:create'),
  -- submit：收银员（店长不授，裁决 b）
  ('ROLE_CASHIER','finance:deposit-slip:submit'),
  -- receive / deposit：出纳 + 财务总监
  ('ROLE_TELLER','finance:deposit-slip:receive'),
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:receive'),
  ('ROLE_TELLER','finance:deposit-slip:deposit'),
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:deposit'),
  -- verify / close / reverse：仅财务总监
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:verify'),
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:close'),
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:reverse'),
  -- finance:bank:view：出纳 + 财务经理 + 店长 + 稽查（财务总监已授）
  ('ROLE_TELLER','finance:bank:view'),
  ('ROLE_FINANCE_MANAGER','finance:bank:view'),
  ('ROLE_STORE_MANAGER','finance:bank:view'),
  ('ROLE_AUDITOR','finance:bank:view'),
  -- finance:bank:import / recon：出纳 + 财务经理（财务总监已授）
  ('ROLE_TELLER','finance:bank:import'),
  ('ROLE_FINANCE_MANAGER','finance:bank:import'),
  ('ROLE_TELLER','finance:bank:recon'),
  ('ROLE_FINANCE_MANAGER','finance:bank:recon');

-- 4) 只插入缺失的绑定，并留痕（permission_assignment_log）
WITH to_insert AS (
  SELECT b.role_id_str, b.permission_code, p.permission_id_str
  FROM _bind b
  JOIN permissions p ON p.permission_code = b.permission_code AND p.deleted = 0
  WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = b.role_id_str AND rp.permission_id = p.permission_id_str AND rp.deleted = 0
  )
), ins AS (
  INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
  SELECT role_id_str, permission_id_str, 0, now() FROM to_insert
  RETURNING role_id, permission_id
)
SELECT count(*) AS newly_bound FROM ins;

-- 5) 留痕：每个新增绑定写一行 permission_assignment_log
--    说明：该表原生语义是「员工角色变更日志」（employee_id 维度），此处复用为「角色-权限绑定日志」，
--    employee_id 置 '-'、operation_type='ROLE_PERMISSION_BIND'、new_roles=角色码、remark=权限码，便于审计追溯。
INSERT INTO permission_assignment_log
  (employee_id, operation_type, old_roles, new_roles, operator, operated_at, remark)
SELECT '-', 'ROLE_PERMISSION_BIND', NULL, b.role_id_str, 'night-handover', now(),
       'permission=' || b.permission_code || ' (' || p.permission_id_str || ')'
FROM _bind b
JOIN permissions p ON p.permission_code = b.permission_code AND p.deleted = 0
WHERE NOT EXISTS (
  SELECT 1 FROM permission_assignment_log l
  WHERE l.operation_type = 'ROLE_PERMISSION_BIND'
    AND l.new_roles = b.role_id_str
    AND l.remark LIKE '%permission=' || b.permission_code || ' (%'
);

COMMIT;

-- 6) 验证：角色 × 权限矩阵
SELECT r.role_id_str, r.role_name,
       count(*) FILTER (WHERE p.permission_code LIKE 'finance:deposit-slip:%') AS deposit_codes,
       count(*) FILTER (WHERE p.permission_code LIKE 'finance:bank:%')          AS bank_codes,
       string_agg(p.permission_code, ', ' ORDER BY p.permission_code)         AS codes
FROM role_permissions rp
JOIN roles r ON r.role_id_str = rp.role_id AND r.deleted = 0
JOIN permissions p ON p.permission_id_str = rp.permission_id AND p.deleted = 0
WHERE rp.deleted = 0 AND (p.permission_code LIKE 'finance:bank:%' OR p.permission_code LIKE 'finance:deposit-slip:%')
GROUP BY r.role_id_str, r.role_name
ORDER BY r.role_id_str;

-- 7) 验证：绑定日志行数（应等于历次新增绑定数之和）
SELECT count(*) AS bind_log_rows FROM permission_assignment_log WHERE operation_type = 'ROLE_PERMISSION_BIND';