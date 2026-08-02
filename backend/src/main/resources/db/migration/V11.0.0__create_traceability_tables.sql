-- =============================================
-- 食品追溯子系统 - 核心表结构 V11.0.0
-- 包含：追溯码、追溯链、进货台账、索证索票、
--       合规报告、检查项、预警规则、预警记录
-- =============================================

-- T1. 食品追溯码表（核心表）
CREATE TABLE IF NOT EXISTS food_trace_codes (
    trace_code_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trace_code VARCHAR(64) UNIQUE NOT NULL,                    -- 追溯码（如 TC20260425001 或二维码内容）
    trace_type INTEGER NOT NULL,                               -- 1菜品追溯码 2原料批次追溯码 3物流追溯码 4检验报告码
    target_type INTEGER NOT NULL,                              -- 1成品菜品 2半成品 3原材料 4包装材料
    target_id BIGINT,                                          -- 关联的目标ID（food_id或material_id）
    target_name VARCHAR(200),                                  -- 目标名称（冗余方便查询）
    batch_no VARCHAR(50),                                      -- 批次号
    production_date DATE,                                      -- 生产/加工日期
    expiry_date DATE,                                          -- 有效期/保质期至
    supplier_id BIGINT,                                        -- 供应商ID（来自采购系统）
    supplier_name VARCHAR(100),                                -- 供应商名称（冗余）
    purchase_stockin_id BIGINT,                                -- 入库单ID（来自采购系统）
    warehouse_id BIGINT,                                       -- 所在仓库ID（来自仓储系统）
    current_location VARCHAR(200),                             -- 当前位置（仓库/加工间/餐桌/已售出）
    status INTEGER DEFAULT 1,                                  -- 1正常 2即将过期 3已过期 4已召回 5已消费
    risk_level INTEGER DEFAULT 1,                              -- 1低风险 2中风险 3高风险
    qr_code_image_url VARCHAR(500),                            -- 二维码图片URL
    chain_data JSONB,                                          -- 追溯链数据JSON（关键！存储完整链条信息）
    extra_info TEXT,                                           -- 额外信息（如检测报告编号）
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE food_trace_codes IS '食品追溯码表 - 系统核心表，记录每个可追溯单元的唯一标识和完整链路';
COMMENT ON COLUMN food_trace_codes.trace_code IS '唯一追溯码，可用于生成二维码';
COMMENT ON COLUMN food_trace_codes.trace_type IS '追溯类型：1-菜品 2-原料批次 3-物流 4-检验报告';
COMMENT ON COLUMN food_trace_codes.target_type IS '目标类型：1-成品菜品 2-半成品 3-原材料 4-包装材料';
COMMENT ON COLUMN food_trace_codes.status IS '状态：1-正常 2-即将过期 3-已过期 4-已召回 5-已消费';
COMMENT ON COLUMN food_trace_codes.risk_level IS '风险等级：1-低风险 2-中风险 3-高风险';
COMMENT ON COLUMN food_trace_codes.chain_data IS '追溯链完整数据，JSON格式存储';

-- 重要索引
CREATE INDEX idx_trace_code_target ON food_trace_codes(target_type, target_id);
CREATE INDEX idx_trace_code_batch ON food_trace_codes(batch_no);
CREATE INDEX idx_trace_code_supplier ON food_trace_codes(supplier_id);
CREATE INDEX idx_trace_code_status_risk ON food_trace_codes(status, risk_level);
CREATE INDEX idx_trace_code_code ON food_trace_codes(trace_code);


-- T2. 追溯链节点表
CREATE TABLE IF NOT EXISTS trace_chain_nodes (
    node_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trace_code_id BIGINT NOT NULL,                             -- 关联追溯码
    node_sequence INTEGER NOT NULL,                            -- 序号，从1开始
    node_type INTEGER NOT NULL,                                -- 1采购入库 2仓储入库 3加工制作 4出库发货 5上架销售 6检验检测 7消费者扫码
    event_time TIMESTAMP NOT NULL,                             -- 该环节发生时间
    location VARCHAR(200),                                     -- 地点描述
    operator_id BIGINT,                                        -- 操作人ID
    operator_name VARCHAR(100),                                -- 操作人姓名
    detail_json JSONB,                                         -- 该环节详细信息JSON
    attachment_url VARCHAR(500),                               -- 附件URL（质检照片/检验报告）
    geo_location VARCHAR(100),                                 -- 经纬度（可选）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE trace_chain_nodes IS '追溯链节点表 - 记录追溯链条中的每个环节详情';
COMMENT ON COLUMN trace_chain_nodes.node_type IS '节点类型：1-采购入库 2-仓储入库 3-加工制作 4-出库发货 5-上架销售 6-检验检测 7-消费者扫码';

CREATE INDEX idx_chain_node_trace_code ON trace_chain_nodes(trace_code_id);
CREATE INDEX idx_chain_node_sequence ON trace_chain_nodes(trace_code_id, node_sequence);
CREATE INDEX idx_chain_node_type ON trace_chain_nodes(node_type);
CREATE INDEX idx_chain_node_event_time ON trace_chain_nodes(event_time);


-- T3. 进货台账表
CREATE TABLE IF NOT EXISTS purchase_ledgers (
    ledger_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ledger_no VARCHAR(50) UNIQUE NOT NULL,                     -- 台账编号
    purchase_stockin_id BIGINT,                                -- 关联采购入库单
    supplier_id BIGINT NOT NULL,                               -- 供应商ID
    supplier_name VARCHAR(200) NOT NULL,                       -- 供应商名称
    material_name VARCHAR(200) NOT NULL,                       -- 物料名称
    specification VARCHAR(200),                                -- 规格型号
    unit VARCHAR(50) NOT NULL,                                 -- 单位
    quantity DECIMAL(14,4) NOT NULL,                           -- 数量
    unit_price DECIMAL(14,2) NOT NULL,                         -- 单价
    amount DECIMAL(16,2) NOT NULL,                             -- 金额
    batch_no VARCHAR(50),                                      -- 批次号
    production_date DATE,                                      -- 生产日期
    expiry_date DATE,                                          -- 有效期至
    quality_inspection_result INTEGER DEFAULT 3,               -- 质检结果：1合格 2不合格 3待检
    inspection_user_id BIGINT,                                 -- 质检人ID
    inspection_time TIMESTAMP,                                 -- 质检时间
    certificate_no VARCHAR(100),                               -- 索证票号/检疫证号
    certificate_type INTEGER,                                  -- 证件类型：1检疫证 2合格证 3检测报告 4其他
    certificate_image_url VARCHAR(500),                        -- 证件图片
    storage_location VARCHAR(200),                             -- 存放位置
    remark TEXT,                                               -- 备注
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE purchase_ledgers IS '进货台账表 - 电子化进货台账，符合食品安全法要求';
COMMENT ON COLUMN purchase_ledgers.quality_inspection_result IS '质检结果：1-合格 2-不合格 3-待检';
COMMENT ON COLUMN purchase_ledgers.certificate_type IS '证件类型：1-检疫证 2-合格证 3-检测报告 4-其他';

CREATE INDEX idx_ledger_supplier ON purchase_ledgers(supplier_id);
CREATE INDEX idx_ledger_material ON purchase_ledgers(material_name);
CREATE INDEX idx_ledger_batch ON purchase_ledgers(batch_no);
CREATE INDEX idx_ledger_date ON purchase_ledgers(create_time);
CREATE INDEX idx_ledger_stockin ON purchase_ledgers(purchase_stockin_id);


-- T4. 索证索票管理表
CREATE TABLE IF NOT EXISTS certificate_managements (
    cert_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cert_no VARCHAR(100) UNIQUE NOT NULL,                      -- 证件号码
    cert_type INTEGER NOT NULL,                                -- 证件类型
    supplier_id BIGINT,                                        -- 持证方（供应商）ID
    supplier_name VARCHAR(200),                                -- 持证方名称
    cert_name VARCHAR(200) NOT NULL,                           -- 证件名称
    issue_date DATE,                                           -- 发证日期
    expiry_date DATE,                                          -- 有效期至
    issuing_authority VARCHAR(200),                            -- 发证机构
    cert_image_url VARCHAR(500),                               -- 证件扫描件/电子件URL
    cert_status INTEGER DEFAULT 1,                             -- 1有效 2即将到期 3已过期 4已注销
    reminder_days INTEGER DEFAULT 30,                          -- 提前提醒天数
    related_ledger_ids JSONB,                                  -- 关联的进货台账ID数组
    remark TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE certificate_managements IS '索证索票管理表 - 管理各类证照的电子档案';
COMMENT ON COLUMN certificate_managements.cert_type IS '证件类型：1-营业执照 2-食品经营许可证 3-卫生许可证 4-检疫证 5-合格证 6-检测报告 7-其他';
COMMENT ON COLUMN certificate_managements.cert_status IS '状态：1-有效 2-即将到期 3-已过期 4-已注销';

CREATE INDEX idx_cert_type ON certificate_managements(cert_type);
CREATE INDEX idx_cert_supplier ON certificate_managements(supplier_id);
CREATE INDEX idx_cert_status ON certificate_managements(cert_status);
CREATE INDEX idx_cert_expiry ON certificate_managements(expiry_date);


-- T5. 合规报告表
CREATE TABLE IF NOT EXISTS compliance_reports (
    report_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_no VARCHAR(50) UNIQUE NOT NULL,                     -- 报告编号
    report_type INTEGER NOT NULL,                              -- 报告类型
    report_period VARCHAR(50),                                 -- 报告期间
    store_id BIGINT,                                           -- 门店ID
    scope INTEGER NOT NULL,                                    -- 检查范围
    total_items INTEGER DEFAULT 0,                             -- 应检查项总数
    passed_items INTEGER DEFAULT 0,                            -- 通过项数
    failed_items INTEGER DEFAULT 0,                            -- 不通过项数
    pending_items INTEGER DEFAULT 0,                           -- 待整改项数
    score INTEGER,                                             -- 得分 0-100
    grade VARCHAR(10),                                         -- 等级 A/B/C/D/E
    inspector_id BIGINT,                                       -- 检查人ID
    inspect_time TIMESTAMP,                                    -- 检查时间
    conclusion TEXT,                                           -- 结论与建议
    attachments_json JSONB,                                    -- 附件列表
    status INTEGER DEFAULT 1,                                  -- 1草稿 2已提交 3已审核 4已归档
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE compliance_reports IS '合规报告表 - 食品安全合规检查报告';
COMMENT ON COLUMN compliance_reports.report_type IS '报告类型：1-日常自查 2-月度合规 3-专项检查 4-年度审计';
COMMENT ON COLUMN compliance_reports.scope IS '检查范围：1-食材溯源 2-人员健康 3-环境卫生 4-设备设施（支持多选位运算）';
COMMENT ON COLUMN compliance_reports.grade IS '等级：A-优秀 B-良好 C-合格 D-不合格 E-严重';
COMMENT ON COLUMN compliance_reports.status IS '状态：1-草稿 2-已提交 3-已审核 4-已归档';

CREATE INDEX idx_report_type ON compliance_reports(report_type);
CREATE INDEX idx_report_store ON compliance_reports(store_id);
CREATE INDEX idx_report_status ON compliance_reports(status);
CREATE INDEX idx_report_time ON compliance_reports(inspect_time);


-- T6. 合规检查项明细表
CREATE TABLE IF NOT EXISTS compliance_check_items (
    item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id BIGINT NOT NULL,                                 -- 关联报告
    check_category INTEGER NOT NULL,                           -- 检查分类
    check_item_name VARCHAR(200) NOT NULL,                     -- 检查项名称
    standard_requirement TEXT,                                 -- 标准要求
    actual_status INTEGER DEFAULT 3,                           -- 实际状态
    evidence_photo_url VARCHAR(500),                           -- 证据照片
    issue_description TEXT,                                    -- 问题描述
    rectification_required BOOLEAN DEFAULT FALSE,              -- 是否需要整改
    deadline DATE,                                             -- 整改期限
    responsible_person VARCHAR(100),                           -- 责任人
    sort_order INTEGER DEFAULT 0,                              -- 排序
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE compliance_check_items IS '合规检查项明细表 - 记录每个检查项的具体结果';
COMMENT ON COLUMN compliance_check_items.check_category IS '检查分类：1-食材溯源 2-人员健康 3-环境卫生 4-设备设施';
COMMENT ON COLUMN compliance_check_items.actual_status IS '实际状态：1-符合 2-不符合 3-不适用';

CREATE INDEX idx_item_report ON compliance_check_items(report_id);
CREATE INDEX idx_item_category ON compliance_check_items(check_category);
CREATE INDEX idx_item_status ON compliance_check_items(actual_status);


-- T7. 临期预警规则表
CREATE TABLE IF NOT EXISTS expiry_warning_rules (
    rule_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,                           -- 规则名称
    warning_type INTEGER NOT NULL,                             -- 预警类型
    advance_days INTEGER NOT NULL,                             -- 提前天数
    check_frequency INTEGER DEFAULT 2,                         -- 检查频率
    notify_method INTEGER DEFAULT 4,                           -- 通知方式
    notify_roles JSONB,                                        -- 通知角色
    is_enabled BOOLEAN DEFAULT TRUE,                           -- 是否启用
    last_check_time TIMESTAMP,                                 -- 最后检查时间
    remark TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE expiry_warning_rules IS '临期预警规则表 - 定义临期物品的预警规则';
COMMENT ON COLUMN expiry_warning_rules.warning_type IS '预警类型：1-原料临期 2-证件临期 3-健康证临期 4-合同到期';
COMMENT ON COLUMN expiry_warning_rules.check_frequency IS '检查频率：1-每日 2-每周 3-每月';
COMMENT ON COLUMN expiry_warning_rules.notify_method IS '通知方式：1-站内信 2-邮件 3-短信 4-全部';

CREATE INDEX idx_rule_type ON expiry_warning_rules(warning_type);
CREATE INDEX idx_rule_enabled ON expiry_warning_rules(is_enabled);


-- T8. 预警记录表
CREATE TABLE IF NOT EXISTS expiry_warning_records (
    record_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_id BIGINT NOT NULL,                                   -- 关联规则
    target_type INTEGER NOT NULL,                              -- 目标类型
    target_id BIGINT,                                          -- 目标ID
    target_name VARCHAR(200) NOT NULL,                         -- 目标名称
    expiry_date DATE NOT NULL,                                 -- 到期日期
    days_remaining INTEGER NOT NULL,                           -- 剩余天数
    warning_level INTEGER NOT NULL,                            -- 预警级别
    message TEXT NOT NULL,                                     -- 预警消息
    is_handled BOOLEAN DEFAULT FALSE,                          -- 是否已处理
    handle_time TIMESTAMP,                                     -- 处理时间
    handle_result TEXT,                                        -- 处理结果
    notify_time TIMESTAMP,                                     -- 通知时间
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE expiry_warning_records IS '预警记录表 - 记录所有临期预警信息';
COMMENT ON COLUMN expiry_warning_records.target_type IS '目标类型：1-原料批次 2-证件 3-健康证 4-合同';
COMMENT ON COLUMN expiry_warning_records.warning_level IS '预警级别：1-一般 2-紧急 3-严重';

CREATE INDEX idx_record_rule ON expiry_warning_records(rule_id);
CREATE INDEX idx_record_target ON expiry_warning_records(target_type, target_id);
CREATE INDEX idx_record_level ON expiry_warning_records(warning_level);
CREATE INDEX idx_record_handled ON expiry_warning_records(is_handled);
CREATE INDEX idx_record_expiry ON expiry_warning_records(expiry_date);


-- =============================================
-- 初始化默认预警规则数据
-- =============================================
INSERT INTO expiry_warning_rules (rule_name, warning_type, advance_days, check_frequency, notify_method, notify_roles, is_enabled)
VALUES
    ('原料临期预警', 1, 7, 1, 4, '["admin","manager","purchaser"]'::jsonb, TRUE),
    ('证件到期预警', 2, 30, 1, 4, '["admin","manager"]'::jsonb, TRUE),
    ('员工健康证临期预警', 3, 30, 3, 4, '["admin","hr"]'::jsonb, TRUE),
    ('合同到期预警', 4, 30, 3, 4, '["admin","manager","legal"]'::jsonb, TRUE)
ON CONFLICT DO NOTHING;
