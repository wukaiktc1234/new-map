-- ============================================================
-- 门店 POS 收款账户三件套补建（模块 1.1 多门店隔离，2026-10-23）
--
-- 背景
--   POS 支付入账（PosSaleFinanceRecorder）选账户规则已改为：
--     1) 候选集**必须**是订单门店（orders.store_id → bank_accounts.store_id）且 status=1 的账户；
--     2) 再按支付方式匹配：现金/余额→名称含「现金」或 account_type=2；
--        微信→名称含「微信」；支付宝→名称含「支付宝」；银行卡→account_type=1；
--     3) 匹配不到就**不入账**（支付仍成功，ERROR 日志 + fundFlowNo=null），
--        不再回退到全局/第一个启用账户。
--   ⇒ 门店若没配对应渠道账户，收银员会看到「支付成功但财务没这笔钱」。
--
-- 本脚本做什么
--   给每个「有 POS 订单（orders 表）」的 store_id 幂等补建三件套收款账户：
--     现金账户      account_type=2  bank_name='现金'      名称='门店N现金账户'
--     微信商户账户  account_type=1  bank_name='微信支付'  名称='门店N微信商户账户'
--     支付宝账户    account_type=1  bank_name='支付宝'    名称='门店N支付宝商户账户'
--
-- !! 重要：新开门店必须配账户 !!
--   * 本脚本**不会**在应用启动时自动执行（避免启动时隐式写业务数据），只作为人工/巡检脚本；
--   * 新开门店产生第一笔 POS 订单后，请人工/巡检执行本脚本（或直接建三件套账户），
--     否则该门店的微信/支付宝/现金收款只会入 order_payment_records，不进 fund_flows
--     —— 财务侧表现为「钱没进来」，只有 ERROR 日志 + fundFlowNo=null 可查；
--   * 建议把本脚本挂到日巡检/开店 checklist，执行后核对文末「自查」SQL 的 cash_cnt/wechat_cnt/alipay_cnt 均为 1。
--
-- 幂等
--   NOT EXISTS 守卫按 (store_id, 支付渠道关键字) 判重：该门店已有启用且名称/银行名含该关键字的账户则跳过；
--   账号列取值 CASH-STORE-N / WECHAT-STORE-N / ALIPAY-STORE-N，同样不会重复。可反复执行。
--
-- 用法：psql -h localhost -p 5432 -U postgres -d food_traceability -f <本文件>
-- ============================================================

WITH store_ids AS (
    SELECT DISTINCT store_id
    FROM orders
    WHERE store_id IS NOT NULL
      AND deleted = 0
),
channel AS (
    -- kw = 支付渠道关键字（既用于账户命名，也用于 NOT EXISTS 判重）
    SELECT '现金'::text AS kw, 2 AS account_type, '现金账户'::text AS name_suffix,
           '现金'::text AS bank_name, 'CASH'::text AS code_prefix
    UNION ALL
    SELECT '微信', 1, '微信商户账户', '微信支付', 'WECHAT'
    UNION ALL
    SELECT '支付宝', 1, '支付宝商户账户', '支付宝', 'ALIPAY'
)
INSERT INTO bank_accounts (
    account_name, bank_name, account_number, account_number_masked,
    account_type, balance, currency, status, store_id, scope, remark,
    create_time, update_time, deleted, version
)
SELECT
    '门店' || s.store_id || c.name_suffix,
    c.bank_name,
    c.code_prefix || '-STORE-' || s.store_id,
    '****' || c.code_prefix || 'S' || s.store_id,
    c.account_type,
    0,
    'CNY',
    1,                      -- 1 = 启用
    s.store_id,
    'STORE',
    'POS 门店' || c.kw || '收款账户（2026-10-23 多门店隔离自动补建）',
    NOW(), NOW(), 0, 0
FROM store_ids s
CROSS JOIN channel c
WHERE NOT EXISTS (
    SELECT 1
    FROM bank_accounts a
    WHERE a.store_id = s.store_id
      AND a.deleted = 0
      AND a.status = 1
      AND (a.account_name LIKE '%' || c.kw || '%'
           OR a.bank_name LIKE '%' || c.kw || '%')
);

-- ------------------------------------------------------------
-- 自查：每个门店三件套是否齐全（三列应各为 1；缺列说明该渠道收款不能入账）
-- ------------------------------------------------------------
-- SELECT o.store_id,
--        COUNT(*) FILTER (WHERE a.account_name LIKE '%现金%' AND a.account_type = 2)  AS cash_cnt,
--        COUNT(*) FILTER (WHERE a.account_name LIKE '%微信%' AND a.account_type = 1)  AS wechat_cnt,
--        COUNT(*) FILTER (WHERE a.account_name LIKE '%支付宝%' AND a.account_type = 1) AS alipay_cnt,
--        STRING_AGG(a.account_id || ':' || a.account_name, ' | ' ORDER BY a.account_id) AS accounts
-- FROM (SELECT DISTINCT store_id FROM orders WHERE store_id IS NOT NULL AND deleted = 0) o
-- LEFT JOIN bank_accounts a ON a.store_id = o.store_id AND a.deleted = 0 AND a.status = 1
-- GROUP BY o.store_id ORDER BY o.store_id;
