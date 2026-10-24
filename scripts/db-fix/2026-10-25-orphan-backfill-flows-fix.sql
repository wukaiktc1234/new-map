-- 2026-10-25 P3 前置修复 3：2 条孤儿补记流水（FF202610250010/011）处置 —— 走 (b) 路径
-- 判定依据：对应订单 P2BF010413401 / P2BF010607008 的 orders.deleted=1，A1 的 orders.deleted=0 条件已排除它们
--           → A1 命中数处置前后均为 0，说明这 2 条流水对 A1 归零没有贡献，属「无支付事实的夹具流水」
-- 处置：status=3（作废）+ remark 追加 [testFixture]，**保留行与审计**，不物理删除；不改金额、不改账户余额
BEGIN;
SET client_encoding = 'UTF8';

UPDATE fund_flows
   SET status = 3,
       remark = COALESCE(remark, '') || ' [testFixture:对应订单已软删且无有效支付记录，非业务流水，P3 前置作废]',
       update_time = now()
 WHERE flow_no IN ('FF202610250010', 'FF202610250011')
   AND status <> 3;

COMMIT;

SELECT flow_no, order_id, amount, status, left(remark, 90) AS remark FROM fund_flows WHERE flow_no IN ('FF202610250010','FF202610250011');