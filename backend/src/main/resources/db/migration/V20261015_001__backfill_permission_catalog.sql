-- ============================================================
-- N31（board §24.15a）：权限目录补全 —— 286 个缺失权限码入目录 + 按模块授予角色
--
-- 背景（N32 扫描器实测，2026-10-15）：
--   注解引用 372 个不同权限码，permissions 目录仅 132 个 -> 286 个码「注解有、目录无」。
--   目录缺失 -> role_permissions 无法授予 -> 相关端点对全部角色 403；
--   其中 67 处（34 码）连 admin 都进不去（无 '*' 兜底且不在 AdminPermissions.ALL）。
--   证据：docs/audit/auth-annotation-literal-audit-001.md
--
-- Owner 裁决 2：绝不改 964 处代码注解，改用本迁移补目录 + 授权。
--
-- 三步：
--   1) 286 码 INSERT 进 permissions（幂等）
--   2) 目录内全部权限码授予 ROLE_ADMIN / ROLE_OWNER（含历史悬空 role_id '1'）
--   3) 按模块前缀授予 7 个业务角色（矩阵见下）
--
-- 授权矩阵（role_id_str <- 模块前缀）：
--   ROLE_FINANCE_DIRECTOR    <- finance, recharge, recharge-plan, recharge-settings, refund
--   ROLE_STORE_MANAGER       <- refund, member, coupon, order, product, trace, traceability, label, store, schedule, alert, warehouse, inventory, other-inbound, loss-outbound, signature
--   ROLE_WAREHOUSE_MANAGER   <- warehouse, inventory, other-inbound, loss-outbound, signature
--   ROLE_PURCHASE_MANAGER    <- purchase
--   ROLE_HR_DIRECTOR         <- hr
--   ROLE_OPS_DIRECTOR        <- operations
--   ROLE_AUDITOR             <- audit
--   未列出的模块（system / device / hardware / notification / backup / scheduler /
--                    esign / seal / sys / permission / admin / file）仅 ROLE_ADMIN
--
-- 幂等：所有 INSERT 带 NOT EXISTS 守卫，可重复执行。
-- 回退：
--   DELETE FROM role_permissions WHERE permission_id IN (SELECT permission_id_str FROM permissions WHERE permission_code IN (<本文件的 286 码>));
--   DELETE FROM permissions WHERE permission_code IN (<本文件的 286 码>);
-- 注：permissions.permission_id / role_permissions.id 均为 GENERATED ALWAYS AS IDENTITY -> 不显式赋值。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 286 个缺失权限码入目录
--    permission_id_str = 'PERM_' + upper(code 中 ':'/'-' 换 '_')，最长 40 字符（列宽 50）
--    permission_name 暂用 code 本身（多为 API 级码，非菜单项，后续可人工润色）
-- ------------------------------------------------------------
INSERT INTO permissions (permission_id_str, permission_name, permission_code,
                        permission_type, module, status, create_time, update_time, deleted)
SELECT 'PERM_' || UPPER(REPLACE(REPLACE(v.code, ':', '_'), '-', '_')),
       v.code, v.code, 1, split_part(v.code, ':', 1), 1, NOW(), NOW(), 0
FROM (VALUES
    ('admin:user:export'),
    ('alert:delete'),
    ('alert:edit'),
    ('alert:query'),
    ('audit:log:archive'),
    ('audit:log:export'),
    ('audit:log:query'),
    ('backup:config'),
    ('backup:create'),
    ('backup:delete'),
    ('backup:download'),
    ('backup:query'),
    ('backup:restore'),
    ('coupon:manage'),
    ('coupon:view'),
    ('device:alert:manage'),
    ('device:alert:view'),
    ('device:communication:manage'),
    ('device:communication:view'),
    ('device:locker:manage'),
    ('device:locker:view'),
    ('device:manage:manage'),
    ('device:manage:view'),
    ('device:management:manage'),
    ('device:management:view'),
    ('device:print-task:manage'),
    ('device:print-task:view'),
    ('device:print-template:manage'),
    ('device:print-template:view'),
    ('device:scan:manage'),
    ('device:scan:view'),
    ('device:sdk-printer:manage'),
    ('device:sdk-printer:view'),
    ('device:simulator:manage'),
    ('device:simulator:view'),
    ('device:status-history:view'),
    ('device:tts:manage'),
    ('device:tts:view'),
    ('device:weighing:manage'),
    ('device:weighing:view'),
    ('esign:delete'),
    ('esign:manage'),
    ('esign:view'),
    ('file:query'),
    ('finance:approval-flow-config:manage'),
    ('finance:approval-flow-config:view'),
    ('finance:approval:approve'),
    ('finance:approval:view'),
    ('finance:audit-log:view'),
    ('finance:auto-voucher:create'),
    ('finance:auto-voucher:delete'),
    ('finance:auto-voucher:export'),
    ('finance:auto-voucher:update'),
    ('finance:auto-voucher:view'),
    ('finance:bank:create'),
    ('finance:bank:delete'),
    ('finance:bank:manage'),
    ('finance:bank:query'),
    ('finance:bank:reconcile'),
    ('finance:bank:update'),
    ('finance:bank:view'),
    ('finance:budget:create'),
    ('finance:budget:query'),
    ('finance:budget:update'),
    ('finance:invoice:verify'),
    ('finance:period:approve'),
    ('finance:period:create'),
    ('finance:period:query'),
    ('finance:period:update'),
    ('finance:receipt:delete'),
    ('finance:receipt:edit'),
    ('finance:receivable:approve'),
    ('finance:receivable:create'),
    ('finance:receivable:delete'),
    ('finance:receivable:query'),
    ('finance:receivable:update'),
    ('finance:record:approve'),
    ('finance:record:create'),
    ('finance:record:update'),
    ('finance:record:view'),
    ('finance:reimbursement:approve'),
    ('finance:reimbursement:create'),
    ('finance:reimbursement:pay'),
    ('finance:reimbursement:view'),
    ('finance:report:config'),
    ('finance:report:view'),
    ('finance:statistics:view'),
    ('finance:tax:create'),
    ('finance:tax:delete'),
    ('finance:tax:update'),
    ('finance:tax:view'),
    ('finance:voucher:approve'),
    ('finance:voucher:create'),
    ('finance:voucher:query'),
    ('finance:voucher:update'),
    ('finance:warning:create'),
    ('finance:warning:delete'),
    ('finance:warning:process'),
    ('finance:warning:view'),
    ('hardware:capture'),
    ('hardware:config:manage'),
    ('hardware:config:view'),
    ('hardware:delete'),
    ('hardware:device:manage'),
    ('hardware:device:view'),
    ('hardware:edit'),
    ('hardware:epson-test:manage'),
    ('hardware:epson-test:view'),
    ('hardware:init:manage'),
    ('hardware:label-printer:manage'),
    ('hardware:label-printer:view'),
    ('hardware:print'),
    ('hardware:print-format:manage'),
    ('hardware:print-format:view'),
    ('hardware:scan'),
    ('hardware:view'),
    ('hr:knowledge-base:manage'),
    ('hr:knowledge-base:view'),
    ('inventory:create'),
    ('inventory:deduct'),
    ('inventory:delete'),
    ('inventory:execute'),
    ('inventory:lock'),
    ('inventory:update'),
    ('label:template:create'),
    ('label:template:delete'),
    ('label:template:print'),
    ('label:template:update'),
    ('label:template:view'),
    ('loss-outbound:approve'),
    ('loss-outbound:create'),
    ('loss-outbound:list'),
    ('loss-outbound:view'),
    ('member:alert:manage'),
    ('member:alert:view'),
    ('member:analysis:manage'),
    ('member:analysis:view'),
    ('member:export'),
    ('member:level:manage'),
    ('member:level:view'),
    ('member:manage'),
    ('member:points:manage'),
    ('member:points:view'),
    ('member:recharge:export'),
    ('member:recharge:manage'),
    ('member:recharge:view'),
    ('member:view'),
    ('notification:deadletter:delete'),
    ('notification:deadletter:query'),
    ('notification:deadletter:retry'),
    ('notification:event:test'),
    ('notification:message:query'),
    ('notification:message:send'),
    ('notification:setting:edit'),
    ('notification:setting:query'),
    ('notification:statistics:query'),
    ('notification:template:create'),
    ('notification:template:delete'),
    ('notification:template:edit'),
    ('notification:template:query'),
    ('operations:dashboard:manage'),
    ('operations:dashboard:view'),
    ('operations:strategy:manage'),
    ('operations:strategy:view'),
    ('order:create'),
    ('order:pay'),
    ('order:queue'),
    ('order:refund'),
    ('other-inbound:create'),
    ('other-inbound:list'),
    ('other-inbound:view'),
    ('permission:assign'),
    ('permission:view'),
    ('product:analysis:export'),
    ('product:analysis:view'),
    ('product:cost:export'),
    ('product:food:export'),
    ('product:food:import'),
    ('product:view'),
    ('purchase:archive:create'),
    ('purchase:archive:delete'),
    ('purchase:archive:edit'),
    ('purchase:archive:view'),
    ('purchase:category:create'),
    ('purchase:category:delete'),
    ('purchase:category:edit'),
    ('purchase:category:view'),
    ('purchase:order:freeze'),
    ('purchase:order:terminate'),
    ('purchase:return:approve'),
    ('purchase:return:confirm'),
    ('purchase:return:create'),
    ('purchase:return:delete'),
    ('purchase:return:edit'),
    ('purchase:return:view'),
    ('purchase:stockin:cancel'),
    ('purchase:stockin:complete'),
    ('purchase:stockin:intervene'),
    ('purchase:stockin:print'),
    ('purchase:stockin:scan'),
    ('purchase:stockin:view'),
    ('purchase:supplier:manage'),
    ('purchase:trace:view'),
    ('recharge-plan:manage'),
    ('recharge-plan:view'),
    ('recharge-settings:manage'),
    ('recharge-settings:view'),
    ('recharge:view'),
    ('refund:manage'),
    ('refund:view'),
    ('schedule:entry:edit'),
    ('schedule:plan:create'),
    ('schedule:plan:delete'),
    ('schedule:plan:edit'),
    ('schedule:shift:configure'),
    ('schedule:template:manage'),
    ('schedule:view:self'),
    ('scheduler:task:create'),
    ('scheduler:task:delete'),
    ('scheduler:task:edit'),
    ('scheduler:task:query'),
    ('scheduler:task:trigger'),
    ('seal:delete'),
    ('seal:manage'),
    ('seal:view'),
    ('signature:delete'),
    ('signature:manage'),
    ('signature:view'),
    ('store:queue:view'),
    ('store:table:manage'),
    ('store:table:view'),
    ('sys:config:edit'),
    ('sys:config:query'),
    ('system:aimodel:manage'),
    ('system:config'),
    ('system:database:delete'),
    ('system:dict:create'),
    ('system:dict:delete'),
    ('system:dict:query'),
    ('system:dict:update'),
    ('system:init:manage'),
    ('system:init:view'),
    ('system:log:view'),
    ('system:permission:create'),
    ('system:permission:delete'),
    ('system:permission:manage'),
    ('system:permission:read'),
    ('system:permission:update'),
    ('system:permission:update-sort'),
    ('system:permission:update-status'),
    ('system:policy:create'),
    ('system:policy:delete'),
    ('system:policy:update'),
    ('system:policy:view'),
    ('system:role:assign-permissions'),
    ('system:role:create'),
    ('system:role:delete'),
    ('system:role:read'),
    ('system:role:set-data-scope'),
    ('system:role:update'),
    ('system:role:update-status'),
    ('system:stats:view'),
    ('system:user:assign-roles'),
    ('system:user:create'),
    ('system:user:read'),
    ('system:user:reset-password'),
    ('system:user:unassign-roles'),
    ('system:user:update'),
    ('system:user:update-status'),
    ('trace:create'),
    ('trace:delete'),
    ('trace:query'),
    ('trace:recall'),
    ('trace:update'),
    ('traceability:expiry-alert:manage'),
    ('traceability:expiry-alert:view'),
    ('traceability:quality:manage'),
    ('traceability:quality:view'),
    ('warehouse:create'),
    ('warehouse:delete'),
    ('warehouse:inbound:manage'),
    ('warehouse:inventory:view'),
    ('warehouse:location:delete'),
    ('warehouse:outbound:manage'),
    ('warehouse:overview:view'),
    ('warehouse:update')
) AS v(code)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_code = v.code)
  AND NOT EXISTS (SELECT 1 FROM permissions p2
                  WHERE p2.permission_id_str = 'PERM_' || UPPER(REPLACE(REPLACE(v.code, ':', '_'), '-', '_')));

-- ------------------------------------------------------------
-- 2. admin / owner 获得目录内全部权限码（原有 132 + 新增 286）
--    另补历史悬空 role_id '1'（admin 用户在 user_roles 里持有 '1'，roles 表无此行）
-- ------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id_str, p.permission_id_str, 0, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.role_id_str IN ('ROLE_ADMIN', 'ROLE_OWNER')
  AND r.deleted = 0
  AND p.deleted = 0 AND p.status = 1
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                  WHERE rp.role_id = r.role_id_str
                    AND rp.permission_id = p.permission_id_str
                    AND rp.deleted = 0);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT '1', p.permission_id_str, 0, NOW()
FROM permissions p
WHERE p.deleted = 0 AND p.status = 1
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                  WHERE rp.role_id = '1' AND rp.permission_id = p.permission_id_str AND rp.deleted = 0);

-- ------------------------------------------------------------
-- 3. 按模块前缀授予业务角色（矩阵见文件头）
-- ------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT v.role_id, p.permission_id_str, 0, NOW()
FROM (VALUES
    ('ROLE_FINANCE_DIRECTOR', 'finance'),
    ('ROLE_FINANCE_DIRECTOR', 'recharge'),
    ('ROLE_FINANCE_DIRECTOR', 'recharge-plan'),
    ('ROLE_FINANCE_DIRECTOR', 'recharge-settings'),
    ('ROLE_FINANCE_DIRECTOR', 'refund'),
    ('ROLE_STORE_MANAGER', 'refund'),
    ('ROLE_STORE_MANAGER', 'member'),
    ('ROLE_STORE_MANAGER', 'coupon'),
    ('ROLE_STORE_MANAGER', 'order'),
    ('ROLE_STORE_MANAGER', 'product'),
    ('ROLE_STORE_MANAGER', 'trace'),
    ('ROLE_STORE_MANAGER', 'traceability'),
    ('ROLE_STORE_MANAGER', 'label'),
    ('ROLE_STORE_MANAGER', 'store'),
    ('ROLE_STORE_MANAGER', 'schedule'),
    ('ROLE_STORE_MANAGER', 'alert'),
    ('ROLE_STORE_MANAGER', 'warehouse'),
    ('ROLE_STORE_MANAGER', 'inventory'),
    ('ROLE_STORE_MANAGER', 'other-inbound'),
    ('ROLE_STORE_MANAGER', 'loss-outbound'),
    ('ROLE_STORE_MANAGER', 'signature'),
    ('ROLE_WAREHOUSE_MANAGER', 'warehouse'),
    ('ROLE_WAREHOUSE_MANAGER', 'inventory'),
    ('ROLE_WAREHOUSE_MANAGER', 'other-inbound'),
    ('ROLE_WAREHOUSE_MANAGER', 'loss-outbound'),
    ('ROLE_WAREHOUSE_MANAGER', 'signature'),
    ('ROLE_PURCHASE_MANAGER', 'purchase'),
    ('ROLE_HR_DIRECTOR', 'hr'),
    ('ROLE_OPS_DIRECTOR', 'operations'),
    ('ROLE_AUDITOR', 'audit')
) AS v(role_id, module_prefix)
JOIN permissions p ON p.permission_code LIKE v.module_prefix || ':%'
WHERE p.deleted = 0 AND p.status = 1
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                  WHERE rp.role_id = v.role_id
                    AND rp.permission_id = p.permission_id_str
                    AND rp.deleted = 0);

-- ------------------------------------------------------------
-- 4. 自检（执行后人工核对，不入迁移）
-- ------------------------------------------------------------
-- SELECT count(*) FROM permissions WHERE deleted = 0;                      -- 期望 132 + 286 = 418
-- SELECT count(*) FROM role_permissions WHERE deleted = 0;                 -- 应显著增加
-- SELECT rp.role_id, count(*) FROM role_permissions rp WHERE rp.deleted = 0 GROUP BY 1 ORDER BY 2 DESC;
