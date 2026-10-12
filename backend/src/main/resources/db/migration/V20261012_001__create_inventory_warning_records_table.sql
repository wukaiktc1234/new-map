-- =====================================================
-- N17（board §24.3r）：库存预警记录表迁移脚本（PostgreSQL）
-- 版本：V20261012_001
-- 卡面：board §24.3r N17 —— Flyway 建表 + 调度器零改动
-- 根因（2026-10-12 活库实测）：实体 `InventoryWarningRecord` @TableName("inventory_warning_records")
--   （复数）活库不存在 → InventoryWarningScheduler.createWarningRecord() 每小时 insert 必败
--   （异常被 :79-81 catch 吞，静默失败）。
-- 活库同名表辨析（2026-10-12 psql 证据）：
--   inventory_warning_records（复数）= 本迁移创建（缺失）
--   inventory_warning_record（单数） = 预警操作审计表（warning_id/action_type/operator_*，10 列，
--                                       schema 与本实体不同，勿混淆、勿改名复用）
--   inventory_warning_rules（复数）  = 现行规则表，已存在（调度器读取侧正常）
--   inventory_warning / inventory_warning_rule（单数）= legacy 两代，本卡不触碰
-- 列集 = 实体 12 字段（rule_id/inventory_id/warehouse_id/warning_type/current_value/
--   threshold_value/message/is_handled/handle_time/create_time + record_id 主键、deleted 逻辑删）
-- 调度器（InventoryWarningScheduler.java）零改动；C7 前置实测的 warehouse_id 读回修复
--   （locationService.resolveWarehouseIdByLocationId）随本表可用后自动生效（卡 N17 范围外不重复改）
-- 回退：DROP TABLE inventory_warning_records;（新表无业务数据依赖，零数据损失）
-- =====================================================

CREATE TABLE IF NOT EXISTS inventory_warning_records (
    record_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_id BIGINT,
    inventory_id BIGINT,
    warehouse_id BIGINT,
    warning_type INTEGER,
    current_value NUMERIC,
    threshold_value NUMERIC,
    message TEXT,
    is_handled BOOLEAN NOT NULL DEFAULT FALSE,
    handle_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 调度器去重查询索引（rule_id + inventory_id + is_handled，见 createWarningRecord :177-182）
CREATE INDEX IF NOT EXISTS idx_inventory_warning_records_dedup
    ON inventory_warning_records(rule_id, inventory_id) WHERE deleted = 0;
