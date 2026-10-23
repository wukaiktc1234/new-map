-- ============================================================
-- 2026-10-23-bank-account-type-normalize.sql
-- 修复：资金管理页 /api/v1/finance/fund-flows 500
--   Cause: org.postgresql.util.PSQLException: 不良的类型值 int : bank
--   BankAccount.accountType 是 Integer，但库里 bank_accounts.account_type
--   是 character varying，混有 'bank' / 'cash' / '1' 等值 → 读取即抛
--   DataIntegrityViolationException，整页 500（traceId=680bd8650713）
--
-- 映射口径（与前端 BankAccountTypeMap / 实体注释对齐，值域 1..5）：
--   1 = basic / bank / 基本户
--   2 = general / 一般户（原 'cash' 现金账户并入一般户，前端无 cash 分支）
--   3 = payroll / 工资户
--   4 = reserve / 备用金
--   5 = other / 其他（未知或越界值一律归 5）
--   纯数字字符串：1..5 原样保留；0、6 及以上越界值归 5
--
-- 可重复执行：先判列类型，已是 integer 的库只做值域兜底 + 约束/注释补齐，
-- 不重复做类型转换；表不存在则整段跳过。
-- 用法：psql -h localhost -U postgres -d food_traceability -f 本文件
-- ============================================================

BEGIN;

DO $$
DECLARE
    v_type   text;
    v_count  bigint;
BEGIN
    SELECT data_type INTO v_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name   = 'bank_accounts'
       AND column_name  = 'account_type';

    IF v_type IS NULL THEN
        RAISE NOTICE '[skip] public.bank_accounts.account_type 不存在，跳过';
        RETURN;
    END IF;

    RAISE NOTICE '[info] 当前列类型 = %', v_type;

    IF v_type IN ('character varying', 'character', 'text') THEN
        -- 1) 先摘掉 varchar 默认值 'bank'（否则 ALTER TYPE 无法执行）
        EXECUTE 'ALTER TABLE public.bank_accounts ALTER COLUMN account_type DROP DEFAULT';

        -- 2) 值归一（只在值发生变化时更新，保留 update_time 语义）
        WITH norm AS (
            SELECT account_id,
                   CASE
                       WHEN btrim(account_type) ~ '^[0-9]+$'
                            AND btrim(account_type)::int BETWEEN 1 AND 5
                           THEN btrim(account_type)::int
                       WHEN btrim(account_type) ~ '^[0-9]+$'
                           THEN 5
                       WHEN lower(btrim(account_type)) IN ('bank', 'basic', '基本户', '基本存款账户') THEN 1
                       WHEN lower(btrim(account_type)) IN ('cash', 'general', '一般户', '一般存款账户')  THEN 2
                       WHEN lower(btrim(account_type)) IN ('payroll', '工资户')                        THEN 3
                       WHEN lower(btrim(account_type)) IN ('reserve', '备用金')                        THEN 4
                       WHEN lower(btrim(account_type)) IN ('other', '其他')                            THEN 5
                       ELSE 5
                   END AS new_type
              FROM public.bank_accounts
        )
        UPDATE public.bank_accounts b
           SET account_type = norm.new_type::text,
               update_time  = CURRENT_TIMESTAMP
          FROM norm
         WHERE b.account_id = norm.account_id
           AND b.account_type IS DISTINCT FROM norm.new_type::text;

        GET DIAGNOSTICS v_count = ROW_COUNT;
        RAISE NOTICE '[info] 值归一更新 % 行', v_count;

        -- 3) 列类型 varchar -> integer（USING 再兜一层，任何异常字符落 5，保证转换不失败）
        EXECUTE 'ALTER TABLE public.bank_accounts
                 ALTER COLUMN account_type TYPE integer
                 USING (CASE WHEN account_type ~ ''^[0-9]+$'' AND account_type::int BETWEEN 1 AND 5
                             THEN account_type::int
                             WHEN account_type ~ ''^[0-9]+$'' THEN 5
                             ELSE 5 END)';

        EXECUTE 'ALTER TABLE public.bank_accounts ALTER COLUMN account_type SET DEFAULT 1';

        RAISE NOTICE '[ok] account_type 已改为 integer（默认值 1）';
    ELSE
        RAISE NOTICE '[skip] 列已是 % 类型，跳过类型转换', v_type;

        -- 已是整数列：只兜越界/NULL 值
        UPDATE public.bank_accounts
           SET account_type = CASE
                                  WHEN account_type IS NULL THEN 1
                                  WHEN account_type < 1     THEN 1
                                  WHEN account_type > 5     THEN 5
                                  ELSE account_type
                              END,
               update_time  = CURRENT_TIMESTAMP
         WHERE account_type IS NULL
            OR account_type < 1
            OR account_type > 5;

        GET DIAGNOSTICS v_count = ROW_COUNT;
        IF v_count > 0 THEN
            RAISE NOTICE '[info] 越界值兜底 % 行', v_count;
        END IF;

        EXECUTE 'ALTER TABLE public.bank_accounts ALTER COLUMN account_type SET DEFAULT 1';
    END IF;

    -- 4) 值域约束（写入侧防非数字/越界，避免再次把读取侧打崩）
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conname  = 'ck_bank_accounts_account_type_range'
           AND conrelid = 'public.bank_accounts'::regclass
    ) THEN
        EXECUTE 'ALTER TABLE public.bank_accounts
                 ADD CONSTRAINT ck_bank_accounts_account_type_range
                 CHECK (account_type BETWEEN 1 AND 5)';
        RAISE NOTICE '[ok] 已添加约束 ck_bank_accounts_account_type_range (1..5)';
    ELSE
        RAISE NOTICE '[skip] 约束 ck_bank_accounts_account_type_range 已存在';
    END IF;

    -- 5) 注释口径
    EXECUTE 'COMMENT ON COLUMN public.bank_accounts.account_type IS '
         || quote_literal('账户类型（integer）：1-基本户(basic/bank) 2-一般户(general，原 cash 并入) 3-工资户(payroll) 4-备用金(reserve) 5-其他(other/未知兜底)。值域约束 ck_bank_accounts_account_type_range 1..5。2026-10-23 由 varchar 归一为 integer，修复 fund-flows 500。');
END $$;

-- 6) 结果自检
SELECT account_type, COUNT(*) AS rows
  FROM public.bank_accounts
 GROUP BY account_type
 ORDER BY account_type;

SELECT pg_typeof(account_type) AS col_type,
       COUNT(*)               AS total_rows,
       MIN(account_type)      AS min_type,
       MAX(account_type)      AS max_type
  FROM public.bank_accounts
 GROUP BY pg_typeof(account_type);

COMMIT;
