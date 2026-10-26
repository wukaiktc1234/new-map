\set ON_ERROR_STOP on
SET client_encoding='UTF8';
BEGIN;
-- 【测试数据审计措辞修正】512 条 E2E 清场核销补「测试数据核销，非真实业务」
UPDATE recon_differences
   SET resolution_remark = COALESCE(resolution_remark,'') || ' ｜ 测试数据核销，非真实业务（E2E 清场，非真实业务核销）'
 WHERE status = 'CLOSED'
   AND COALESCE(resolution_remark,'') LIKE '%E2E-period-guard-202610261111%'
   AND COALESCE(resolution_remark,'') NOT LIKE '%测试数据核销，非真实业务%';
COMMIT;
SELECT count(*) AS closed_total, count(*) FILTER (WHERE COALESCE(resolution_remark,'') LIKE '%测试数据核销，非真实业务%') AS with_test_wording FROM recon_differences WHERE status='CLOSED';