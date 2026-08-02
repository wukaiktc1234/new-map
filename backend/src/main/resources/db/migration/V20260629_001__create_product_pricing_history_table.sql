-- ============================================================
-- 产品定价历史记录表
-- 版本: V20260629_001
-- 说明: 创建产品定价历史记录表，记录所有价格变动（用于审计和分析）
-- ============================================================

CREATE TABLE IF NOT EXISTS product_pricing_history (
    pricing_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_type      VARCHAR(20)  NOT NULL,
    product_name      VARCHAR(200) NOT NULL,
    product_id        BIGINT       NOT NULL,
    old_sale_price    BIGINT,
    new_sale_price    BIGINT       NOT NULL,
    cost_price        BIGINT,
    pricing_strategy  VARCHAR(30)  DEFAULT 'MANUAL',
    remark            TEXT,
    operator_id       BIGINT,
    operator_name     VARCHAR(100),
    effective_date    DATE,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     DEFAULT 0,

    CONSTRAINT uk_pricing_history_unique UNIQUE (product_type, product_id, effective_date, create_time)
);

CREATE INDEX IF NOT EXISTS idx_pricing_history_product_id     ON product_pricing_history (product_id);
CREATE INDEX IF NOT EXISTS idx_pricing_history_product_type   ON product_pricing_history (product_type);
CREATE INDEX IF NOT EXISTS idx_pricing_history_effective_date ON product_pricing_history (effective_date);
CREATE INDEX IF NOT EXISTS idx_pricing_history_create_time    ON product_pricing_history (create_time);
CREATE INDEX IF NOT EXISTS idx_pricing_history_operator_id    ON product_pricing_history (operator_id);

COMMENT ON TABLE  product_pricing_history IS '产品定价历史记录表（审计）';
COMMENT ON COLUMN product_pricing_history.pricing_id IS '定价记录主键ID';
COMMENT ON COLUMN product_pricing_history.product_type IS '产品类型: FOOD-菜品, COMBO-套餐';
COMMENT ON COLUMN product_pricing_history.product_name IS '产品名称（冗余存储，便于查询）';
COMMENT ON COLUMN product_pricing_history.product_id IS '产品ID（关联 foods.food_id 或 dish_combos.combo_id）';
COMMENT ON COLUMN product_pricing_history.old_sale_price IS '原售价（分）';
COMMENT ON COLUMN product_pricing_history.new_sale_price IS '新售价（分）';
COMMENT ON COLUMN product_pricing_history.cost_price IS '成本价（分，快照）';
COMMENT ON COLUMN product_pricing_history.pricing_strategy IS '定价策略: MANUAL-手动调整, AUTO-自动计算, BATCH-批量调价';
COMMENT ON COLUMN product_pricing_history.remark IS '备注';
COMMENT ON COLUMN product_pricing_history.operator_id IS '操作人ID';
COMMENT ON COLUMN product_pricing_history.operator_name IS '操作人姓名';
COMMENT ON COLUMN product_pricing_history.effective_date IS '生效日期';
COMMENT ON COLUMN product_pricing_history.create_time IS '创建时间';
COMMENT ON COLUMN product_pricing_history.update_time IS '更新时间';
COMMENT ON COLUMN product_pricing_history.deleted IS '逻辑删除: 0-未删除, 1-已删除';
