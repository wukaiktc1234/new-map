-- =====================================================================
-- V20261028_009__compliance_evidence_tables.sql
-- 合规留痕三件套之一/二（**按实体反推**，生产库无结构可抄）：
--   material_usage_record —— 出餐扣料留痕（食品安全：原料使用可追溯）
--   trace_code_log       —— 追溯码操作留痕（一物一码全流程审计）
--
-- 说明：operation_logs 原计划在此批，但复核发现 **OperationLogEntity 映射的是 sys_operation_logs**（生产库已存在），
--       /v1/operation-logs 查的就是那张表；此前给它打的 @UnfinishedFeature(table='operation_logs') 属误标，已移除。
--       operation_logs 本体（entity OperationLog）仅被 OrderTimeoutTask 注入、无调用 → 属死代码，本批不建。
--
-- 类型推断：String→varchar / text；BigDecimal→numeric(14,4)；LocalDateTime→timestamp；Long→bigint；Integer→integer
-- =====================================================================

-- 1) material_usage_record（出餐扣料留痕）
CREATE TABLE IF NOT EXISTS material_usage_record (
    id              bigint GENERATED ALWAYS AS IDENTITY,
    record_id       varchar(64)  NOT NULL,          -- String recordId（UR+时间戳，应用生成）
    trace_code_id   varchar(64),                     -- String traceCodeId
    trace_code      varchar(128),                    -- String traceCode
    material_id     varchar(64),                     -- String materialId
    material_name   varchar(200),                    -- String materialName
    used_quantity   numeric(14,4),                   -- BigDecimal usedQuantity
    unit            varchar(20),                     -- String unit
    order_id        varchar(64),                     -- String orderId
    kitchen_order_id varchar(64),                    -- String kitchenOrderId
    order_number    varchar(64),                     -- String orderNumber
    dish_name       varchar(200),                    -- String dishName
    operator_id     varchar(64),                     -- String operatorId
    operator_name   varchar(100),                    -- String operatorName
    usage_time      timestamp without time zone,     -- LocalDateTime usageTime
    remark          varchar(500),                    -- String remark
    create_time     timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted         integer NOT NULL DEFAULT 0,
    CONSTRAINT material_usage_record_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mur_record_id ON material_usage_record (record_id);
CREATE INDEX IF NOT EXISTS idx_mur_kitchen_order ON material_usage_record (kitchen_order_id) WHERE deleted = 0;  -- 按后厨单查扣料
CREATE INDEX IF NOT EXISTS idx_mur_trace_code ON material_usage_record (trace_code) WHERE deleted = 0;           -- 追溯反查
CREATE INDEX IF NOT EXISTS idx_mur_usage_time ON material_usage_record (usage_time);                            -- 时间范围报表

-- 2) trace_code_log（追溯码操作留痕）
CREATE TABLE IF NOT EXISTS trace_code_log (
    trace_code_log_id    varchar(64) NOT NULL,       -- String（IdType.ASSIGN_ID）
    trace_code_id        varchar(64),                -- String
    code                 varchar(128),               -- String
    operation_type       varchar(32),                -- String（SCAN/STATUS_CHANGE/RECALL…）
    before_status        varchar(32),                -- String
    after_status         varchar(32),                -- String
    operator             varchar(100),               -- String
    operator_id          bigint,                     -- Long
    operation_time       timestamp without time zone,-- LocalDateTime
    operation_location   varchar(200),               -- String
    operation_device     varchar(100),               -- String
    operation_description varchar(500),              -- String
    CONSTRAINT trace_code_log_pkey PRIMARY KEY (trace_code_log_id)
);
CREATE INDEX IF NOT EXISTS idx_tcl_trace_code_id ON trace_code_log (trace_code_id);
CREATE INDEX IF NOT EXISTS idx_tcl_code ON trace_code_log (code);
CREATE INDEX IF NOT EXISTS idx_tcl_time ON trace_code_log (operation_time);

COMMENT ON TABLE material_usage_record IS '出餐扣料留痕；V20261028_009 按实体反推生成（生产库无结构），字段以实体为准，待产品正式设计复核';
COMMENT ON TABLE trace_code_log IS '追溯码操作留痕；V20261028_009 按实体反推生成（生产库无结构），字段以实体为准，待产品正式设计复核';
