-- V20261030_001__permissions_parent_id_to_bigint.sql
--
-- P0（Lead 2026-10-30）：permissions.parent_id 是 character varying，而实体 Permission.parentId 是 Long，
-- 导致 AdminPermissionCatalog.reload() 抛「Error attempting to get column 'parent_id' from result set」→
-- admin 的 DB 权限目录兜底**从未生效**，admin 只剩硬编码 AdminPermissions.ALL(352 码)，
-- 而 pos:* 不在那 352 里 → admin 反而进不去 POS 端点。
--
-- 值取证（2026-10-30，两库一致，481 行）：
--   NULL      479 行
--   '149'     1 行（纯数字，是真正的 parent 引用）
--   ''        1 行（空串，语义等于「无父级」，是脏值）  ← 就是它让 Long 转换失败
-- → 判定属于「情形 A + 需清洗一处脏值」：列本应是 bigint，空串归一为 NULL。
--
-- 本迁移只做两件事：① UPDATE '' → NULL；② 列类型 varchar → bigint。
-- **不改任何 Java 代码**（实体保持 Long），因此不影响 2888 处 parentId 用法。
--
-- 断言：改完后 ① 类型为 bigint；② 不存在空串；③ 行数不变；④ 非空值个数不变（1）。

DO $$
DECLARE
    v_before_rows bigint;
    v_before_nonnull bigint;
    v_empty bigint;
    v_type text;
BEGIN
    SELECT count(*), count(parent_id) INTO v_before_rows, v_before_nonnull FROM permissions;
    SELECT count(*) INTO v_empty FROM permissions WHERE parent_id = '';

    RAISE NOTICE 'V001 前置：permissions 行数=%，非空 parent_id=% 行，空串=% 行', v_before_rows, v_before_nonnull, v_empty;

    -- ① 空串归一为 NULL（语义：无父级）
    UPDATE permissions SET parent_id = NULL WHERE parent_id = '';

    -- ② 类型转换（NULLIF 兜底，理论上此刻已无空串）
    ALTER TABLE permissions
        ALTER COLUMN parent_id TYPE bigint
        USING NULLIF(parent_id, '')::bigint;

    -- 断言 ①：类型必须是 bigint
    SELECT data_type INTO v_type FROM information_schema.columns
     WHERE table_schema='public' AND table_name='permissions' AND column_name='parent_id';
    IF v_type <> 'bigint' THEN
        RAISE EXCEPTION 'V001 断言失败：parent_id 类型为 %，期望 bigint', v_type;
    END IF;

    -- 断言 ②：不得残留空串
    IF EXISTS (SELECT 1 FROM permissions WHERE parent_id IS NOT NULL AND parent_id::text = '') THEN
        RAISE EXCEPTION 'V001 断言失败：仍存在空串 parent_id';
    END IF;

    -- 断言 ③：行数不变
    IF (SELECT count(*) FROM permissions) <> v_before_rows THEN
        RAISE EXCEPTION 'V001 断言失败：permissions 行数从 % 变为 %', v_before_rows, (SELECT count(*) FROM permissions);
    END IF;

    -- 断言 ④：非空值个数不变（空串转 NULL 会让计数 -1，故期望 = 原非空数 - 空串数）
    IF (SELECT count(parent_id) FROM permissions) <> v_before_nonnull - v_empty THEN
        RAISE EXCEPTION 'V001 断言失败：非空 parent_id 个数异常（原 % 行非空，其中空串 % 行）', v_before_nonnull, v_empty;
    END IF;

    RAISE NOTICE 'V001 OK：parent_id → bigint；空串 % 行已归一为 NULL；行数 % 不变', v_empty, v_before_rows;
END $$;
