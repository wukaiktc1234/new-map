-- ============================================================
-- 库存预警表建表脚本
-- 对应实体: InventoryWarning (inventory_warning)
-- 对应初始化器: InventoryDatabaseInitializer.createInventoryWarningTable()
--
-- 说明：本脚本仅创建 inventory_warning 主表。
--       预警规则（inventory_warning_rule）和预警处理记录（inventory_warning_record）
--       作为可选扩展表，预留给后续业务扩展使用。
--
-- 注意：H2 开发环境由 InventoryDatabaseInitializer.createInventoryWarningTable 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 库存预警主表（对应实体 InventoryWarning）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_warning (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventory_id    BIGINT,
    product_id      BIGINT,
    product_name    VARCHAR(200),
    warehouse_id    BIGINT,
    warehouse_name   VARCHAR(200),
    current_stock   INTEGER,
    safe_stock      INTEGER,
    warning_type    INTEGER,
    warning_level    INTEGER,
    status           INTEGER      DEFAULT 0,
    handler          VARCHAR(100),
    handle_time      TIMESTAMP,
    handle_remark    VARCHAR(500),
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(100),
    deleted          INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_warning IS '库存预警主表';
COMMENT ON COLUMN inventory_warning.id IS '预警ID（主键，自增）';
COMMENT ON COLUMN inventory_warning.inventory_id IS '库存记录ID';
COMMENT ON COLUMN inventory_warning.product_id IS '产品/物料ID';
COMMENT ON COLUMN inventory_warning.product_name IS '产品名称（冗余）';
COMMENT ON COLUMN inventory_warning.warehouse_id IS '仓库ID';
COMMENT ON COLUMN inventory_warning.warehouse_name IS '仓库名称（冗余）';
COMMENT ON COLUMN inventory_warning.current_stock IS '当前库存';
COMMENT ON COLUMN inventory_warning.safe_stock IS '安全库存';
COMMENT ON COLUMN inventory_warning.warning_type IS '预警类型：1低库存 2高库存 3临期 4过期';
COMMENT ON COLUMN inventory_warning.warning_level IS '预警等级：1低 2中 3高';
COMMENT ON COLUMN inventory_warning.status IS '处理状态：0未处理 1处理中 2已处理';
COMMENT ON COLUMN inventory_warning.handler IS '处理人';
COMMENT ON COLUMN inventory_warning.handle_time IS '处理时间';
COMMENT ON COLUMN inventory_warning.handle_remark IS '处理备注';
COMMENT ON COLUMN inventory_warning.create_time IS '创建时间';
COMMENT ON COLUMN inventory_warning.update_time IS '更新时间';
COMMENT ON COLUMN inventory_warning.update_by IS '更新人';
COMMENT ON COLUMN inventory_warning.deleted IS '逻辑删除：0未删除 1已删除';

-- 普通索引：按库存ID/仓库/预警类型/状态查询
CREATE INDEX IF NOT EXISTS idx_inventory_warning_inventory_id  ON inventory_warning (inventory_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warning_warehouse_id   ON inventory_warning (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warning_warning_type   ON inventory_warning (warning_type);
CREATE INDEX IF NOT EXISTS idx_inventory_warning_status         ON inventory_warning (status);

-- ------------------------------------------------------------
-- 2. 库存预警规则表（可选扩展，预留供后续业务使用）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_warning_rule (
    rule_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_code        VARCHAR(50)  NOT NULL,
    rule_name        VARCHAR(100) NOT NULL,
    rule_type        INTEGER,
    threshold_value DECIMAL(12, 2),
    warning_level    INTEGER,
    enabled          INTEGER      DEFAULT 1,
    remark            VARCHAR(500),
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_warning_rule IS '库存预警规则表（预留扩展）';
COMMENT ON COLUMN inventory_warning_rule.rule_id IS '规则ID（主键，自增）';
COMMENT ON COLUMN inventory_warning_rule.rule_code IS '规则编码（唯一）';
COMMENT ON COLUMN inventory_warning_rule.rule_name IS '规则名称';
COMMENT ON COLUMN inventory_warning_rule.rule_type IS '规则类型：1低库存 2高库存 3临期 4过期';
COMMENT ON COLUMN inventory_warning_rule.threshold_value IS '阈值';
COMMENT ON COLUMN inventory_warning_rule.warning_level IS '预警等级：1低 2中 3高';
COMMENT ON COLUMN inventory_warning_rule.enabled IS '是否启用：1启用 0禁用';
COMMENT ON COLUMN inventory_warning_rule.remark IS '备注';
COMMENT ON COLUMN inventory_warning_rule.create_time IS '创建时间';
COMMENT ON COLUMN inventory_warning_rule.update_time IS '更新时间';
COMMENT ON COLUMN inventory_warning_rule.deleted IS '逻辑删除：0未删除 1已删除';

-- 唯一索引：规则编码
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_warning_rule_rule_code ON inventory_warning_rule (rule_code);
-- 普通索引：按类型/启用状态查询
CREATE INDEX IF NOT EXISTS idx_inventory_warning_rule_rule_type ON inventory_warning_rule (rule_type);
CREATE INDEX IF NOT EXISTS idx_inventory_warning_rule_enabled  ON inventory_warning_rule (enabled);

-- ------------------------------------------------------------
-- 3. 库存预警处理记录表（可选扩展，预留供后续业务使用）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_warning_record (
    record_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warning_id      BIGINT       NOT NULL,
    action_type     INTEGER,
    action_desc      VARCHAR(500),
    operator_id      BIGINT,
    operator_name   VARCHAR(100),
    action_time      TIMESTAMP,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_warning_record IS '库存预警处理记录表（预留扩展）';
COMMENT ON COLUMN inventory_warning_record.record_id IS '记录ID（主键，自增）';
COMMENT ON COLUMN inventory_warning_record.warning_id IS '关联预警ID';
COMMENT ON COLUMN inventory_warning_record.action_type IS '处理类型：1通知 2跟进 3关闭';
COMMENT ON COLUMN inventory_warning_record.action_desc IS '处理描述';
COMMENT ON COLUMN inventory_warning_record.operator_id IS '操作人ID';
COMMENT ON COLUMN inventory_warning_record.operator_name IS '操作人姓名';
COMMENT ON COLUMN inventory_warning_record.action_time IS '处理时间';
COMMENT ON COLUMN inventory_warning_record.create_time IS '创建时间';
COMMENT ON COLUMN inventory_warning_record.update_time IS '更新时间';
COMMENT ON COLUMN inventory_warning_record.deleted IS '逻辑删除：0未删除 1已删除';

-- 普通索引：按预警ID查询
CREATE INDEX IF NOT EXISTS idx_inventory_warning_record_warning_id ON inventory_warning_record (warning_id);
