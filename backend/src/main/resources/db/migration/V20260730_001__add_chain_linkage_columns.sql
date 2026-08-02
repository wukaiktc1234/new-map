-- ============================================================
-- V20260730_001: 补充资产/采购/库存链路字段
-- 背景：
--   为打通“公司初始化 → 部门 → 物料档案 → 采购申请 → 采购订单 →
--   采购收货 → 库存/资产”全链路，补充必要的关联字段与索引。
-- 兼容性：
--   本脚本使用 H2 / PostgreSQL 兼容语法，Flyway 仅执行一次。
-- ============================================================

-- ============================================================
-- 1. 资产主表：补充责任部门、采购订单、供应商、申请单关联
-- ============================================================
ALTER TABLE asset_masters_enhanced
    ADD COLUMN IF NOT EXISTS department_id       BIGINT,
    ADD COLUMN IF NOT EXISTS department_name     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS purchase_order_id   BIGINT,
    ADD COLUMN IF NOT EXISTS purchase_order_no   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS supplier_id         BIGINT,
    ADD COLUMN IF NOT EXISTS supplier_name       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS request_id          VARCHAR(32),
    ADD COLUMN IF NOT EXISTS request_no          VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_asset_masters_department_id     ON asset_masters_enhanced(department_id);
CREATE INDEX IF NOT EXISTS idx_asset_masters_purchase_order_id ON asset_masters_enhanced(purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_asset_masters_supplier_id       ON asset_masters_enhanced(supplier_id);
CREATE INDEX IF NOT EXISTS idx_asset_masters_request_id        ON asset_masters_enhanced(request_id);

COMMENT ON COLUMN asset_masters_enhanced.department_id     IS '责任部门ID';
COMMENT ON COLUMN asset_masters_enhanced.department_name   IS '责任部门名称';
COMMENT ON COLUMN asset_masters_enhanced.purchase_order_id IS '关联采购订单ID';
COMMENT ON COLUMN asset_masters_enhanced.purchase_order_no IS '关联采购订单编号';
COMMENT ON COLUMN asset_masters_enhanced.supplier_id       IS '供应商ID';
COMMENT ON COLUMN asset_masters_enhanced.supplier_name     IS '供应商名称';
COMMENT ON COLUMN asset_masters_enhanced.request_id        IS '关联采购申请ID';
COMMENT ON COLUMN asset_masters_enhanced.request_no        IS '关联采购申请编号';

-- ============================================================
-- 2. 采购申请：统一默认申请类型为物料（material / asset）
-- ============================================================
ALTER TABLE purchase_request
    ALTER COLUMN request_type SET DEFAULT 'material';

COMMENT ON COLUMN purchase_request.request_type IS '申请类型：material(物料) / asset(资产) / routine(常规)';

-- ============================================================
-- 3. 库存主表：补充物料档案关联、状态、乐观锁版本
-- ============================================================
ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS material_id          BIGINT,
    ADD COLUMN IF NOT EXISTS material_code        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS material_category_id BIGINT,
    ADD COLUMN IF NOT EXISTS material_category_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS status               INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS version              INT DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_inventory_material_id          ON inventory(material_id);
CREATE INDEX IF NOT EXISTS idx_inventory_material_category_id ON inventory(material_category_id);
CREATE INDEX IF NOT EXISTS idx_inventory_status               ON inventory(status);

COMMENT ON COLUMN inventory.material_id           IS '物料档案ID';
COMMENT ON COLUMN inventory.material_code         IS '物料编码';
COMMENT ON COLUMN inventory.material_category_id  IS '物料分类ID';
COMMENT ON COLUMN inventory.material_category_name IS '物料分类名称';
COMMENT ON COLUMN inventory.status                IS '库存状态：1正常 2预警 3过期 4冻结';
COMMENT ON COLUMN inventory.version               IS '乐观锁版本号';

-- ============================================================
-- 4. 公司初始化记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS company_init_record (
    record_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_name     VARCHAR(200) NOT NULL,
    company_code     VARCHAR(100),
    legal_person     VARCHAR(100),
    contact_phone    VARCHAR(50),
    address          TEXT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'completed',
    init_version     VARCHAR(20)  DEFAULT '1.0.0',
    remark           TEXT,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT uk_company_init_record_company_code UNIQUE (company_code)
);

CREATE INDEX IF NOT EXISTS idx_company_init_record_status ON company_init_record(status);

COMMENT ON TABLE company_init_record IS '公司初始化记录表';
COMMENT ON COLUMN company_init_record.status IS '初始化状态：completed(已完成) / draft(草稿)';
