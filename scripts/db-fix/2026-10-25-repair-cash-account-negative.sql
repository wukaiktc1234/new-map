SET client_encoding='UTF8';
BEGIN;
-- 【测试数据修复，非业务逻辑】混跑夹具把门店1现金户打成负（-35480 分），先清派生流水再归零，恢复可测基线
SELECT account_id, balance FROM bank_accounts WHERE account_id = 20;
DELETE FROM fund_flows WHERE deleted = 0 AND (flow_no LIKE 'FFP42%' OR remark LIKE '%[reconReverseOf=%' OR remark LIKE '%MIX%');
UPDATE bank_accounts SET balance = 0, version = COALESCE(version,0) + 1, update_time = now() WHERE account_id = 20;
COMMIT;
SELECT account_id, balance, version FROM bank_accounts WHERE account_id IN (20,21);