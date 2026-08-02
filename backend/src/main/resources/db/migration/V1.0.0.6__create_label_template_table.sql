-- 标签模板表
CREATE TABLE IF NOT EXISTS label_template (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100),
    template_name VARCHAR(100),
    template_code VARCHAR(50),
    template_type VARCHAR(20) DEFAULT 'FOOD',
    label_width INTEGER DEFAULT 40,
    label_height INTEGER DEFAULT 30,
    dpi INTEGER DEFAULT 300,
    gap_size INTEGER DEFAULT 2,
    print_speed INTEGER DEFAULT 3,
    print_density INTEGER DEFAULT 12,
    direction INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    elements TEXT,
    layout_config TEXT,
    enabled SMALLINT DEFAULT 1,
    is_default SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_label_template_type ON label_template(template_type);
CREATE INDEX IF NOT EXISTS idx_label_template_enabled ON label_template(enabled);
CREATE INDEX IF NOT EXISTS idx_label_template_is_default ON label_template(is_default);

COMMENT ON TABLE label_template IS '标签模板配置表';
COMMENT ON COLUMN label_template.id IS '模板ID';
COMMENT ON COLUMN label_template.name IS '模板名称';
COMMENT ON COLUMN label_template.template_name IS '模板显示名称';
COMMENT ON COLUMN label_template.template_code IS '模板编码';
COMMENT ON COLUMN label_template.template_type IS '模板类型: FOOD/MATERIAL/CUSTOM';
COMMENT ON COLUMN label_template.label_width IS '标签宽度(mm)';
COMMENT ON COLUMN label_template.label_height IS '标签高度(mm)';
COMMENT ON COLUMN label_template.dpi IS '打印DPI';
COMMENT ON COLUMN label_template.gap_size IS '间隙(mm)';
COMMENT ON COLUMN label_template.print_speed IS '打印速度';
COMMENT ON COLUMN label_template.print_density IS '打印浓度';
COMMENT ON COLUMN label_template.direction IS '打印方向';
COMMENT ON COLUMN label_template.sort_order IS '排序';
COMMENT ON COLUMN label_template.elements IS '元素配置(JSON)';
COMMENT ON COLUMN label_template.layout_config IS '布局配置(JSON)';
COMMENT ON COLUMN label_template.enabled IS '是否启用';
COMMENT ON COLUMN label_template.is_default IS '是否默认模板';

-- 插入默认模板（使用 ON CONFLICT 实现 PostgreSQL 兼容的幂等插入）
INSERT INTO label_template (id, name, template_name, template_code, template_type, label_width, label_height, dpi, gap_size, print_speed, print_density, direction, elements, enabled, is_default, created_by)
VALUES (
    'default-food-template',
    '食品追溯标签',
    '食品追溯标签',
    'FOOD_TRACE_DEFAULT',
    'FOOD',
    40,
    30,
    300,
    2,
    3,
    12,
    1,
    '[{"id":"material_name","type":"text","x":2,"y":2,"width":25,"height":6,"fontSize":16,"fontWeight":"bold","dataField":"materialName"},{"id":"qrcode","type":"qrcode","x":28,"y":2,"width":10,"height":10,"dataField":"traceCode"},{"id":"store_name","type":"text","x":2,"y":10,"width":36,"height":4,"fontSize":12,"dataField":"storeName","prefix":"门店:"},{"id":"shelf_life","type":"text","x":2,"y":15,"width":18,"height":4,"fontSize":12,"dataField":"shelfLifeDays","prefix":"保质:","suffix":"天"},{"id":"expiry_date","type":"text","x":20,"y":15,"width":18,"height":4,"fontSize":12,"dataField":"expiryDate","prefix":"到期:"},{"id":"supplier_name","type":"text","x":2,"y":20,"width":36,"height":4,"fontSize":12,"dataField":"supplierName","prefix":"供应:"},{"id":"trace_code","type":"text","x":2,"y":25,"width":36,"height":4,"fontSize":12,"dataField":"traceCode"}]',
    1,
    1,
    'system'
)
ON CONFLICT (id) DO NOTHING;
