-- ============================================================
-- POS 叫号号（callNumber）落库 DDL（2026-10-23 无人值守修复）
--
-- 背景：POS 叫号单/小票/叫号屏此前直接显示订单号 T20261023031，
--       与线下快餐（KFC 式 A001 / B023）不符。本次新增独立叫号号段：
--       格式 ^[A-Z][0-9]{3,4}$，按「门店 + 自然日 + 类型前缀」每天从 1 自增，跨天自动重置。
--
-- 落库位置（复用现有表，不新造表）：
--   1) orders.call_number      —— canonical 订单主表，POS 订单查询/详情/小票取这里；
--   2) call_record.call_number —— POS 叫号屏（/v1/call-number/*）记录表，叫号屏显示这里；
--   3) order_sequence          —— 已存在的通用号段表，叫号号段复用其 (sequence_key, sequence_value)，
--                                 键形如 CALL-{storeId}-{yyyyMMdd}-{A|B|C}，并发自增由
--                                 INSERT ... ON CONFLICT DO UPDATE ... RETURNING 保证原子性。
--
-- 幂等：全部带 IF NOT EXISTS，可反复执行。
-- 用法：psql -h localhost -p 5432 -U postgres -d food_traceability -f <本文件>
-- ============================================================

-- ---------- 1. 订单主表：POS 叫号号 ----------
ALTER TABLE orders ADD COLUMN IF NOT EXISTS call_number varchar(8);

COMMENT ON COLUMN orders.call_number IS 'POS 叫号号（A001/B023，按门店每天从 1 自增，跨天重置）；NULL 表示历史订单未取号';

-- 同一门店、同一自然日、同一叫号号只能出现一次（防并发重复取号）
-- 表达式索引：create_time::date 取自然日；timestamp -> date 为 immutable 转换，可用于索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_call_number_store_day
    ON orders (store_id, ((create_time)::date), call_number)
    WHERE call_number IS NOT NULL;

-- 叫号号反查（叫号屏/客服按叫号号找单）
CREATE INDEX IF NOT EXISTS idx_orders_call_number ON orders (call_number) WHERE call_number IS NOT NULL;

-- ---------- 2. 叫号屏记录表：POS 叫号号 ----------
ALTER TABLE call_record ADD COLUMN IF NOT EXISTS call_number varchar(8);

COMMENT ON COLUMN call_record.call_number IS 'POS 叫号号（与 orders.call_number 一致），叫号屏优先展示';

CREATE INDEX IF NOT EXISTS idx_call_record_call_number ON call_record (call_number) WHERE call_number IS NOT NULL;

-- ---------- 3. 号段表说明（无结构变更，仅复用） ----------
-- order_sequence(sequence_key varchar(50) PK, sequence_value int not null default 0, create_time, update_time)
-- 叫号号段键：CALL-{storeId}-{yyyyMMdd}-{A|B|C}
-- 取号 SQL（原子，单语句并发安全）：
--   INSERT INTO order_sequence (sequence_key, sequence_value, create_time, update_time)
--   VALUES ('CALL-1-20261023-A', 1, NOW(), NOW())
--   ON CONFLICT (sequence_key)
--   DO UPDATE SET sequence_value = order_sequence.sequence_value + 1, update_time = NOW()
--   RETURNING sequence_value;
