-- ============================================================================
-- V20261024_002__stockin_header_location_not_null.sql
--
-- 整改 3 · 第二阶段：把**表头** purchase_stockins.location_id 也强制 NOT NULL
--   Owner 口径：「必须强制写入 purchase_stockin_items.location_id 和
--                purchase_stockins.location_id … 绝不允许落 NULL」。
--
-- 与 V20261024_001 的关系：
--   * 001 负责建列 + 索引 + 两条库位外键，并把 **items** 侧先硬起来；
--   * 002 只负责表头 NOT NULL，等 fund-500 第二阶段（表头回填）完成后再上，
--     两阶段分开落地，避免 items 被表头数据拖住。
--
-- 前置条件（由 fund-500 第二阶段负责）：
--   scripts/db-fix/2026-10-24-fix-stockin-location-id.sql
--   —— ID 映射链：purchase_orders.warehouse_id/store_id → location_id_map → locations；
--      **禁止**按「仓库编码 ↔ 库位编码」之类的字符串软匹配推断库位。
--   无法归属的入库单按业务裁决逻辑删除后再重跑本迁移；本迁移不造数据。
--
-- 幂等性：列存在性 / is_nullable 均查 information_schema，重复手工执行不报错。
--
-- 手工验证（psql）：**请用单事务方式执行**，否则 psql 逐条自动提交，中途失败会留下半成品：
--   psql -1 -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d food_traceability -f 本文件
--   （Flyway 会把整个迁移放在一个事务里执行，失败自动整体回滚。）
--   期望：select is_nullable from information_schema.columns
--          where table_name='purchase_stockins' and column_name='location_id';  -- NO
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 0) 依赖检查：列不存在说明 V20261024_001 还没应用
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'purchase_stockins'
           AND column_name = 'location_id'
    ) THEN
        RAISE EXCEPTION USING
            MESSAGE = 'purchase_stockins.location_id 列不存在：请先应用 V20261024_001__stockin_location_hardening.sql 再执行本迁移',
            HINT = 'Flyway 正常情况下会按版本号顺序自动先跑 001；手工验证时请按 001 → 002 顺序执行。';
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 1) 前置数据闸门：表头仍有 NULL 时给出可读失败原因
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_stockin_null   bigint;
    v_stockin_orphan bigint;
BEGIN
    SELECT count(*) INTO v_stockin_null
      FROM purchase_stockins
     WHERE location_id IS NULL;

    IF v_stockin_null > 0 THEN
        RAISE EXCEPTION USING
            MESSAGE = format(
                'purchase_stockins.location_id 仍有 %s 行 NULL，无法 SET NOT NULL（整改3 第二阶段：表头库位回填未完成）', v_stockin_null),
            DETAIL = format(
                '入库单表头 location_id 为空的单据数：%s', v_stockin_null),
            HINT = '请先执行 scripts/db-fix/2026-10-24-fix-stockin-location-id.sql（ID 映射链：purchase_orders.warehouse_id/store_id → location_id_map → locations），'
                || '无法归属的单据按业务裁决逻辑删除后再启动应用/重跑本迁移。';
    END IF;

    SELECT count(*) INTO v_stockin_orphan
      FROM purchase_stockins s
     WHERE s.location_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = s.location_id);

    IF v_stockin_orphan > 0 THEN
        RAISE EXCEPTION USING
            MESSAGE = format(
                'purchase_stockins.location_id 有 %s 行指向不存在的库位（悬空外键），无法 SET NOT NULL', v_stockin_orphan),
            HINT = '请先修正这些入库单的库位引用（或按业务裁决逻辑删除）后重跑本迁移。';
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 2) 表头 NOT NULL（幂等）
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'purchase_stockins'
           AND column_name = 'location_id'
           AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE purchase_stockins
            ALTER COLUMN location_id SET NOT NULL;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 3) 迁移自检
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_nullable text;
BEGIN
    SELECT is_nullable INTO v_nullable
      FROM information_schema.columns
     WHERE table_schema = current_schema()
       AND table_name = 'purchase_stockins'
       AND column_name = 'location_id';

    IF v_nullable IS DISTINCT FROM 'NO' THEN
        RAISE EXCEPTION '自检失败：purchase_stockins.location_id 仍可空（is_nullable=%）', v_nullable;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_purchase_stockins_location'
           AND conrelid = 'purchase_stockins'::regclass
    ) THEN
        RAISE EXCEPTION '自检失败：缺少外键 fk_purchase_stockins_location（应由 V20261024_001 建立）';
    END IF;

    RAISE NOTICE '整改3 第二阶段完成：purchase_stockins.location_id 已 NOT NULL，库位 FK 就位。';
END
$$;
