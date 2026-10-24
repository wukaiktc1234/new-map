-- V20261025_001__fund_flows_recon_reverse_unique.sql
-- P3 前置修复 1(c)：为「冲正反向流水」加**部分唯一索引**，在数据库层保证
-- 同一条原流水只会被冲正一次（即使应用层并发检查同时通过，也只会有一个 INSERT 成功）。
--
-- 幂等键约定：反向流水的 remark 一定包含且只包含一个 [reconReverseOf=<原 flow_no>] 标记
-- （见 DepositSlipServiceImpl#reverse：remark = "缴款单冲正：<slipNo> 原流水 <flowNo> [reconReverseOf=<flowNo>]"）。
--
-- 【索引选型说明】
--   1. 因为「一条 remark 只含一个键」，直接对 remark 全文做唯一索引即可等价于「对键唯一」：
--      不同原流水 → 键不同 → remark 不同；同一原流水 → remark 完全相同 → 冲突。故不需要表达式索引。
--   2. 采用**部分索引（partial index）**只覆盖冲正流水（remark LIKE '%[reconReverseOf=%' 且 deleted=0），
--      不占用其它流水的索引空间，也不影响历史 remark 重复的普通流水。
--   3. 若将来出现「一条 remark 打包多个键」的写法，本索引将不足以去重，届时应改为表达式索引，例如：
--      CREATE UNIQUE INDEX ... ON fund_flows ((regexp_match(remark, '\[reconReverseOf=([^\]]+)\]'))[1])
--      WHERE deleted = 0 AND remark LIKE '%[reconReverseOf=%';
--      本次不改，因为当前写入路径始终只写一个键（已 grep 确认）。
--   4. 唯一冲突在 DepositSlipServiceImpl#reverse 里被 catch（DuplicateKeyException）转成业务 400，
--      不会冒泡成 500。
--
-- 幂等：IF NOT EXISTS，可重复执行。

CREATE UNIQUE INDEX IF NOT EXISTS uk_fund_flows_recon_reverse
    ON fund_flows (remark)
    WHERE deleted = 0 AND remark LIKE '%[reconReverseOf=%';

COMMENT ON INDEX uk_fund_flows_recon_reverse IS
    'P3 前置：冲正反向流水部分唯一索引（remark 含 [reconReverseOf=<原flow_no>]），防并发重复冲正';
