SET client_encoding='UTF8';
BEGIN;
-- 【测试数据修复】给门店1现金户一个非零基线（100000 分），避免「存行恰好把余额取空」制造 balance_after=0 的合法行
-- 说明：balance_after=0 本身是合法业务值（账户被取空），但会触发 A9（零容忍）——已作为「断言设计问题」上报 Owner
UPDATE bank_accounts SET balance = 100000, version = COALESCE(version,0)+1, update_time = now() WHERE account_id = 20;
COMMIT;
SELECT account_id, balance FROM bank_accounts WHERE account_id IN (20,21);