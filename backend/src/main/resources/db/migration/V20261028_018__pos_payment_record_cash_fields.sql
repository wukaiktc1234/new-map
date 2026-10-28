-- =====================================================================
-- V20261028_018__pos_payment_record_cash_fields.sql
--
-- ⚠ 版本号修正（2026-10-28 16:5x，fund-500）：本文件初建时名为 V20261028_01__…，
--   与既有的 V20261028_001__create_position_code_rules.sql **解析成同一个 Flyway 版本 20261028.001**，
--   导致 Flyway 扫描时直接报 "Found more than one migration with version 20261028.001"，
--   **整个迁移链无法启动（应用重启即失败）**。已改名为 _018 消除冲突；SQL 内容未改。
--
-- 起因：POS 收银前端在现金结账时已收集「实收现金 cashReceived（元）」
--       「找零 changeAmount（元）」「付款码 paymentCode」，但支付记录表
--       order_payment_records 无对应列，导致数据被丢弃、无法对账。
-- 处置：**只增量加列**，三列全部可空（老客户端不传 → NULL，行为不变）；
--       不改动任何已有表结构与已有迁移脚本（避免破坏 Flyway checksum）。
-- 口径：cash_received / change_amount 以「分」为单位（BIGINT），
--       与 payment_amount 一致；应用层负责 元 → 分 换算。
-- =====================================================================

ALTER TABLE order_payment_records ADD COLUMN IF NOT EXISTS cash_received BIGINT;
ALTER TABLE order_payment_records ADD COLUMN IF NOT EXISTS change_amount BIGINT;
ALTER TABLE order_payment_records ADD COLUMN IF NOT EXISTS payment_code VARCHAR(64);

COMMENT ON COLUMN order_payment_records.cash_received IS '实收现金（分，可空；现金结账时由 POS 前端上传的元金额换算）';
COMMENT ON COLUMN order_payment_records.change_amount IS '找零（分，可空）';
COMMENT ON COLUMN order_payment_records.payment_code IS '付款码/交易码（可空，最长 64 字符）';
