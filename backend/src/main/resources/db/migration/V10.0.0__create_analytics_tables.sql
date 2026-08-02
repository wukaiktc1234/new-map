-- ============================================================
-- 数据分析与决策系统数据库迁移脚本
-- 版本: V10.0.0
-- 说明: 创建数据分析系统全部7张表
-- 日期: 2026-04-25
-- 模块: L (数据分析)
-- ============================================================

-- ===============================
-- 模块1: 看板配置表 (dashboard_configs)
-- ===============================
CREATE TABLE IF NOT EXISTS dashboard_configs (
    config_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT,                       -- 用户ID（支持个性化配置）
    dashboard_type     INTEGER NOT NULL,             -- 1总览 2销售 3库存 4财务 5会员
    widget_layout      JSONB,                        -- 组件布局配置JSON
    refresh_interval   INTEGER DEFAULT 300,          -- 刷新间隔（秒）
    is_default         BOOLEAN DEFAULT false,        -- 是否默认配置
    create_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_dashboard_configs_user_id ON dashboard_configs(user_id) WHERE deleted = 0;
CREATE INDEX idx_dashboard_configs_type ON dashboard_configs(dashboard_type) WHERE deleted = 0;
COMMENT ON TABLE dashboard_configs IS '看板配置表';
COMMENT ON COLUMN dashboard_configs.config_id IS '配置ID';
COMMENT ON COLUMN dashboard_configs.user_id IS '用户ID';
COMMENT ON COLUMN dashboard_configs.dashboard_type IS '看板类型: 1总览 2销售 3库存 4财务 5会员';
COMMENT ON COLUMN dashboard_configs.widget_layout IS '组件布局配置JSON';
COMMENT ON COLUMN dashboard_configs.refresh_interval IS '刷新间隔（秒）';
COMMENT ON COLUMN dashboard_configs.is_default IS '是否默认配置';

-- ===============================
-- 模块2: 销售分析报表表 (sales_analysis_reports)
-- ===============================
CREATE TABLE IF NOT EXISTS sales_analysis_reports (
    report_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_no              VARCHAR(32) UNIQUE NOT NULL,       -- 报表编号
    report_type            INTEGER NOT NULL,                  -- 1日报 2周报 3月报 4季报 5年报
    period_start           DATE NOT NULL,                     -- 统计周期开始日期
    period_end             DATE NOT NULL,                     -- 统计周期结束日期
    total_orders           INTEGER DEFAULT 0,                 -- 订单总数
    total_amount           BIGINT DEFAULT 0,                  -- 总销售额（分）
    avg_order_value        BIGINT DEFAULT 0,                  -- 客单价（分）
    refund_count           INTEGER DEFAULT 0,                 -- 退款单数
    refund_amount          BIGINT DEFAULT 0,                  -- 退款额（分）
    dine_in_amount         BIGINT DEFAULT 0,                  -- 堂食销售额（分）
    takeout_amount         BIGINT DEFAULT 0,                  -- 外卖销售额（分）
    self_pickup_amount     BIGINT DEFAULT 0,                  -- 自提销售额（分）
    peak_hour              INTEGER,                           -- 高峰时段（0-23）
    top_foods_json         JSONB,                             -- TOP10菜品销量JSON
    payment_method_stats   JSONB,                             -- 支付方式统计JSON
    generate_time          TIMESTAMP,                         -- 生成时间
    create_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_sales_reports_period ON sales_analysis_reports(period_start, period_end) WHERE deleted = 0;
CREATE INDEX idx_sales_reports_type ON sales_analysis_reports(report_type) WHERE deleted = 0;
CREATE INDEX idx_sales_reports_no ON sales_analysis_reports(report_no) WHERE deleted = 0;
COMMENT ON TABLE sales_analysis_reports IS '销售分析报表表';
COMMENT ON COLUMN sales_analysis_reports.report_id IS '报表ID';
COMMENT ON COLUMN sales_analysis_reports.report_no IS '报表编号';
COMMENT ON COLUMN sales_analysis_reports.report_type IS '报表类型: 1日报 2周报 3月报 4季报 5年报';
COMMENT ON COLUMN sales_analysis_reports.total_orders IS '订单总数';
COMMENT ON COLUMN sales_analysis_reports.total_amount IS '总销售额（分）';
COMMENT ON COLUMN sales_analysis_reports.avg_order_value IS '客单价（分）';
COMMENT ON COLUMN sales_analysis_reports.peak_hour IS '高峰时段（0-23）';

-- ===============================
-- 模块3: 库存分析报表表 (inventory_analysis_reports)
-- ===============================
CREATE TABLE IF NOT EXISTS inventory_analysis_reports (
    report_id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_no                VARCHAR(32) UNIQUE NOT NULL,       -- 报表编号
    report_date              DATE NOT NULL,                     -- 报表日期
    total_sku_count          INTEGER DEFAULT 0,                 -- SKU总数
    total_inventory_value    BIGINT DEFAULT 0,                  -- 库存总值（分）
    turnover_rate            DECIMAL(10,2) DEFAULT 0,           -- 周转率%
    out_of_stock_count       INTEGER DEFAULT 0,                 -- 缺货SKU数
    overstock_count          INTEGER DEFAULT 0,                 -- 积压SKU数
    waste_amount             BIGINT DEFAULT 0,                  -- 报损金额（分）
    warning_count            INTEGER DEFAULT 0,                 -- 预警次数
    category_analysis_json   JSONB,                             -- 分类库存分析
    turnover_ranking_json    JSONB,                             -- 周转率排名
    generate_time            TIMESTAMP,                         -- 生成时间
    create_time              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_inventory_reports_date ON inventory_analysis_reports(report_date) WHERE deleted = 0;
CREATE INDEX idx_inventory_reports_no ON inventory_analysis_reports(report_no) WHERE deleted = 0;
COMMENT ON TABLE inventory_analysis_reports IS '库存分析报表表';
COMMENT ON COLUMN inventory_analysis_reports.report_id IS '报表ID';
COMMENT ON COLUMN inventory_analysis_reports.report_no IS '报表编号';
COMMENT ON COLUMN inventory_analysis_reports.total_sku_count IS 'SKU总数';
COMMENT ON COLUMN inventory_analysis_reports.total_inventory_value IS '库存总值（分）';
COMMENT ON COLUMN inventory_analysis_reports.turnover_rate IS '周转率%';
COMMENT ON COLUMN inventory_analysis_reports.out_of_stock_count IS '缺货SKU数';
COMMENT ON COLUMN inventory_analysis_reports.waste_amount IS '报损金额（分）';

-- ===============================
-- 模块4: 产品分析报表表 (product_analysis_reports)
-- ===============================
CREATE TABLE IF NOT EXISTS product_analysis_reports (
    report_id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_no                    VARCHAR(32) UNIQUE NOT NULL,       -- 报表编号
    period_start                 DATE NOT NULL,                     -- 统计周期开始日期
    period_end                   DATE NOT NULL,                     -- 统计周期结束日期
    total_foods_count            INTEGER DEFAULT 0,                 -- 在售菜品数
    total_combos_count           INTEGER DEFAULT 0,                 -- 套餐数
    gross_profit_total           BIGINT DEFAULT 0,                  -- 毛利总额（分）
    gross_profit_rate            DECIMAL(10,2) DEFAULT 0,           -- 毛利率%
    food_contribution_top10      JSONB,                             -- 菜品毛利贡献TOP10 JSON
    combo_popularity             JSONB,                             -- 套餐受欢迎度排名
    new_food_performance         JSONB,                             -- 新品表现
    price_sensitivity_data       JSONB,                             -- 价格敏感度数据
    generate_time                TIMESTAMP,                         -- 生成时间
    create_time                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                      INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_reports_period ON product_analysis_reports(period_start, period_end) WHERE deleted = 0;
CREATE INDEX idx_product_reports_no ON product_analysis_reports(report_no) WHERE deleted = 0;
COMMENT ON TABLE product_analysis_reports IS '产品分析报表表';
COMMENT ON COLUMN product_analysis_reports.report_id IS '报表ID';
COMMENT ON COLUMN product_analysis_reports.gross_profit_total IS '毛利总额（分）';
COMMENT ON COLUMN product_analysis_reports.gross_profit_rate IS '毛利率%';
COMMENT ON COLUMN product_analysis_reports.total_foods_count IS '在售菜品数';

-- ===============================
-- 模块5: 客户分析报表表 (customer_analysis_reports)
-- ===============================
CREATE TABLE IF NOT EXISTS customer_analysis_reports (
    report_id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_no                    VARCHAR(32) UNIQUE NOT NULL,       -- 报表编号
    period                       VARCHAR(20) NOT NULL,              -- 统计周期
    total_members                INTEGER DEFAULT 0,                 -- 会员总数
    new_members_count            INTEGER DEFAULT 0,                 -- 新增会员
    active_members_count         INTEGER DEFAULT 0,                 -- 活跃会员
    churned_members_count        INTEGER DEFAULT 0,                 -- 流失会员
    retention_rate               DECIMAL(10,2) DEFAULT 0,           -- 留存率%
    avg_frequency                DECIMAL(10,2) DEFAULT 0,           -- 平均消费频次
    avg_ticket_size              BIGINT DEFAULT 0,                  -- 平均客单价（分）
    customer_lifecycle_dist      JSONB,                             -- 生命周期分布JSON
    rfm_distribution             JSONB,                             -- RFM分布JSON
    clv_avg                      BIGINT DEFAULT 0,                  -- 客户生命周期价值均值（分）
    generate_time                TIMESTAMP,                         -- 生成时间
    create_time                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                      INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_customer_reports_period ON customer_analysis_reports(period) WHERE deleted = 0;
CREATE INDEX idx_customer_reports_no ON customer_analysis_reports(report_no) WHERE deleted = 0;
COMMENT ON TABLE customer_analysis_reports IS '客户分析报表表';
COMMENT ON COLUMN customer_analysis_reports.report_id IS '报表ID';
COMMENT ON COLUMN customer_analysis_reports.total_members IS '会员总数';
COMMENT ON COLUMN customer_analysis_reports.retention_rate IS '留存率%';
COMMENT ON COLUMN customer_analysis_reports.clv_avg IS '客户生命周期价值均值（分）';

-- ===============================
-- 模块6: 自定义报表表 (custom_reports)
-- ===============================
CREATE TABLE IF NOT EXISTS custom_reports (
    report_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_name            VARCHAR(100) NOT NULL,            -- 报表名称
    report_description     TEXT,                              -- 报表描述
    owner_user_id          BIGINT NOT NULL,                   -- 所有者用户ID
    sql_query              TEXT NOT NULL,                     -- 自定义SQL查询
    query_params_config    JSONB,                             -- 参数配置JSON
    chart_type             INTEGER DEFAULT 1,                 -- 1表格 2柱状图 3折线图 4饼图 5散点图
    schedule_cron          VARCHAR(100),                      -- 定时执行cron表达式
    last_executed_at       TIMESTAMP,                         -- 最后执行时间
    is_public              BOOLEAN DEFAULT false,             -- 是否公开
    create_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_custom_reports_owner ON custom_reports(owner_user_id) WHERE deleted = 0;
CREATE INDEX idx_custom_reports_name ON custom_reports(report_name) WHERE deleted = 0;
COMMENT ON TABLE custom_reports IS '自定义报表表';
COMMENT ON COLUMN custom_reports.report_id IS '报表ID';
COMMENT ON COLUMN custom_reports.report_name IS '报表名称';
COMMENT ON COLUMN custom_reports.sql_query IS '自定义SQL查询';
COMMENT ON COLUMN custom_reports.chart_type IS '图表类型: 1表格 2柱状图 3折线图 4饼图 5散点图';
COMMENT ON COLUMN custom_reports.schedule_cron IS '定时执行cron表达式';
COMMENT ON COLUMN custom_reports.is_public IS '是否公开';

-- ===============================
-- 模块7: 自定义报表执行历史表 (custom_report_executions)
-- ===============================
CREATE TABLE IF NOT EXISTS custom_report_executions (
    execution_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id              BIGINT NOT NULL,                     -- 报表ID
    executed_by            BIGINT,                              -- 执行人ID
    execution_status       INTEGER DEFAULT 1,                  -- 1运行中 2成功 3失败
    result_data_json       JSONB,                               -- 查询结果JSON
    error_message          TEXT,                                -- 错误信息
    execution_time_ms      BIGINT DEFAULT 0,                    -- 耗时毫秒
    executed_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 执行时间
    create_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_execution_report FOREIGN KEY (report_id) REFERENCES custom_reports(report_id)
);

CREATE INDEX idx_executions_report_id ON custom_report_executions(report_id) WHERE deleted = 0;
CREATE INDEX idx_executions_status ON custom_report_executions(execution_status) WHERE deleted = 0;
CREATE INDEX idx_executions_time ON custom_report_executions(executed_at) WHERE deleted = 0;
COMMENT ON TABLE custom_report_executions IS '自定义报表执行历史表';
COMMENT ON COLUMN custom_report_executions.execution_id IS '执行ID';
COMMENT ON COLUMN custom_report_executions.report_id IS '报表ID';
COMMENT ON COLUMN custom_report_executions.execution_status IS '执行状态: 1运行中 2成功 3失败';
COMMENT ON COLUMN custom_report_executions.execution_time_ms IS '耗时毫秒';
