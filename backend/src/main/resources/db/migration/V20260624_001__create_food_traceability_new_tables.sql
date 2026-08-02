-- ============================================
-- 食品追溯模块 Phase 1 - 新增表结构
-- 包含：检验记录、质量标准、质量记录、消费者扫码记录、临期预警记录
-- 版本: V20260624_001
-- 说明: 全部使用 BIGINT GENERATED ALWAYS AS IDENTITY 自增主键
--       兼容 H2 (MODE=PostgreSQL) 与 PostgreSQL 18
-- ============================================

-- ============================================
-- T1. 检验记录表
-- 记录入库检验、加工检验、成品检验等全流程检验数据
-- ============================================
CREATE TABLE IF NOT EXISTS food_inspection (
    inspection_id        BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inspection_no        VARCHAR(64)    NOT NULL,
    trace_code_id        BIGINT,
    trace_code           VARCHAR(64),
    batch_no             VARCHAR(64),
    supplier_id          BIGINT,
    supplier_name        VARCHAR(200),
    material_id          VARCHAR(32),
    material_name        VARCHAR(200),
    inspection_type      VARCHAR(20)    NOT NULL,           -- INCOMING入库检验/PROCESS加工检验/FINAL成品检验
    inspection_result    VARCHAR(20)    NOT NULL,           -- QUALIFIED合格/UNQUALIFIED不合格/CONDITIONAL有条件合格
    inspection_item      VARCHAR(200),
    inspection_value     VARCHAR(200),
    standard_value       VARCHAR(200),
    inspection_unit      VARCHAR(50),
    inspector_id         BIGINT,
    inspector_name       VARCHAR(100),
    inspection_time      TIMESTAMP      NOT NULL,
    inspection_location  VARCHAR(200),
    report_url           VARCHAR(500),
    inspection_items     JSONB,                             -- 检验项明细（JSON数组）
    remark               VARCHAR(500),
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE food_inspection IS '检验记录表 - 记录入库/加工/成品检验全流程数据';
COMMENT ON COLUMN food_inspection.inspection_no IS '检验编号（唯一）';
COMMENT ON COLUMN food_inspection.trace_code_id IS '关联追溯码ID';
COMMENT ON COLUMN food_inspection.trace_code IS '追溯码（冗余便于查询）';
COMMENT ON COLUMN food_inspection.batch_no IS '批次号';
COMMENT ON COLUMN food_inspection.supplier_id IS '供应商ID';
COMMENT ON COLUMN food_inspection.material_id IS '物料ID';
COMMENT ON COLUMN food_inspection.inspection_type IS '检验类型：INCOMING入库检验/PROCESS加工检验/FINAL成品检验';
COMMENT ON COLUMN food_inspection.inspection_result IS '检验结果：QUALIFIED合格/UNQUALIFIED不合格/CONDITIONAL有条件合格';
COMMENT ON COLUMN food_inspection.inspection_item IS '检验项目';
COMMENT ON COLUMN food_inspection.inspection_value IS '检验值';
COMMENT ON COLUMN food_inspection.standard_value IS '标准值';
COMMENT ON COLUMN food_inspection.inspection_unit IS '检验单位';
COMMENT ON COLUMN food_inspection.inspector_id IS '检验员ID';
COMMENT ON COLUMN food_inspection.inspector_name IS '检验员姓名';
COMMENT ON COLUMN food_inspection.inspection_time IS '检验时间';
COMMENT ON COLUMN food_inspection.inspection_location IS '检验地点';
COMMENT ON COLUMN food_inspection.report_url IS '检验报告URL';
COMMENT ON COLUMN food_inspection.inspection_items IS '检验项明细（JSON数组）';

-- 唯一索引：检验编号
CREATE UNIQUE INDEX uk_food_inspection_inspection_no ON food_inspection (inspection_no) WHERE deleted = 0;
-- 普通索引
CREATE INDEX idx_food_inspection_trace_code_id ON food_inspection (trace_code_id) WHERE deleted = 0;
CREATE INDEX idx_food_inspection_batch_no ON food_inspection (batch_no) WHERE deleted = 0;
CREATE INDEX idx_food_inspection_supplier_id ON food_inspection (supplier_id) WHERE deleted = 0;
CREATE INDEX idx_food_inspection_result ON food_inspection (inspection_result) WHERE deleted = 0;


-- ============================================
-- T2. 质量标准表
-- 维护国标/行标/企标/地方标准等质量标准库
-- ============================================
CREATE TABLE IF NOT EXISTS food_quality_standard (
    standard_id           BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    standard_no           VARCHAR(64)    NOT NULL,
    standard_name         VARCHAR(200)   NOT NULL,
    standard_type         VARCHAR(20)    NOT NULL,           -- NATIONAL国标/INDUSTRY行标/ENTERPRISE企标/LOCAL地方
    category              VARCHAR(100),                      -- 食品分类
    target_material_id    VARCHAR(32),                       -- 适用物料ID
    target_material_name  VARCHAR(200),                      -- 适用物料名称
    indicator_items       JSONB          NOT NULL,           -- 指标项JSON数组：[{name,value,unit,min,max}]
    status                VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE生效/INACTIVE失效
    effective_date        DATE,                              -- 生效日期
    expiry_date           DATE,                              -- 失效日期
    issuing_authority     VARCHAR(200),                      -- 发布机构
    version               VARCHAR(20),                       -- 版本号
    remark                VARCHAR(500),
    create_time           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE food_quality_standard IS '质量标准表 - 维护国标/行标/企标/地方标准库';
COMMENT ON COLUMN food_quality_standard.standard_no IS '标准编号（唯一）';
COMMENT ON COLUMN food_quality_standard.standard_name IS '标准名称';
COMMENT ON COLUMN food_quality_standard.standard_type IS '标准类型：NATIONAL国标/INDUSTRY行标/ENTERPRISE企标/LOCAL地方';
COMMENT ON COLUMN food_quality_standard.category IS '食品分类';
COMMENT ON COLUMN food_quality_standard.target_material_id IS '适用物料ID';
COMMENT ON COLUMN food_quality_standard.target_material_name IS '适用物料名称';
COMMENT ON COLUMN food_quality_standard.indicator_items IS '指标项JSON数组：[{name,value,unit,min,max}]';
COMMENT ON COLUMN food_quality_standard.status IS '状态：ACTIVE生效/INACTIVE失效';
COMMENT ON COLUMN food_quality_standard.effective_date IS '生效日期';
COMMENT ON COLUMN food_quality_standard.expiry_date IS '失效日期';
COMMENT ON COLUMN food_quality_standard.issuing_authority IS '发布机构';
COMMENT ON COLUMN food_quality_standard.version IS '版本号';

-- 唯一索引：标准编号
CREATE UNIQUE INDEX uk_food_quality_standard_no ON food_quality_standard (standard_no) WHERE deleted = 0;
-- 普通索引
CREATE INDEX idx_food_quality_standard_type ON food_quality_standard (standard_type) WHERE deleted = 0;
CREATE INDEX idx_food_quality_standard_status ON food_quality_standard (status) WHERE deleted = 0;


-- ============================================
-- T3. 质量记录表
-- 记录质量检测数据及异常处理全过程
-- ============================================
CREATE TABLE IF NOT EXISTS food_quality_record (
    quality_record_id      BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    record_no              VARCHAR(64)    NOT NULL,
    trace_code_id          BIGINT,
    trace_code             VARCHAR(64),
    batch_no               VARCHAR(64),
    standard_id            BIGINT,                            -- 关联质量标准ID
    standard_name          VARCHAR(200),                      -- 冗余标准名称
    material_id            VARCHAR(32),
    material_name          VARCHAR(200),
    inspection_data        JSONB          NOT NULL,           -- 检验数据JSON对象
    abnormal_level         VARCHAR(20)    NOT NULL DEFAULT 'NORMAL',  -- NORMAL正常/WARNING警告/CRITICAL严重
    handling_status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING',  -- PENDING待处理/PROCESSING处理中/RESOLVED已解决/CLOSED已关闭
    handling_result        VARCHAR(500),                      -- 处理结果
    handled_by_id          BIGINT,                            -- 处理人ID
    handled_by_name        VARCHAR(100),                      -- 处理人姓名
    handled_time           TIMESTAMP,                         -- 处理时间
    root_cause             VARCHAR(500),                      -- 根本原因
    affected_trace_codes   JSONB,                             -- 受影响追溯码列表
    remark                 VARCHAR(500),
    create_time            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE food_quality_record IS '质量记录表 - 记录质量检测数据及异常处理全过程';
COMMENT ON COLUMN food_quality_record.record_no IS '记录编号（唯一）';
COMMENT ON COLUMN food_quality_record.trace_code_id IS '关联追溯码ID';
COMMENT ON COLUMN food_quality_record.trace_code IS '追溯码（冗余）';
COMMENT ON COLUMN food_quality_record.batch_no IS '批次号';
COMMENT ON COLUMN food_quality_record.standard_id IS '关联质量标准ID';
COMMENT ON COLUMN food_quality_record.standard_name IS '冗余标准名称';
COMMENT ON COLUMN food_quality_record.material_id IS '物料ID';
COMMENT ON COLUMN food_quality_record.material_name IS '物料名称';
COMMENT ON COLUMN food_quality_record.inspection_data IS '检验数据JSON对象';
COMMENT ON COLUMN food_quality_record.abnormal_level IS '异常级别：NORMAL正常/WARNING警告/CRITICAL严重';
COMMENT ON COLUMN food_quality_record.handling_status IS '处理状态：PENDING待处理/PROCESSING处理中/RESOLVED已解决/CLOSED已关闭';
COMMENT ON COLUMN food_quality_record.handling_result IS '处理结果';
COMMENT ON COLUMN food_quality_record.handled_by_id IS '处理人ID';
COMMENT ON COLUMN food_quality_record.handled_by_name IS '处理人姓名';
COMMENT ON COLUMN food_quality_record.handled_time IS '处理时间';
COMMENT ON COLUMN food_quality_record.root_cause IS '根本原因';
COMMENT ON COLUMN food_quality_record.affected_trace_codes IS '受影响追溯码列表（JSON数组）';

-- 唯一索引：记录编号
CREATE UNIQUE INDEX uk_food_quality_record_no ON food_quality_record (record_no) WHERE deleted = 0;
-- 普通索引
CREATE INDEX idx_food_quality_record_trace_code_id ON food_quality_record (trace_code_id) WHERE deleted = 0;
CREATE INDEX idx_food_quality_record_abnormal_level ON food_quality_record (abnormal_level) WHERE deleted = 0;
CREATE INDEX idx_food_quality_record_handling_status ON food_quality_record (handling_status) WHERE deleted = 0;


-- ============================================
-- T4. 消费者扫码记录表
-- 记录消费者扫码查询追溯信息的全量行为数据
-- ============================================
CREATE TABLE IF NOT EXISTS trace_scan_record (
    scan_id                BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trace_code_id          BIGINT         NOT NULL,
    trace_code             VARCHAR(64)    NOT NULL,
    scan_time              TIMESTAMP      NOT NULL,
    scan_location          VARCHAR(200),                      -- 扫码位置（粗略）
    scan_location_detail   JSONB,                             -- 位置详情：{province,city,district,address,lat,lng}
    ip_address             VARCHAR(50),
    user_agent             VARCHAR(500),
    device_type            VARCHAR(20),                       -- MOBILE/PC/TABLET
    scan_count             INTEGER        NOT NULL DEFAULT 1, -- 该码第几次扫码
    create_time            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE trace_scan_record IS '消费者扫码记录表 - 记录扫码查询追溯信息的行为数据';
COMMENT ON COLUMN trace_scan_record.trace_code_id IS '追溯码ID';
COMMENT ON COLUMN trace_scan_record.trace_code IS '追溯码（冗余）';
COMMENT ON COLUMN trace_scan_record.scan_time IS '扫码时间';
COMMENT ON COLUMN trace_scan_record.scan_location IS '扫码位置（粗略描述）';
COMMENT ON COLUMN trace_scan_record.scan_location_detail IS '位置详情JSON：{province,city,district,address,lat,lng}';
COMMENT ON COLUMN trace_scan_record.ip_address IS 'IP地址';
COMMENT ON COLUMN trace_scan_record.user_agent IS 'User-Agent';
COMMENT ON COLUMN trace_scan_record.device_type IS '设备类型：MOBILE/PC/TABLET';
COMMENT ON COLUMN trace_scan_record.scan_count IS '该追溯码第几次扫码（从1开始）';

-- 普通索引
CREATE INDEX idx_trace_scan_record_trace_code_id ON trace_scan_record (trace_code_id) WHERE deleted = 0;
CREATE INDEX idx_trace_scan_record_scan_time ON trace_scan_record (scan_time) WHERE deleted = 0;


-- ============================================
-- T5. 临期预警记录表
-- 记录物料/食品的临期预警及处理全过程
-- ============================================
CREATE TABLE IF NOT EXISTS expiry_alert_record (
    alert_id            BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trace_code_id       BIGINT         NOT NULL,
    trace_code          VARCHAR(64)    NOT NULL,
    trace_type          VARCHAR(20)    NOT NULL,              -- MATERIAL原料/FOOD食品
    material_id         VARCHAR(32),
    material_name       VARCHAR(200),
    batch_no            VARCHAR(64),
    supplier_id         BIGINT,
    supplier_name       VARCHAR(200),
    production_date     DATE,
    expiry_date         DATE           NOT NULL,
    remaining_days      INTEGER        NOT NULL,              -- 剩余天数（可为负数）
    alert_level         VARCHAR(10)    NOT NULL,              -- RED红/YELLOW黄/GREEN绿
    handling_status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',  -- PENDING待处理/SCRAPPED已报损/RETURNED已退货/RESOLVED已处理
    handling_action     VARCHAR(200),                          -- 处理动作
    handled_by_id       BIGINT,
    handled_by_name     VARCHAR(100),
    handled_time        TIMESTAMP,
    remark              VARCHAR(500),
    create_time         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE expiry_alert_record IS '临期预警记录表 - 记录物料/食品临期预警及处理全过程';
COMMENT ON COLUMN expiry_alert_record.trace_code_id IS '追溯码ID';
COMMENT ON COLUMN expiry_alert_record.trace_code IS '追溯码（冗余）';
COMMENT ON COLUMN expiry_alert_record.trace_type IS '追溯类型：MATERIAL原料/FOOD食品';
COMMENT ON COLUMN expiry_alert_record.material_id IS '物料ID';
COMMENT ON COLUMN expiry_alert_record.material_name IS '物料名称';
COMMENT ON COLUMN expiry_alert_record.batch_no IS '批次号';
COMMENT ON COLUMN expiry_alert_record.supplier_id IS '供应商ID';
COMMENT ON COLUMN expiry_alert_record.supplier_name IS '供应商名称';
COMMENT ON COLUMN expiry_alert_record.production_date IS '生产日期';
COMMENT ON COLUMN expiry_alert_record.expiry_date IS '过期日期';
COMMENT ON COLUMN expiry_alert_record.remaining_days IS '剩余天数（可为负数，表示已过期）';
COMMENT ON COLUMN expiry_alert_record.alert_level IS '预警级别：RED红/YELLOW黄/GREEN绿';
COMMENT ON COLUMN expiry_alert_record.handling_status IS '处理状态：PENDING待处理/SCRAPPED已报损/RETURNED已退货/RESOLVED已处理';
COMMENT ON COLUMN expiry_alert_record.handling_action IS '处理动作';
COMMENT ON COLUMN expiry_alert_record.handled_by_id IS '处理人ID';
COMMENT ON COLUMN expiry_alert_record.handled_by_name IS '处理人姓名';
COMMENT ON COLUMN expiry_alert_record.handled_time IS '处理时间';

-- 普通索引
CREATE INDEX idx_expiry_alert_record_trace_code_id ON expiry_alert_record (trace_code_id) WHERE deleted = 0;
CREATE INDEX idx_expiry_alert_record_alert_level ON expiry_alert_record (alert_level) WHERE deleted = 0;
CREATE INDEX idx_expiry_alert_record_handling_status ON expiry_alert_record (handling_status) WHERE deleted = 0;
CREATE INDEX idx_expiry_alert_record_expiry_date ON expiry_alert_record (expiry_date) WHERE deleted = 0;
