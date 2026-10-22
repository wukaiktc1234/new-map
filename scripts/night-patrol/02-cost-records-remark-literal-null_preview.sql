/* =============================================================================
 * 文件    : 02-cost-records-remark-literal-null_preview.sql
 * 标题    : cost_records.remark 的字面 null 残留（拼接 bug）
 * 目标    : 清除 cost_records 备注里由字符串拼接 bug 产生的字面 ' - null' 尾巴（金额本身有效，只改备注文本）
 * 预查影响: 25 行（remark 形如 '采购入库成本 - null'，全部 cost_type='7'、deleted=0；其中 6 行是 2026-10-23 新增之外的存量）
 * 回滚    : 见 [ROLLBACK] 段：无法逆向恢复原文本，故脚本前先 SELECT 原值归档（第 1 段输出即回滚依据）
 * 幂等守卫: 变更语句仅匹配以 ' - null' 结尾的 remark，重复执行为 0 行
 * 用法    : psql -f 02-cost-records-remark-literal-null_preview.sql     —— 默认**只跑 SELECT 预览**，不产生任何变更
 * 变更段  : 全部包在 [OWNER-EXECUTE] 注释块内，解除注释后才会生效，需 Owner 执行
 * 红线    : 本文件只读 + 软标记；不含任何物理移除语句、不含 DDL
 * 时点    : 2026-10-23（分支 ai-night-patrol-1，只读巡检产出）
 * ========================================================================== */

SET client_encoding = 'UTF8';

-- ============================ 1. 预览：待处理 25 行 ============================
SELECT cost_id, cost_no, cost_type, amount, remark
FROM cost_records
WHERE remark LIKE '% - null'
  AND deleted = 0
ORDER BY cost_id;

SELECT count(*) AS affected_rows
FROM cost_records
WHERE remark LIKE '% - null'
  AND deleted = 0;

-- ============================ 2. 归档（回滚依据，执行前务必留存输出） ============================
-- 把第 1 段结果另存为 CSV 即为回滚依据：
--   psql -c "COPY (SELECT cost_id, remark FROM cost_records WHERE remark LIKE '% - null' AND deleted = 0) TO STDOUT WITH CSV HEADER" > cost_remark_archive.csv
SELECT cost_id, remark AS original_remark, length(remark) AS len
FROM cost_records
WHERE remark LIKE '% - null'
  AND deleted = 0
ORDER BY cost_id;

-- ============================ 3. 分布核实（确认不是别的语义） ============================
SELECT cost_type, count(*) AS rows_cnt, min(amount) AS min_amt, max(amount) AS max_amt
FROM cost_records
WHERE remark LIKE '% - null'
  AND deleted = 0
GROUP BY cost_type;

/* ============================ [OWNER-EXECUTE] 变更段 ============================
 * 只裁剪备注尾巴，不动 amount / cost_type / 关联关系。
 *
 * UPDATE cost_records
 *    SET remark = btrim(replace(remark, ' - null', '')),
 *        update_time = now()
 *  WHERE remark LIKE '% - null'
 *    AND deleted = 0;
 * -- 预计影响：25 行
 * ================================================================================ */

/* ============================ [ROLLBACK] 回滚 ============================
 * 备注被裁剪后无法从库内还原原文，回滚只能靠第 2 段导出的 CSV 逐条写回：
 *
 * UPDATE cost_records SET remark = '原值' WHERE cost_id = 具体id AND deleted = 0;
 * -- 逐条执行，来源为 cost_remark_archive.csv
 * ======================================================================== */

-- ============================ 4. 变更后复核 ============================
SELECT count(*) AS remaining_literal_null
FROM cost_records
WHERE remark LIKE '% - null'
  AND deleted = 0;
