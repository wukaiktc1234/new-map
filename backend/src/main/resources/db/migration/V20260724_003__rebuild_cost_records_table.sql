-- 重建 cost_records 表，使其与 CostRecord 实体严格对齐
-- 背景：历史上存在旧表 cost_record（单数）及兼容视图 cost_records，字段与当前实体不一致，
--       导致创建成本记录时报错。本迁移直接替换为符合实体定义的 cost_records 表。

-- 1. 删除旧的兼容视图（如果存在）
DROP VIEW IF EXISTS cost_records;

-- 2. 删除旧表（如果存在），其字段已与新实体不兼容且无有效业务数据需要保留
DROP TABLE IF EXISTS cost_record;

-- 3. 创建新的 cost_records 表
CREATE TABLE IF NOT EXISTS cost_records (
    cost_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cost_no             VARCHAR(32) NOT NULL UNIQUE,
    cost_type           INTEGER NOT NULL,
    period              VARCHAR(10) NOT NULL,
    cost_center_id      BIGINT,
    amount              BIGINT NOT NULL DEFAULT 0,
    quantity            DECIMAL(18,4),
    unit_price          DECIMAL(18,4),
    calculation_method  INTEGER DEFAULT 1,
    related_voucher_id  BIGINT,
    remark              TEXT,
    created_by          VARCHAR(64),
    updated_by          VARCHAR(64),
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,
    version             INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cost_records_period ON cost_records(period) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_cost_records_type ON cost_records(cost_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_cost_records_center ON cost_records(cost_center_id) WHERE deleted = 0;
