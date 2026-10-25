-- ============================================================================
-- V20261025_003__flow_no_burn_ledger.sql
--
-- BL-5（P3-5 项 2）：流水号「烧号账」—— 取号成功但 fund_flows 未落库的号必须留痕
--
-- 背景：flow_no 由 order_sequence 原子自增分配，一旦分配出去就永久占用；
--   若随后的插入失败（唯一键冲突 / 乐观锁耗尽 / 业务校验失败），这个号就成了「洞」。
--   没有账就查不出洞，也无法满足审计 A25。
--
-- 判定口径（A25）：archived_flag = false 且 flow_no 不在 fund_flows 里的行数，
--   必须等于「已消耗但未落库」的真实数量；正常路径（取号 + 落库同事务成功）不写本表。
--
-- 写入时机（实现见 FlowNoBurnLedgerRecorder#recordBurn）：
--   * 只在「取号成功之后、fund_flows 插入/记账失败」的 catch 块里写；
--   * 取号本身失败不写（没有消耗号，不算烧号）；
--   * **必须 REQUIRES_NEW 独立事务**：业务事务马上要回滚，同事务写账会被一起回滚掉，
--     那样等于没记 —— 这是本表最关键的实现约束。
--
-- 归档：90 天后由 scripts/db-fix/20261025-archive-burn-ledger.sql 置 archived_flag=true，
--   **不做物理删除**（审计要求可追溯）。
--
-- 幂等：CREATE TABLE/INDEX IF NOT EXISTS + COMMENT 覆盖写；重复执行 0 变更。
-- 回退：DROP TABLE fund_flow_no_burn_ledger;
-- ============================================================================

CREATE TABLE IF NOT EXISTS fund_flow_no_burn_ledger (
    ledger_id     BIGSERIAL     PRIMARY KEY,
    flow_no       VARCHAR(64)   NOT NULL,
    seq_key       VARCHAR(32)   NOT NULL,
    seq_value     BIGINT        NOT NULL,
    reason        VARCHAR(200)  NOT NULL,
    created_by    VARCHAR(64),
    create_time   TIMESTAMP     NOT NULL DEFAULT now(),
    archived_flag BOOLEAN       NOT NULL DEFAULT false,
    archived_time TIMESTAMP
);

COMMENT ON TABLE fund_flow_no_burn_ledger IS
    '烧号账：取号成功但流水未落库的号；A25 判定 archived_flag=false 的行数必须与「号段已用 − 实际流水」对得上';
COMMENT ON COLUMN fund_flow_no_burn_ledger.flow_no IS '已消耗但未落库的流水号（如 FF202610250081）';
COMMENT ON COLUMN fund_flow_no_burn_ledger.seq_key IS '号段键，如 FLOW20261025';
COMMENT ON COLUMN fund_flow_no_burn_ledger.seq_value IS '被消耗的序号（号段自增返回值）';
COMMENT ON COLUMN fund_flow_no_burn_ledger.reason IS '失败原因：duplicate-key: uk_fund_flows_flow_no / optim lock exhausted / business validation failed …';
COMMENT ON COLUMN fund_flow_no_burn_ledger.created_by IS '写账人（登录用户名；无上下文时写 system）';
COMMENT ON COLUMN fund_flow_no_burn_ledger.archived_flag IS '90 天归档标记；false=活跃（A25 只统计 false 的行）';
COMMENT ON COLUMN fund_flow_no_burn_ledger.archived_time IS '归档时间（归档不删除数据）';

CREATE INDEX IF NOT EXISTS idx_burn_ledger_archived ON fund_flow_no_burn_ledger (archived_flag, create_time);
CREATE INDEX IF NOT EXISTS idx_burn_ledger_flow_no  ON fund_flow_no_burn_ledger (flow_no);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = current_schema() AND table_name = 'fund_flow_no_burn_ledger'
    ) THEN
        RAISE EXCEPTION '自检失败：fund_flow_no_burn_ledger 未创建';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema() AND indexname = 'idx_burn_ledger_archived')
       OR NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema() AND indexname = 'idx_burn_ledger_flow_no') THEN
        RAISE EXCEPTION '自检失败：烧号账索引缺失';
    END IF;
    RAISE NOTICE 'BL-5 完成：fund_flow_no_burn_ledger 就绪（含 archived/flow_no 两个索引）。';
END
$$;
