-- ============================================================
-- Flyway Migration: V1.0.0__init_postgresql.sql
-- Description: 食品溯源系统 - PostgreSQL 初始化建表脚本
-- Target:     PostgreSQL 18
-- Based on:   DatabaseInitConfig.java H2 DDL (converted)
-- Author:     System Auto-Generated
-- Date:       2026-04-05
-- ============================================================
-- 转换规则说明:
--   AUTO_INCREMENT          -> GENERATED ALWAYS AS IDENTITY / SERIAL
--   TINYINT                 -> SMALLINT
--   DATETIME                -> TIMESTAMP / TIMESTAMP WITH TIME ZONE
--   TEXT(长文本)            -> TEXT
--   JSON                    -> JSONB
--   ENGINE=InnoDB 等MySQL语法 -> 删除
--   COMMENT                 -> 使用 COLUMN COMMENT 或表级 COMMENT
--   ON UPDATE CURRENT_TIMESTAMP -> 通过触发器实现（此处省略，由应用层维护）
-- ============================================================

BEGIN;

-- ============================================================
-- 1. 部门表 (departments)
-- ============================================================
CREATE TABLE IF NOT EXISTS departments (
    department_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dept_name        VARCHAR(100) NOT NULL,
    dept_code        VARCHAR(50)  NOT NULL,
    parent_id        BIGINT,
    level            INT          DEFAULT 1,
    status           INT          DEFAULT 1,
    description      VARCHAR(500),
    sort_order       INT          DEFAULT 0,
    employee_count   INT          DEFAULT 0,
    base_salary      DECIMAL(12,2),
    deleted          INTEGER      DEFAULT 0,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    updated_by       BIGINT,

    CONSTRAINT uk_departments_dept_code UNIQUE (dept_code)
);

CREATE INDEX IF NOT EXISTS idx_departments_parent_id   ON departments (parent_id);
CREATE INDEX IF NOT EXISTS idx_departments_status      ON departments (status);
CREATE INDEX IF NOT EXISTS idx_departments_deleted     ON departments (deleted);

COMMENT ON TABLE departments IS '部门表';
COMMENT ON COLUMN departments.department_id IS '部门主键ID';
COMMENT ON COLUMN departments.dept_name IS '部门名称';
COMMENT ON COLUMN departments.dept_code IS '部门编码(唯一)';
COMMENT ON COLUMN departments.parent_id IS '父部门ID';
COMMENT ON COLUMN departments.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 2. 职位表 (positions)
-- ============================================================
CREATE TABLE IF NOT EXISTS positions (
    position_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    position_name      VARCHAR(100) NOT NULL,
    position_code      VARCHAR(50)  NOT NULL,
    department_id      BIGINT,
    level              VARCHAR(50),
    position_level_id  BIGINT,
    description        VARCHAR(500),
    employee_count     INT          DEFAULT 0,
    status             INT          DEFAULT 1,
    deleted            INTEGER      DEFAULT 0,
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_positions_position_code UNIQUE (position_code)
);

CREATE INDEX IF NOT EXISTS idx_positions_department_id    ON positions (department_id);
CREATE INDEX IF NOT EXISTS idx_positions_position_level_id ON positions (position_level_id);
CREATE INDEX IF NOT EXISTS idx_positions_status           ON positions (status);

COMMENT ON TABLE positions IS '职位表';
COMMENT ON COLUMN positions.position_id IS '职位主键ID';
COMMENT ON COLUMN positions.position_name IS '职位名称';
COMMENT ON COLUMN positions.position_code IS '职位编码(唯一)';
COMMENT ON COLUMN positions.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 3. 职级表 (position_levels)
-- ============================================================
CREATE TABLE IF NOT EXISTS position_levels (
    position_level_id  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    level_code         VARCHAR(20)  NOT NULL,
    level_name         VARCHAR(100) NOT NULL,
    level_value        INT          NOT NULL,
    base_salary        DECIMAL(12,2),
    salary_range       VARCHAR(100),
    description        VARCHAR(500),
    sort_order         INT          DEFAULT 0,
    status             SMALLINT     DEFAULT 1,
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted            SMALLINT     DEFAULT 0,
    version            INT          DEFAULT 0,

    CONSTRAINT uk_position_levels_level_code UNIQUE (level_code)
);

CREATE INDEX IF NOT EXISTS idx_position_levels_status ON position_levels (status);

COMMENT ON TABLE position_levels IS '职级表';
COMMENT ON COLUMN position_levels.level_code IS '职级编码(唯一), 如P1-P5';


-- ============================================================
-- 4. 员工表 (employees)
-- ============================================================
CREATE TABLE IF NOT EXISTS employees (
    employee_id        VARCHAR(50)  PRIMARY KEY,
    employee_name      VARCHAR(100) NOT NULL,
    gender             VARCHAR(20),
    employee_code      VARCHAR(50),
    phone              VARCHAR(20),
    email              VARCHAR(100),
    department_id      VARCHAR(50),
    position_id        VARCHAR(50),
    position_level_id  BIGINT,
    status             VARCHAR(20)  DEFAULT 'active',
    store_id           BIGINT,
    hire_date          TIMESTAMP,
    resign_date        TIMESTAMP,
    salary_level       VARCHAR(20),
    base_salary        DECIMAL(12,2) DEFAULT 0.00,
    address            VARCHAR(500),
    id_card            VARCHAR(50),
    bank_card          VARCHAR(50),
    emergency_contact  VARCHAR(100),
    emergency_phone    VARCHAR(20),
    photo_url          VARCHAR(500),
    remark             VARCHAR(500),
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER      DEFAULT 0,

    CONSTRAINT uk_employees_employee_code UNIQUE (employee_code)
);

CREATE INDEX IF NOT EXISTS idx_employees_department_id     ON employees (department_id);
CREATE INDEX IF NOT EXISTS idx_employees_position_id       ON employees (position_id);
CREATE INDEX IF NOT EXISTS idx_employees_position_level_id ON employees (position_level_id);
CREATE INDEX IF NOT EXISTS idx_employees_status            ON employees (status);
CREATE INDEX IF NOT EXISTS idx_employees_phone             ON employees (phone);

COMMENT ON TABLE employees IS '员工表';
COMMENT ON COLUMN employees.employee_id IS '员工主键ID(业务编号)';
COMMENT ON COLUMN employees.employee_name IS '员工姓名';
COMMENT ON COLUMN employees.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 5. 用户表 (users)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username                  VARCHAR(100) NOT NULL,
    password                  VARCHAR(255) NOT NULL,
    name                      VARCHAR(100),
    email                     VARCHAR(100),
    phone                     VARCHAR(20),
    status                    VARCHAR(20)  DEFAULT '1',
    roles                     VARCHAR(500),
    department_id             VARCHAR(50),
    store_id                  VARCHAR(50),
    employee_code             VARCHAR(50),
    role                      VARCHAR(50),
    role_names                VARCHAR(255),
    is_locked                 SMALLINT     DEFAULT 0,
    lock_time                 TIMESTAMP,
    password_error_count      INT          DEFAULT 0,
    last_password_error_time  TIMESTAMP,
    need_change_password      SMALLINT     DEFAULT 0,
    last_password_change_time TIMESTAMP,
    last_login_time           TIMESTAMP,
    last_login_ip             VARCHAR(50),
    avatar                    VARCHAR(500),
    deleted                   SMALLINT     DEFAULT 0,
    created_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(50),
    updated_by                VARCHAR(50),

    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_users_phone          ON users (phone);
CREATE INDEX IF NOT EXISTS idx_users_email          ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_employee_code  ON users (employee_code);
CREATE INDEX IF NOT EXISTS idx_users_status         ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_last_login_time ON users (last_login_time);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.user_id IS '用户主键ID';
COMMENT ON COLUMN users.username IS '登录用户名(唯一)';
COMMENT ON COLUMN users.password IS '加密密码';
COMMENT ON COLUMN users.is_locked IS '是否锁定: 0-未锁定, 1-已锁定';
COMMENT ON COLUMN users.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 6. 角色表 (roles)
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    role_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id_str   VARCHAR(50)  NOT NULL,
    role_name     VARCHAR(100) NOT NULL,
    role_code     VARCHAR(50)  NOT NULL,
    description   VARCHAR(500),
    status        VARCHAR(20)  DEFAULT 'active',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT     DEFAULT 0,
    version       INT          DEFAULT 0,

    CONSTRAINT uk_roles_role_id_str UNIQUE (role_id_str),
    CONSTRAINT uk_roles_role_code UNIQUE (role_code)
);

CREATE INDEX IF NOT EXISTS idx_roles_status ON roles (status);

COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.role_id_str IS '角色标识(如ROLE_ADMIN)';
COMMENT ON COLUMN roles.role_code IS '角色编码(唯一, 如admin/manager)';
COMMENT ON COLUMN roles.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 7. 用户角色关联表 (user_roles)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     VARCHAR(50)  NOT NULL,
    role_id     VARCHAR(50)  NOT NULL,
    deleted     SMALLINT     DEFAULT 0,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles (role_id);

COMMENT ON TABLE user_roles IS '用户角色关联表';
COMMENT ON COLUMN user_roles.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 8. 权限表 (permissions)
-- ============================================================
CREATE TABLE IF NOT EXISTS permissions (
    permission_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    permission_id_str VARCHAR(50)  NOT NULL,
    permission_name   VARCHAR(100) NOT NULL,
    permission_code   VARCHAR(100) NOT NULL,
    resource_type     VARCHAR(50),
    resource_path     VARCHAR(200),
    parent_id         VARCHAR(50),
    description       VARCHAR(500),
    sort_order        INT          DEFAULT 0,
    status            VARCHAR(20)  DEFAULT 'active',
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     DEFAULT 0,

    CONSTRAINT uk_permissions_permission_id_str UNIQUE (permission_id_str),
    CONSTRAINT uk_permissions_permission_code UNIQUE (permission_code)
);

CREATE INDEX IF NOT EXISTS idx_permissions_resource_type ON permissions (resource_type);
CREATE INDEX IF NOT EXISTS idx_permissions_parent_id      ON permissions (parent_id);
CREATE INDEX IF NOT EXISTS idx_permissions_status         ON permissions (status);

COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.permission_id_str IS '权限标识(如PERM_USER_VIEW)';
COMMENT ON COLUMN permissions.permission_code IS '权限编码(唯一, 如user:view)';
COMMENT ON COLUMN permissions.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 9. 角色权限关联表 (role_permissions)
-- ============================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id     VARCHAR(50)  NOT NULL,
    permission_id VARCHAR(50) NOT NULL,
    deleted     SMALLINT     DEFAULT 0,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_role_permissions_role_perm UNIQUE (role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions (permission_id);

COMMENT ON TABLE role_permissions IS '角色权限关联表';
COMMENT ON COLUMN role_permissions.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 10. 用户权限关联表 (user_permissions)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_permissions (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        VARCHAR(50)  NOT NULL,
    permission_id  VARCHAR(50)  NOT NULL,
    deleted        SMALLINT     DEFAULT 0,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_permissions_user_perm UNIQUE (user_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_user_permissions_permission_id ON user_permissions (permission_id);

COMMENT ON TABLE user_permissions IS '用户权限关联表(直接授权)';
COMMENT ON COLUMN user_permissions.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 11. 菜品分类表 (food_category)
-- ============================================================
CREATE TABLE IF NOT EXISTS food_category (
    category_id   VARCHAR(50)  PRIMARY KEY,
    category_name VARCHAR(50)  NOT NULL,
    category_code VARCHAR(50),
    parent_id     VARCHAR(50),
    description   VARCHAR(200),
    sort_order    INT          DEFAULT 0,
    status        VARCHAR(20)  DEFAULT 'active',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50),
    deleted       INTEGER      DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_food_category_parent_id ON food_category (parent_id);
CREATE INDEX IF NOT EXISTS idx_food_category_status    ON food_category (status);

COMMENT ON TABLE food_category IS '菜品分类表';
COMMENT ON COLUMN food_category.category_id IS '分类主键ID';
COMMENT ON COLUMN food_category.category_name IS '分类名称';
COMMENT ON COLUMN food_category.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 12. 菜品/食品表 (food)
-- ============================================================
CREATE TABLE IF NOT EXISTS food (
    food_code        VARCHAR(50)  PRIMARY KEY,
    food_name        VARCHAR(100) NOT NULL,
    food_category    VARCHAR(50),
    food_price       DECIMAL(12,2),
    cost_price       DECIMAL(12,2),
    food_desc        VARCHAR(500),
    food_image       VARCHAR(500),
    food_status      VARCHAR(20)  DEFAULT 'active',
    batch_number     VARCHAR(100),
    trace_code       VARCHAR(100),
    manufacturer     VARCHAR(200),
    production_date  TIMESTAMP,
    expiration_date  TIMESTAMP,
    stock            INT          DEFAULT 999,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_food_food_category ON food (food_category);
CREATE INDEX IF NOT EXISTS idx_food_food_status   ON food (food_status);
CREATE INDEX IF NOT EXISTS idx_food_trace_code    ON food (trace_code);

COMMENT ON TABLE food IS '菜品/食品表';
COMMENT ON COLUMN food.food_code IS '菜品编码(主键)';
COMMENT ON COLUMN food.food_name IS '菜品名称';
COMMENT ON COLUMN food.food_price IS '售价';
COMMENT ON COLUMN food.cost_price IS '成本价';
COMMENT ON COLUMN food.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 13. 产品表 (product)
-- ============================================================
CREATE TABLE IF NOT EXISTS product (
    product_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    code          VARCHAR(50),
    category_id   BIGINT,
    category_name VARCHAR(100),
    unit          VARCHAR(20),
    price         DECIMAL(12,2),
    cost_price    DECIMAL(12,2),
    barcode       VARCHAR(100),
    specification VARCHAR(200),
    origin        VARCHAR(200),
    supplier_id   BIGINT,
    supplier_name VARCHAR(200),
    status        SMALLINT     DEFAULT 1,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT     DEFAULT 0,

    CONSTRAINT uk_product_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_product_category_id ON product (category_id);
CREATE INDEX IF NOT EXISTS idx_product_supplier_id  ON product (supplier_id);
CREATE INDEX IF NOT EXISTS idx_product_barcode      ON product (barcode);
CREATE INDEX IF NOT EXISTS idx_product_status       ON product (status);

COMMENT ON TABLE product IS '产品表(物料/原料)';
COMMENT ON COLUMN product.product_id IS '产品主键ID';
COMMENT ON COLUMN product.code IS '产品编码(唯一)';
COMMENT ON COLUMN product.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 14. 库存表 (inventory)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory (
    inventory_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id       BIGINT,
    product_name     VARCHAR(200),
    warehouse_id     BIGINT,
    warehouse_name   VARCHAR(200),
    store_id         BIGINT,
    store_name       VARCHAR(200),
    current_stock    INT           DEFAULT 0,
    safety_stock     INT           DEFAULT 0,
    unit             VARCHAR(20),
    cost_price       DECIMAL(12,2) DEFAULT 0,
    stock_value      DECIMAL(12,2) DEFAULT 0,
    warning_level    INT           DEFAULT 0,
    inventory_type   INT           DEFAULT 1,
    last_update_time TIMESTAMP,
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER       DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_inventory_product_id   ON inventory (product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse_id  ON inventory (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_store_id      ON inventory (store_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warning_level ON inventory (warning_level);

COMMENT ON TABLE inventory IS '库存表';
COMMENT ON COLUMN inventory.inventory_id IS '库存记录主键ID';
COMMENT ON COLUMN inventory.current_stock IS '当前库存数量';
COMMENT ON COLUMN inventory.safety_stock IS '安全库存阈值';
COMMENT ON COLUMN inventory.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 15. 后厨订单表 (kitchen_order)
-- ============================================================
CREATE TABLE IF NOT EXISTS kitchen_order (
    kitchen_order_id       VARCHAR(50),
    order_id               VARCHAR(50),
    order_number           VARCHAR(50),
    order_type             SMALLINT,
    table_number           VARCHAR(20),
    dish_items             TEXT,
    total_dishes           INT,
    total_amount           DECIMAL(12,2) DEFAULT 0,
    payment_method         VARCHAR(50)   DEFAULT '现金',
    priority               SMALLINT     DEFAULT 0,
    status                 VARCHAR(20)   DEFAULT 'pending',
    receive_time           TIMESTAMP,
    make_start_time        TIMESTAMP,
    make_complete_time     TIMESTAMP,
    serve_time             TIMESTAMP,
    cancel_time            TIMESTAMP,
    cancel_reason          VARCHAR(500),
    chef_id                BIGINT,
    chef_name              VARCHAR(50),
    store_id               BIGINT,
    store_name             VARCHAR(100),
    material_consumed      SMALLINT     DEFAULT 0,
    material_consume_time  TIMESTAMP,
    material_locked        SMALLINT     DEFAULT 0,
    material_lock_time     TIMESTAMP,
    food_trace_codes       TEXT,
    remark                 VARCHAR(500),
    tray_id                BIGINT,
    tray_code              VARCHAR(50),
    tray_bind_time         TIMESTAMP,
    created_at             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(50),
    updated_by             VARCHAR(50),
    transaction_id         VARCHAR(100),
    deleted                SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_kitchen_order_order_id     ON kitchen_order (order_id);
CREATE INDEX IF NOT EXISTS idx_kitchen_order_order_number  ON kitchen_order (order_number);
CREATE INDEX IF NOT EXISTS idx_kitchen_order_status        ON kitchen_order (status);
CREATE INDEX IF NOT EXISTS idx_kitchen_order_chef_id       ON kitchen_order (chef_id);
CREATE INDEX IF NOT EXISTS idx_kitchen_order_store_id      ON kitchen_order (store_id);
CREATE INDEX IF NOT EXISTS idx_kitchen_order_receive_time  ON kitchen_order (receive_time);
CREATE INDEX IF NOT EXISTS idx_kitchen_order_created_at    ON kitchen_order (created_at);

COMMENT ON TABLE kitchen_order IS '后厨订单表';
COMMENT ON COLUMN kitchen_order.kitchen_order_id IS '后厨订单编号';
COMMENT ON COLUMN kitchen_order.status IS '订单状态: pending/cooking/completed/cancelled';
COMMENT ON COLUMN kitchen_order.dish_items IS '菜品明细JSON';
COMMENT ON COLUMN kitchen_order.deleted IS '逻辑删除: 0-未删除, 1-已删除';
COMMENT ON COLUMN kitchen_order.tray_id IS '托盘ID';
COMMENT ON COLUMN kitchen_order.tray_code IS '托盘码';
COMMENT ON COLUMN kitchen_order.tray_bind_time IS '托盘绑定时间';


-- ============================================================
-- 16. 系统设置表 (sys_settings)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_settings (
    setting_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    setting_group     VARCHAR(50),
    setting_key       VARCHAR(100)  NOT NULL,
    scope_type        VARCHAR(20)   DEFAULT 'global',
    scope_id          VARCHAR(50),
    setting_value     TEXT,
    value_type        VARCHAR(50),
    setting_name      VARCHAR(100),
    setting_desc      VARCHAR(500),
    options           TEXT,
    validation_rules  TEXT,
    sort_order        INT           DEFAULT 0,
    required_permission VARCHAR(100),
    is_enabled        INTEGER       DEFAULT 1,
    is_system         INTEGER       DEFAULT 0,
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    deleted           INTEGER       DEFAULT 0,

    CONSTRAINT uk_sys_settings_setting_key UNIQUE (setting_key, scope_type, scope_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_settings_setting_group ON sys_settings (setting_group);
CREATE INDEX IF NOT EXISTS idx_sys_settings_scope_type    ON sys_settings (scope_type);
CREATE INDEX IF NOT EXISTS idx_sys_settings_is_enabled     ON sys_settings (is_enabled);

COMMENT ON TABLE sys_settings IS '系统设置表';
COMMENT ON COLUMN sys_settings.setting_key IS '设置键(唯一)';
COMMENT ON COLUMN sys_settings.setting_value IS '设置值(JSON/文本)';
COMMENT ON COLUMN sys_settings.scope_type IS '作用域类型: global/store/user';
COMMENT ON COLUMN sys_settings.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 17. 供应商表 (suppliers)
-- ============================================================
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    supplier_id_str VARCHAR(50)  NOT NULL,
    supplier_name VARCHAR(200)  NOT NULL,
    contact_person VARCHAR(100),
    phone          VARCHAR(20),
    address        VARCHAR(500),
    status         VARCHAR(20)   DEFAULT 'active',
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT      DEFAULT 0,
    version        INT           DEFAULT 0,

    CONSTRAINT uk_suppliers_supplier_id_str UNIQUE (supplier_id_str)
);

CREATE INDEX IF NOT EXISTS idx_suppliers_status ON suppliers (status);
CREATE INDEX IF NOT EXISTS idx_suppliers_phone  ON suppliers (phone);

COMMENT ON TABLE suppliers IS '供应商表';
COMMENT ON COLUMN suppliers.supplier_id_str IS '供应商编码(唯一, 如SUP001)';
COMMENT ON COLUMN suppliers.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 18. 采购订单表 (purchase_orders)
-- ============================================================
CREATE TABLE IF NOT EXISTS purchase_orders (
    order_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id_str   VARCHAR(50)  NOT NULL,
    order_no       VARCHAR(100)  NOT NULL,
    supplier_id    VARCHAR(50),
    supplier_name  VARCHAR(200),
    total_amount   DECIMAL(12,2),
    status         VARCHAR(20)   DEFAULT 'pending',
    order_date     DATE,
    expected_date  DATE,
    store_id       VARCHAR(50),
    request_id     VARCHAR(32),
    request_no     VARCHAR(50),
    contract_id    VARCHAR(32),
    contract_no    VARCHAR(50),
    source_type    VARCHAR(20)   DEFAULT 'manual',
    priority       VARCHAR(20)   DEFAULT 'normal',
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT      DEFAULT 0,
    version        INT           DEFAULT 0,

    CONSTRAINT uk_purchase_orders_order_id_str UNIQUE (order_id_str),
    CONSTRAINT uk_purchase_orders_order_no    UNIQUE (order_no)
);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier_id ON purchase_orders (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_status      ON purchase_orders (status);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_order_date  ON purchase_orders (order_date);

COMMENT ON TABLE purchase_orders IS '采购订单表';
COMMENT ON COLUMN purchase_orders.order_id_str IS '采购订单标识(唯一)';
COMMENT ON COLUMN purchase_orders.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 19. 采购订单明细表 (purchase_order_items)
-- ============================================================
CREATE TABLE IF NOT EXISTS purchase_order_items (
    item_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id_str   VARCHAR(50)  NOT NULL,
    order_id      VARCHAR(50)  NOT NULL,
    material_id   VARCHAR(50),
    material_name VARCHAR(200),
    quantity      DECIMAL(12,2),
    unit          VARCHAR(20),
    unit_price    DECIMAL(12,2),
    total_price   DECIMAL(12,2),
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT      DEFAULT 0,

    CONSTRAINT uk_purchase_order_items_item_id_str UNIQUE (item_id_str)
);

CREATE INDEX IF NOT EXISTS idx_purchase_order_items_order_id ON purchase_order_items (order_id);

COMMENT ON TABLE purchase_order_items IS '采购订单明细表';
COMMENT ON COLUMN purchase_order_items.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 20. 物料追溯码表 (material_trace_code)
-- ============================================================
CREATE TABLE IF NOT EXISTS material_trace_code (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trace_code_id       VARCHAR(50)  NOT NULL,
    trace_code          VARCHAR(100)  NOT NULL,
    material_id         VARCHAR(50)  NOT NULL,
    material_name       VARCHAR(200),
    batch_no            VARCHAR(100),
    supplier_id         VARCHAR(50),
    supplier_name       VARCHAR(200),
    purchase_order_id   VARCHAR(50),
    purchase_order_no   VARCHAR(100),
    quantity            DECIMAL(12,2),
    unit                VARCHAR(20),
    production_date     DATE,
    expiry_date         DATE,
    storage_location    VARCHAR(200),
    status              VARCHAR(20)   DEFAULT 'active',
    generate_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    scan_time           TIMESTAMP,
    use_time            TIMESTAMP,
    used_quantity       DECIMAL(12,2),
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      DEFAULT 0,
    version             INT           DEFAULT 0,

    CONSTRAINT uk_material_trace_code_trace_code_id UNIQUE (trace_code_id),
    CONSTRAINT uk_material_trace_code_trace_code   UNIQUE (trace_code)
);

CREATE INDEX IF NOT EXISTS idx_material_trace_code_material_id ON material_trace_code (material_id);
CREATE INDEX IF NOT EXISTS idx_material_trace_code_batch_no    ON material_trace_code (batch_no);
CREATE INDEX IF NOT EXISTS idx_material_trace_code_status      ON material_trace_code (status);

COMMENT ON TABLE material_trace_code IS '物料追溯码表';
COMMENT ON COLUMN material_trace_code.trace_code IS '追溯码(唯一)';
COMMENT ON COLUMN material_trace_code.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 21. 食品追溯码表 (food_trace_code)
-- ============================================================
CREATE TABLE IF NOT EXISTS food_trace_code (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trace_code_id       VARCHAR(50)  NOT NULL,
    trace_code          VARCHAR(100)  NOT NULL,
    order_id            VARCHAR(50)  NOT NULL,
    order_no            VARCHAR(100),
    dish_id             VARCHAR(50),
    dish_name           VARCHAR(200),
    material_trace_codes TEXT,
    kitchen_station     VARCHAR(100),
    chef_id             VARCHAR(50),
    chef_name           VARCHAR(100),
    status              VARCHAR(20)   DEFAULT 'active',
    generate_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    print_time          TIMESTAMP,
    scan_time           TIMESTAMP,
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      DEFAULT 0,
    version             INT           DEFAULT 0,

    CONSTRAINT uk_food_trace_code_trace_code_id UNIQUE (trace_code_id),
    CONSTRAINT uk_food_trace_code_trace_code   UNIQUE (trace_code)
);

CREATE INDEX IF NOT EXISTS idx_food_trace_code_order_id ON food_trace_code (order_id);
CREATE INDEX IF NOT EXISTS idx_food_trace_code_dish_id  ON food_trace_code (dish_id);
CREATE INDEX IF NOT EXISTS idx_food_trace_code_status  ON food_trace_code (status);

COMMENT ON TABLE food_trace_code IS '食品追溯码表';
COMMENT ON COLUMN food_trace_code.trace_code IS '食品追溯码(唯一)';
COMMENT ON COLUMN food_trace_code.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 22. 物料消耗表 (material_consumption)
-- ============================================================
CREATE TABLE IF NOT EXISTS material_consumption (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consumption_id        VARCHAR(50)  NOT NULL,
    kitchen_order_id      VARCHAR(50)  NOT NULL,
    material_trace_code_id VARCHAR(50) NOT NULL,
    material_id           VARCHAR(50),
    material_name         VARCHAR(200),
    quantity              DECIMAL(12,2),
    unit                  VARCHAR(20),
    chef_id               VARCHAR(50),
    chef_name             VARCHAR(100),
    consumption_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_at            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted               SMALLINT      DEFAULT 0,
    version               INT           DEFAULT 0,

    CONSTRAINT uk_material_consumption_consumption_id UNIQUE (consumption_id)
);

CREATE INDEX IF NOT EXISTS idx_material_consumption_kitchen_order_id ON material_consumption (kitchen_order_id);
CREATE INDEX IF NOT EXISTS idx_material_consumption_material_id      ON material_consumption (material_id);

COMMENT ON TABLE material_consumption IS '物料消耗记录表';
COMMENT ON COLUMN material_consumption.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 23. 菜谱配方表 (dish_recipe)
-- ============================================================
CREATE TABLE IF NOT EXISTS dish_recipe (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id     VARCHAR(50)  NOT NULL,
    dish_id       VARCHAR(50)  NOT NULL,
    dish_name     VARCHAR(200),
    material_id   VARCHAR(50)  NOT NULL,
    material_name VARCHAR(200),
    quantity      DECIMAL(12,2),
    unit          VARCHAR(20),
    is_required   SMALLINT     DEFAULT 1,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT     DEFAULT 0,
    version       INT          DEFAULT 0,

    CONSTRAINT uk_dish_recipe_recipe_id UNIQUE (recipe_id)
);

CREATE INDEX IF NOT EXISTS idx_dish_recipe_dish_id     ON dish_recipe (dish_id);
CREATE INDEX IF NOT EXISTS idx_dish_recipe_material_id ON dish_recipe (material_id);

COMMENT ON TABLE dish_recipe IS '菜谱配方表(菜品-物料关联)';
COMMENT ON COLUMN dish_recipe.is_required IS '是否必需: 0-非必需, 1-必需';
COMMENT ON COLUMN dish_recipe.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 24. 成本记录表 (cost_record)
-- ============================================================
CREATE TABLE IF NOT EXISTS cost_record (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cost_id        VARCHAR(50)  NOT NULL,
    cost_type      VARCHAR(50)  NOT NULL,
    cost_category  VARCHAR(100),
    amount         DECIMAL(12,2) NOT NULL,
    business_id    VARCHAR(50),
    business_type  VARCHAR(50),
    business_no    VARCHAR(100),
    description    VARCHAR(500),
    record_date    DATE,
    store_id       VARCHAR(50),
    operator_id    VARCHAR(50),
    operator_name  VARCHAR(100),
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT      DEFAULT 0,
    version        INT           DEFAULT 0,

    CONSTRAINT uk_cost_record_cost_id UNIQUE (cost_id)
);

CREATE INDEX IF NOT EXISTS idx_cost_record_cost_type     ON cost_record (cost_type);
CREATE INDEX IF NOT EXISTS idx_cost_record_record_date   ON cost_record (record_date);
CREATE INDEX IF NOT EXISTS idx_cost_record_business_type ON cost_record (business_type);

COMMENT ON TABLE cost_record IS '成本记录表';
COMMENT ON COLUMN cost_record.cost_id IS '成本记录标识(唯一)';
COMMENT ON COLUMN cost_record.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 25. 库存流水表 (inventory_transactions)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id   VARCHAR(50)  NOT NULL,
    inventory_id     VARCHAR(50)  NOT NULL,
    material_id      VARCHAR(50),
    transaction_type VARCHAR(50),
    quantity         DECIMAL(12,2),
    before_quantity  DECIMAL(12,2),
    after_quantity   DECIMAL(12,2),
    business_id      VARCHAR(50),
    business_type    VARCHAR(50),
    store_id         VARCHAR(50),
    operator_id      VARCHAR(50),
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT      DEFAULT 0,

    CONSTRAINT uk_inventory_transactions_transaction_id UNIQUE (transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_transactions_inventory_id     ON inventory_transactions (inventory_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_transaction_type ON inventory_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_business_id       ON inventory_transactions (business_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_created_at       ON inventory_transactions (created_at);

COMMENT ON TABLE inventory_transactions IS '库存流水表(出入库记录)';
COMMENT ON COLUMN inventory_transactions.transaction_type IS '交易类型: inbound/outbound/adjust/check';
COMMENT ON COLUMN inventory_transactions.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 26. 财务记录表 (finance_records)
-- ============================================================
CREATE TABLE IF NOT EXISTS finance_records (
    record_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    record_id_str VARCHAR(50)  NOT NULL,
    type          VARCHAR(50)  NOT NULL,
    category      VARCHAR(100),
    amount        DECIMAL(12,2) NOT NULL,
    business_id   VARCHAR(50),
    business_type VARCHAR(50),
    business_no   VARCHAR(100),
    description   VARCHAR(500),
    record_date   TIMESTAMP,
    store_id      VARCHAR(50),
    operator_id   VARCHAR(50),
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT      DEFAULT 0,
    version       INT           DEFAULT 0,

    CONSTRAINT uk_finance_records_record_id_str UNIQUE (record_id_str)
);

CREATE INDEX IF NOT EXISTS idx_finance_records_type         ON finance_records (type);
CREATE INDEX IF NOT EXISTS idx_finance_records_category      ON finance_records (category);
CREATE INDEX IF NOT EXISTS idx_finance_records_record_date   ON finance_records (record_date);
CREATE INDEX IF NOT EXISTS idx_finance_records_business_type ON finance_records (business_type);

COMMENT ON TABLE finance_records IS '财务记录表';
COMMENT ON COLUMN finance_records.type IS '财务类型: income/expense';
COMMENT ON COLUMN finance_records.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 27. 工资记录表 (salary_records)
-- ============================================================
CREATE TABLE IF NOT EXISTS salary_records (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    salary_id_str  VARCHAR(50)  NOT NULL,
    employee_id    VARCHAR(50)  NOT NULL,
    employee_name  VARCHAR(100),
    base_salary    DECIMAL(12,2),
    bonus          DECIMAL(12,2),
    deduction      DECIMAL(12,2),
    actual_salary  DECIMAL(12,2),
    salary_month   VARCHAR(20),
    pay_time       TIMESTAMP,
    status         VARCHAR(20)   DEFAULT 'pending',
    store_id       VARCHAR(50),
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT      DEFAULT 0,
    version        INT           DEFAULT 0,

    CONSTRAINT uk_salary_records_salary_id_str UNIQUE (salary_id_str)
);

CREATE INDEX IF NOT EXISTS idx_salary_records_employee_id   ON salary_records (employee_id);
CREATE INDEX IF NOT EXISTS idx_salary_records_salary_month  ON salary_records (salary_month);
CREATE INDEX IF NOT EXISTS idx_salary_records_status        ON salary_records (status);

COMMENT ON TABLE salary_records IS '工资记录表';
COMMENT ON COLUMN salary_records.salary_month IS '工资月份, 如2026-04';
COMMENT ON COLUMN salary_records.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 28. 设备表 (devices)
-- ============================================================
CREATE TABLE IF NOT EXISTS devices (
    device_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id_str      VARCHAR(50)  NOT NULL,
    device_name        VARCHAR(200) NOT NULL,
    device_type        VARCHAR(50),
    device_model       VARCHAR(100),
    serial_number      VARCHAR(100),
    connection_type    VARCHAR(50),
    connection_config  JSONB,
    status             VARCHAR(20)   DEFAULT 'offline',
    store_id           VARCHAR(50),
    last_online_time   TIMESTAMP,
    created_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted            SMALLINT      DEFAULT 0,
    version            INT           DEFAULT 0,

    CONSTRAINT uk_devices_device_id_str UNIQUE (device_id_str),
    CONSTRAINT uk_devices_serial_number  UNIQUE (serial_number)
);

CREATE INDEX IF NOT EXISTS idx_devices_device_type ON devices (device_type);
CREATE INDEX IF NOT EXISTS idx_devices_status      ON devices (status);
CREATE INDEX IF NOT EXISTS idx_devices_store_id    ON devices (store_id);

COMMENT ON TABLE devices IS '设备表';
COMMENT ON COLUMN devices.connection_config IS '连接配置(JSON格式, 使用JSONB存储)';
COMMENT ON COLUMN devices.status IS '设备状态: online/offline/error/maintenance';
COMMENT ON COLUMN devices.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 29. 设备状态日志表 (device_status_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS device_status_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    log_id_str   VARCHAR(50)  NOT NULL,
    device_id    VARCHAR(50)  NOT NULL,
    status       VARCHAR(20),
    message      VARCHAR(500),
    log_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     DEFAULT 0,

    CONSTRAINT uk_device_status_log_log_id_str UNIQUE (log_id_str)
);

CREATE INDEX IF NOT EXISTS idx_device_status_log_device_id ON device_status_log (device_id);
CREATE INDEX IF NOT EXISTS idx_device_status_log_log_time  ON device_status_log (log_time);

COMMENT ON TABLE device_status_log IS '设备状态日志表';
COMMENT ON COLUMN device_status_log.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 30. 促销活动表 (promotion)
-- ============================================================
CREATE TABLE IF NOT EXISTS promotion (
    promotion_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    type           SMALLINT     DEFAULT 1,
    discount_type  SMALLINT,
    discount_value DECIMAL(10,2),
    min_amount     DECIMAL(10,2),
    max_discount   DECIMAL(10,2),
    start_time     TIMESTAMP,
    end_time       TIMESTAMP,
    status         SMALLINT     DEFAULT 1,
    is_featured    SMALLINT     DEFAULT 0,
    sort_order     INT          DEFAULT 0,
    image_url      VARCHAR(500),
    banner_url     VARCHAR(500),
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_promotion_status     ON promotion (status);
CREATE INDEX IF NOT EXISTS idx_promotion_start_time ON promotion (start_time);
CREATE INDEX IF NOT EXISTS idx_promotion_end_time   ON promotion (end_time);

COMMENT ON TABLE promotion IS '促销活动表';
COMMENT ON COLUMN promotion.type IS '促销类型: 1-折扣 2-满减 3-赠品';
COMMENT ON COLUMN promotion.discount_type IS '折扣方式: 1-百分比 2-固定金额';
COMMENT ON COLUMN promotion.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 31. 套餐表 (dish_combo)
-- ============================================================
CREATE TABLE IF NOT EXISTS dish_combo (
    combo_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_code VARCHAR(50),
    combo_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price       DECIMAL(10,2),
    status      VARCHAR(20)  DEFAULT 'active',
    image_url   VARCHAR(500),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted     INTEGER      DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dish_combo_combo_code ON dish_combo (combo_code);
CREATE INDEX IF NOT EXISTS idx_dish_combo_status     ON dish_combo (status);

COMMENT ON TABLE dish_combo IS '套餐表';
COMMENT ON COLUMN dish_combo.combo_code IS '套餐编码';
COMMENT ON COLUMN dish_combo.combo_name IS '套餐名称';
COMMENT ON COLUMN dish_combo.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 32. 套餐库存关联表 (combo_inventory)
-- ============================================================
CREATE TABLE IF NOT EXISTS combo_inventory (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_id       BIGINT         NOT NULL,
    inventory_id   BIGINT         NOT NULL,
    inventory_name VARCHAR(100)   NOT NULL,
    quantity       DECIMAL(10,2)  NOT NULL,
    unit           VARCHAR(20)    NOT NULL,
    remark         VARCHAR(500),
    created_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50),
    deleted        INTEGER        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_combo_inventory_combo_id     ON combo_inventory (combo_id);
CREATE INDEX IF NOT EXISTS idx_combo_inventory_inventory_id ON combo_inventory (inventory_id);

COMMENT ON TABLE combo_inventory IS '套餐-库存关联表';
COMMENT ON COLUMN combo_inventory.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 33. 菜品库存关联表 (dish_inventory)
-- ============================================================
CREATE TABLE IF NOT EXISTS dish_inventory (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dish_id        VARCHAR(50)   NOT NULL,
    inventory_id   BIGINT         NOT NULL,
    inventory_name VARCHAR(100),
    quantity       DECIMAL(10,2)  NOT NULL,
    unit           VARCHAR(20),
    remark         VARCHAR(500),
    created_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50),
    deleted        INTEGER        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dish_inventory_dish_id      ON dish_inventory (dish_id);
CREATE INDEX IF NOT EXISTS idx_dish_inventory_inventory_id ON dish_inventory (inventory_id);

COMMENT ON TABLE dish_inventory IS '菜品-库存关联表';
COMMENT ON COLUMN dish_inventory.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 34. 操作日志表 (sys_operation_logs)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_operation_logs (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operation_type  VARCHAR(50),
    operation_module VARCHAR(100),
    operation_desc  VARCHAR(500),
    request_method  VARCHAR(10),
    request_url     VARCHAR(500),
    request_params  TEXT,
    response_result TEXT,
    status          VARCHAR(20),
    error_msg       TEXT,
    operator_id     VARCHAR(50),
    operator_name   VARCHAR(100),
    operator_ip     VARCHAR(50),
    operation_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    duration        BIGINT,
    business_id     VARCHAR(100),
    deleted         SMALLINT      DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sys_operation_logs_operator_id    ON sys_operation_logs (operator_id);
CREATE INDEX IF NOT EXISTS idx_sys_operation_logs_operation_type ON sys_operation_logs (operation_type);
CREATE INDEX IF NOT EXISTS idx_sys_operation_logs_operation_time ON sys_operation_logs (operation_time);
CREATE INDEX IF NOT EXISTS idx_sys_operation_logs_business_id     ON sys_operation_logs (business_id);

COMMENT ON TABLE sys_operation_logs IS '系统操作日志表';
COMMENT ON COLUMN sys_operation_logs.operation_time IS '操作时间';
COMMENT ON COLUMN sys_operation_logs.duration IS '执行耗时(毫秒)';
COMMENT ON COLUMN sys_operation_logs.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 35. 登录日志表 (login_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS login_log (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        VARCHAR(50),
    username       VARCHAR(100),
    login_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    login_ip       VARCHAR(50),
    login_location VARCHAR(200),
    browser        VARCHAR(100),
    os             VARCHAR(100),
    login_status   VARCHAR(20),
    login_msg      VARCHAR(500),
    logout_time    TIMESTAMP,
    deleted        SMALLINT      DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_login_log_user_id    ON login_log (user_id);
CREATE INDEX IF NOT EXISTS idx_login_log_login_time  ON login_log (login_time);
CREATE INDEX IF NOT EXISTS idx_login_log_login_status ON login_log (login_status);

COMMENT ON TABLE login_log IS '登录日志表';
COMMENT ON COLUMN login_log.login_status IS '登录状态: success/failure/locked';
COMMENT ON COLUMN login_log.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 36. 密码重置日志表 (password_reset_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS password_reset_log (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       VARCHAR(50),
    username      VARCHAR(100),
    reset_type    VARCHAR(50),
    reset_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    reset_ip      VARCHAR(50),
    reset_status  VARCHAR(20),
    reset_msg     VARCHAR(500),
    operator_id   VARCHAR(50),
    operator_name VARCHAR(100),
    deleted       SMALLINT      DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_password_reset_log_user_id      ON password_reset_log (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_log_reset_time    ON password_reset_log (reset_time);
CREATE INDEX IF NOT EXISTS idx_password_reset_log_reset_status  ON password_reset_log (reset_status);

COMMENT ON TABLE password_reset_log IS '密码重置日志表';
COMMENT ON COLUMN password_reset_log.reset_type IS '重置类型: email/phone/admin';
COMMENT ON COLUMN password_reset_log.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 37. 定时任务表 (scheduled_task)
-- ============================================================
CREATE TABLE IF NOT EXISTS scheduled_task (
    task_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_name            VARCHAR(200) NOT NULL,
    task_code            VARCHAR(100) NOT NULL,
    description          TEXT,
    task_group           VARCHAR(100)   DEFAULT 'default',
    job_handler          VARCHAR(500),
    cron_expression      VARCHAR(100),
    interval_seconds     INT            DEFAULT 0,
    task_type            SMALLINT       DEFAULT 1,
    status               SMALLINT       DEFAULT 1,
    execution_status     VARCHAR(50)    DEFAULT 'idle',
    concurrent_policy    SMALLINT       DEFAULT 0,
    max_retry_count      INT            DEFAULT 3,
    retry_interval_sec   INT            DEFAULT 5,
    timeout_seconds      INT            DEFAULT 300,
    misfire_policy       VARCHAR(50)    DEFAULT 'fire_once',
    last_execution_time  TIMESTAMP,
    next_execution_time  TIMESTAMP,
    last_execution_msg   TEXT,
    is_builtin           SMALLINT       DEFAULT 0,
    version              INT            DEFAULT 0,
    deleted              SMALLINT       DEFAULT 0,
    created_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    create_user_id       BIGINT,
    create_username      VARCHAR(100),
    update_user_id       BIGINT,
    update_username      VARCHAR(100),

    CONSTRAINT uk_scheduled_task_task_code UNIQUE (task_code)
);

CREATE INDEX IF NOT EXISTS idx_scheduled_task_task_group  ON scheduled_task (task_group);
CREATE INDEX IF NOT EXISTS idx_scheduled_task_status       ON scheduled_task (status);
CREATE INDEX IF NOT EXISTS idx_scheduled_task_execution_status ON scheduled_task (execution_status);

COMMENT ON TABLE scheduled_task IS '定时任务表';
COMMENT ON COLUMN scheduled_task.task_code IS '任务编码(唯一)';
COMMENT ON COLUMN scheduled_task.cron_expression IS 'Cron表达式';
COMMENT ON COLUMN scheduled_task.execution_status IS '执行状态: idle/running/paused/error';
COMMENT ON COLUMN scheduled_task.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 38. 全局配置表 (global_config)
-- ============================================================
CREATE TABLE IF NOT EXISTS global_config (
    config_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(50),
    description VARCHAR(500),
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      DEFAULT 0,
    version     INT           DEFAULT 0,

    CONSTRAINT uk_global_config_config_key UNIQUE (config_key)
);

COMMENT ON TABLE global_config IS '全局配置表';
COMMENT ON COLUMN global_config.config_key IS '配置键(唯一)';
COMMENT ON COLUMN global_config.config_value IS '配置值';
COMMENT ON COLUMN global_config.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 39. 审计日志表 (audit_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          VARCHAR(50),
    username         VARCHAR(100),
    operation        VARCHAR(100),
    module           VARCHAR(100),
    ip               VARCHAR(50),
    request_url      VARCHAR(500),
    request_method   VARCHAR(10),
    request_params   TEXT,
    response_status  VARCHAR(20),
    error_message    TEXT,
    execution_time   BIGINT,
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT       DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_audit_log_user_id      ON audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_operation     ON audit_log (operation);
CREATE INDEX IF NOT EXISTS idx_audit_log_module        ON audit_log (module);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at    ON audit_log (created_at);

COMMENT ON TABLE audit_log IS '审计日志表';
COMMENT ON COLUMN audit_log.execution_time IS '执行耗时(毫秒)';
COMMENT ON COLUMN audit_log.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 40. 标签模板表 (label_template)
-- ============================================================
CREATE TABLE IF NOT EXISTS label_template (
    id                VARCHAR(50) PRIMARY KEY,
    name              VARCHAR(100),
    template_name     VARCHAR(100),
    template_code     VARCHAR(50),
    template_type     VARCHAR(20)   DEFAULT 'FOOD',
    description       VARCHAR(500),
    category          VARCHAR(50),
    size              TEXT,
    background_color  VARCHAR(20),
    label_width       INT           DEFAULT 40,
    label_height      INT           DEFAULT 30,
    dpi               INT           DEFAULT 300,
    gap_size          INT           DEFAULT 2,
    print_speed       INT           DEFAULT 3,
    print_density     INT           DEFAULT 12,
    direction         INT           DEFAULT 1,
    sort_order        INT           DEFAULT 0,
    elements          TEXT,
    layout_config     TEXT,
    enabled           BOOLEAN       DEFAULT TRUE,
    is_default        BOOLEAN       DEFAULT FALSE,
    is_active         BOOLEAN       DEFAULT TRUE,
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),
    version           INT           DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_label_template_template_type ON label_template (template_type);
CREATE INDEX IF NOT EXISTS idx_label_template_enabled        ON label_template (enabled);
CREATE INDEX IF NOT EXISTS idx_label_template_is_default     ON label_template (is_default);

COMMENT ON TABLE label_template IS '标签模板表';
COMMENT ON COLUMN label_template.template_type IS '模板类型: FOOD/MATERIAL/PRODUCT';
COMMENT ON COLUMN label_template.elements IS '标签元素配置(JSON)';
COMMENT ON COLUMN label_template.layout_config IS '布局配置(JSON)';


-- ============================================================
-- 41. 采购申请表 (purchase_request)
-- ============================================================
CREATE TABLE IF NOT EXISTS purchase_request (
    request_id     VARCHAR(32) PRIMARY KEY,
    request_no     VARCHAR(50)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    request_type   VARCHAR(20)  NOT NULL DEFAULT 'routine',
    department_id  VARCHAR(32),
    department_name VARCHAR(100),
    applicant_id   VARCHAR(32),
    applicant_name VARCHAR(50),
    total_amount   DECIMAL(12,2) DEFAULT 0.00,
    status         VARCHAR(20)  NOT NULL DEFAULT 'draft',
    priority       VARCHAR(20)  DEFAULT 'normal',
    expected_date  DATE,
    description    TEXT,
    reject_reason  TEXT,
    approved_by    VARCHAR(50),
    approved_time  TIMESTAMP,
    budget_id      VARCHAR(32),
    budget_status  VARCHAR(20),
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50),
    deleted        SMALLINT      DEFAULT 0,
    deleted_by     VARCHAR(50),
    deleted_time   TIMESTAMP,

    CONSTRAINT uk_purchase_request_request_no UNIQUE (request_no)
);

CREATE INDEX IF NOT EXISTS idx_purchase_request_status        ON purchase_request (status);
CREATE INDEX IF NOT EXISTS idx_purchase_request_applicant_id  ON purchase_request (applicant_id);
CREATE INDEX IF NOT EXISTS idx_purchase_request_department_id ON purchase_request (department_id);
CREATE INDEX IF NOT EXISTS idx_purchase_request_created_at    ON purchase_request (created_at);

COMMENT ON TABLE purchase_request IS '采购申请表';
COMMENT ON COLUMN purchase_request.request_id IS '申请主键ID';
COMMENT ON COLUMN purchase_request.status IS '状态: draft/pending/approved/rejected/completed';
COMMENT ON COLUMN purchase_request.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 42. 采购申请明细表 (purchase_request_item)
-- ============================================================
CREATE TABLE IF NOT EXISTS purchase_request_item (
    item_id            VARCHAR(32) PRIMARY KEY,
    request_id         VARCHAR(32)  NOT NULL,
    food_id            VARCHAR(32),
    food_name          VARCHAR(100) NOT NULL,
    food_code          VARCHAR(50),
    specification      VARCHAR(100),
    quantity           DECIMAL(10,2) NOT NULL,
    unit               VARCHAR(20),
    estimated_price    DECIMAL(10,2),
    subtotal_amount    DECIMAL(12,2),
    remark             VARCHAR(500),
    created_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted            SMALLINT      DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_purchase_request_item_request_id ON purchase_request_item (request_id);
CREATE INDEX IF NOT EXISTS idx_purchase_request_item_food_id     ON purchase_request_item (food_id);

COMMENT ON TABLE purchase_request_item IS '采购申请明细表';
COMMENT ON COLUMN purchase_request_item.item_id IS '明细主键ID';
COMMENT ON COLUMN purchase_request_item.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 43. 其他入库单表 (other_inbound)
-- ============================================================
CREATE TABLE IF NOT EXISTS other_inbound (
    inbound_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inbound_no   VARCHAR(50)  NOT NULL,
    inbound_type VARCHAR(50),
    product_id   BIGINT,
    product_name VARCHAR(200),
    quantity     INT          DEFAULT 0,
    unit         VARCHAR(20),
    warehouse_id BIGINT,
    warehouse_name VARCHAR(100),
    inbound_time TIMESTAMP,
    operator_id  BIGINT,
    operator_name VARCHAR(50),
    source_id    BIGINT,
    return_source VARCHAR(50),
    remark       VARCHAR(500),
    status       INT          DEFAULT 0,
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted      INTEGER       DEFAULT 0,

    CONSTRAINT uk_other_inbound_inbound_no UNIQUE (inbound_no)
);

CREATE INDEX IF NOT EXISTS idx_other_inbound_product_id   ON other_inbound (product_id);
CREATE INDEX IF NOT EXISTS idx_other_inbound_warehouse_id ON other_inbound (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_other_inbound_inbound_time ON other_inbound (inbound_time);

COMMENT ON TABLE other_inbound IS '其他入库单表';
COMMENT ON COLUMN other_inbound.inbound_no IS '入库单号(唯一)';
COMMENT ON COLUMN other_inbound.status IS '状态: 0-待确认 1-已确认 2-已完成';
COMMENT ON COLUMN other_inbound.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 44. 报损出库单表 (loss_outbound)
-- ============================================================
CREATE TABLE IF NOT EXISTS loss_outbound (
    loss_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loss_no      VARCHAR(50)  NOT NULL,
    product_id   BIGINT,
    product_name VARCHAR(200),
    quantity     INT          DEFAULT 0,
    unit         VARCHAR(20),
    warehouse_id BIGINT,
    warehouse_name VARCHAR(100),
    loss_time    TIMESTAMP,
    operator_id  BIGINT,
    operator_name VARCHAR(50),
    loss_reason  VARCHAR(500),
    remark       VARCHAR(500),
    status       INT          DEFAULT 0,
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted      INTEGER       DEFAULT 0,

    CONSTRAINT uk_loss_outbound_loss_no UNIQUE (loss_no)
);

CREATE INDEX IF NOT EXISTS idx_loss_outbound_product_id   ON loss_outbound (product_id);
CREATE INDEX IF NOT EXISTS idx_loss_outbound_warehouse_id ON loss_outbound (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_loss_outbound_loss_time    ON loss_outbound (loss_time);

COMMENT ON TABLE loss_outbound IS '报损出库单表';
COMMENT ON COLUMN loss_outbound.loss_no IS '报损单号(唯一)';
COMMENT ON COLUMN loss_outbound.status IS '状态: 0-待确认 1-已确认 2-已完成';
COMMENT ON COLUMN loss_outbound.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 45. 电子凭证表 (electronic_voucher)
-- ============================================================
CREATE TABLE IF NOT EXISTS electronic_voucher (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL DEFAULT 0,
    voucher_type        VARCHAR(50)    NOT NULL,
    voucher_no          VARCHAR(100),
    source_file_path    VARCHAR(1000),
    source_file_hash    VARCHAR(64),
    source_file_size    BIGINT,
    source_file_type    VARCHAR(20),
    xml_content         TEXT,
    parsed_data         JSONB,
    total_amount        DECIMAL(18,2),
    currency            VARCHAR(20)    DEFAULT 'CNY',
    issue_date          DATE,
    signature_status    SMALLINT       DEFAULT 0,
    verify_status       SMALLINT       DEFAULT 0,
    verify_time         TIMESTAMP,
    verify_message      VARCHAR(500),
    business_id         BIGINT,
    business_type       VARCHAR(50),
    finance_voucher_id  BIGINT,
    archive_id          BIGINT,
    status              SMALLINT       DEFAULT 0,
    version             INT            DEFAULT 0,
    created_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             SMALLINT       DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_electronic_voucher_tenant_type  ON electronic_voucher (tenant_id, voucher_type);
CREATE INDEX IF NOT EXISTS idx_electronic_voucher_tenant_status ON electronic_voucher (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_electronic_voucher_tenant_created ON electronic_voucher (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_electronic_voucher_business_type  ON electronic_voucher (business_type);
CREATE INDEX IF NOT EXISTS idx_electronic_voucher_business_id    ON electronic_voucher (business_id);

COMMENT ON TABLE electronic_voucher IS '电子凭证表';
COMMENT ON COLUMN electronic_voucher.parsed_data IS '解析后的结构化数据(JSONB)';
COMMENT ON COLUMN electronic_voucher.signature_status IS '验签状态: 0-未验签 1-成功 -1-失败';
COMMENT ON COLUMN electronic_voucher.verify_status IS '验真状态: 0-未验真 1-通过 -1-不通过';
COMMENT ON COLUMN electronic_voucher.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 46. 电子发票表 (electronic_invoice)
-- ============================================================
CREATE TABLE IF NOT EXISTS electronic_invoice (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id                BIGINT          NOT NULL DEFAULT 0,
    voucher_id               BIGINT          NOT NULL,
    invoice_type             VARCHAR(50),
    invoice_code             VARCHAR(50),
    invoice_no               VARCHAR(50),
    issue_date               DATE,
    buyer_name               VARCHAR(200),
    buyer_tax_no             VARCHAR(50),
    buyer_address            VARCHAR(200),
    buyer_bank               VARCHAR(200),
    seller_name              VARCHAR(200),
    seller_tax_no            VARCHAR(50),
    seller_address           VARCHAR(200),
    seller_bank              VARCHAR(200),
    total_amount             DECIMAL(18,2),
    tax_amount               DECIMAL(18,2),
    amount_without_tax       DECIMAL(18,2),
    currency                 VARCHAR(20)     DEFAULT 'CNY',
    deductible_status        SMALLINT        DEFAULT 1,
    deductible_amount        DECIMAL(18,2),
    certify_status           SMALLINT        DEFAULT 0,
 certify_time              TIMESTAMP,
    certify_period           VARCHAR(20),
    transfer_out_amount      DECIMAL(18,2)   DEFAULT 0,
    remark                   VARCHAR(1000),
    check_code               VARCHAR(100),
    machine_no               VARCHAR(50),
    payee                    VARCHAR(50),
    checker                  VARCHAR(50),
    issuer                   VARCHAR(50),
    security_code            VARCHAR(500),
    unique_code              VARCHAR(50),
    qr_code                  VARCHAR(500),
    pdf_url                  VARCHAR(1000),
    ofd_url                  VARCHAR(1000),
    xml_url                  VARCHAR(1000),
    created_at               TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_by               BIGINT,
    updated_by               BIGINT,
    deleted                  SMALLINT        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_electronic_invoice_voucher_id  ON electronic_invoice (voucher_id);
CREATE INDEX IF NOT EXISTS idx_electronic_invoice_tenant_id   ON electronic_invoice (tenant_id);
CREATE INDEX IF NOT EXISTS idx_electronic_invoice_invoice_no  ON electronic_invoice (invoice_no);
CREATE INDEX IF NOT EXISTS idx_electronic_invoice_issue_date  ON electronic_invoice (issue_date);

COMMENT ON TABLE electronic_invoice IS '电子发票表';
COMMENT ON COLUMN electronic_invoice.voucher_id IS '关联电子凭证ID';
COMMENT ON COLUMN electronic_invoice.deductible_status IS '抵扣状态: 0-不可抵扣 1-可抵扣';
COMMENT ON COLUMN electronic_invoice.certify_status IS '认证状态: 0-未认证 1-已认证';
COMMENT ON COLUMN electronic_invoice.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 47. 发票明细行项目表 (electronic_invoice_item)
-- ============================================================
CREATE TABLE IF NOT EXISTS electronic_invoice_item (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id        BIGINT          NOT NULL,
    item_no           INT,
    goods_code        VARCHAR(50),
    goods_name        VARCHAR(200),
    specification     VARCHAR(100),
    unit              VARCHAR(20),
    quantity          DECIMAL(18,4),
    unit_price        DECIMAL(18,4),
    amount            DECIMAL(18,2),
    tax_rate          DECIMAL(5,2),
    tax_amount        DECIMAL(18,2),
    total_amount      DECIMAL(18,2),
    discount_amount   DECIMAL(18,2),
    preferential_flag SMALLINT        DEFAULT 0,
    zero_tax_rate_flag VARCHAR(20),
    tenant_id         BIGINT,
    created_by        BIGINT,
    created_at        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_electronic_invoice_item_invoice_id ON electronic_invoice_item (invoice_id);
CREATE INDEX IF NOT EXISTS idx_electronic_invoice_item_tenant_id  ON electronic_invoice_item (tenant_id);

COMMENT ON TABLE electronic_invoice_item IS '发票明细行项目表';
COMMENT ON COLUMN electronic_invoice_item.preferential_flag IS '优惠政策标识';
COMMENT ON COLUMN electronic_invoice_item.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 48. 验签记录表 (voucher_signature_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS voucher_signature_log (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id                 BIGINT          NOT NULL DEFAULT 0,
    voucher_id                BIGINT          NOT NULL,
    sign_result               SMALLINT,
    verify_time               TIMESTAMP,
    signer                    VARCHAR(200),
    certificate_serial         VARCHAR(100),
    certificate_issuer         VARCHAR(200),
    certificate_valid_from     TIMESTAMP,
    certificate_valid_to       TIMESTAMP,
    signature_algorithm       VARCHAR(50),
    error_message             VARCHAR(500),
    created_at                TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted                   SMALLINT        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_voucher_signature_log_tenant_voucher ON voucher_signature_log (tenant_id, voucher_id);
CREATE INDEX IF NOT EXISTS idx_voucher_signature_log_verify_time    ON voucher_signature_log (verify_time);

COMMENT ON TABLE voucher_signature_log IS '验签记录表';
COMMENT ON COLUMN voucher_signature_log.sign_result IS '验签结果: 1-成功 0-失败 -1-异常';
COMMENT ON COLUMN voucher_signature_log.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 49. 铁路电子客票表 (electronic_train_ticket)
-- ============================================================
CREATE TABLE IF NOT EXISTS electronic_train_ticket (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id                BIGINT          NOT NULL DEFAULT 0,
    voucher_id               BIGINT          NOT NULL,
    e_ticket_number          VARCHAR(50),
    electronic_invoice_number VARCHAR(50),
    issue_party              VARCHAR(200),
    issue_party_code         VARCHAR(50),
    date_of_issue            DATE,
    departure_station        VARCHAR(100),
    destination_station      VARCHAR(100),
    train_number             VARCHAR(20),
    travel_date              DATE,
    departure_time           VARCHAR(20),
    seat_level               VARCHAR(50),
    carriage                 VARCHAR(20),
    seat                     VARCHAR(50),
    passenger_name           VARCHAR(100),
    id_number                VARCHAR(50),
    fare                     DECIMAL(18,2),
    tax_rate                 DECIMAL(5,4),
    tax_amount               DECIMAL(18,2),
    total_amount             DECIMAL(18,2),
    buyer_name               VARCHAR(200),
    buyer_tax_no             VARCHAR(50),
    seller_name              VARCHAR(200),
    seller_tax_no            VARCHAR(50),
    remarks                  VARCHAR(500),
    created_at               TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted                  SMALLINT        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_electronic_train_ticket_voucher_id     ON electronic_train_ticket (voucher_id);
CREATE INDEX IF NOT EXISTS idx_electronic_train_ticket_e_ticket_number ON electronic_train_ticket (e_ticket_number);

COMMENT ON TABLE electronic_train_ticket IS '铁路电子客票表';
COMMENT ON COLUMN electronic_train_ticket.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 50. 航空电子客票表 (electronic_flight_ticket)
-- ============================================================
CREATE TABLE IF NOT EXISTS electronic_flight_ticket (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id                BIGINT          NOT NULL DEFAULT 0,
    voucher_id               BIGINT          NOT NULL,
    e_ticket_number          VARCHAR(50),
    electronic_invoice_number VARCHAR(50),
    issue_party              VARCHAR(200),
    issue_date               DATE,
    passenger_name           VARCHAR(100),
    id_number                VARCHAR(50),
    flight_segments          TEXT,
    fare                     DECIMAL(18,2),
    fuel_surcharge           DECIMAL(18,2),
    tax_rate                 DECIMAL(5,4),
    tax_amount               DECIMAL(18,2),
    civil_aviation_fund      DECIMAL(18,2),
    total_amount             DECIMAL(18,2),
    buyer_name               VARCHAR(200),
    buyer_tax_no             VARCHAR(50),
    seller_name              VARCHAR(200),
    seller_tax_no            VARCHAR(50),
    verification_code        VARCHAR(50),
    created_at               TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted                  SMALLINT        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_electronic_flight_ticket_voucher_id     ON electronic_flight_ticket (voucher_id);
CREATE INDEX IF NOT EXISTS idx_electronic_flight_ticket_e_ticket_number ON electronic_flight_ticket (e_ticket_number);

COMMENT ON TABLE electronic_flight_ticket IS '航空电子客票表';
COMMENT ON COLUMN electronic_flight_ticket.flight_segments IS '航段信息JSON';
COMMENT ON COLUMN electronic_flight_ticket.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 51. 银行电子回单表 (electronic_bank_receipt)
-- ============================================================
CREATE TABLE IF NOT EXISTS electronic_bank_receipt (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 0,
    voucher_id          BIGINT          NOT NULL,
    receipt_number      VARCHAR(100),
    issue_date          DATE,
    receipt_type        VARCHAR(50),
    credit_debit_flag   VARCHAR(10),
    cash_transfer_flag  VARCHAR(10),
    bookkeeping_date    DATE,
    bookkeeping_time    VARCHAR(20),
    bookkeeper          VARCHAR(100),
    transaction_amount  DECIMAL(18,2),
    currency            VARCHAR(20)     DEFAULT 'CNY',
    payer_account_name  VARCHAR(200),
    payer_account_number VARCHAR(50),
    payer_opening_bank  VARCHAR(200),
    payee_account_name  VARCHAR(200),
    payee_account_number VARCHAR(50),
    payee_opening_bank  VARCHAR(200),
    source_doc_type     VARCHAR(50),
    source_doc_number   VARCHAR(50),
    transaction_code    VARCHAR(50),
    usage               VARCHAR(500),
    notes               VARCHAR(500),
    identifying_code    VARCHAR(200),
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT        DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_electronic_bank_receipt_voucher_id      ON electronic_bank_receipt (voucher_id);
CREATE INDEX IF NOT EXISTS idx_electronic_bank_receipt_receipt_number  ON electronic_bank_receipt (receipt_number);

COMMENT ON TABLE electronic_bank_receipt IS '银行电子回单表';
COMMENT ON COLUMN electronic_bank_receipt.credit_debit_flag IS '借贷标识: C-借 D-贷';
COMMENT ON COLUMN electronic_bank_receipt.cash_transfer_flag IS '现转标识: C-现 T-转';
COMMENT ON COLUMN electronic_bank_receipt.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 52. 邀请发送记录表 (invitation_send_record)
-- ============================================================
CREATE TABLE IF NOT EXISTS invitation_send_record (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    archive_id       BIGINT,
    invitation_code  VARCHAR(50)   NOT NULL,
    bound_email      VARCHAR(100),
    bound_phone      VARCHAR(20),
    bound_name       VARCHAR(50),
    employee_code    VARCHAR(50),
    preset_username  VARCHAR(50),
    use_count        INT           DEFAULT 0,
    max_use_count    INT           DEFAULT 1,
    status           VARCHAR(20)   DEFAULT 'UNUSED',
    valid_days       INT           DEFAULT 7,
    expire_time      TIMESTAMP,
    used_time        TIMESTAMP,
    used_by          BIGINT,
    send_method      VARCHAR(20)   DEFAULT 'EMAIL',
    send_status      VARCHAR(20)   DEFAULT 'PENDING',
    send_time        TIMESTAMP,
    error_message    TEXT,
    batch_id         VARCHAR(50),
    create_by        BIGINT,
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER        DEFAULT 0,

    CONSTRAINT uk_invitation_send_record_invitation_code UNIQUE (invitation_code)
);

CREATE INDEX IF NOT EXISTS idx_invitation_send_record_archive_id ON invitation_send_record (archive_id);
CREATE INDEX IF NOT EXISTS idx_invitation_send_record_status      ON invitation_send_record (status);
CREATE INDEX IF NOT EXISTS idx_invitation_send_record_expire_time ON invitation_send_record (expire_time);

COMMENT ON TABLE invitation_send_record IS '邀请发送记录表';
COMMENT ON COLUMN invitation_send_record.invitation_code IS '邀请码(唯一)';
COMMENT ON COLUMN invitation_send_record.status IS '状态: UNUSED/USED/EXPIRED';
COMMENT ON COLUMN invitation_send_record.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 53. 入职档案表 (onboarding_archive)
-- ============================================================
CREATE TABLE IF NOT EXISTS onboarding_archive (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id     VARCHAR(50),
    candidate_name   VARCHAR(50),
    email            VARCHAR(100),
    phone            VARCHAR(20),
    id_card          VARCHAR(50),
    position         VARCHAR(100),
    position_level   VARCHAR(20)   DEFAULT 'STAFF',
    department_id    VARCHAR(50),
    department_name  VARCHAR(100),
    role_id          VARCHAR(50),
    role_name        VARCHAR(100),
    expected_salary  DECIMAL(12,2),
    final_salary     DECIMAL(12,2),
    onboard_date     DATE,
    employee_code    VARCHAR(50),
    preset_username  VARCHAR(50),
    invitation_code_id BIGINT,
    user_id          BIGINT,
    status           VARCHAR(20)   DEFAULT 'CREATED',
    create_by        BIGINT,
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER       DEFAULT 0,

    CONSTRAINT uk_onboarding_archive_employee_code UNIQUE (employee_code)
);

CREATE INDEX IF NOT EXISTS idx_onboarding_archive_email  ON onboarding_archive (email);
CREATE INDEX IF NOT EXISTS idx_onboarding_archive_status  ON onboarding_archive (status);

COMMENT ON TABLE onboarding_archive IS '入职档案表';
COMMENT ON COLUMN onboarding_archive.employee_code IS '员工编号(唯一, 入职后绑定)';
COMMENT ON COLUMN onboarding_archive.status IS '状态: CREATED/APPROVED/ONBOARDED/COMPLETED/CANCELLED';
COMMENT ON COLUMN onboarding_archive.deleted IS '逻辑删除: 0-未删除, 1-已删除';


-- ============================================================
-- 54. 审批记录表 (approval_record)
-- ============================================================
CREATE TABLE IF NOT EXISTS approval_record (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    archive_id    BIGINT         NOT NULL,
    step_number   INT            NOT NULL,
    reviewer_id   BIGINT,
    reviewer_name VARCHAR(50),
    action        VARCHAR(20),
    comment       VARCHAR(500),
    action_time   TIMESTAMP,
    status        VARCHAR(20)   DEFAULT 'PENDING',
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_record_archive_id   ON approval_record (archive_id);
CREATE INDEX IF NOT EXISTS idx_approval_record_reviewer_id  ON approval_record (reviewer_id);
CREATE INDEX IF NOT EXISTS idx_approval_record_status      ON approval_record (status);

COMMENT ON TABLE approval_record IS '审批记录表';
COMMENT ON COLUMN approval_record.action IS '审批操作: APPROVE/REJECT/FORWARD/CANCEL';
COMMENT ON COLUMN approval_record.status IS '审批状态: PENDING/APPROVED/REJECTED/SKIPPED';


COMMIT;

-- ============================================================
-- 脚本结束 - 共创建 54 张表
-- ============================================================
-- 核心表清单:
--   1.  departments          部门表
--   2.  positions            职位表
--   3.  position_levels      职级表
--   4.  employees            员工表
--   5.  users                用户表
--   6.  roles                角色表
--   7.  user_roles           用户角色关联表
--   8.  permissions          权限表
--   9.  role_permissions     角色权限关联表
--  10. user_permissions      用户权限关联表
--  11. food_category         菜品分类表
--  12. food                  菜品/食品表
--  13. product               产品表
--  14. inventory             库存表
--  15. kitchen_order         后厨订单表
--  16. sys_settings          系统设置表
--  17. suppliers             供应商表
--  18. purchase_orders       采购订单表
--  19. purchase_order_items  采购订单明细表
--  20. material_trace_code   物料追溯码表
--  21. food_trace_code       食品追溯码表
--  22. material_consumption  物料消耗表
--  23. dish_recipe           菜谱配方表
--  24. cost_record           成本记录表
--  25. inventory_transactions 库存流水表
--  26. finance_records       财务记录表
--  27. salary_records        工资记录表
--  28. devices               设备表
--  29. device_status_log     设备状态日志表
--  30. promotion             促销活动表
--  31. dish_combo            套餐表
--  32. combo_inventory       套餐库存关联表
--  33. dish_inventory        菜品库存关联表
--  34. sys_operation_logs    操作日志表
--  35. login_log             登录日志表
--  36. password_reset_log    密码重置日志表
--  37. scheduled_task        定时任务表
--  38. global_config         全局配置表
--  39. audit_log             审计日志表
--  40. label_template        标签模板表
--  41. purchase_request      采购申请表
--  42. purchase_request_item 采购申请明细表
--  43. other_inbound         其他入库单表
--  44. loss_outbound         报损出库单表
--  45. electronic_voucher    电子凭证表
--  46. electronic_invoice    电子发票表
--  47. electronic_invoice_item 发票明细行项目表
--  48. voucher_signature_log 验签记录表
--  49. electronic_train_ticket  铁路电子客票表
--  50. electronic_flight_ticket 航空电子客票表
--  51. electronic_bank_receipt  银行电子回单表
--  52. invitation_send_record  邀请发送记录表
--  53. onboarding_archive      入职档案表
--  54. approval_record         审批记录表
--
-- PostgreSQL 特性使用:
--   - GENERATED ALWAYS AS IDENTITY (替代AUTO_INCREMENT)
--   - SMALLINT (替代TINYINT)
--   - BOOLEAN (用于enabled/is_active/is_default等字段)
--   - JSONB (用于parsed_data/connection_config等JSON字段)
--   - TIMESTAMP (替代DATETIME)
--   - COMMENT ON (替代MySQL的COLUMN COMMENT)
--   - 约束命名规范: uk_{table}_{field}, idx_{table}_{field}
--   - 所有表包含deleted字段(逻辑删除)
--   - 时间字段统一使用created_at/updated_at
--   - 主键命名: {table}_id 格式
-- ============================================================
