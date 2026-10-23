-- ============================================================
-- 2026-10-24-release-one-tray.sql
-- R9：让 e2e_core_chain.py 的「托盘绑定」不再恒 SKIP（只补数据，不改脚本）
--
-- 【为什么 SKIP】
--   e2e 的 bind_tray 在 --tray auto 下先调 GET /v1/tray/idle（KDS 凭证会把门店收敛成
--   凭证门店，默认 store_id=1），再挑第一张本店托盘去 POST /v1/tray/bind-order。
--   bindOrder 只允许「status='idle'」的托盘绑定（TrayServiceImpl.bindOrder 第 132 行），
--   而活库里门店 1 的 11 张托盘全部停留在 bound（历史 KDS/POS 造数留下的绑定），
--   空闲列表返回空 → 脚本走 skip 分支，永远看不到真实绑定结果。
--
-- 【口径说明】
--   tray 表的状态机取值是 idle / bound / making / ready / served / cleaning / damaged，
--   没有 'available'；本脚本所说的「回到可用」就是 status='idle'。
--
-- 【做法（幂等）】
--   1) 若门店 1 已经存在 status='idle' 的托盘 → 什么都不做（NOTICE skip，0 行）。
--   2) 否则释放一张：优先 tray_code='KDS-TEST-001'（最老的 KDS 测试托盘，绑在 13:43 的旧单上），
--      找不到则取门店 1 里 last_use_time 最早的 bound 托盘。
--      释放 = status='idle'、current_order_id/current_kitchen_order_id/bind_time 清空、
--      last_state_change_time=now；use_count / last_use_time 保留（不篡改历史统计）。
--   3) 释放前把该行快照进 tray_backup_20261024_release（表已存在则跳过），可精确回滚。
--
-- 【副作用说明】
--   只改托盘表，不动 kitchen_orders：旧后厨单仍保留 tray_id/tray_code 的历史引用，
--   但绑定只校验托盘当前 status，所以不影响新单绑定；若将来要彻底解绑旧单，另开脚本。
--
-- 【运维约定】
--   e2e 每跑一次会把这张托盘重新绑走（idle→bound），所以**每次跑 e2e_core_chain 之前**
--   先执行一次本脚本（幂等，已有空闲托盘时 0 行）。
--
-- 用法：psql -h localhost -U postgres -d food_traceability -f 本文件
-- 回滚：
--   UPDATE tray t SET status=b.status, current_order_id=b.current_order_id,
--          current_kitchen_order_id=b.current_kitchen_order_id, bind_time=b.bind_time,
--          last_state_change_time=b.last_state_change_time, update_time=CURRENT_TIMESTAMP
--     FROM tray_backup_20261024_release b WHERE t.id=b.id;
-- ============================================================

\set ON_ERROR_STOP on

BEGIN;

-- ---------- 0) 备份将被释放的那一行（先算目标，再备份） ----------
DO $$
DECLARE
    v_id     bigint;
    v_code   text;
    v_idle   int;
    v_rows   bigint;
BEGIN
    SELECT count(*) INTO v_idle FROM public.tray
     WHERE deleted = 0 AND store_id = 1 AND status = 'idle';

    IF v_idle > 0 THEN
        RAISE NOTICE '[skip] 门店1 已有 % 张空闲托盘，无需释放', v_idle;
        RETURN;
    END IF;

    -- 目标：优先 KDS-TEST-001，否则门店1 最早使用过的 bound 托盘
    SELECT id, tray_code INTO v_id, v_code
      FROM public.tray
     WHERE deleted = 0 AND store_id = 1 AND status <> 'idle'
     ORDER BY CASE WHEN tray_code = 'KDS-TEST-001' THEN 0 ELSE 1 END,
              last_use_time NULLS FIRST, id
     LIMIT 1;

    IF v_id IS NULL THEN
        RAISE NOTICE '[skip] 门店1 没有可释放的托盘（无 store_id=1 的托盘行）';
        RETURN;
    END IF;

    IF to_regclass('public.tray_backup_20261024_release') IS NULL THEN
        CREATE TABLE public.tray_backup_20261024_release AS
        SELECT * FROM public.tray WHERE id = v_id;
        GET DIAGNOSTICS v_rows = ROW_COUNT;
        RAISE NOTICE '[ok] 已备份待释放托盘 id=% code=%（% 行）', v_id, v_code, v_rows;
    ELSE
        RAISE NOTICE '[skip] 备份表 tray_backup_20261024_release 已存在，不覆盖';
    END IF;

    RAISE NOTICE '[info] 本次将释放：id=%, tray_code=%, store_id=1', v_id, v_code;
END $$;

-- ---------- 1) BEFORE ----------
\echo '== BEFORE：门店1 托盘状态分布 =='
SELECT status, count(*) AS rows FROM public.tray
 WHERE deleted = 0 AND store_id = 1 GROUP BY status ORDER BY status;

-- ---------- 2) 释放（仅当门店1无 idle 时生效） ----------
UPDATE public.tray t
   SET status                    = 'idle',
       current_order_id          = NULL,
       current_kitchen_order_id  = NULL,
       bind_time                 = NULL,
       last_state_change_time    = CURRENT_TIMESTAMP,
       update_time               = CURRENT_TIMESTAMP
 WHERE t.id = (
       SELECT x.id
         FROM public.tray x
        WHERE x.deleted = 0
          AND x.store_id = 1
          AND x.status <> 'idle'
          AND NOT EXISTS (
                SELECT 1 FROM public.tray y
                 WHERE y.deleted = 0 AND y.store_id = 1 AND y.status = 'idle')
        ORDER BY CASE WHEN x.tray_code = 'KDS-TEST-001' THEN 0 ELSE 1 END,
                 x.last_use_time NULLS FIRST, x.id
        LIMIT 1)
   AND t.status <> 'idle';

-- ---------- 3) AFTER ----------
\echo '== AFTER：门店1 托盘状态分布 =='
SELECT status, count(*) AS rows FROM public.tray
 WHERE deleted = 0 AND store_id = 1 GROUP BY status ORDER BY status;

\echo '== AFTER：门店1 空闲托盘（/v1/tray/idle 的口径）=='
SELECT id, tray_code, tray_name, store_id, status, last_use_time
  FROM public.tray
 WHERE deleted = 0 AND store_id = 1 AND status = 'idle'
 ORDER BY id;

COMMIT;
