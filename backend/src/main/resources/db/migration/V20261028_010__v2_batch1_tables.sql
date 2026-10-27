-- =====================================================================
-- V20261028_010__v2_batch1_tables.sql
-- V2.0 第一批（Owner 2026-10-28 批准开工）：
--   over_age_worker      —— 超龄用工（字段**按前端 types/hr/over-age-worker.ts 反推**，不另设计）
--   scan_record          —— 后厨扫码记录（补 action_type 区分 绑定/出餐；user_id 允许为空）
--   takeout_locker       —— 取餐柜（V2.0 功能，前端菜单置灰）
--   locker_slot          —— 取餐柜格口（V2.0 功能）
--   supplier_evaluations —— 供应商评估（V2.0 功能）
-- 说明：全部为「两边都无表」，故均按实体反推；生产库无历史数据，无需回填。
-- =====================================================================

-- 1) over_age_worker（前端 types/hr/over-age-worker.ts:47-77 → 15 业务字段 + 审计列）
CREATE TABLE IF NOT EXISTS over_age_worker (
    id                          bigint GENERATED ALWAYS AS IDENTITY,
    employee_id                 varchar(64)  NOT NULL,
    employee_name               varchar(100),
    retirement_date             varchar(20),   -- 前端为 string（YYYY-MM-DD），保持与前端一致
    reemployment_date           varchar(20),
    agreement_no                varchar(64),
    agreement_start_date        varchar(20),
    agreement_end_date          varchar(20),
    work_injury_insurance_no    varchar(64),
    work_injury_insurance_expiry varchar(20),
    health_check_expiry         varchar(20),
    health_check_result         varchar(20),   -- qualified/restricted/unqualified
    restricted_positions        jsonb,         -- 前端 string[]；JsonbStringTypeHandler
    remark                      varchar(500),
    deleted                     integer NOT NULL DEFAULT 0,
    create_by                   bigint,
    create_time                 timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by                   bigint,
    update_time                 timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT over_age_worker_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_oaw_employee ON over_age_worker (employee_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_oaw_agreement_end ON over_age_worker (agreement_end_date) WHERE deleted = 0;  -- 协议到期预警
CREATE INDEX IF NOT EXISTS idx_oaw_health_expiry ON over_age_worker (health_check_expiry) WHERE deleted = 0;  -- 体检到期预警

-- 2) scan_record（扫描留痕；scan_device 生产库无表，故 device_id/device_code 允许为空）
CREATE TABLE IF NOT EXISTS scan_record (
    id               bigint GENERATED ALWAYS AS IDENTITY,
    scan_code        varchar(200),                -- 扫了什么码
    code_type        varchar(32),                 -- 码类型
    action_type      varchar(32),                 -- ★新增：BIND(绑定) / OUTBOUND(出餐) / QUERY / UNKNOWN
    target_type      varchar(32),                 -- ★新增：ORDER / DISH / TRAY（扫的是什么对象）
    target_id        varchar(64),                 -- ★新增：对应对象ID（订单号/菜品ID/托盘码）
    device_id        bigint,                      -- 允许为空（设备注册表可能缺失）
    device_code      varchar(64),
    scan_time        timestamp without time zone,
    operator         varchar(100),
    operator_id      bigint,                      -- ★允许为空（后厨共用账号）
    scan_location    varchar(200),
    scan_result      varchar(32),
    failure_reason   varchar(500),
    business_type    varchar(32),
    business_id      bigint,
    process_status   varchar(20),                 -- PROCESSED / ERROR
    remark           varchar(500),
    create_time      timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time      timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    create_by        varchar(64),
    update_by        varchar(64),
    deleted          integer NOT NULL DEFAULT 0,
    CONSTRAINT scan_record_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sr_time ON scan_record (scan_time);
CREATE INDEX IF NOT EXISTS idx_sr_action ON scan_record (action_type, scan_time);   -- 按动作+时间查
CREATE INDEX IF NOT EXISTS idx_sr_target ON scan_record (target_type, target_id);   -- 按订单/菜品/托盘反查
CREATE INDEX IF NOT EXISTS idx_sr_business ON scan_record (business_type, business_id);

-- 3) takeout_locker（取餐柜；V2.0 功能，前端置灰）
CREATE TABLE IF NOT EXISTS takeout_locker (
    id                  bigint GENERATED ALWAYS AS IDENTITY,
    locker_code         varchar(64) NOT NULL,
    locker_name         varchar(100),
    locker_type         varchar(32),
    device_ip           varchar(64),
    device_port         integer,
    status              varchar(32),
    connection_type     varchar(32),
    total_slots         integer,
    available_slots     integer,
    location            varchar(200),
    last_operation_time timestamp without time zone,
    today_usage_count   integer DEFAULT 0,
    manager             varchar(100),
    remark              varchar(500),
    create_time         timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time         timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    create_by           varchar(64),
    update_by           varchar(64),
    deleted             integer NOT NULL DEFAULT 0,
    CONSTRAINT takeout_locker_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tl_code ON takeout_locker (locker_code) WHERE deleted = 0;

-- 4) locker_slot（格口）
CREATE TABLE IF NOT EXISTS locker_slot (
    id                   bigint GENERATED ALWAYS AS IDENTITY,
    locker_id            bigint,
    locker_code          varchar(64),
    slot_code            varchar(32),
    slot_type            varchar(32),
    status               varchar(32),           -- FREE/OCCUPIED/PICKED/TIMEOUT
    order_id             bigint,
    pickup_code          varchar(32),
    put_time             timestamp without time zone,
    expected_pickup_time timestamp without time zone,
    pickup_time          timestamp without time zone,
    put_operator         varchar(100),
    pickup_operator      varchar(100),
    timeout_minutes      integer,
    remark               varchar(500),
    create_time          timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time          timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    create_by            varchar(64),
    update_by            varchar(64),
    deleted              integer NOT NULL DEFAULT 0,
    CONSTRAINT locker_slot_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ls_slot ON locker_slot (locker_id, slot_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ls_order ON locker_slot (order_id) WHERE deleted = 0;

-- 5) supplier_evaluations（供应商评估；V2.0 功能）
CREATE TABLE IF NOT EXISTS supplier_evaluations (
    evaluation_id    bigint GENERATED ALWAYS AS IDENTITY,
    supplier_id      bigint,
    evaluation_period varchar(32),
    delivery_score   numeric(6,2),
    quality_score    numeric(6,2),
    price_score      numeric(6,2),
    service_score    numeric(6,2),
    total_score      numeric(6,2),
    order_count      integer,
    on_time_rate     numeric(6,2),
    qualified_rate   numeric(6,2),
    evaluation_user_id bigint,
    evaluation_time  timestamp without time zone,
    remark           varchar(500),
    CONSTRAINT supplier_evaluations_pkey PRIMARY KEY (evaluation_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_se_supplier_period ON supplier_evaluations (supplier_id, evaluation_period);
