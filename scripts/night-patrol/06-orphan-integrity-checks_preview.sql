/* =============================================================================
 * 文件    : 06-orphan-integrity-checks_preview.sql
 * 标题    : 孤儿引用完整性检查（只读，无变更段）
 * 目标    : 核查三类孤儿：kitchen_order.chef_id 指向不存在员工、tray.current_order_id 指向不存在订单、kitchen_order 指向不存在/已删订单
 * 预查影响: 0 行 / 0 行 / 0 行（2026-10-23 实测三类全部为 0，本脚本作为回归基线保留，供后续每晚复跑）
 * 回滚    : 无变更 → 无需回滚
 * 幂等守卫: 纯只读，可重复执行
 * 用法    : psql -f 06-orphan-integrity-checks_preview.sql     —— 默认**只跑 SELECT 预览**，不产生任何变更
 * 变更段  : 全部包在 [OWNER-EXECUTE] 注释块内，解除注释后才会生效，需 Owner 执行
 * 红线    : 本文件只读；不含任何物理移除语句、不含 DDL
 * 时点    : 2026-10-23（分支 ai-night-patrol-1，只读巡检产出）
 * ========================================================================== */

SET client_encoding = 'UTF8';

-- ============================ 1. 厨师孤儿 ============================
SELECT ko.kitchen_order_id, ko.order_number, ko.chef_id, ko.chef_name, ko.status, ko.deleted
FROM kitchen_order ko
WHERE ko.chef_id IS NOT NULL
  AND btrim(ko.chef_id::text) <> ''
  AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.employee_id::text = ko.chef_id::text)
ORDER BY ko.kitchen_order_id;

SELECT count(*) AS chef_orphans
FROM kitchen_order ko
WHERE ko.chef_id IS NOT NULL
  AND btrim(ko.chef_id::text) <> ''
  AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.employee_id::text = ko.chef_id::text);

-- ============================ 2. 托盘指向不存在订单 ============================
SELECT t.id, t.tray_code, t.status, t.current_order_id, t.current_kitchen_order_id
FROM tray t
WHERE t.current_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id::text = t.current_order_id::text)
ORDER BY t.id;

SELECT count(*) AS tray_order_orphans
FROM tray t
WHERE t.current_order_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id::text = t.current_order_id::text);

-- ============================ 3. 厨单指向不存在 / 已删订单 ============================
SELECT ko.kitchen_order_id, ko.order_id, ko.order_number, ko.status, ko.deleted
FROM kitchen_order ko
WHERE ko.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id = ko.order_id AND o.deleted = 0)
ORDER BY ko.kitchen_order_id;

SELECT count(*) AS kitchen_order_orphans
FROM kitchen_order ko
WHERE ko.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id = ko.order_id AND o.deleted = 0);

-- ============================ 4. 据库/业务口径断点：扣料明细缺失（只读，无法自动补） ============================
-- 21 张厨单标记 material_consumed=1，但 order_material_requirement 全表 0 行 → 扣料明细从未落库。
-- 这属于「业务未写入」，不存在可安全回填的来源，仅登记不处理。
SELECT (SELECT count(*) FROM kitchen_order WHERE material_consumed = 1 AND deleted = 0) AS consumed_kitchen_orders,
       (SELECT count(*) FROM order_material_requirement) AS omr_rows;
