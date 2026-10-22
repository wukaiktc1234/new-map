/* =============================================================================
 * 文件    : 04-tray-null-store_preview.sql
 * 标题    : TRAY-005 门店归属为空的托盘
 * 目标    : 定位 tray.store_id 为空的托盘，并核对它们是否被 KDS 门店校验拦住（本脚本不做归属改写）
 * 预查影响: 2 行（id=8 KDS-COMBO-224311、id=9 KDS-COMBOSRV-224348，均为 status='ready'、deleted=0）
 * 回滚    : 本脚本不含默认变更；若 Owner 分配门店，回滚为 UPDATE tray SET store_id=NULL, store_name=NULL WHERE id IN (8,9)
 * 幂等守卫: 变更段带 store_id IS NULL 条件，重复执行影响 0 行
 * 用法    : psql -f 04-tray-null-store_preview.sql     —— 默认**只跑 SELECT 预览**，不产生任何变更
 * 变更段  : 全部包在 [OWNER-EXECUTE] 注释块内，解除注释后才会生效，需 Owner 执行
 * 红线    : 本文件只读 + 软标记 / 字段回填；不含任何物理移除语句、不含 DDL
 * 时点    : 2026-10-23（分支 ai-night-patrol-1，只读巡检产出）
 * ========================================================================== */

SET client_encoding = 'UTF8';

-- ============================ 1. 预览：2 行 ============================
SELECT id, tray_code, tray_name, tray_type, status, store_id, store_name,
       current_order_id, current_kitchen_order_id, deleted
FROM tray
WHERE store_id IS NULL
ORDER BY id;

SELECT count(*) AS affected_rows FROM tray WHERE store_id IS NULL;

-- ============================ 2. 这些托盘当前的占用状态 ============================
SELECT status, count(*) AS cnt
FROM tray
WHERE store_id IS NULL
GROUP BY status;

-- ============================ 3. 是否已绑定在途厨单（有绑定就不能随便改归属） ============================
SELECT t.id, t.tray_code, t.status, t.current_kitchen_order_id, ko.status AS kitchen_status
FROM tray t
LEFT JOIN kitchen_order ko ON ko.kitchen_order_id::text = t.current_kitchen_order_id::text
WHERE t.store_id IS NULL
ORDER BY t.id;

-- ============================ 4. 可选目标门店（分配前确认门店存在） ============================
SELECT store_id, store_name FROM stores ORDER BY store_id;

/* ============================ [OWNER-EXECUTE] 变更段（业务决策，非清理） ============================
 * ⚠️ 这是业务动作（托盘分配门店），不是脏数据清理。按 TRAY-005 结论，管理端缺「托盘分配」入口，
 *    正确做法是走业务功能而不是直接改库。若 Owner 裁决直接落库，用下面模板（把 STORE_ID/STORE_NAME 换成真实值）：
 *
 * UPDATE tray
 *    SET store_id = 'STORE_ID',
 *        store_name = 'STORE_NAME',
 *        update_time = now(),
 *        update_by = 'OWNER'
 *  WHERE id IN (8, 9)
 *    AND store_id IS NULL;
 * -- 预计影响：2 行；幂等（store_id IS NULL）
 * ================================================================================ */

/* ============================ [ROLLBACK] 回滚 ============================
 * UPDATE tray SET store_id = NULL, store_name = NULL, update_time = now()
 *  WHERE id IN (8, 9) AND store_id = 'STORE_ID';
 * ======================================================================== */
