-- ============================================================
-- scripts/qa/r8_location_coverage.sql
-- R8：location 归属覆盖率只读排查（供 Owner 决策 app.dashboard.store-scope.enabled 是否可开）
--
-- 安全：整个事务 SET TRANSACTION READ ONLY，全部 SELECT；不 UPDATE / 不 ALTER / 不碰任何配置开关。
--
-- 六段：
--   §1 users 侧覆盖率 + 启用但无 location 的账号清单（含角色 code）
--   §2 employees 侧覆盖率 + 在职但无 location 的清单
--   §3 交叉兜底：users.location_id 为空但该 user 的 employees.location_id 非空 → 可兜底
--   §4 有效性：悬空 location（不在 locations 表）/ 引用已删除库位（locations.deleted=1）
--   §5 业务影响预估：近 30 天订单按 store_id → location_id_map(stores_new) 的覆盖比例
--   §6 结论：三个可直接决策的数字
--
-- 口径说明：
--   * 启用用户 = users.deleted=0 AND status=1；在职员工 = employees.deleted=0 AND COALESCE(employee_status,status)=1
--   * 门店→库位唯一合法映射 = location_id_map(src_table='stores_new', src_id=store_id)；**不做**任何字符串匹配
--   * orders 的时间字段用 create_time（与交班/日结口径一致）
--
-- 用法：psql -h localhost -U postgres -d food_traceability -f scripts/qa/r8_location_coverage.sql
--       python scripts/qa/r8_location_coverage.py            （exit 0 固定，--json 机器可读）
-- ============================================================

\set ON_ERROR_STOP on
\pset pager off

BEGIN;
SET TRANSACTION READ ONLY;

-- ------------------------------------------------------------
\echo ''
\echo '===== §1 users 侧 location 覆盖率 ====='
SELECT count(*)                                                             AS users_total,
       count(*) FILTER (WHERE location_id IS NOT NULL)                      AS loc_set,
       count(*) FILTER (WHERE location_id IS NULL)                          AS loc_null,
       round(100.0 * count(*) FILTER (WHERE location_id IS NOT NULL) / NULLIF(count(*), 0), 2) AS loc_set_pct
  FROM users
 WHERE deleted = 0;

\echo '-- §1b 按状态分组（status=1 启用 / 其他停用）--'
SELECT status,
       count(*)                                                 AS users,
       count(*) FILTER (WHERE location_id IS NOT NULL)           AS loc_set,
       count(*) FILTER (WHERE location_id IS NULL)               AS loc_null,
       round(100.0 * count(*) FILTER (WHERE location_id IS NOT NULL) / NULLIF(count(*), 0), 2) AS loc_set_pct
  FROM users
 WHERE deleted = 0
 GROUP BY status
 ORDER BY status;

\echo '-- §1c 启用但无 location_id 的账号（最多 30 行，含角色 code）--'
SELECT u.user_id,
       u.username,
       COALESCE(string_agg(DISTINCT r.role_code, ',' ORDER BY r.role_code), '(无角色行)') AS roles
  FROM users u
  LEFT JOIN user_roles ur ON ur.user_id = u.user_id::text AND COALESCE(ur.deleted, 0) = 0
  LEFT JOIN roles r       ON (r.role_id_str = ur.role_id OR r.role_id::text = ur.role_id)
 WHERE u.deleted = 0 AND u.status = 1 AND u.location_id IS NULL
 GROUP BY u.user_id, u.username
 ORDER BY u.user_id
 LIMIT 30;

-- ------------------------------------------------------------
\echo ''
\echo '===== §2 employees 侧 location 覆盖率 ====='
SELECT count(*)                                                             AS employees_total,
       count(*) FILTER (WHERE location_id IS NOT NULL)                      AS loc_set,
       count(*) FILTER (WHERE location_id IS NULL)                          AS loc_null,
       round(100.0 * count(*) FILTER (WHERE location_id IS NOT NULL) / NULLIF(count(*), 0), 2) AS loc_set_pct
  FROM employees
 WHERE deleted = 0;

\echo '-- §2b 在职（status/employee_status=1）但无 location_id（最多 30 行）--'
SELECT employee_id, employee_name, employee_code, status, employee_status
  FROM employees
 WHERE deleted = 0
   AND COALESCE(employee_status, status) = 1
   AND location_id IS NULL
 ORDER BY employee_id
 LIMIT 30;

-- ------------------------------------------------------------
\echo ''
\echo '===== §3 交叉兜底：用户 location 能否由 employees 补齐 ====='
SELECT count(*) AS users_null_loc,
       count(*) FILTER (WHERE emp_loc IS NOT NULL) AS can_fallback_by_employee,
       count(*) FILTER (WHERE emp_loc IS NULL)     AS cannot_fallback
  FROM (
      SELECT u.user_id,
             (SELECT e.location_id FROM employees e
               WHERE e.user_id = u.user_id AND e.deleted = 0 AND e.location_id IS NOT NULL
               ORDER BY e.employee_id LIMIT 1) AS emp_loc
        FROM users u
       WHERE u.deleted = 0 AND u.location_id IS NULL
  ) t;

-- ------------------------------------------------------------
\echo ''
\echo '===== §4 有效性：悬空 / 已删除库位引用 ====='
SELECT 'users' AS tbl,
       count(*) FILTER (WHERE u.location_id IS NOT NULL
                          AND NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = u.location_id)) AS dangling,
       count(*) FILTER (WHERE u.location_id IS NOT NULL
                          AND EXISTS (SELECT 1 FROM locations l WHERE l.location_id = u.location_id AND l.deleted = 1)) AS refs_deleted_loc
  FROM users u WHERE u.deleted = 0
UNION ALL
SELECT 'employees',
       count(*) FILTER (WHERE e.location_id IS NOT NULL
                          AND NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = e.location_id)),
       count(*) FILTER (WHERE e.location_id IS NOT NULL
                          AND EXISTS (SELECT 1 FROM locations l WHERE l.location_id = e.location_id AND l.deleted = 1))
  FROM employees e WHERE e.deleted = 0
UNION ALL
SELECT 'purchase_stockins',
       count(*) FILTER (WHERE s.location_id IS NOT NULL
                          AND NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = s.location_id)),
       count(*) FILTER (WHERE s.location_id IS NOT NULL
                          AND EXISTS (SELECT 1 FROM locations l WHERE l.location_id = s.location_id AND l.deleted = 1))
  FROM purchase_stockins s WHERE s.deleted = 0;

-- ------------------------------------------------------------
\echo ''
\echo '===== §5 业务影响预估：近 30 天订单的 store→location 可限定比例 ====='
WITH o AS (
    SELECT o.order_id, o.store_id
      FROM orders o
     WHERE o.deleted = 0 AND o.create_time >= now() - INTERVAL '30 days'
), mapped AS (
    SELECT o.*, m.location_id,
           EXISTS (SELECT 1 FROM locations l WHERE l.location_id = m.location_id AND l.deleted = 0) AS loc_valid
      FROM o
      LEFT JOIN location_id_map m ON m.src_table = 'stores_new' AND m.src_id = o.store_id
)
SELECT count(*)                                                       AS orders_30d,
       count(*) FILTER (WHERE store_id IS NULL)                        AS no_store_id,
       count(*) FILTER (WHERE store_id IS NOT NULL AND location_id IS NULL) AS store_unmapped,
       count(*) FILTER (WHERE loc_valid)                               AS order_loc_resolvable,
       round(100.0 * count(*) FILTER (WHERE loc_valid) / NULLIF(count(*), 0), 2) AS resolvable_pct
  FROM mapped;

\echo '-- §5b 按门店拆分（近 30 天）--'
WITH o AS (
    SELECT store_id FROM orders
     WHERE deleted = 0 AND create_time >= now() - INTERVAL '30 days'
)
SELECT COALESCE(o.store_id::text, 'NULL') AS store_id,
       s.store_name,
       count(*)                        AS orders,
       m.location_id                    AS mapped_location_id,
       CASE WHEN m.location_id IS NULL THEN '未映射'
            WHEN EXISTS (SELECT 1 FROM locations l WHERE l.location_id = m.location_id AND l.deleted = 0) THEN '可限定'
            ELSE '库位无效' END        AS scope_result
  FROM o
  LEFT JOIN stores_new s       ON s.store_id = o.store_id
  LEFT JOIN location_id_map m  ON m.src_table = 'stores_new' AND m.src_id = o.store_id
 GROUP BY o.store_id, s.store_name, m.location_id
 ORDER BY count(*) DESC;

-- ------------------------------------------------------------
\echo ''
\echo '===== §6 结论：三个可直接决策的数字 ====='
WITH u AS (
    SELECT count(*)                                                       AS active_users,
           count(*) FILTER (WHERE location_id IS NOT NULL)                 AS active_users_with_loc
      FROM users WHERE deleted = 0 AND status = 1
), e AS (
    SELECT count(*)                                                       AS active_emps,
           count(*) FILTER (WHERE location_id IS NOT NULL)                 AS active_emps_with_loc
      FROM employees WHERE deleted = 0 AND COALESCE(employee_status, status) = 1
), d AS (
    SELECT (SELECT count(*) FROM users x WHERE x.deleted = 0 AND x.location_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = x.location_id))
         + (SELECT count(*) FROM employees x WHERE x.deleted = 0 AND x.location_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = x.location_id))
         + (SELECT count(*) FROM users x WHERE x.deleted = 0 AND x.location_id IS NOT NULL
              AND EXISTS (SELECT 1 FROM locations l WHERE l.location_id = x.location_id AND l.deleted = 1))
         + (SELECT count(*) FROM employees x WHERE x.deleted = 0 AND x.location_id IS NOT NULL
              AND EXISTS (SELECT 1 FROM locations l WHERE l.location_id = x.location_id AND l.deleted = 1)) AS bad_refs
)
SELECT u.active_users,
       u.active_users_with_loc,
       round(100.0 * u.active_users_with_loc / NULLIF(u.active_users, 0), 2) AS pct_1_active_user_coverage,
       e.active_emps,
       e.active_emps_with_loc,
       round(100.0 * e.active_emps_with_loc / NULLIF(e.active_emps, 0), 2)   AS pct_2_active_emp_coverage,
       d.bad_refs                                                            AS n_3_bad_location_refs
  FROM u, e, d;

COMMIT;
