-- ============================================================
-- scripts/db-fix/2026-10-24-finance-recon-permissions.sql
-- P0-B：财务对账/导入模块 新权限码种子（§7 共 10 个新码）
--
-- 依据：docs/design/finance-reconciliation-design.md §7「权限点与角色矩阵」
--   · 沿用既有命名空间 finance:，不新建命名空间（已裁决 11）
--   · 只种权限码，**默认不绑定角色**（角色矩阵涉及「出纳 / 审计」两个活库尚不明确的角色，
--     绑定交给 Owner 裁决；如需绑定，用 -v bind_roles=1 走文件末尾的可选块）
--
-- 幂等：全部 INSERT 带 NOT EXISTS 守卫（按 permission_id_str 判重），可重复执行。
-- permission_id 是 IDENTITY ALWAYS → 不显式给值。
--
-- 回滚：
--   DELETE FROM role_permissions WHERE permission_id IN (SELECT permission_id_str FROM permissions WHERE permission_code IN (...));
--   DELETE FROM permissions WHERE permission_code IN ('finance:bank:import', ...);
--
-- 用法：
--   psql ... -f 本文件               → 只种码
--   psql ... -v bind_roles=1 -f 本文件 → 种码 + 按 §7 矩阵绑定（含待裁决说明）
-- ============================================================

\set ON_ERROR_STOP on

BEGIN;

-- ------------------------------------------------------------
-- 1) 10 个新权限码
-- ------------------------------------------------------------
INSERT INTO permissions (permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
SELECT v.id_str, v.name, v.code, 1, 'finance', 1, NOW(), NOW(), 0
  FROM (VALUES
    ('PERM_FINANCE_BANK_IMPORT',              '银行流水导入',        'finance:bank:import'),
    ('PERM_FINANCE_BANK_RECON',               '自动勾稽与差异处理',  'finance:bank:recon'),
    ('PERM_FINANCE_DEPOSIT_SLIP_VIEW',        '查看现金交款单',      'finance:deposit-slip:view'),
    ('PERM_FINANCE_DEPOSIT_SLIP_CREATE',      '生成现金交款单',      'finance:deposit-slip:create'),
    ('PERM_FINANCE_DEPOSIT_SLIP_SUBMIT',      '提交现金交款单',      'finance:deposit-slip:submit'),
    ('PERM_FINANCE_DEPOSIT_SLIP_RECEIVE',     '出纳收妥现金',        'finance:deposit-slip:receive'),
    ('PERM_FINANCE_DEPOSIT_SLIP_DEPOSIT',     '存行登记',            'finance:deposit-slip:deposit'),
    ('PERM_FINANCE_DEPOSIT_SLIP_VERIFY',      '现金交款单复核',      'finance:deposit-slip:verify'),
    ('PERM_FINANCE_DEPOSIT_SLIP_CLOSE',       '现金交款单归档',      'finance:deposit-slip:close'),
    ('PERM_FINANCE_DEPOSIT_SLIP_REVERSE',     '现金交款单冲正',      'finance:deposit-slip:reverse')
  ) AS v(id_str, name, code)
 WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_id_str = v.id_str);

-- ------------------------------------------------------------
-- 2) 结果核对
-- ------------------------------------------------------------
SELECT permission_id, permission_id_str, permission_code, permission_name, status, deleted
  FROM permissions
 WHERE permission_code IN (
        'finance:bank:import','finance:bank:recon',
        'finance:deposit-slip:view','finance:deposit-slip:create','finance:deposit-slip:submit',
        'finance:deposit-slip:receive','finance:deposit-slip:deposit','finance:deposit-slip:verify',
        'finance:deposit-slip:close','finance:deposit-slip:reverse')
 ORDER BY permission_code;

SELECT count(*) AS seeded_codes
  FROM permissions
 WHERE permission_code IN (
        'finance:bank:import','finance:bank:recon',
        'finance:deposit-slip:view','finance:deposit-slip:create','finance:deposit-slip:submit',
        'finance:deposit-slip:receive','finance:deposit-slip:deposit','finance:deposit-slip:verify',
        'finance:deposit-slip:close','finance:deposit-slip:reverse');

-- ------------------------------------------------------------
-- 2.1) **P0 必需三码**：授予 ROLE_FINANCE_DIRECTOR（qa_admin 的角色）
--     为什么必须先绑：C 卡之后权限是「user_roles → roles → role_permissions → permissions」**实时查库**，
--     自签 token 里的 claims 不再生效；不绑角色则 P0 验收接口一律 403。
--     只绑 view / import / recon 三码；**7 个 finance:deposit-slip:* 仍不绑**（等 Owner 定出纳映射）。
--     注：role_permissions.permission_id 存的是 permissions.permission_id_str（varchar），不是数字主键。
-- ------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT v.role_id_str, p.permission_id_str, 0, NOW()
  FROM (VALUES
        ('ROLE_FINANCE_DIRECTOR', 'PERM_FINANCE_BANK_VIEW'),
        ('ROLE_FINANCE_DIRECTOR', 'PERM_FINANCE_BANK_IMPORT'),
        ('ROLE_FINANCE_DIRECTOR', 'PERM_FINANCE_BANK_RECON')
       ) AS v(role_id_str, permission_id_str)
  JOIN roles r       ON r.role_id_str = v.role_id_str AND r.deleted = 0
  JOIN permissions p ON p.permission_id_str = v.permission_id_str AND p.deleted = 0
 WHERE NOT EXISTS (
        SELECT 1 FROM role_permissions rp
         WHERE rp.role_id = v.role_id_str
           AND rp.permission_id = v.permission_id_str
           AND COALESCE(rp.deleted, 0) = 0);

-- 验收断言：应为 3
SELECT count(*) AS p0_required_codes_granted
  FROM role_permissions rp
  JOIN roles r        ON r.role_id_str = rp.role_id
  JOIN permissions p  ON p.permission_id_str = rp.permission_id
 WHERE r.role_id_str = 'ROLE_FINANCE_DIRECTOR'
   AND p.permission_code IN ('finance:bank:view','finance:bank:import','finance:bank:recon')
   AND COALESCE(rp.deleted, 0) = 0;

-- 反证：7 个 deposit-slip 码不得有任何绑定（应为 0）
SELECT count(*) AS deposit_slip_bindings_should_be_0
  FROM role_permissions rp
  JOIN permissions p ON p.permission_id_str = rp.permission_id
 WHERE p.permission_code LIKE 'finance:deposit-slip:%'
   AND COALESCE(rp.deleted, 0) = 0;

COMMIT;

-- ============================================================
-- 3) 可选：按 §7 角色矩阵绑定（默认关闭，需 Owner 裁决后开启）
--    矩阵：出纳✅ / 财务主管✅ / 店长(本店) / 审计(只读)
--    活库角色映射（**待裁决**）：
--      出纳     → ROLE_CASHIER（活库无「出纳」专属角色，CASHIER 是否等同需 Owner 确认）
--      财务主管 → ROLE_FINANCE_DIRECTOR + ROLE_FINANCE_MANAGER
--      店长     → ROLE_STORE_MANAGER
--      审计     → ROLE_AUDITOR（只读：仅 view 类）
-- ============================================================
\if :{?bind_roles}
BEGIN;

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id_str, p.permission_id_str, 0, NOW()
  FROM (VALUES
    -- 出纳(?ROLE_CASHIER)：import / recon / deposit-slip view+create+receive+deposit / bank:view
    ('ROLE_CASHIER','PERM_FINANCE_BANK_IMPORT'),
    ('ROLE_CASHIER','PERM_FINANCE_BANK_RECON'),
    ('ROLE_CASHIER','PERM_FINANCE_DEPOSIT_SLIP_VIEW'),
    ('ROLE_CASHIER','PERM_FINANCE_DEPOSIT_SLIP_CREATE'),
    ('ROLE_CASHIER','PERM_FINANCE_DEPOSIT_SLIP_RECEIVE'),
    ('ROLE_CASHIER','PERM_FINANCE_DEPOSIT_SLIP_DEPOSIT'),
    -- 财务主管：全部 10 个
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_BANK_IMPORT'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_BANK_RECON'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_DEPOSIT_SLIP_VIEW'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_DEPOSIT_SLIP_CREATE'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_DEPOSIT_SLIP_SUBMIT'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_DEPOSIT_SLIP_RECEIVE'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_DEPOSIT_SLIP_DEPOSIT'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_DEPOSIT_SLIP_VERIFY'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_DEPOSIT_SLIP_CLOSE'),
    ('ROLE_FINANCE_DIRECTOR','PERM_FINANCE_DEPOSIT_SLIP_REVERSE'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_BANK_IMPORT'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_BANK_RECON'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_VIEW'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_CREATE'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_SUBMIT'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_RECEIVE'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_DEPOSIT'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_VERIFY'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_CLOSE'),
    ('ROLE_FINANCE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_REVERSE'),
    -- 店长：本店 view + submit（收银员动作由店长代行）
    ('ROLE_STORE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_VIEW'),
    ('ROLE_STORE_MANAGER','PERM_FINANCE_DEPOSIT_SLIP_SUBMIT'),
    -- 审计：只读
    ('ROLE_AUDITOR','PERM_FINANCE_DEPOSIT_SLIP_VIEW')
  ) AS r(role_id_str, permission_id_str)
  JOIN roles r ON r.role_id_str = v.role_id_str AND r.deleted = 0
  JOIN permissions p ON p.permission_id_str = v.permission_id_str AND p.deleted = 0
 WHERE NOT EXISTS (
       SELECT 1 FROM role_permissions rp
        WHERE rp.role_id = r.role_id_str AND rp.permission_id = p.permission_id_str AND COALESCE(rp.deleted,0)=0);

SELECT count(*) AS bound_rows FROM role_permissions
 WHERE permission_id IN ('PERM_FINANCE_BANK_IMPORT','PERM_FINANCE_BANK_RECON',
       'PERM_FINANCE_DEPOSIT_SLIP_VIEW','PERM_FINANCE_DEPOSIT_SLIP_CREATE','PERM_FINANCE_DEPOSIT_SLIP_SUBMIT',
       'PERM_FINANCE_DEPOSIT_SLIP_RECEIVE','PERM_FINANCE_DEPOSIT_SLIP_DEPOSIT','PERM_FINANCE_DEPOSIT_SLIP_VERIFY',
       'PERM_FINANCE_DEPOSIT_SLIP_CLOSE','PERM_FINANCE_DEPOSIT_SLIP_REVERSE');

COMMIT;
\endif
