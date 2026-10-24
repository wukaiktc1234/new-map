-- 2026-10-25 §5：8 行双扣缺陷产物追加可追溯标记（维持 deleted=1，不恢复、不改 A14 断言）
-- 映射：软删行 → 同单同方向同账户的「正常」流水号（现存 deleted=0）
BEGIN;
SET client_encoding = 'UTF8';

UPDATE fund_flows SET remark = COALESCE(remark,'') || ' [duplicateOfDoubleDeduct:FF202610250376]', update_time = now()
 WHERE flow_id = 1040 AND COALESCE(remark,'') NOT LIKE '%[duplicateOfDoubleDeduct:%';
UPDATE fund_flows SET remark = COALESCE(remark,'') || ' [duplicateOfDoubleDeduct:FF202610250377]', update_time = now()
 WHERE flow_id = 1041 AND COALESCE(remark,'') NOT LIKE '%[duplicateOfDoubleDeduct:%';
UPDATE fund_flows SET remark = COALESCE(remark,'') || ' [duplicateOfDoubleDeduct:FF202610250382]', update_time = now()
 WHERE flow_id = 1042 AND COALESCE(remark,'') NOT LIKE '%[duplicateOfDoubleDeduct:%';
UPDATE fund_flows SET remark = COALESCE(remark,'') || ' [duplicateOfDoubleDeduct:FF202610250383]', update_time = now()
 WHERE flow_id = 1043 AND COALESCE(remark,'') NOT LIKE '%[duplicateOfDoubleDeduct:%';
UPDATE fund_flows SET remark = COALESCE(remark,'') || ' [duplicateOfDoubleDeduct:FF202610250384]', update_time = now()
 WHERE flow_id = 1049 AND COALESCE(remark,'') NOT LIKE '%[duplicateOfDoubleDeduct:%';
UPDATE fund_flows SET remark = COALESCE(remark,'') || ' [duplicateOfDoubleDeduct:FF202610250385]', update_time = now()
 WHERE flow_id = 1050 AND COALESCE(remark,'') NOT LIKE '%[duplicateOfDoubleDeduct:%';
UPDATE fund_flows SET remark = COALESCE(remark,'') || ' [duplicateOfDoubleDeduct:FF202610250390]', update_time = now()
 WHERE flow_id = 1051 AND COALESCE(remark,'') NOT LIKE '%[duplicateOfDoubleDeduct:%';
UPDATE fund_flows SET remark = COALESCE(remark,'') || ' [duplicateOfDoubleDeduct:FF202610250391]', update_time = now()
 WHERE flow_id = 1052 AND COALESCE(remark,'') NOT LIKE '%[duplicateOfDoubleDeduct:%';

COMMIT;

SELECT flow_id, flow_no, account_id, flow_direction AS dir, status, deleted,
       right(remark, 46) AS remark_tail
  FROM fund_flows WHERE flow_id IN (1040,1041,1042,1043,1049,1050,1051,1052) ORDER BY flow_id;