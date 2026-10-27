-- =====================================================================
-- V20261027_001__orders_idempotency_key_unique.sql
-- bug-③：orders.idempotency_key 唯一部分索引
--   为什么用**部分索引**：历史订单 6 行 idempotency_key 全为 NULL（Owner 裁定不补——没有原始 key 可回溯，
--   任何补值都是编造）。普通唯一索引下多个 NULL 不冲突，但部分索引语义更明确：只约束「真正带 key 的订单」。
--   依赖：应用侧 PosOrderCreateServiceImpl 在 insert 前写 key（同事务），并捕获 DuplicateKeyException 回查原单。
-- =====================================================================
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_idempotency_key
    ON orders (idempotency_key)
 WHERE idempotency_key IS NOT NULL;

COMMENT ON INDEX uk_orders_idempotency_key IS 'bug-③ 幂等键唯一（部分索引：仅非 NULL 生效；重复提交返回首次订单）';