-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- ============================================================================
-- 食品溯源系统 (Food Traceability System) - PostgreSQL 生产环境建表脚本
-- 版本: v0.12
-- 生成时间: 2026-04-06
-- 说明: 基于 Entity 类自动生成，涵盖全部业务表结构
-- 兼容: PostgreSQL 14+
-- ============================================================================

-- 设置客户端编码
SET client_encoding = 'UTF8';

-- ============================================================================
-- 第一部分：系统核心表（用户、角色、权限、菜单、部门）
-- ============================================================================

-- --------------------------------------------------------------------------
-- 1. 用户表 (users)
-- 对应实体: User.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS users CASCADE;
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    name            VARCHAR(100)    DEFAULT NULL,
    email           VARCHAR(255)    DEFAULT NULL,
    phone           VARCHAR(20)     DEFAULT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT '1',
    roles           TEXT            DEFAULT NULL,
    department_id   VARCHAR(50)     DEFAULT NULL,
    store_id        VARCHAR(50)     DEFAULT NULL,
    employee_code   VARCHAR(50)     DEFAULT NULL,
    is_locked       BOOLEAN         NOT NULL DEFAULT FALSE,
    lock_time       TIMESTAMP       DEFAULT NULL,
    password_error_count  INTEGER   NOT NULL DEFAULT 0,
    last_password_error_time TIMESTAMP DEFAULT NULL,
    need_change_password BOOLEAN     NOT NULL DEFAULT FALSE,
    last_password_change_time TIMESTAMP DEFAULT NULL,
    last_login_time      TIMESTAMP    DEFAULT NULL,
    last_login_ip        VARCHAR(50)  DEFAULT NULL,
    avatar               VARCHAR(500) DEFAULT NULL,
    deleted              INTEGER      NOT NULL DEFAULT 0,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(50)  DEFAULT NULL,
    updated_by           VARCHAR(50)  DEFAULT NULL
);
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID（主键，自增）';
COMMENT ON COLUMN users.username IS '用户名（登录名）';
COMMENT ON COLUMN users.password IS '密码（BCrypt加密）';
COMMENT ON COLUMN users.name IS '用户姓名';
COMMENT ON COLUMN users.email IS '用户邮箱';
COMMENT ON COLUMN users.phone IS '用户手机号';
COMMENT ON COLUMN users.status IS '用户状态（1-正常，0-禁用）';
COMMENT ON COLUMN users.roles IS '角色ID列表（JSON格式）';
COMMENT ON COLUMN users.department_id IS '所属部门ID';
COMMENT ON COLUMN users.store_id IS '所属门店ID';
COMMENT ON COLUMN users.employee_code IS '员工编号';
COMMENT ON COLUMN users.is_locked IS '是否被锁定';
COMMENT ON COLUMN users.deleted IS '逻辑删除标记（0-未删除，1-已删除）';
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_department_id ON users(department_id);
CREATE INDEX idx_users_store_id ON users(store_id);
CREATE INDEX idx_users_status ON users(status);

-- --------------------------------------------------------------------------
-- 2. 用户角色关联表 (user_roles)
-- 对应实体: UserRole.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS user_roles CASCADE;
CREATE TABLE user_roles (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    role_id     VARCHAR(64) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     INTEGER     NOT NULL DEFAULT 0
);
COMMENT ON TABLE user_roles IS '用户角色关联表';
COMMENT ON COLUMN user_roles.user_id IS '用户ID';
COMMENT ON COLUMN user_roles.role_id IS '角色ID';
CREATE INDEX uk_user_roles_user_role ON user_roles(user_id, role_id) WHERE deleted = 0;

-- --------------------------------------------------------------------------
-- 3. 角色表 (roles)
-- 对应实体: Role.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS roles CASCADE;
CREATE TABLE roles (
    id                  VARCHAR(64)     PRIMARY KEY,
    role_code           VARCHAR(100)    NOT NULL UNIQUE,
    role_name           VARCHAR(100)    NOT NULL,
    description         VARCHAR(500)    DEFAULT NULL,
    role_level          INTEGER         DEFAULT 0,
    status              INTEGER         NOT NULL DEFAULT 1,
    permissions         TEXT            DEFAULT NULL,
    role_type           INTEGER         DEFAULT 2,
    is_system           BOOLEAN         NOT NULL DEFAULT FALSE,
    parent_id           VARCHAR(64)     DEFAULT NULL,
    data_scope          VARCHAR(30)     DEFAULT 'self',
    accessible_stores   TEXT            DEFAULT NULL,
    accessible_departments TEXT         DEFAULT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT          DEFAULT NULL,
    updated_by          BIGINT          DEFAULT NULL,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INTEGER         NOT NULL DEFAULT 1,
    ext_data            JSONB           DEFAULT NULL
);
COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.id IS '角色ID（雪花算法生成）';
COMMENT ON COLUMN roles.role_code IS '角色编码（唯一）';
COMMENT ON COLUMN roles.role_name IS '角色名称';
COMMENT ON COLUMN roles.status IS '状态（1-启用，0-禁用）';
COMMENT ON COLUMN roles.role_type IS '角色类型（1-系统角色，2-自定义角色）';
COMMENT ON COLUMN roles.data_scope IS '数据权限范围（all/company/store/department/self）';
COMMENT ON COLUMN roles.deleted IS '逻辑删除标记';
CREATE INDEX idx_roles_role_code ON roles(role_code);
CREATE INDEX idx_roles_status ON roles(status);

-- --------------------------------------------------------------------------
-- 4. 角色权限关联表 (role_permissions)
-- 对应实体: RolePermission.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS role_permissions CASCADE;
CREATE TABLE role_permissions (
    id            BIGSERIAL   PRIMARY KEY,
    role_id       BIGINT      NOT NULL,
    permission_id BIGINT      NOT NULL,
    deleted       INTEGER     NOT NULL DEFAULT 0
);
COMMENT ON TABLE role_permissions IS '角色权限关联表';
CREATE INDEX uk_role_perm_role_perm ON role_permissions(role_id, permission_id) WHERE deleted = 0;

-- --------------------------------------------------------------------------
-- 5. 权限表 (permissions)
-- 对应实体: Permission.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS permissions CASCADE;
CREATE TABLE permissions (
    id              BIGSERIAL       PRIMARY KEY,
    permission_code VARCHAR(100)    NOT NULL UNIQUE,
    permission_name VARCHAR(100)    NOT NULL,
    permission_type INTEGER         NOT NULL DEFAULT 1,
    module          VARCHAR(50)     DEFAULT NULL,
    parent_id       BIGINT          DEFAULT 0,
    level           INTEGER         DEFAULT 1,
    sort_order      INTEGER         DEFAULT 0,
    status          INTEGER         NOT NULL DEFAULT 1,
    icon            VARCHAR(100)    DEFAULT NULL,
    path            VARCHAR(255)    DEFAULT NULL,
    component       VARCHAR(255)    DEFAULT NULL,
    redirect        VARCHAR(255)    DEFAULT NULL,
    is_hidden       INTEGER         NOT NULL DEFAULT 0,
    is_cache        INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    created_by      BIGINT          DEFAULT NULL,
    updated_by      BIGINT          DEFAULT NULL
);
COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.permission_code IS '权限编码';
COMMENT ON COLUMN permissions.permission_name IS '权限名称';
COMMENT ON COLUMN permissions.permission_type IS '权限类型（1-菜单，2-按钮，3-接口）';
CREATE INDEX idx_permissions_parent_id ON permissions(parent_id);
CREATE INDEX idx_permissions_module ON permissions(module);

-- --------------------------------------------------------------------------
-- 6. 菜单表 (menus)
-- 对应实体: Menu.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS menus CASCADE;
CREATE TABLE menus (
    menu_id        VARCHAR(64)      PRIMARY KEY,
    menu_name      VARCHAR(100)     NOT NULL,
    menu_code      VARCHAR(100)     UNIQUE DEFAULT NULL,
    parent_id      VARCHAR(64)      DEFAULT NULL,
    level          INTEGER          DEFAULT 1,
    path           VARCHAR(500)     DEFAULT NULL,
    url            VARCHAR(255)     DEFAULT NULL,
    icon           VARCHAR(100)     DEFAULT NULL,
    sort_order     INTEGER          DEFAULT 0,
    status         VARCHAR(20)      NOT NULL DEFAULT 'active',
    menu_type      VARCHAR(20)      NOT NULL DEFAULT 'menu',
    permission_code VARCHAR(100)    DEFAULT NULL,
    component      VARCHAR(255)     DEFAULT NULL,
    visible        BOOLEAN          NOT NULL DEFAULT TRUE,
    keep_alive     BOOLEAN          NOT NULL DEFAULT FALSE,
    created_time   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50)      DEFAULT NULL,
    updated_by     VARCHAR(50)      DEFAULT NULL,
    deleted        BOOLEAN          NOT NULL DEFAULT FALSE,
    version        INTEGER          NOT NULL DEFAULT 1,
    ext_data       JSONB            DEFAULT NULL
);
COMMENT ON TABLE menus IS '菜单表';
COMMENT ON COLUMN menus.menu_id IS '菜单ID（雪花算法生成）';
COMMENT ON COLUMN menus.menu_name IS '菜单名称';
COMMENT ON COLUMN menus.menu_type IS '菜单类型（directory/menu/button）';
COMMENT ON COLUMN menus.status IS '状态（active/inactive）';
CREATE INDEX idx_menus_parent_id ON menus(parent_id);
CREATE INDEX idx_menus_menu_type ON menus(menu_type);

-- --------------------------------------------------------------------------
-- 7. 部门表 (departments)
-- 对应实体: Department.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS departments CASCADE;
CREATE TABLE departments (
    id              BIGSERIAL       PRIMARY KEY,
    dept_name       VARCHAR(100)    NOT NULL,
    dept_code       VARCHAR(50)     UNIQUE NOT NULL,
    parent_id       BIGINT          DEFAULT NULL,
    level           INTEGER         NOT NULL DEFAULT 1,
    status          INTEGER         NOT NULL DEFAULT 1,
    description     VARCHAR(500)    DEFAULT NULL,
    sort_order      INTEGER         DEFAULT 0,
    employee_count  INTEGER         NOT NULL DEFAULT 0,
    base_salary     DECIMAL(12,2)   DEFAULT NULL,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT          DEFAULT NULL,
    updated_by      BIGINT          DEFAULT NULL
);
COMMENT ON TABLE departments IS '部门表';
COMMENT ON COLUMN departments.id IS '部门ID（主键自增）';
COMMENT ON COLUMN departments.dept_name IS '部门名称';
COMMENT ON COLUMN departments.dept_code IS '部门编码（唯一）';
COMMENT ON COLUMN departments.parent_id IS '父部门ID';
COMMENT ON COLUMN departments.level IS '部门层级';
COMMENT ON COLUMN departments.status IS '状态（1-正常，0-禁用）';
CREATE INDEX idx_departments_parent_id ON departments(parent_id);
CREATE INDEX idx_departments_dept_code ON departments(dept_code);
CREATE INDEX idx_departments_level ON departments(level);

-- --------------------------------------------------------------------------
-- 8. 员工表 (employees)
-- 对应实体: Employee.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS employees CASCADE;
CREATE TABLE employees (
    employee_id        VARCHAR(64)      PRIMARY KEY,
    employee_name      VARCHAR(100)     NOT NULL,
    gender             VARCHAR(20)      DEFAULT NULL,
    employee_code      VARCHAR(50)      UNIQUE NOT NULL,
    phone              VARCHAR(20)      DEFAULT NULL,
    email              VARCHAR(255)     DEFAULT NULL,
    department_id      VARCHAR(64)      DEFAULT NULL,
    position_id        VARCHAR(64)      DEFAULT NULL,
    position_level_id  BIGINT           DEFAULT NULL,
    status             VARCHAR(20)      NOT NULL DEFAULT 'active',
    store_id           VARCHAR(64)      DEFAULT NULL,
    hire_date          TIMESTAMP        DEFAULT NULL,
    resign_date        TIMESTAMP        DEFAULT NULL,
    salary_level       VARCHAR(20)      DEFAULT NULL,
    base_salary        DECIMAL(12,2)    DEFAULT NULL,
    address            VARCHAR(500)     DEFAULT NULL,
    id_card            VARCHAR(18)      DEFAULT NULL,
    bank_card          VARCHAR(30)      DEFAULT NULL,
    emergency_contact  VARCHAR(50)      DEFAULT NULL,
    emergency_phone    VARCHAR(20)      DEFAULT NULL,
    photo_url          VARCHAR(500)     DEFAULT NULL,
    remark             VARCHAR(500)     DEFAULT NULL,
    employment_type    VARCHAR(20)      DEFAULT 'full-time',
    created_time       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER          NOT NULL DEFAULT 0,
    created_by         VARCHAR(50)      DEFAULT NULL,
    updated_by         VARCHAR(50)      DEFAULT NULL
);
COMMENT ON TABLE employees IS '员工表';
COMMENT ON COLUMN employees.employee_id IS '员工ID（UUID主键）';
COMMENT ON COLUMN employees.employee_name IS '员工姓名';
COMMENT ON COLUMN employees.employee_code IS '员工编码（唯一）';
COMMENT ON COLUMN employees.status IS '员工状态（active-在职，inactive-离职，probation-试用期）';
COMMENT ON COLUMN employees.employment_type IS '用工类型（full-time全职，part-time兼职）';
CREATE INDEX idx_employees_employee_code ON employees(employee_code);
CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_store_id ON employees(store_id);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employees_position_id ON employees(position_id);

-- --------------------------------------------------------------------------
-- 9. 职位表 (positions)
-- 对应实体: Position.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS positions CASCADE;
CREATE TABLE positions (
    id                  BIGSERIAL       PRIMARY KEY,
    position_name       VARCHAR(100)    NOT NULL,
    position_code       VARCHAR(50)     UNIQUE NOT NULL,
    department_id       BIGINT          DEFAULT NULL,
    level               VARCHAR(50)     DEFAULT NULL,
    position_level_id   BIGINT          DEFAULT NULL,
    status              INTEGER         NOT NULL DEFAULT 1,
    description         VARCHAR(500)    DEFAULT NULL,
    sort_order          INTEGER         DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50)      DEFAULT NULL,
    updated_by          VARCHAR(50)      DEFAULT NULL,
    deleted             INTEGER         NOT NULL DEFAULT 0
);
COMMENT ON TABLE positions IS '职位表';
COMMENT ON COLUMN positions.id IS '职位ID';
COMMENT ON COLUMN positions.position_name IS '职位名称';
COMMENT ON COLUMN positions.position_code IS '职位编码（唯一）';
CREATE INDEX idx_positions_department_id ON positions(department_id);
CREATE INDEX idx_positions_position_code ON positions(position_code);


-- ============================================================================
-- 第二部分：食品管理与溯源表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 10. 食品分类表 (food_category)
-- 对应实体: FoodCategory.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS food_category CASCADE;
CREATE TABLE food_category (
    id              VARCHAR(64)      PRIMARY KEY,
    category_code   VARCHAR(50)      NOT NULL,
    category_name   VARCHAR(100)     NOT NULL,
    parent_id       VARCHAR(64)      DEFAULT NULL,
    sort_order      INTEGER          DEFAULT 0,
    description     VARCHAR(500)     DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'active',
    create_time     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       VARCHAR(50)      DEFAULT NULL,
    update_by       VARCHAR(50)      DEFAULT NULL
);
COMMENT ON TABLE food_category IS '食品分类表';
COMMENT ON COLUMN food_category.id IS '分类ID';
COMMENT ON COLUMN food_category.category_code IS '分类编码';
COMMENT ON COLUMN food_category.category_name IS '分类名称';
CREATE INDEX idx_food_category_parent_id ON food_category(parent_id);
CREATE INDEX idx_food_category_category_code ON food_category(category_code);

-- --------------------------------------------------------------------------
-- 11. 食品表 (food) - 核心大表，44个字段
-- 对应实体: Food.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS food CASCADE;
CREATE TABLE food (
    food_code             VARCHAR(64)      PRIMARY KEY,
    food_name             VARCHAR(200)     NOT NULL,
    food_category         VARCHAR(64)      DEFAULT NULL,
    food_price            DECIMAL(10,2)    DEFAULT NULL,
    cost_price            DECIMAL(10,2)    DEFAULT NULL,
    food_desc             VARCHAR(1000)   DEFAULT NULL,
    food_image            VARCHAR(500)     DEFAULT NULL,
    food_status           VARCHAR(20)      DEFAULT 'active',
    batch_number           VARCHAR(100)    DEFAULT NULL,
    trace_code            VARCHAR(100)     DEFAULT NULL,
    manufacturer           VARCHAR(200)    DEFAULT NULL,
    production_date       TIMESTAMP        DEFAULT NULL,
    expiration_date        TIMESTAMP        DEFAULT NULL,
    stock                  INTEGER          DEFAULT 0,
    food_description      TEXT             DEFAULT NULL,
    shelf_life_days        INTEGER          DEFAULT NULL,
    production_address    VARCHAR(500)     DEFAULT NULL,
    nutrition_info         TEXT             DEFAULT NULL,
    storage_conditions     VARCHAR(200)    DEFAULT NULL,
    price                  DECIMAL(10,2)    DEFAULT NULL,
    weight                 DECIMAL(10,4)    DEFAULT NULL,
    quality_grade          VARCHAR(50)      DEFAULT NULL,
    quality_report_no      VARCHAR(100)    DEFAULT NULL,
    quality_check_date     TIMESTAMP        DEFAULT NULL,
    certification_info     TEXT             DEFAULT NULL,
    food_image_url         VARCHAR(500)     DEFAULT NULL,
    trace_code_image_url   VARCHAR(500)     DEFAULT NULL,
    trace_status           INTEGER          DEFAULT 0,
    create_time            TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by              VARCHAR(50)      DEFAULT NULL,
    update_by              VARCHAR(50)      DEFAULT NULL,
    remarks                VARCHAR(500)     DEFAULT NULL,
    device_id              VARCHAR(100)     DEFAULT NULL,
    sensor_data            JSONB             DEFAULT NULL,
    rfid_tag               VARCHAR(100)     DEFAULT NULL,
    gps_location           VARCHAR(200)     DEFAULT NULL,
    collect_time           TIMESTAMP        DEFAULT NULL,
    created_at             TIMESTAMP        DEFAULT NULL,
    updated_at             TIMESTAMP        DEFAULT NULL,
    deleted                INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE food IS '食品信息表（核心业务表，含44个字段）';
COMMENT ON COLUMN food.food_code IS '食品编码（主键，手动输入）';
COMMENT ON COLUMN food.food_name IS '食品名称';
COMMENT ON COLUMN food.food_category IS '食品分类ID';
COMMENT ON COLUMN food.food_price IS '食品售价';
COMMENT ON COLUMN food.cost_price IS '成本价';
COMMENT ON COLUMN food.food_status IS '食品状态';
COMMENT ON COLUMN food.stock IS '库存数量';
COMMENT ON COLUMN food.trace_status IS '溯源状态（0-未溯源，1-已溯源）';
COMMENT ON COLUMN food.deleted IS '逻辑删除标记（0-未删除，1-已删除）';
CREATE INDEX idx_food_food_name ON food(food_name);
CREATE INDEX idx_food_food_category ON food(food_category);
CREATE INDEX idx_food_food_status ON food(food_status);
CREATE INDEX idx_food_batch_number ON food(batch_number);
CREATE INDEX idx_food_trace_code ON food(trace_code);
CREATE INDEX idx_food_manufacturer ON food(manufacturer);
CREATE INDEX idx_food_create_time ON food(create_time);

-- --------------------------------------------------------------------------
-- 12. 食品溯源流程表 (food_trace)
-- 对应实体: FoodTrace.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS food_trace CASCADE;
CREATE TABLE food_trace (
    trace_id           VARCHAR(64)      PRIMARY KEY,
    food_id            VARCHAR(64)      NOT NULL,
    food_name          VARCHAR(200)     DEFAULT NULL,
    batch_number       VARCHAR(100)     DEFAULT NULL,
    process_stage      INTEGER          NOT NULL,
    stage_name         VARCHAR(100)     DEFAULT NULL,
    operation_type     VARCHAR(50)      DEFAULT NULL,
    operator_id        VARCHAR(64)      DEFAULT NULL,
    operator_name      VARCHAR(100)     DEFAULT NULL,
    operation_time     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    location           VARCHAR(300)     DEFAULT NULL,
    temperature        DECIMAL(6,2)     DEFAULT NULL,
    humidity           DECIMAL(6,2)     DEFAULT NULL,
    detail_info        JSONB             DEFAULT NULL,
    images             JSONB             DEFAULT NULL,
    remark             VARCHAR(500)     DEFAULT NULL,
    create_time        TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE food_trace IS '食品溯源流程表';
COMMENT ON COLUMN food_trace.trace_id IS '溯源ID（主键）';
COMMENT ON COLUMN food_trace.process_stage IS '流程阶段（1-生产，2-加工，3-质检，4-包装，5-运输，6-仓储，7-销售）';
CREATE INDEX idx_food_trace_food_id ON food_trace(food_id);
CREATE INDEX idx_food_trace_batch_number ON food_trace(batch_number);
CREATE INDEX idx_food_trace_process_stage ON food_trace(process_stage);

-- --------------------------------------------------------------------------
-- 13. 食品追溯码表 (food_trace_code)
-- 对应实体: FoodTraceCode.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS food_trace_code CASCADE;
CREATE TABLE food_trace_code (
    id                      BIGSERIAL       PRIMARY KEY,
    trace_code_id           VARCHAR(64)      NOT NULL UNIQUE,
    trace_code              VARCHAR(100)     NOT NULL UNIQUE,
    qr_code_url             VARCHAR(500)     DEFAULT NULL,
    order_id                VARCHAR(64)      DEFAULT NULL,
    order_number            VARCHAR(100)     DEFAULT NULL,
    order_type              INTEGER          DEFAULT 0,
    dish_id                 VARCHAR(64)      DEFAULT NULL,
    dish_name               VARCHAR(200)     DEFAULT NULL,
    dish_price              DECIMAL(10,2)    DEFAULT NULL,
    quantity                INTEGER          DEFAULT 1,
    production_station      VARCHAR(100)     DEFAULT NULL,
    produce_time            TIMESTAMP        DEFAULT NULL,
    inventory_deduction     JSONB             DEFAULT NULL,
    material_trace_codes    JSONB             DEFAULT NULL,
    material_details        JSONB             DEFAULT NULL,
    kitchen_order_id        BIGINT           DEFAULT NULL,
    make_status             VARCHAR(20)      DEFAULT 'pending',
    make_start_time         TIMESTAMP        DEFAULT NULL,
    make_complete_time      TIMESTAMP        DEFAULT NULL,
    chef_id                 BIGINT           DEFAULT NULL,
    chef_name               VARCHAR(100)     DEFAULT NULL,
    serve_time              TIMESTAMP        DEFAULT NULL,
    serve_method            VARCHAR(20)      DEFAULT 'dine_in',
    table_number            VARCHAR(20)      DEFAULT NULL,
    store_id                BIGINT           DEFAULT NULL,
    store_name              VARCHAR(100)     DEFAULT NULL,
    status                  VARCHAR(20)      DEFAULT 'created',
    print_time              TIMESTAMP        DEFAULT NULL,
    print_count             INTEGER          NOT NULL DEFAULT 0,
    material_cost           DECIMAL(10,2)    DEFAULT 0,
    labor_cost              DECIMAL(10,2)    DEFAULT 0,
    total_cost              DECIMAL(10,2)    DEFAULT 0,
    remark                  VARCHAR(500)     DEFAULT NULL,
    create_time             TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by               VARCHAR(50)      DEFAULT NULL,
    update_by               VARCHAR(50)      DEFAULT NULL,
    deleted                 INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE food_trace_code IS '食品追溯码表（对外使用的追溯标签）';
COMMENT ON COLUMN food_trace_code.trace_code IS '追溯码（唯一编码）';
COMMENT ON COLUMN food_trace_code.make_status IS '制作状态（pending/making/completed/served）';
COMMENT ON COLUMN food_trace_code.status IS '状态（created/printed/served/expired）';
CREATE INDEX idx_food_trace_code_trace_code ON food_trace_code(trace_code);
CREATE INDEX idx_food_trace_code_order_id ON food_trace_code(order_id);
CREATE INDEX idx_food_trace_code_kitchen_order_id ON food_trace_code(kitchen_order_id);
CREATE INDEX idx_food_trace_code_status ON food_trace_code(status);
CREATE INDEX idx_food_trace_code_create_time ON food_trace_code(create_time);


-- ============================================================================
-- 第三部分：订单模块表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 14. 订单表 (orders) - 30+字段
-- 对应实体: Order.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS orders CASCADE;
CREATE TABLE orders (
    order_id             VARCHAR(64)      PRIMARY KEY,
    user_id              VARCHAR(64)      DEFAULT NULL,
    order_number         VARCHAR(100)     NOT NULL UNIQUE,
    order_type           INTEGER          NOT NULL DEFAULT 0,
    order_status         INTEGER          NOT NULL DEFAULT 0,
    order_amount         DECIMAL(12,2)    NOT NULL DEFAULT 0,
    discount_amount      DECIMAL(12,2)    NOT NULL DEFAULT 0,
    actual_amount        DECIMAL(12,2)    NOT NULL DEFAULT 0,
    payment_method       INTEGER          DEFAULT NULL,
    transaction_id       VARCHAR(100)     DEFAULT NULL,
    payment_time         TIMESTAMP        DEFAULT NULL,
    delivery_address     VARCHAR(500)     DEFAULT NULL,
    contact_name         VARCHAR(100)     DEFAULT NULL,
    contact_phone        VARCHAR(20)      DEFAULT NULL,
    remarks              VARCHAR(500)     DEFAULT NULL,
    order_source         INTEGER          DEFAULT 0,
    merchant_id          VARCHAR(64)      DEFAULT NULL,
    deliveryman_id       VARCHAR(64)      DEFAULT NULL,
    estimated_delivery_time TIMESTAMP     DEFAULT NULL,
    actual_delivery_time  TIMESTAMP      DEFAULT NULL,
    cancel_reason        VARCHAR(500)     DEFAULT NULL,
    refund_reason        VARCHAR(500)     DEFAULT NULL,
    refund_amount        DECIMAL(12,2)    DEFAULT NULL,
    refund_time          TIMESTAMP        DEFAULT NULL,
    create_time          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by            VARCHAR(50)      DEFAULT NULL,
    update_by            VARCHAR(50)      DEFAULT NULL,
    deleted              INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE orders IS '订单表';
COMMENT ON COLUMN orders.order_id IS '订单ID（主键，雪花算法）';
COMMENT ON COLUMN orders.order_number IS '订单编号（唯一）';
COMMENT ON COLUMN orders.order_type IS '订单类型（0-堂食，1-外卖，2-自提）';
COMMENT ON COLUMN orders.order_status IS '订单状态（0-待支付，1-已支付，2-待配送，3-配送中，4-已完成，5-已取消，6-退款中，7-已退款）';
COMMENT ON COLUMN orders.payment_method IS '支付方式（0-微信支付，1-支付宝，2-现金，3-银行卡）';
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_status ON orders(order_status);
CREATE INDEX idx_orders_order_type ON orders(order_type);
CREATE INDEX idx_orders_create_time ON orders(create_time);
CREATE INDEX idx_orders_merchant_id ON orders(merchant_id);

-- --------------------------------------------------------------------------
-- 15. 订单项表 (order_items)
-- 对应实体: OrderItem.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS order_items CASCADE;
CREATE TABLE order_items (
    order_item_id     VARCHAR(64)      PRIMARY KEY,
    order_id          VARCHAR(64)      NOT NULL,
    food_id           VARCHAR(64)      DEFAULT NULL,
    food_name         VARCHAR(200)     DEFAULT NULL,
    unit_price        DECIMAL(10,2)    NOT NULL DEFAULT 0,
    quantity          INTEGER          NOT NULL DEFAULT 1,
    subtotal_amount   DECIMAL(12,2)    NOT NULL DEFAULT 0,
    food_image_url    VARCHAR(500)     DEFAULT NULL,
    specification     VARCHAR(200)     DEFAULT NULL,
    create_time       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE order_items IS '订单项表';
COMMENT ON COLUMN order_items.order_item_id IS '订单项ID（主键，雪花算法）';
COMMENT ON COLUMN order_items.order_id IS '订单ID（外键）';
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_food_id ON order_items(food_id);

-- --------------------------------------------------------------------------
-- 16. 后厨订单表 (kitchen_order)
-- 对应实体: KitchenOrder.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS kitchen_order CASCADE;
CREATE TABLE kitchen_order (
    id                      BIGSERIAL       PRIMARY KEY,
    kitchen_order_id        VARCHAR(64)      NOT NULL UNIQUE,
    order_id                VARCHAR(64)      DEFAULT NULL,
    order_number            VARCHAR(100)     DEFAULT NULL,
    order_type              INTEGER          DEFAULT 0,
    table_number            VARCHAR(20)      DEFAULT NULL,
    dish_items              JSONB             DEFAULT NULL,
    total_dishes            INTEGER          DEFAULT 0,
    priority                INTEGER          DEFAULT 0,
    status                  VARCHAR(20)      NOT NULL DEFAULT 'pending',
    receive_time            TIMESTAMP        DEFAULT NULL,
    make_start_time         TIMESTAMP        DEFAULT NULL,
    make_complete_time      TIMESTAMP        DEFAULT NULL,
    serve_time              TIMESTAMP        DEFAULT NULL,
    cancel_time             TIMESTAMP        DEFAULT NULL,
    cancel_reason           VARCHAR(300)     DEFAULT NULL,
    chef_id                 BIGINT           DEFAULT NULL,
    chef_name               VARCHAR(100)     DEFAULT NULL,
    store_id                BIGINT           DEFAULT NULL,
    store_name              VARCHAR(100)     DEFAULT NULL,
    material_consumed       INTEGER          NOT NULL DEFAULT 0,
    material_consume_time   TIMESTAMP        DEFAULT NULL,
    material_locked         INTEGER          NOT NULL DEFAULT 0,
    material_lock_time      TIMESTAMP        DEFAULT NULL,
    food_trace_codes        JSONB             DEFAULT NULL,
    tray_id                 BIGINT           DEFAULT NULL,
    tray_code               VARCHAR(50)      DEFAULT NULL,
    tray_bind_time          TIMESTAMP        DEFAULT NULL,
    remark                  VARCHAR(500)     DEFAULT NULL,
    create_time             TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by               VARCHAR(50)      DEFAULT NULL,
    update_by               VARCHAR(50)      DEFAULT NULL,
    deleted                 INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE kitchen_order IS '后厨订单表';
COMMENT ON COLUMN kitchen_order.kitchen_order_id IS '后厨订单ID（唯一）';
COMMENT ON COLUMN kitchen_order.status IS '状态（pending/received/making/completed/served/cancelled）';
COMMENT ON COLUMN kitchen_order.priority IS '优先级（0-普通，1-加急，2-特急）';
CREATE INDEX idx_kitchen_order_kitchen_order_id ON kitchen_order(kitchen_order_id);
CREATE INDEX idx_kitchen_order_order_id ON kitchen_order(order_id);
CREATE INDEX idx_kitchen_order_status ON kitchen_order(status);
CREATE INDEX idx_kitchen_order_store_id ON kitchen_order(store_id);
CREATE INDEX idx_kitchen_order_chef_id ON kitchen_order(chef_id);
CREATE INDEX idx_kitchen_order_create_time ON kitchen_order(create_time);

-- --------------------------------------------------------------------------
-- 17. 叫号记录表 (call_record)
-- 对应实体: CallRecord.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS call_record CASCADE;
CREATE TABLE call_record (
    id              BIGSERIAL   PRIMARY KEY,
    order_id        VARCHAR(64) DEFAULT NULL,
    order_number    VARCHAR(100) DEFAULT NULL,
    table_number    VARCHAR(20) DEFAULT NULL,
    order_type      VARCHAR(20) DEFAULT NULL,
    item_count      INTEGER     DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'waiting',
    call_count      INTEGER     NOT NULL DEFAULT 1,
    first_call_time TIMESTAMP   DEFAULT NULL,
    last_call_time  TIMESTAMP   DEFAULT NULL,
    pick_time       TIMESTAMP   DEFAULT NULL,
    wait_seconds    INTEGER     DEFAULT 0,
    create_time     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE call_record IS '叫号记录表';
COMMENT ON COLUMN call_record.status IS '状态（waiting/called/completed/cancelled）';
CREATE INDEX idx_call_record_order_id ON call_record(order_id);
CREATE INDEX idx_call_record_table_number ON call_record(table_number);
CREATE INDEX idx_call_record_status ON call_record(status);
CREATE INDEX idx_call_record_create_time ON call_record(create_time);

-- --------------------------------------------------------------------------
-- 18. 桌台表 (dining_table)
-- 对应实体: DiningTable.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS dining_table CASCADE;
CREATE TABLE dining_table (
    id              BIGSERIAL       PRIMARY KEY,
    table_number    VARCHAR(20)     NOT NULL UNIQUE,
    table_name      VARCHAR(100)     DEFAULT NULL,
    capacity        INTEGER          DEFAULT 4,
    area            VARCHAR(50)      DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'available',
    qr_code         VARCHAR(500)     DEFAULT NULL,
    current_order_id VARCHAR(64)    DEFAULT NULL,
    guest_count     INTEGER          DEFAULT 0,
    seated_at       TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE dining_table IS '餐桌/桌台表';
COMMENT ON COLUMN dining_table.status IS '状态（available/occupied/reserved/maintenance）';
CREATE INDEX idx_dining_table_status ON dining_table(status);
CREATE INDEX idx_dining_table_area ON dining_table(area);


-- ============================================================================
-- 第四部分：库存与仓库管理表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 19. 库存表 (inventory)
-- 对应实体: Inventory.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS inventory CASCADE;
CREATE TABLE inventory (
    id                  BIGSERIAL       PRIMARY KEY,
    product_id          BIGINT           DEFAULT NULL,
    product_name        VARCHAR(200)     DEFAULT NULL,
    warehouse_id        BIGINT           DEFAULT NULL,
    warehouse_name      VARCHAR(100)     DEFAULT NULL,
    store_id            BIGINT           DEFAULT NULL,
    store_name          VARCHAR(100)     DEFAULT NULL,
    current_stock       INTEGER          NOT NULL DEFAULT 0,
    safety_stock        INTEGER          DEFAULT 0,
    unit                VARCHAR(20)      DEFAULT '件',
    cost_price          DECIMAL(12,2)    DEFAULT 0,
    stock_value         DECIMAL(14,4)    DEFAULT 0,
    warning_level       INTEGER          DEFAULT 0,
    inventory_type      INTEGER          DEFAULT 1,
    last_update_time    TIMESTAMP        DEFAULT NULL,
    created_at          TIMESTAMP        DEFAULT NULL,
    updated_at          TIMESTAMP        DEFAULT NULL,
    deleted             INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE inventory IS '库存表';
COMMENT ON COLUMN inventory.current_stock IS '当前库存数量';
COMMENT ON COLUMN inventory.safety_stock IS '安全库存预警值';
CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_warehouse_id ON inventory(warehouse_id);
CREATE INDEX idx_inventory_store_id ON inventory(store_id);

-- --------------------------------------------------------------------------
-- 20. 仓库表 (warehouse)
-- 对应实体: Warehouse.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS warehouse CASCADE;
CREATE TABLE warehouse (
    id              BIGSERIAL       PRIMARY KEY,
    warehouse_code  VARCHAR(50)      NOT NULL UNIQUE,
    warehouse_name  VARCHAR(100)     NOT NULL,
    address         VARCHAR(300)     DEFAULT NULL,
    manager_id      VARCHAR(64)      DEFAULT NULL,
    manager_name    VARCHAR(100)     DEFAULT NULL,
    capacity        INTEGER          DEFAULT 0,
    status          INTEGER          NOT NULL DEFAULT 1,
    store_id        BIGINT           DEFAULT NULL,
    remark          VARCHAR(500)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE warehouse IS '仓库表';
CREATE INDEX idx_warehouse_store_id ON warehouse(store_id);
CREATE INDEX idx_warehouse_status ON warehouse(status);

-- --------------------------------------------------------------------------
-- 21. 门店表 (stores)
-- 对应实体: Store.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS stores CASCADE;
CREATE TABLE stores (
    store_id        VARCHAR(64)      PRIMARY KEY,
    store_name      VARCHAR(100)     NOT NULL,
    store_code      VARCHAR(50)      UNIQUE NOT NULL,
    address         VARCHAR(500)     DEFAULT NULL,
    phone           VARCHAR(20)      DEFAULT NULL,
    manager_id      VARCHAR(64)      DEFAULT NULL,
    manager_name    VARCHAR(100)     DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'active',
    company_id      VARCHAR(64)      DEFAULT NULL,
    region          VARCHAR(100)     DEFAULT NULL,
    store_type      VARCHAR(20)      DEFAULT 'single',
    business_hours  VARCHAR(200)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE stores IS '门店表';
COMMENT ON COLUMN stores.store_id IS '门店ID（主键，雪花算法）';
COMMENT ON COLUMN stores.store_code IS '门店编码（唯一）';
COMMENT ON COLUMN stores.status IS '状态（active-正常，inactive-禁用）';
CREATE INDEX idx_stores_store_code ON stores(store_code);
CREATE INDEX idx_stores_region ON stores(region);
CREATE INDEX idx_stores_status ON stores(status);

-- --------------------------------------------------------------------------
-- 22. 供应商表 (suppliers)
-- 对应实体: Supplier.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS suppliers CASCADE;
CREATE TABLE suppliers (
    id              BIGSERIAL       PRIMARY KEY,
    supplier_id     VARCHAR(64)      DEFAULT NULL,
    name            VARCHAR(200)     NOT NULL,
    contact_person  VARCHAR(100)     DEFAULT NULL,
    phone           VARCHAR(20)      DEFAULT NULL,
    address         VARCHAR(500)     DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'active',
    create_time     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0,
    version         INTEGER          NOT NULL DEFAULT 1
);
COMMENT ON TABLE suppliers IS '供应商表';
CREATE INDEX idx_suppliers_name ON suppliers(name);
CREATE INDEX idx_suppliers_status ON suppliers(status);

-- --------------------------------------------------------------------------
-- 23. 会员表 (member)
-- 对应实体: Member.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS member CASCADE;
CREATE TABLE member (
    id              BIGSERIAL       PRIMARY KEY,
    member_no       VARCHAR(50)      UNIQUE NOT NULL,
    name            VARCHAR(100)     NOT NULL,
    phone           VARCHAR(20)      DEFAULT NULL,
    gender           VARCHAR(10)      DEFAULT NULL,
    birthday         DATE            DEFAULT NULL,
    level           INTEGER          DEFAULT 1,
    level_name      VARCHAR(50)      DEFAULT '普通会员',
    balance         DECIMAL(12,2)    NOT NULL DEFAULT 0,
    points          INTEGER          NOT NULL DEFAULT 0,
    total_spent     DECIMAL(12,2)    NOT NULL DEFAULT 0,
    order_count     INTEGER          NOT NULL DEFAULT 0,
    status          VARCHAR(20)      NOT NULL DEFAULT 'active',
    registered_at   TIMESTAMP        DEFAULT NULL,
    last_visit_at   TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE member IS '会员表';
COMMENT ON COLUMN member.member_no IS '会员编号（唯一）';
CREATE INDEX idx_member_member_no ON member(member_no);
CREATE INDEX idx_member_phone ON member(phone);
CREATE INDEX idx_member_status ON member(status);


-- ============================================================================
-- 第五部分：设备与硬件管理表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 24. 设备表 (device / sys_device)
-- 对应实体: Device.java / SysDevice.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_device CASCADE;
CREATE TABLE sys_device (
    id              BIGSERIAL       PRIMARY KEY,
    device_code     VARCHAR(50)      NOT NULL UNIQUE,
    device_name     VARCHAR(100)     NOT NULL,
    device_type     VARCHAR(50)      NOT NULL,
    model           VARCHAR(100)     DEFAULT NULL,
    manufacturer    VARCHAR(200)     DEFAULT NULL,
    serial_number   VARCHAR(100)     DEFAULT NULL,
    status          INTEGER          NOT NULL DEFAULT 1,
    store_id        BIGINT           DEFAULT NULL,
    location        VARCHAR(200)     DEFAULT NULL,
    ip_address      VARCHAR(50)      DEFAULT NULL,
    config          JSONB            DEFAULT NULL,
    last_online_time TIMESTAMP       DEFAULT NULL,
    remark          VARCHAR(500)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_device IS '设备表';
CREATE INDEX idx_sys_device_device_code ON sys_device(device_code);
CREATE INDEX idx_sys_device_device_type ON sys_device(device_type);
CREATE INDEX idx_sys_device_store_id ON sys_device(store_id);

-- --------------------------------------------------------------------------
-- 25. 托盘表 (tray)
-- 对应实体: Tray.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS tray CASCADE;
CREATE TABLE tray (
    id              BIGSERIAL       PRIMARY KEY,
    tray_code       VARCHAR(50)      NOT NULL UNIQUE,
    tray_type       VARCHAR(20)      DEFAULT 'normal',
    capacity        INTEGER          DEFAULT 4,
    status          VARCHAR(20)      NOT NULL DEFAULT 'idle',
    current_order_id VARCHAR(64)    DEFAULT NULL,
    store_id        BIGINT           DEFAULT NULL,
    location        VARCHAR(100)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE tray IS '托盘表';
COMMENT ON COLUMN tray.status IS '状态（idle/in_use/maintenance）';
CREATE INDEX idx_tray_tray_code ON tray(tray_code);
CREATE INDEX idx_tray_status ON tray(status);
CREATE INDEX idx_tray_store_id ON tray(store_id);

-- --------------------------------------------------------------------------
-- 26. 取餐柜表 (takeout_locker)
-- 对应实体: TakeoutLocker.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS takeout_locker CASCADE;
CREATE TABLE takeout_locker (
    id              BIGSERIAL       PRIMARY KEY,
    locker_code     VARCHAR(50)      NOT NULL UNIQUE,
    locker_name     VARCHAR(100)     DEFAULT NULL,
    slot_count      INTEGER          NOT NULL DEFAULT 10,
    status          VARCHAR(20)      NOT NULL DEFAULT 'online',
    store_id        BIGINT           DEFAULT NULL,
    config          JSONB            DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE takeout_locker IS '取餐柜表';
CREATE INDEX idx_takeout_locker_locker_code ON takeout_locker(locker_code);
CREATE INDEX idx_takeout_locker_store_id ON takeout_locker(store_id);

-- --------------------------------------------------------------------------
-- 27. 取餐柜格口表 (locker_slot)
-- 对应实体: LockerSlot.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS locker_slot CASCADE;
CREATE TABLE locker_slot (
    id              BIGSERIAL       PRIMARY KEY,
    locker_id      BIGINT           NOT NULL,
    slot_number     VARCHAR(20)      NOT NULL,
    slot_size       VARCHAR(20)      DEFAULT 'medium',
    status          VARCHAR(20)      NOT NULL DEFAULT 'empty',
    current_order_id VARCHAR(64)    DEFAULT NULL,
    open_code       VARCHAR(100)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE locker_slot IS '取餐柜格口表';
CREATE INDEX idx_locker_slot_locker_id ON locker_slot(locker_id);
CREATE INDEX idx_locker_slot_status ON locker_slot(status);
CREATE UNIQUE INDEX uk_locker_slot_locker_number ON locker_slot(locker_id, slot_number);


-- ============================================================================
-- 第六部分：日志与审计表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 28. 操作日志表 (sys_operation_logs)
-- 对应实体: OperationLog.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_operation_logs CASCADE;
CREATE TABLE sys_operation_logs (
    id              BIGSERIAL       PRIMARY KEY,
    log_type        VARCHAR(20)      DEFAULT 'operation',
    module          VARCHAR(100)     DEFAULT NULL,
    business_id     VARCHAR(64)      DEFAULT NULL,
    operation       VARCHAR(100)     NOT NULL,
    user_id         VARCHAR(64)      DEFAULT NULL,
    username        VARCHAR(100)     DEFAULT NULL,
    request_url     VARCHAR(500)     DEFAULT NULL,
    request_method  VARCHAR(10)      DEFAULT NULL,
    request_params  TEXT            DEFAULT NULL,
    response_data   TEXT            DEFAULT NULL,
    ip_address      VARCHAR(50)      DEFAULT NULL,
    user_agent      TEXT            DEFAULT NULL,
    execution_time  INTEGER          DEFAULT NULL,
    status          INTEGER          DEFAULT 1,
    error_msg       TEXT            DEFAULT NULL,
    create_time     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_operation_logs IS '系统操作日志表';
COMMENT ON COLUMN sys_operation_logs.log_type IS '日志类型';
COMMENT ON COLUMN sys_operation_logs.module IS '所属模块';
COMMENT ON COLUMN sys_operation_logs.operation IS '操作类型';
COMMENT ON COLUMN sys_operation_logs.status IS '执行状态（1-成功，0-失败）';
CREATE INDEX idx_sys_operation_logs_user_id ON sys_operation_logs(user_id);
CREATE INDEX idx_sys_operation_logs_module ON sys_operation_logs(module);
CREATE INDEX idx_sys_operation_logs_business_id ON sys_operation_logs(business_id);
CREATE INDEX idx_sys_operation_logs_create_time ON sys_operation_logs(create_time);

-- --------------------------------------------------------------------------
-- 29. 登录日志表 (login_log)
-- 对应实体: LoginLog.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS login_log CASCADE;
CREATE TABLE login_log (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT           DEFAULT NULL,
    username        VARCHAR(100)     DEFAULT NULL,
    login_time      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time     TIMESTAMP        DEFAULT NULL,
    login_ip        VARCHAR(50)      DEFAULT NULL,
    login_location  VARCHAR(200)     DEFAULT NULL,
    device_type     VARCHAR(50)      DEFAULT NULL,
    browser         VARCHAR(100)     DEFAULT NULL,
    os              VARCHAR(100)     DEFAULT NULL,
    login_status    INTEGER          NOT NULL DEFAULT 1,
    failure_reason  VARCHAR(300)     DEFAULT NULL,
    session_id      VARCHAR(100)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE login_log IS '登录日志表';
COMMENT ON COLUMN login_log.login_status IS '登录状态（1-成功，0-失败）';
CREATE INDEX idx_login_log_user_id ON login_log(user_id);
CREATE INDEX idx_login_log_username ON login_log(username);
CREATE INDEX idx_login_log_login_time ON login_log(login_time);
CREATE INDEX idx_login_log_login_ip ON login_log(login_ip);

-- --------------------------------------------------------------------------
-- 30. 用户会话表 (user_session)
-- 对应实体: UserSession.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS user_session CASCADE;
CREATE TABLE user_session (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT           NOT NULL,
    session_token   VARCHAR(512)     NOT NULL UNIQUE,
    device_info     VARCHAR(500)     DEFAULT NULL,
    ip_address      VARCHAR(50)      DEFAULT NULL,
    expires_at      TIMESTAMP        NOT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          INTEGER          NOT NULL DEFAULT 1
);
COMMENT ON TABLE user_session IS '用户会话表';
CREATE INDEX idx_user_session_user_id ON user_session(user_id);
CREATE INDEX idx_user_session_session_token ON user_session(session_token);
CREATE INDEX idx_user_session_expires_at ON user_session(expires_at);


-- ============================================================================
-- 第七部分：系统配置表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 31. 系统配置表 (sys_config)
-- 对应实体: SysConfig.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_config CASCADE;
CREATE TABLE sys_config (
    id              BIGSERIAL       PRIMARY KEY,
    config_key      VARCHAR(200)     NOT NULL UNIQUE,
    config_value    TEXT            DEFAULT NULL,
    config_name     VARCHAR(100)     DEFAULT NULL,
    config_group    VARCHAR(100)     DEFAULT 'default',
    description     VARCHAR(500)     DEFAULT NULL,
    data_type       VARCHAR(20)      DEFAULT 'string',
    is_public       BOOLEAN          NOT NULL DEFAULT FALSE,
    sort_order      INTEGER          DEFAULT 0,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.config_key IS '配置键（唯一）';
COMMENT ON COLUMN sys_config.config_value IS '配置值';
COMMENT ON COLUMN sys_config.config_group IS '配置分组';
CREATE INDEX idx_sys_config_config_key ON sys_config(config_key);
CREATE INDEX idx_sys_config_config_group ON sys_config(config_group);

-- --------------------------------------------------------------------------
-- 32. 系统设置表 (sys_setting)
-- 对应实体: SysSetting.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_setting CASCADE;
CREATE TABLE sys_setting (
    id              BIGSERIAL       PRIMARY KEY,
    setting_key     VARCHAR(200)     NOT NULL UNIQUE,
    setting_value   JSONB            DEFAULT NULL,
    setting_name     VARCHAR(100)     DEFAULT NULL,
    setting_group   VARCHAR(100)     DEFAULT 'default',
    description     VARCHAR(500)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_setting IS '系统设置表（支持JSON格式配置值）';
CREATE INDEX idx_sys_setting_setting_key ON sys_setting(setting_key);

-- --------------------------------------------------------------------------
-- 33. 全局配置表 (global_config)
-- 对应实体: GlobalConfig.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS global_config CASCADE;
CREATE TABLE global_config (
    id              BIGSERIAL       PRIMARY KEY,
    config_key      VARCHAR(200)     NOT NULL UNIQUE,
    config_value    JSONB            DEFAULT NULL,
    config_name     VARCHAR(100)     DEFAULT NULL,
    config_type     VARCHAR(20)      DEFAULT 'system',
    description     VARCHAR(500)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE global_config IS '全局配置表';
CREATE INDEX idx_global_config_config_key ON global_config(config_key);

-- --------------------------------------------------------------------------
-- 34. 企业配置表 (enterprise_config)
-- 对应实体: EnterpriseConfig.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS enterprise_config CASCADE;
CREATE TABLE enterprise_config (
    id              BIGSERIAL       PRIMARY KEY,
    config_key      VARCHAR(200)     NOT NULL UNIQUE,
    config_value    JSONB            DEFAULT NULL,
    config_name     VARCHAR(100)     DEFAULT NULL,
    description     VARCHAR(500)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE enterprise_config IS '企业配置表';


-- ============================================================================
-- 第八部分：消息通知表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 35. 通知表 (notification)
-- 对应实体: Notification.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS notification CASCADE;
CREATE TABLE notification (
    id              BIGSERIAL       PRIMARY KEY,
    title           VARCHAR(200)     NOT NULL,
    content         TEXT            DEFAULT NULL,
    type            VARCHAR(20)      NOT NULL DEFAULT 'system',
    receiver_id     VARCHAR(64)      NOT NULL,
    sender_id       VARCHAR(64)      DEFAULT NULL,
    is_read         BOOLEAN          NOT NULL DEFAULT FALSE,
    read_time       TIMESTAMP        DEFAULT NULL,
    extra_data      JSONB            DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE notification IS '通知消息表';
COMMENT ON COLUMN notification.type IS '通知类型（system/order/warning/promotion）';
CREATE INDEX idx_notification_receiver_id ON notification(receiver_id);
CREATE INDEX idx_notification_is_read ON notification(is_read);
CREATE INDEX idx_notification_created_at ON notification(created_at);

-- --------------------------------------------------------------------------
-- 36. 通知设置表 (notification_setting)
-- 对应实体: NotificationSetting.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS notification_setting CASCADE;
CREATE TABLE notification_setting (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         VARCHAR(64)      NOT NULL,
    notify_type     VARCHAR(50)      NOT NULL,
    enabled         BOOLEAN          NOT NULL DEFAULT TRUE,
    channel         VARCHAR(50)      DEFAULT 'in_app',
    template_id     VARCHAR(64)      DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE notification_setting IS '通知设置表';
CREATE UNIQUE INDEX uk_notif_setting_user_type ON notification_setting(user_id, notify_type) WHERE deleted = 0;


-- ============================================================================
-- 第九部分：打印与标签表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 37. 标签模板表 (label_template)
-- 对应实体: LabelTemplate.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS label_template CASCADE;
CREATE TABLE label_template (
    id              BIGSERIAL       PRIMARY KEY,
    template_name   VARCHAR(100)     NOT NULL,
    template_code   VARCHAR(50)      UNIQUE NOT NULL,
    template_type   VARCHAR(20)      NOT NULL DEFAULT 'trace',
    width_mm        INTEGER          NOT NULL DEFAULT 60,
    height_mm       INTEGER          NOT NULL DEFAULT 40,
    content_json    JSONB            NOT NULL,
    style_config    JSONB            DEFAULT NULL,
    status          INTEGER          NOT NULL DEFAULT 1,
    version         INTEGER          NOT NULL DEFAULT 1,
    remark          VARCHAR(500)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE label_template IS '标签模板表';
COMMENT ON COLUMN label_template.template_type IS '模板类型（trace/kitchen/product/ingredient）';
CREATE INDEX idx_label_template_template_code ON label_template(template_code);
CREATE INDEX idx_label_template_template_type ON label_template(template_type);

-- --------------------------------------------------------------------------
-- 38. 打印任务表 (print_task)
-- 对应实体: PrintTask.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS print_task CASCADE;
CREATE TABLE print_task (
    id              BIGSERIAL       PRIMARY KEY,
    task_type       VARCHAR(20)      NOT NULL,
    business_id     VARCHAR(64)      DEFAULT NULL,
    template_id     BIGINT           NOT NULL,
    printer_name    VARCHAR(100)     DEFAULT NULL,
    content_json    JSONB            NOT NULL,
    copies          INTEGER          NOT NULL DEFAULT 1,
    status          VARCHAR(20)      NOT NULL DEFAULT 'pending',
    error_msg       TEXT            DEFAULT NULL,
    printed_at      TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE print_task IS '打印任务表';
COMMENT ON COLUMN print_task.task_type IS '任务类型（label/receipt/kitchen/ticket）';
COMMENT ON COLUMN print_task.status IS '状态（pending/printing/completed/failed）';
CREATE INDEX idx_print_task_task_type ON print_task(task_type);
CREATE INDEX idx_print_task_status ON print_task(status);
CREATE INDEX idx_print_task_business_id ON print_task(business_id);
CREATE INDEX idx_print_task_created_at ON print_task(created_at);


-- ============================================================================
-- 第十部分：定时任务与调度表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 39. 定时任务表 (scheduled_task)
-- 对应实体: ScheduledTask.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS scheduled_task CASCADE;
CREATE TABLE scheduled_task (
    id              BIGSERIAL       PRIMARY KEY,
    task_name       VARCHAR(100)     NOT NULL,
    task_group      VARCHAR(50)      DEFAULT 'DEFAULT',
    task_expression VARCHAR(100)     NOT NULL,
    class_name      VARCHAR(255)     NOT NULL,
    method_name     VARCHAR(100)     NOT NULL,
    params          JSONB            DEFAULT NULL,
    status          INTEGER          NOT NULL DEFAULT 1,
    description     VARCHAR(500)     DEFAULT NULL,
    last_exec_time  TIMESTAMP        DEFAULT NULL,
    next_exec_time  TIMESTAMP        DEFAULT NULL,
    exec_count      INTEGER          NOT NULL DEFAULT 0,
    error_count     INTEGER          NOT NULL DEFAULT 0,
    timeout         INTEGER          DEFAULT 60,
    retry_count     INTEGER          DEFAULT 3,
    retry_interval  INTEGER          DEFAULT 5,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE scheduled_task IS '定时任务表';
COMMENT ON COLUMN scheduled_task.task_expression IS 'Cron表达式';
COMMENT ON COLUMN scheduled_task.status IS '状态（1-启用，0-暂停）';
CREATE INDEX idx_scheduled_task_task_name ON scheduled_task(task_name);
CREATE INDEX idx_scheduled_task_task_group ON scheduled_task(task_group);
CREATE INDEX idx_scheduled_task_status ON scheduled_task(status);

-- --------------------------------------------------------------------------
-- 40. 任务执行日志表 (task_execution_log)
-- 对应实体: TaskExecutionLog.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS task_execution_log CASCADE;
CREATE TABLE task_execution_log (
    id              BIGSERIAL       PRIMARY KEY,
    task_id         BIGINT           NOT NULL,
    task_name       VARCHAR(100)     DEFAULT NULL,
    start_time      TIMESTAMP        NOT NULL,
    end_time        TIMESTAMP        DEFAULT NULL,
    duration_ms     BIGINT           DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'running',
    error_msg       TEXT            DEFAULT NULL,
    result_data     JSONB            DEFAULT NULL,
    server_host     VARCHAR(100)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE task_execution_log IS '任务执行日志表';
CREATE INDEX idx_task_execution_log_task_id ON task_execution_log(task_id);
CREATE INDEX idx_task_execution_log_status ON task_execution_log(status);
CREATE INDEX idx_task_execution_log_start_time ON task_execution_log(start_time);


-- ============================================================================
-- 第十一部分：扫码与称重设备表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 41. 扫码设备表 (scan_device)
-- 对应实体: ScanDevice.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS scan_device CASCADE;
CREATE TABLE scan_device (
    id              BIGSERIAL       PRIMARY KEY,
    device_code     VARCHAR(50)      NOT NULL UNIQUE,
    device_name     VARCHAR(100)     NOT NULL,
    device_type     VARCHAR(20)      DEFAULT 'handheld',
    ip_address      VARCHAR(50)      DEFAULT NULL,
    port            INTEGER          DEFAULT NULL,
    status          INTEGER          NOT NULL DEFAULT 1,
    store_id        BIGINT           DEFAULT NULL,
    location        VARCHAR(200)     DEFAULT NULL,
    last_online_time TIMESTAMP       DEFAULT NULL,
    config          JSONB            DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE scan_device IS '扫码设备表';
CREATE INDEX idx_scan_device_device_code ON scan_device(device_code);
CREATE INDEX idx_scan_device_store_id ON scan_device(store_id);

-- --------------------------------------------------------------------------
-- 42. 扫码记录表 (scan_record)
-- 对应实体: ScanRecord.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS scan_record CASCADE;
CREATE TABLE scan_record (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       BIGINT           NOT NULL,
    scan_code       VARCHAR(200)     NOT NULL,
    scan_type       VARCHAR(20)      DEFAULT 'product',
    business_id     VARCHAR(64)      DEFAULT NULL,
    result_data     JSONB            DEFAULT NULL,
    scan_time       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator_id     VARCHAR(64)      DEFAULT NULL,
    remark          VARCHAR(300)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE scan_record IS '扫码记录表';
CREATE INDEX idx_scan_record_device_id ON scan_record(device_id);
CREATE INDEX idx_scan_record_scan_code ON scan_record(scan_code);
CREATE INDEX idx_scan_record_scan_time ON scan_record(scan_time);

-- --------------------------------------------------------------------------
-- 43. 称重设备表 (weighing_device)
-- 对应实体: WeighingDevice.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS weighing_device CASCADE;
CREATE TABLE weighing_device (
    id              BIGSERIAL       PRIMARY KEY,
    device_code     VARCHAR(50)      NOT NULL UNIQUE,
    device_name     VARCHAR(100)     NOT NULL,
    max_capacity    DECIMAL(12,4)    DEFAULT NULL,
    precision_val  DECIMAL(10,4)    DEFAULT 0.001,
    unit            VARCHAR(20)      DEFAULT 'kg',
    ip_address      VARCHAR(50)      DEFAULT NULL,
    port            INTEGER          DEFAULT NULL,
    status          INTEGER          NOT NULL DEFAULT 1,
    store_id        BIGINT           DEFAULT NULL,
    location        VARCHAR(200)     DEFAULT NULL,
    config          JSONB            DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE weighing_device IS '称重设备表';
CREATE INDEX idx_weighing_device_device_code ON weighing_device(device_code);
CREATE INDEX idx_weighing_device_store_id ON weighing_device(store_id);

-- --------------------------------------------------------------------------
-- 44. 称重记录表 (weighing_record)
-- 对应实体: WeighingRecord.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS weighing_record CASCADE;
CREATE TABLE weighing_record (
    id              BIGSERIAL       PRIMARY KEY,
    device_id       BIGINT           NOT NULL,
    product_id      BIGINT           DEFAULT NULL,
    product_name    VARCHAR(200)     DEFAULT NULL,
    gross_weight    DECIMAL(12,4)    DEFAULT NULL,
    tare_weight     DECIMAL(12,4)    DEFAULT NULL,
    net_weight      DECIMAL(12,4)    DEFAULT NULL,
    unit            VARCHAR(20)      DEFAULT 'kg',
    weigh_time      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator_id     VARCHAR(64)      DEFAULT NULL,
    remark          VARCHAR(300)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE weighing_record IS '称重记录表';
CREATE INDEX idx_weighing_record_device_id ON weighing_record(device_id);
CREATE INDEX idx_weighing_record_product_id ON weighing_record(product_id);
CREATE INDEX idx_weighing_record_weigh_time ON weighing_record(weigh_time);


-- ============================================================================
-- 第十二部分：财务相关表（基础结构）
-- ============================================================================

-- --------------------------------------------------------------------------
-- 45. 财务账户表 (finance_account)
-- 对应实体: FinanceAccount.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS finance_account CASCADE;
CREATE TABLE finance_account (
    id              BIGSERIAL       PRIMARY KEY,
    account_code    VARCHAR(50)      NOT NULL UNIQUE,
    account_name    VARCHAR(100)     NOT NULL,
    account_type    VARCHAR(20)      NOT NULL DEFAULT 'general',
    balance         DECIMAL(16,2)    NOT NULL DEFAULT 0,
    currency        VARCHAR(10)      DEFAULT 'CNY',
    status          INTEGER          NOT NULL DEFAULT 1,
    description     VARCHAR(500)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE finance_account IS '财务账户表';
CREATE INDEX idx_finance_account_account_code ON finance_account(account_code);

-- --------------------------------------------------------------------------
-- 46. 财务记录表 (finance_record)
-- 对应实体: FinanceRecord.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS finance_record CASCADE;
CREATE TABLE finance_record (
    id              BIGSERIAL       PRIMARY KEY,
    record_no       VARCHAR(50)      NOT NULL UNIQUE,
    account_id      BIGINT           NOT NULL,
    record_type     VARCHAR(20)      NOT NULL,
    amount          DECIMAL(16,2)    NOT NULL,
    direction       VARCHAR(10)      NOT NULL,
    balance_after   DECIMAL(16,2)    DEFAULT NULL,
    reference_no    VARCHAR(100)     DEFAULT NULL,
    summary         VARCHAR(500)     DEFAULT NULL,
    detail          TEXT            DEFAULT NULL,
    attachments     JSONB            DEFAULT NULL,
    status          INTEGER          NOT NULL DEFAULT 1,
    approved_by     VARCHAR(50)      DEFAULT NULL,
    approved_at     TIMESTAMP        DEFAULT NULL,
    period_date     DATE            DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE finance_record IS '财务记录表';
CREATE INDEX idx_finance_record_record_no ON finance_record(record_no);
CREATE INDEX idx_finance_record_account_id ON finance_record(account_id);
CREATE INDEX idx_finance_record_record_type ON finance_record(record_type);
CREATE INDEX idx_finance_record_period_date ON finance_record(period_date);

-- --------------------------------------------------------------------------
-- 47. 财务报表表 (finance_report)
-- 对应实体: FinanceReport.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS finance_report CASCADE;
CREATE TABLE finance_report (
    id              BIGSERIAL       PRIMARY KEY,
    report_type     VARCHAR(20)      NOT NULL,
    report_title    VARCHAR(200)     NOT NULL,
    period_type     VARCHAR(20)      NOT NULL DEFAULT 'monthly',
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    data_json       JSONB            DEFAULT NULL,
    summary_text    TEXT            DEFAULT NULL,
    status          INTEGER          NOT NULL DEFAULT 0,
    generated_by    VARCHAR(50)      DEFAULT NULL,
    generated_at    TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE finance_report IS '财务报表表';
CREATE INDEX idx_finance_report_report_type ON finance_report(report_type);
CREATE INDEX idx_finance_report_period ON finance_report(start_date, end_date);


-- ============================================================================
-- 第十三部分：采购相关表（基础结构）
-- ============================================================================

-- --------------------------------------------------------------------------
-- 48. 采购申请表 (purchase_request)
-- 对应实体: PurchaseRequest.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS purchase_request CASCADE;
CREATE TABLE purchase_request (
    id              BIGSERIAL       PRIMARY KEY,
    request_no      VARCHAR(50)      NOT NULL UNIQUE,
    title           VARCHAR(200)     NOT NULL,
    supplier_id     BIGINT           DEFAULT NULL,
    supplier_name   VARCHAR(200)     DEFAULT NULL,
    request_status  VARCHAR(20)      NOT NULL DEFAULT 'draft',
    total_amount    DECIMAL(14,2)    NOT NULL DEFAULT 0,
    currency        VARCHAR(10)      DEFAULT 'CNY',
    apply_dept_id   VARCHAR(64)      DEFAULT NULL,
    apply_user_id   VARCHAR(64)      DEFAULT NULL,
    apply_remark    TEXT            DEFAULT NULL,
    approve_user_id VARCHAR(64)      DEFAULT NULL,
    approve_time    TIMESTAMP        DEFAULT NULL,
    approve_remark  TEXT            DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE purchase_request IS '采购申请表';
CREATE INDEX idx_purchase_request_request_no ON purchase_request(request_no);
CREATE INDEX idx_purchase_request_supplier_id ON purchase_request(supplier_id);
CREATE INDEX idx_purchase_request_status ON purchase_request(request_status);

-- --------------------------------------------------------------------------
-- 49. 采购申请明细表 (purchase_request_item)
-- 对应实体: PurchaseRequestItem.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS purchase_request_item CASCADE;
CREATE TABLE purchase_request_item (
    id              BIGSERIAL       PRIMARY KEY,
    request_id      BIGINT           NOT NULL,
    product_name    VARCHAR(200)     NOT NULL,
    specification  VARCHAR(200)     DEFAULT NULL,
    quantity        INTEGER          NOT NULL DEFAULT 0,
    unit            VARCHAR(20)      DEFAULT '件',
    unit_price      DECIMAL(12,2)    NOT NULL DEFAULT 0,
    amount          DECIMAL(14,2)    NOT NULL DEFAULT 0,
    expected_date   DATE            DEFAULT NULL,
    remark          VARCHAR(300)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE purchase_request_item IS '采购申请明细表';
CREATE INDEX idx_purchase_request_item_request_id ON purchase_request_item(request_id);

-- --------------------------------------------------------------------------
-- 50. 采购订单表 (purchase_order)
-- 对应实体: PurchaseOrder.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS purchase_order CASCADE;
CREATE TABLE purchase_order (
    id              BIGSERIAL       PRIMARY KEY,
    order_no        VARCHAR(50)      NOT NULL UNIQUE,
    request_id      BIGINT           DEFAULT NULL,
    supplier_id     BIGINT           DEFAULT NULL,
    supplier_name   VARCHAR(200)     DEFAULT NULL,
    order_status    VARCHAR(20)      NOT NULL DEFAULT 'pending',
    total_amount    DECIMAL(14,2)    NOT NULL DEFAULT 0,
    paid_amount     DECIMAL(14,2)    NOT NULL DEFAULT 0,
    currency        VARCHAR(10)      DEFAULT 'CNY',
    delivery_date   DATE            DEFAULT NULL,
    receive_status  VARCHAR(20)      DEFAULT 'not_received',
    buyer_id        VARCHAR(64)      DEFAULT NULL,
    remark          TEXT            DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE purchase_order IS '采购订单表';
CREATE INDEX idx_purchase_order_order_no ON purchase_order(order_no);
CREATE INDEX idx_purchase_order_supplier_id ON purchase_order(supplier_id);
CREATE INDEX idx_purchase_order_status ON purchase_order(order_status);

-- --------------------------------------------------------------------------
-- 51. 采购入库表 (purchase_stockin)
-- 对应实体: PurchaseStockin.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS purchase_stockin CASCADE;
CREATE TABLE purchase_stockin (
    id              BIGSERIAL       PRIMARY KEY,
    stockin_no      VARCHAR(50)      NOT NULL UNIQUE,
    order_id        BIGINT           DEFAULT NULL,
    supplier_id     BIGINT           DEFAULT NULL,
    warehouse_id    BIGINT           DEFAULT NULL,
    stockin_status  VARCHAR(20)      NOT NULL DEFAULT 'draft',
    total_quantity  INTEGER          NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2)    NOT NULL DEFAULT 0,
    operator_id     VARCHAR(64)      DEFAULT NULL,
    remark          TEXT            DEFAULT NULL,
    received_at     TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE purchase_stockin IS '采购入库表';
CREATE INDEX idx_purchase_stockin_stockin_no ON purchase_stockin(stockin_no);
CREATE INDEX idx_purchase_stockin_order_id ON purchase_stockin(order_id);


-- ============================================================================
-- 第十四部分：人力资源基础表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 52. 职级表 (position_level)
-- 对应实体: PositionLevel.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS position_level CASCADE;
CREATE TABLE position_level (
    id              BIGSERIAL       PRIMARY KEY,
    level_name      VARCHAR(50)      NOT NULL,
    level_code      VARCHAR(50)      UNIQUE NOT NULL,
    level_rank      INTEGER          NOT NULL DEFAULT 1,
    min_salary      DECIMAL(12,2)    DEFAULT NULL,
    max_salary      DECIMAL(12,2)    DEFAULT NULL,
    description     VARCHAR(500)     DEFAULT NULL,
    status          INTEGER          NOT NULL DEFAULT 1,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE position_level IS '职级表';
CREATE INDEX idx_position_level_level_code ON position_level(level_code);

-- --------------------------------------------------------------------------
-- 53. 健康证表 (health_certificate)
-- 对应实体: HealthCertificate.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS health_certificate CASCADE;
CREATE TABLE health_certificate (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     VARCHAR(64)      NOT NULL,
    certificate_no VARCHAR(100)     NOT NULL,
    issue_date      DATE            DEFAULT NULL,
    expire_date      DATE            NOT NULL,
    issuing_org     VARCHAR(200)     DEFAULT NULL,
    health_check_org VARCHAR(200)   DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'valid',
    image_url       VARCHAR(500)     DEFAULT NULL,
    remark          VARCHAR(300)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE health_certificate IS '健康证表';
CREATE INDEX idx_health_certificate_employee_id ON health_certificate(employee_id);
CREATE INDEX idx_health_certificate_expire_date ON health_certificate(expire_date);

-- --------------------------------------------------------------------------
-- 54. 入职登记表 (onboarding_record)
-- 对应实体: OnboardingRecord.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS onboarding_record CASCADE;
CREATE TABLE onboarding_record (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     VARCHAR(64)      NOT NULL,
    onboarding_type VARCHAR(20)      DEFAULT 'new_hire',
    start_date      DATE            NOT NULL,
    probation_days  INTEGER          DEFAULT 90,
    prob_end_date   DATE            DEFAULT NULL,
    status          VARCHAR(20)      DEFAULT 'in_progress',
    documents       JSONB            DEFAULT NULL,
    remark          TEXT            DEFAULT NULL,
    completed_at    TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE onboarding_record IS '入职登记表';
CREATE INDEX idx_onboarding_record_employee_id ON onboarding_record(employee_id);


-- ============================================================================
-- 第十五部分：电子票据相关表（基础结构）
-- ============================================================================

-- --------------------------------------------------------------------------
-- 55. 电子发票表 (electronic_invoice)
-- 对应实体: ElectronicInvoice.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS electronic_invoice CASCADE;
CREATE TABLE electronic_invoice (
    id              BIGSERIAL       PRIMARY KEY,
    invoice_no      VARCHAR(50)      NOT NULL UNIQUE,
    invoice_code    VARCHAR(20)      NOT NULL,
    invoice_type    VARCHAR(20)      NOT NULL DEFAULT 'general',
    buyer_name      VARCHAR(200)     NOT NULL,
    buyer_tax_no    VARCHAR(50)      DEFAULT NULL,
    seller_name     VARCHAR(200)     NOT NULL,
    seller_tax_no   VARCHAR(50)      NOT NULL,
    total_amount    DECIMAL(14,2)    NOT NULL DEFAULT 0,
    tax_amount      DECIMAL(12,2)    NOT NULL DEFAULT 0,
    issue_date      DATE            DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'draft',
    pdf_url         VARCHAR(500)     DEFAULT NULL,
    xml_content     TEXT            DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)      DEFAULT NULL,
    updated_by      VARCHAR(50)      DEFAULT NULL,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE electronic_invoice IS '电子发票表';
CREATE INDEX idx_electronic_invoice_invoice_no ON electronic_invoice(invoice_no);
CREATE INDEX idx_electronic_invoice_buyer_name ON electronic_invoice(buyer_name);
CREATE INDEX idx_electronic_invoice_issue_date ON electronic_invoice(issue_date);

-- --------------------------------------------------------------------------
-- 56. 电子发票明细表 (electronic_invoice_item)
-- 对应实体: ElectronicInvoiceItem.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS electronic_invoice_item CASCADE;
CREATE TABLE electronic_invoice_item (
    id              BIGSERIAL       PRIMARY KEY,
    invoice_id      BIGINT           NOT NULL,
    line_no         INTEGER          NOT NULL,
    item_name        VARCHAR(200)     NOT NULL,
    specification  VARCHAR(200)     DEFAULT NULL,
    unit            VARCHAR(20)      DEFAULT '件',
    quantity        DECIMAL(14,4)    NOT NULL DEFAULT 0,
    unit_price      DECIMAL(12,2)    NOT NULL DEFAULT 0,
    amount          DECIMAL(14,2)    NOT NULL DEFAULT 0,
    tax_rate        DECIMAL(6,4)     DEFAULT 0,
    tax_amount      DECIMAL(12,2)    NOT NULL DEFAULT 0,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE electronic_invoice_item IS '电子发票明细表';
CREATE INDEX idx_electronic_invoice_item_invoice_id ON electronic_invoice_item(invoice_id);

-- --------------------------------------------------------------------------
-- 57. 电子凭证表 (electronic_voucher)
-- 对应实体: ElectronicVoucher.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS electronic_voucher CASCADE;
CREATE TABLE electronic_voucher (
    id              BIGSERIAL       PRIMARY KEY,
    voucher_no      VARCHAR(50)      NOT NULL UNIQUE,
    voucher_type    VARCHAR(20)      NOT NULL,
    title           VARCHAR(200)     NOT NULL,
    amount          DECIMAL(14,2)    NOT NULL DEFAULT 0,
    valid_from      DATE            NOT NULL,
    valid_until     DATE            NOT NULL,
    holder_name     VARCHAR(200)     DEFAULT NULL,
    holder_id_type  VARCHAR(20)     DEFAULT NULL,
    holder_id       VARCHAR(64)      DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'valid',
    content_json    JSONB            DEFAULT NULL,
    qrcode_url       VARCHAR(500)     DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE electronic_voucher IS '电子凭证表';
CREATE INDEX idx_electronic_voucher_voucher_no ON electronic_voucher(voucher_no);
CREATE INDEX idx_electronic_voucher_voucher_type ON electronic_voucher(voucher_type);
CREATE INDEX idx_electronic_voucher_holder_id ON electronic_voucher(holder_id_type, holder_id);

-- --------------------------------------------------------------------------
-- 58. 电子签名表 (electronic_signature)
-- 对应实体: ElectronicSignature.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS electronic_signature CASCADE;
CREATE TABLE electronic_signature (
    id              BIGSERIAL       PRIMARY KEY,
    signer_id       VARCHAR(64)      NOT NULL,
    signer_name     VARCHAR(100)     NOT NULL,
    signer_cert_no  VARCHAR(100)     DEFAULT NULL,
    signature_type  VARCHAR(20)      DEFAULT 'seal',
    sign_time       TIMESTAMP        NOT NULL,
    signed_data_hash VARCHAR(256)   DEFAULT NULL,
    original_file_url VARCHAR(500)  DEFAULT NULL,
    signed_file_url  VARCHAR(500)   DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'valid',
    verify_time     TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE electronic_signature IS '电子签名表';
CREATE INDEX idx_electronic_signature_signer_id ON electronic_signature(signer_id);
CREATE INDEX idx_electronic_signature_sign_time ON electronic_signature(sign_time);


-- ============================================================================
-- 第十六部分：其他辅助表
-- ============================================================================

-- --------------------------------------------------------------------------
-- 59. 文件附件表 (file_attachment)
-- 对应实体: FileAttachment.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS file_attachment CASCADE;
CREATE TABLE file_attachment (
    id              BIGSERIAL       PRIMARY KEY,
    business_type   VARCHAR(50)      NOT NULL,
    business_id     VARCHAR(64)      NOT NULL,
    file_name       VARCHAR(255)     NOT NULL,
    original_name   VARCHAR(255)     NOT NULL,
    file_path       VARCHAR(500)     NOT NULL,
    file_size       BIGINT           NOT NULL DEFAULT 0,
    file_type       VARCHAR(100)     DEFAULT NULL,
    mime_type       VARCHAR(100)     DEFAULT NULL,
    upload_user_id  VARCHAR(64)      DEFAULT NULL,
    upload_ip       VARCHAR(50)      DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE file_attachment IS '文件附件表';
CREATE INDEX idx_file_attachment_business ON file_attachment(business_type, business_id);
CREATE INDEX idx_file_attachment_upload_user ON file_attachment(upload_user_id);

-- --------------------------------------------------------------------------
-- 60. 注册码表 (registration_code)
-- 对应实体: RegistrationCode.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS registration_code CASCADE;
CREATE TABLE registration_code (
    id              BIGSERIAL       PRIMARY KEY,
    code            VARCHAR(20)      NOT NULL UNIQUE,
    code_type       VARCHAR(20)      NOT NULL DEFAULT 'invite',
    used_by         VARCHAR(64)      DEFAULT NULL,
    used_at         TIMESTAMP        DEFAULT NULL,
    expire_at       TIMESTAMP        NOT NULL,
    max_usage       INTEGER          NOT NULL DEFAULT 1,
    used_count      INTEGER          NOT NULL DEFAULT 0,
    status          VARCHAR(20)      NOT NULL DEFAULT 'active',
    created_by      VARCHAR(50)      DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER          NOT NULL DEFAULT 0
);
COMMENT ON TABLE registration_code IS '注册码/邀请码表';
CREATE INDEX idx_registration_code_code ON registration_code(code);
CREATE INDEX idx_registration_code_code_type ON registration_code(code_type);

-- --------------------------------------------------------------------------
-- 61. 审计日志表 (audit_log)
-- 对应实体: AuditLog.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS audit_log CASCADE;
CREATE TABLE audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    module          VARCHAR(100)     NOT NULL,
    operation       VARCHAR(50)      NOT NULL,
    target_type     VARCHAR(50)      DEFAULT NULL,
    target_id       VARCHAR(64)      DEFAULT NULL,
    before_data     JSONB            DEFAULT NULL,
    after_data      JSONB            DEFAULT NULL,
    diff_detail     JSONB            DEFAULT NULL,
    operator_id     VARCHAR(64)      NOT NULL,
    operator_name   VARCHAR(100)     DEFAULT NULL,
    ip_address      VARCHAR(50)      DEFAULT NULL,
    user_agent      TEXT            DEFAULT NULL,
    operate_time    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)     DEFAULT NULL
);
COMMENT ON TABLE audit_log IS '审计日志表';
CREATE INDEX idx_audit_log_module ON audit_log(module);
CREATE INDEX idx_audit_log_target ON audit_log(target_type, target_id);
CREATE INDEX idx_audit_log_operator_id ON audit_log(operator_id);
CREATE INDEX idx_audit_log_operate_time ON audit_log(operate_time);

-- --------------------------------------------------------------------------
-- 62. 密码历史表 (password_history)
-- 对应实体: PasswordHistory.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS password_history CASCADE;
CREATE TABLE password_history (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT           NOT NULL,
    password_hash   VARCHAR(255)     NOT NULL,
    changed_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_type     VARCHAR(20)      DEFAULT 'manual'
);
COMMENT ON TABLE password_history IS '密码修改历史表';
CREATE INDEX idx_password_history_user_id ON password_history(user_id);

-- --------------------------------------------------------------------------
-- 63. 密码重置日志表 (password_reset_log)
-- 对应实体: PasswordResetLog.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS password_reset_log CASCADE;
CREATE TABLE password_reset_log (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT           NOT NULL,
    reset_token     VARCHAR(255)     NOT NULL,
    token_expire_at TIMESTAMP      NOT NULL,
    reset_ip        VARCHAR(50)      DEFAULT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'pending',
    used_at         TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE password_reset_log IS '密码重置日志表';
CREATE INDEX idx_password_reset_log_user_id ON password_reset_log(user_id);
CREATE INDEX idx_password_reset_log_reset_token ON password_reset_log(reset_token);

-- --------------------------------------------------------------------------
-- 64. 数据变更历史表 (data_change_history)
-- 对应实体: DataChangeHistory.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS data_change_history CASCADE;
CREATE TABLE data_change_history (
    id              BIGSERIAL       PRIMARY KEY,
    table_name      VARCHAR(100)     NOT NULL,
    record_id       VARCHAR(64)      NOT NULL,
    operation_type  VARCHAR(20)      NOT NULL,
    before_data     JSONB            DEFAULT NULL,
    after_data      JSONB            DEFAULT NULL,
    change_fields  JSONB            DEFAULT NULL,
    operator_id     VARCHAR(64)      DEFAULT NULL,
    operator_name   VARCHAR(100)     DEFAULT NULL,
    change_time     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(300)     DEFAULT NULL
);
COMMENT ON TABLE data_change_history IS '数据变更历史表';
CREATE INDEX idx_data_change_history_table ON data_change_history(table_name, record_id);
CREATE INDEX idx_data_change_history_operator ON data_change_history(operator_id);
CREATE INDEX idx_data_change_history_time ON data_change_history(change_time);

-- --------------------------------------------------------------------------
-- 65. 系统初始化状态表 (system_init_status)
-- 对应实体: SystemInitStatus.java
-- --------------------------------------------------------------------------
DROP TABLE IF EXISTS system_init_status CASCADE;
CREATE TABLE system_init_status (
    id              BIGSERIAL       PRIMARY KEY,
    init_module    VARCHAR(50)      NOT NULL,
    init_status    VARCHAR(20)      NOT NULL DEFAULT 'pending',
    init_version   VARCHAR(20)      DEFAULT 'v0.12',
    init_message   TEXT            DEFAULT NULL,
    init_progress  INTEGER          DEFAULT 0,
    started_at     TIMESTAMP        DEFAULT NULL,
    completed_at   TIMESTAMP        DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE system_init_status IS '系统初始化状态表';
CREATE UNIQUE INDEX uk_system_init_module ON system_init_status(init_module);

-- ==========================================================================
-- 建表脚本完成
-- 总计: 65 张核心业务表
-- 包含:
--   系统核心: users, user_roles, roles, role_permissions, permissions, menus,
--            departments, employees, positions, position_level
--   食品溯源: food_category, food, food_trace, food_trace_code
--   订单模块: orders, order_items, kitchen_order, call_record, dining_table
--   库存仓储: inventory, warehouse, stores, suppliers, member
--   设备硬件: sys_device, tray, takeout_locker, locker_slot,
--            scan_device, scan_record, weighing_device, weighing_record
--   日志审计: sys_operation_logs, login_log, user_session,
--            audit_log, password_history, password_reset_log, data_change_history
--   系统配置: sys_config, sys_setting, global_config, enterprise_config,
--            system_init_status
--   消息通知: notification, notification_setting
--   打印标签: label_template, print_task
--   定时调度: scheduled_task, task_execution_log
--   财务管理: finance_account, finance_record, finance_report
--   采购管理: purchase_request, purchase_request_item,
--               purchase_order, purchase_stockin
--   人力资源: health_certificate, onboarding_record
--   电子票据: electronic_invoice, electronic_invoice_item,
--               electronic_voucher, electronic_signature
--   辅助功能: file_attachment, registration_code
-- ==========================================================================

-- 输出统计信息
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM information_schema.tables
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
    RAISE NOTICE 'PostgreSQL Schema v0.12 建表完成! 共创建 % 张业务表', table_count;
END $$;
