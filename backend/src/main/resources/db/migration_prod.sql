-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- =============================================================
-- 食品溯源系统 - 生产环境数据库初始化脚本（PostgreSQL版）
-- 版本：1.0.0 (PG兼容重写)
-- 日期：2026-04-29
-- 注意：原脚本为MySQL语法，已完全改写为PostgreSQL兼容
--       生产环境部署请使用：pg_dump / pg_restore 或 Flyway
-- =============================================================

-- =============================================================
-- 重要说明
-- =============================================================
-- 1. PostgreSQL 不支持 CREATE DATABASE ... CHARACTER SET（编码在创建集群时确定）
-- 2. PostgreSQL 不支持 USE 语句（使用连接字符串指定数据库）
-- 3. 用户/权限管理语法与MySQL不同，请使用独立的安全脚本
-- 4. 此脚本仅包含表结构和基础数据，不含用户权限配置
-- 5. 建议通过 Flyway 管理生产环境迁移，而非手动执行此脚本
-- =============================================================

-- =============================================================
-- 以下为原脚本中 MySQL 特有命令的 PG 替代方案说明：
--   原始: CREATE DATABASE food_traceability CHARACTER SET utf8mb4;
--   替代: 在 initdb 时指定 --encoding=UTF8，或 CREATE DATABASE food_traceability ENCODING 'UTF8';
--
--   原始: USE food_traceability;
--   替代: 连接时指定数据库: psql -d food_traceability
--
--   原始: CREATE USER ... IDENTIFIED BY ...;
--   替代: CREATE USER username WITH PASSWORD 'xxx';
--
--   原始: GRANT ALL ON db.* TO user;
--   替代: GRANT ALL PRIVILEGES ON DATABASE db TO user;
--
--   原始: FLUSH PRIVILEGES;
--   替代: PG不需要，权限变更即时生效
-- =============================================================

-- =============================================================
-- 生产环境建表（基于 V1.0.0__init_postgresql.sql 的精简版本）
-- 完整版本请参考 V1.0.0__init_postgresql.sql（54张表）
-- =============================================================

BEGIN;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username             VARCHAR(100) NOT NULL,
    password             VARCHAR(255) NOT NULL,
    name                 VARCHAR(100),
    email                VARCHAR(100),
    phone                VARCHAR(20),
    status               SMALLINT DEFAULT 1,
    department_id        BIGINT,
    store_id             VARCHAR(50),
    last_login_time      TIMESTAMP,
    last_login_ip        VARCHAR(50),
    is_locked            SMALLINT DEFAULT 0,
    lock_time            TIMESTAMP,
    password_error_count INTEGER DEFAULT 0,
    last_password_error_time TIMESTAMP,
    need_change_password SMALLINT DEFAULT 0,
    last_password_change_time TIMESTAMP,
    avatar               VARCHAR(500),
    deleted              SMALLINT DEFAULT 0,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(50),
    updated_by           VARCHAR(50),

    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_department ON users(department_id);

COMMENT ON TABLE users IS '用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_code  VARCHAR(50) NOT NULL,
    role_name  VARCHAR(100) NOT NULL,
    description TEXT,
    role_type  SMALLINT DEFAULT 1,
    level      INTEGER DEFAULT 1,
    status     SMALLINT DEFAULT 1,
    is_system  SMALLINT DEFAULT 0,
    deleted    SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_roles_role_code UNIQUE (role_code)
);

CREATE INDEX IF NOT EXISTS idx_roles_role_code ON roles(role_code);
CREATE INDEX IF NOT EXISTS idx_roles_status ON roles(status);

COMMENT ON TABLE roles IS '角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    permission_type SMALLINT DEFAULT 1,
    module          VARCHAR(50),
    parent_id       BIGINT DEFAULT 0,
    level           INTEGER DEFAULT 1,
    sort_order      INTEGER DEFAULT 0,
    status          SMALLINT DEFAULT 1,
    deleted         SMALLINT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_permissions_permission_code UNIQUE (permission_code)
);

CREATE INDEX IF NOT EXISTS idx_permissions_permission_code ON permissions(permission_code);
CREATE INDEX IF NOT EXISTS idx_permissions_parent_id ON permissions(parent_id);

COMMENT ON TABLE permissions IS '权限表';

-- 部门表
CREATE TABLE IF NOT EXISTS departments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dept_code   VARCHAR(50) NOT NULL,
    dept_name   VARCHAR(100) NOT NULL,
    parent_id   BIGINT DEFAULT 0,
    level       INTEGER DEFAULT 1,
    sort_order  INTEGER DEFAULT 0,
    status      SMALLINT DEFAULT 1,
    description TEXT,
    deleted     SMALLINT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_departments_dept_code UNIQUE (dept_code)
);

CREATE INDEX IF NOT EXISTS idx_departments_dept_code ON departments(dept_code);
CREATE INDEX IF NOT EXISTS idx_departments_parent_id ON departments(parent_id);

COMMENT ON TABLE departments IS '部门表';

-- 产品表（物料/原料）
CREATE TABLE IF NOT EXISTS product (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    code          VARCHAR(50),
    category_id   BIGINT,
    category_name VARCHAR(100),
    unit          VARCHAR(20),
    price         DECIMAL(10,2),
    cost_price    DECIMAL(10,2),
    barcode       VARCHAR(100),
    specification VARCHAR(200),
    origin        VARCHAR(200),
    supplier_id   BIGINT,
    supplier_name VARCHAR(200),
    status        SMALLINT DEFAULT 1,
    deleted       SMALLINT DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_product_code UNIQUE (code)
);

COMMENT ON TABLE product IS '产品表(物料/原料)';

-- 库存表
CREATE TABLE IF NOT EXISTS inventory (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    product_name    VARCHAR(100) NOT NULL,
    warehouse_id    BIGINT NOT NULL,
    warehouse_name  VARCHAR(50) NOT NULL,
    current_stock   INTEGER NOT NULL DEFAULT 0,
    safety_stock    INTEGER NOT NULL DEFAULT 0,
    unit            VARCHAR(20) NOT NULL,
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse_id ON inventory(warehouse_id);

COMMENT ON TABLE inventory IS '库存表';

-- 仓库表
CREATE TABLE IF NOT EXISTS warehouse (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name     VARCHAR(50) NOT NULL,
    code     VARCHAR(20) NOT NULL,
    type     VARCHAR(20) NOT NULL,
    location VARCHAR(100) NOT NULL,
    manager  VARCHAR(20) NOT NULL,
    status   SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_warehouse_code UNIQUE (code)
);

COMMENT ON TABLE warehouse IS '仓库表';

-- =============================================================
-- 插入基础数据
-- =============================================================

INSERT INTO roles (role_code, role_name, description, role_type, level, status, is_system)
VALUES
    ('admin', 'Super Admin', 'System super admin, has all permissions', 1, 999, 1, 1),
    ('user', 'Normal User', 'System normal user, has basic permissions', 1, 1, 1, 1)
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO departments (dept_code, dept_name, parent_id, level, sort_order, status, description)
VALUES
    ('DEPT001', '总经办', 0, 1, 1, 1, '公司最高管理部门'),
    ('DEPT002', '财务部', 0, 1, 2, 1, '负责公司财务核算和管理'),
    ('DEPT003', '采购部', 0, 1, 3, 1, '负责食品原材料采购'),
    ('DEPT004', '仓储部', 0, 1, 4, 1, '负责仓库管理和库存控制'),
    ('DEPT005', '销售部', 0, 1, 5, 1, '负责产品销售和客户管理'),
    ('DEPT006', '生产部', 0, 1, 6, 1, '负责食品生产加工'),
    ('DEPT007', '质检部', 0, 1, 7, 1, '负责产品质量检测'),
    ('DEPT008', '行政部', 0, 1, 8, 1, '负责公司行政事务')
ON CONFLICT (dept_code) DO NOTHING;

INSERT INTO warehouse (name, code, type, location, manager, status)
VALUES
    ('主仓库', 'WH001', 'main', '北京市朝阳区', '张三', 1),
    ('备用仓库', 'WH002', 'backup', '北京市海淀区', '李四', 1),
    ('临时仓库', 'WH003', 'temp', '北京市丰台区', '王五', 1)
ON CONFLICT (code) DO NOTHING;

COMMIT;

-- =============================================================
-- 验证步骤（PG版本）
-- =============================================================
-- 1. 验证所有表已创建成功：\dt 或 SELECT tablename FROM pg_tables WHERE schemaname='public';
-- 2. 验证基础数据已插入：SELECT COUNT(*) FROM roles; SELECT COUNT(*) FROM departments;
-- 3. 验证索引已创建：\di 或 SELECT indexname FROM pg_indexes WHERE schemaname='public';
-- 4. 备份命令: pg_dump -U postgres -d food_traceability > backup_$(date +%Y%m%d).sql
-- =============================================================
