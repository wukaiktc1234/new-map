-- =====================================================================
-- V20261028_019__fen_columns_disable_notice.sql
--
-- 起因：E 批 2/3 建表时为满足「金额 bigint 存分 + _fen 后缀」新标准，
--       保留了旧实体的 numeric(元) 列并**新增了 *_fen 影子列**；
--       但**双写尚未实现** → `_fen` 当前恒为 NULL。
--
-- 风险（比不加列更容易误导）：看 schema 会以为已有分单位数据；
--       若有人直接 SELECT *_fen 做对账，会得到全 NULL → 静默无数据。
--
-- 处置：给每个 _fen 列加**数据库注释**，让「未启用」这件事跟着数据字典走，
--       而不是只写在文档里（文档会被忽略，注释会出现在任何 \d+ 输出里）。
--
-- 启用条件见 docs/audit/tech-debt-schema.md P1-1：
--   ① 双写实现并上线（Service 层、与主行同事务）；
--   ② 等值校验 SELECT count(*) ... WHERE *_fen IS DISTINCT FROM (元列*100) 连续 0 差异；
--   ③ 任一报表/对账逻辑开始读 _fen 之前，必须先满足 ①②。
-- =====================================================================

COMMENT ON COLUMN self_purchase.total_amount_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN self_purchase_item.unit_price_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN self_purchase_item.subtotal_amount_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN receivable.amount_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN receivable.received_amount_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN receivable.remaining_amount_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN requisition_requests.department_budget_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN requisition_requests.estimated_cost_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN travel_requests.estimated_budget_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN travel_requests.actual_amount_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN travel_requests.advance_payment_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
COMMENT ON COLUMN travel_requests.daily_allowance_fen IS '当前未启用（双写未上线），恒为 NULL；启用时间与条件见 docs/audit/tech-debt-schema.md P1-1';
