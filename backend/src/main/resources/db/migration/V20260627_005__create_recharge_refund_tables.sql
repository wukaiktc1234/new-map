-- ============================================================
-- 营销储值/退款模块 - 数据库表结构
-- 版本: V20260627_005
-- 说明: 充值方案、充值记录、退款申请、赠送余额明细、财务流水、系统设置、异常告警、会员等级
-- 设计原则：
--   1. 本金/赠送余额分离（影响退款、过期、会计）
--   2. 金额字段以分为单位存储（BIGINT），前端以元为单位
--   3. 状态字段使用语义化字符串（active/inactive/pending/approved/rejected/executed/cancelled）
--   4. 主键 VARCHAR(32)（雪花算法）
-- 兼容 H2 (MODE=PostgreSQL) 与 PostgreSQL 18
-- ============================================================

-- =============================================
-- 1. 充值方案表 (recharge_plans)
-- =============================================
CREATE TABLE IF NOT EXISTS recharge_plans (
    plan_id           VARCHAR(32) NOT NULL,
    plan_name         VARCHAR(100) NOT NULL,
    plan_type         VARCHAR(20) NOT NULL DEFAULT 'standard',
    recharge_amount   BIGINT NOT NULL,
    bonus_amount      BIGINT NOT NULL DEFAULT 0,
    bonus_rate        INTEGER NOT NULL DEFAULT 0,
    bonus_points      INTEGER NOT NULL DEFAULT 0,
    bonus_type        VARCHAR(20) NOT NULL DEFAULT 'balance',
    validity_days     INTEGER NOT NULL DEFAULT 180,
    is_recommended    BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order        INTEGER NOT NULL DEFAULT 0,
    status            VARCHAR(20) NOT NULL DEFAULT 'active',
    description       TEXT,
    promotion_id      VARCHAR(32),
    start_time        TIMESTAMP,
    end_time          TIMESTAMP,
    target_audience   VARCHAR(30),
    extra_bonus       TEXT,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_recharge_plans PRIMARY KEY (plan_id)
);

CREATE INDEX IF NOT EXISTS idx_recharge_plans_status ON recharge_plans(status);
CREATE INDEX IF NOT EXISTS idx_recharge_plans_sort ON recharge_plans(sort_order, is_recommended);
CREATE INDEX IF NOT EXISTS idx_recharge_plans_type ON recharge_plans(plan_type);
CREATE INDEX IF NOT EXISTS idx_recharge_plans_deleted ON recharge_plans(deleted);

COMMENT ON TABLE recharge_plans IS '充值方案表';
COMMENT ON COLUMN recharge_plans.plan_type IS '方案类型: standard-标准 activity-活动 tiered-阶梯 custom-自定义';
COMMENT ON COLUMN recharge_plans.recharge_amount IS '充值金额（分）';
COMMENT ON COLUMN recharge_plans.bonus_amount IS '赠送金额（分）';
COMMENT ON COLUMN recharge_plans.bonus_rate IS '赠送比例（百分比）';
COMMENT ON COLUMN recharge_plans.bonus_type IS '赠送类型: balance-余额 coupon-券 points-积分 mixed-混合';
COMMENT ON COLUMN recharge_plans.validity_days IS '赠送有效期天数（-1永久，0跟随系统默认）';
COMMENT ON COLUMN recharge_plans.status IS '状态: active-启用 inactive-停用';

ALTER TABLE recharge_plans ADD CONSTRAINT chk_recharge_plans_type
    CHECK (plan_type IN ('standard', 'activity', 'tiered', 'custom'));
ALTER TABLE recharge_plans ADD CONSTRAINT chk_recharge_plans_bonus_type
    CHECK (bonus_type IN ('balance', 'coupon', 'points', 'mixed'));
ALTER TABLE recharge_plans ADD CONSTRAINT chk_recharge_plans_status
    CHECK (status IN ('active', 'inactive'));

-- =============================================
-- 2. 充值记录表 (recharge_records)
-- =============================================
CREATE TABLE IF NOT EXISTS recharge_records (
    record_id         VARCHAR(32) NOT NULL,
    record_no         VARCHAR(32) NOT NULL,
    member_id         VARCHAR(32) NOT NULL,
    member_name       VARCHAR(50),
    member_phone      VARCHAR(20),
    plan_id           VARCHAR(32),
    plan_name         VARCHAR(100),
    recharge_amount   BIGINT NOT NULL,
    principal_amount  BIGINT NOT NULL,
    bonus_amount      BIGINT NOT NULL DEFAULT 0,
    bonus_points      INTEGER NOT NULL DEFAULT 0,
    payment_method    VARCHAR(20) NOT NULL,
    payment_status    VARCHAR(20) NOT NULL DEFAULT 'pending',
    payment_time      TIMESTAMP,
    transaction_no    VARCHAR(64),
    bonus_expire_time TIMESTAMP,
    refund_status     VARCHAR(20) NOT NULL DEFAULT 'none',
    refund_amount     BIGINT NOT NULL DEFAULT 0,
    refund_time       TIMESTAMP,
    refund_reason     VARCHAR(500),
    refund_approver   VARCHAR(50),
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_recharge_records PRIMARY KEY (record_id),
    CONSTRAINT uk_recharge_records_no UNIQUE (record_no)
);

CREATE INDEX IF NOT EXISTS idx_recharge_records_member ON recharge_records(member_id);
CREATE INDEX IF NOT EXISTS idx_recharge_records_status ON recharge_records(payment_status);
CREATE INDEX IF NOT EXISTS idx_recharge_records_refund_status ON recharge_records(refund_status);
CREATE INDEX IF NOT EXISTS idx_recharge_records_time ON recharge_records(payment_time);
CREATE INDEX IF NOT EXISTS idx_recharge_records_plan ON recharge_records(plan_id);
CREATE INDEX IF NOT EXISTS idx_recharge_records_deleted ON recharge_records(deleted);

COMMENT ON TABLE recharge_records IS '充值记录表';
COMMENT ON COLUMN recharge_records.recharge_amount IS '充值金额（分，用户实付）';
COMMENT ON COLUMN recharge_records.principal_amount IS '本金入账金额（分）';
COMMENT ON COLUMN recharge_records.bonus_amount IS '赠送金额（分）';
COMMENT ON COLUMN recharge_records.payment_method IS '支付方式: wechat/alipay/cash/bank_card/balance';
COMMENT ON COLUMN recharge_records.payment_status IS '支付状态: pending/success/failed/refunded/partial_refunded';
COMMENT ON COLUMN recharge_records.refund_status IS '退款状态: none/pending/approved/rejected/refunded';

ALTER TABLE recharge_records ADD CONSTRAINT chk_recharge_records_pmt_method
    CHECK (payment_method IN ('wechat', 'alipay', 'cash', 'bank_card', 'balance'));
ALTER TABLE recharge_records ADD CONSTRAINT chk_recharge_records_pmt_status
    CHECK (payment_status IN ('pending', 'success', 'failed', 'refunded', 'partial_refunded'));
ALTER TABLE recharge_records ADD CONSTRAINT chk_recharge_records_refund_status
    CHECK (refund_status IN ('none', 'pending', 'approved', 'rejected', 'refunded'));

-- =============================================
-- 3. 退款申请表 (refund_requests)
-- =============================================
CREATE TABLE IF NOT EXISTS refund_requests (
    refund_id              VARCHAR(32) NOT NULL,
    recharge_record_id     VARCHAR(32) NOT NULL,
    recharge_record_no     VARCHAR(32),
    member_id              VARCHAR(32) NOT NULL,
    member_name            VARCHAR(50),
    member_phone           VARCHAR(20),
    requested_amount       BIGINT NOT NULL,
    refundable_principal   BIGINT NOT NULL DEFAULT 0,
    actual_refund_amount   BIGINT NOT NULL DEFAULT 0,
    bonus_handling         VARCHAR(20) NOT NULL DEFAULT 'clear',
    status                 VARCHAR(20) NOT NULL DEFAULT 'pending',
    applicant              VARCHAR(50),
    apply_time             TIMESTAMP,
    approver               VARCHAR(50),
    approve_time           TIMESTAMP,
    approve_comment        VARCHAR(500),
    executor               VARCHAR(50),
    execute_time           TIMESTAMP,
    refund_reason          VARCHAR(500) NOT NULL,
    refund_method          VARCHAR(20),
    remark                 VARCHAR(500),
    create_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_refund_requests PRIMARY KEY (refund_id)
);

CREATE INDEX IF NOT EXISTS idx_refund_requests_member ON refund_requests(member_id);
CREATE INDEX IF NOT EXISTS idx_refund_requests_record ON refund_requests(recharge_record_id);
CREATE INDEX IF NOT EXISTS idx_refund_requests_status ON refund_requests(status);
CREATE INDEX IF NOT EXISTS idx_refund_requests_apply_time ON refund_requests(apply_time);
CREATE INDEX IF NOT EXISTS idx_refund_requests_deleted ON refund_requests(deleted);

COMMENT ON TABLE refund_requests IS '退款申请表';
COMMENT ON COLUMN refund_requests.requested_amount IS '申请退款金额（分）';
COMMENT ON COLUMN refund_requests.refundable_principal IS '可退本金（分，系统计算）';
COMMENT ON COLUMN refund_requests.actual_refund_amount IS '实际退款金额（分）';
COMMENT ON COLUMN refund_requests.bonus_handling IS '赠送处理: clear-清零 proportional-按比例扣减';
COMMENT ON COLUMN refund_requests.status IS '状态: pending/approved/rejected/executed/cancelled';

ALTER TABLE refund_requests ADD CONSTRAINT chk_refund_requests_bonus_handling
    CHECK (bonus_handling IN ('clear', 'proportional'));
ALTER TABLE refund_requests ADD CONSTRAINT chk_refund_requests_status
    CHECK (status IN ('pending', 'approved', 'rejected', 'executed', 'cancelled'));

-- =============================================
-- 4. 赠送余额明细表 (bonus_balance_details)
-- =============================================
CREATE TABLE IF NOT EXISTS bonus_balance_details (
    bonus_id            VARCHAR(32) NOT NULL,
    member_id           VARCHAR(32) NOT NULL,
    recharge_record_id  VARCHAR(32) NOT NULL,
    bonus_amount        BIGINT NOT NULL,
    remaining_amount    BIGINT NOT NULL,
    expire_time         TIMESTAMP,
    status              VARCHAR(20) NOT NULL DEFAULT 'active',
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_bonus_balance_details PRIMARY KEY (bonus_id)
);

CREATE INDEX IF NOT EXISTS idx_bonus_balance_member ON bonus_balance_details(member_id);
CREATE INDEX IF NOT EXISTS idx_bonus_balance_record ON bonus_balance_details(recharge_record_id);
CREATE INDEX IF NOT EXISTS idx_bonus_balance_status ON bonus_balance_details(status);
CREATE INDEX IF NOT EXISTS idx_bonus_balance_expire ON bonus_balance_details(expire_time);
CREATE INDEX IF NOT EXISTS idx_bonus_balance_deleted ON bonus_balance_details(deleted);

COMMENT ON TABLE bonus_balance_details IS '赠送余额明细表（支持过期管理）';
COMMENT ON COLUMN bonus_balance_details.bonus_amount IS '赠送金额（分）';
COMMENT ON COLUMN bonus_balance_details.remaining_amount IS '剩余金额（分）';
COMMENT ON COLUMN bonus_balance_details.status IS '状态: active-有效 expired-过期 refunded-已退';

ALTER TABLE bonus_balance_details ADD CONSTRAINT chk_bonus_balance_status
    CHECK (status IN ('active', 'expired', 'refunded'));

-- =============================================
-- 5. 储值财务流水表 (recharge_finance_logs)
-- =============================================
CREATE TABLE IF NOT EXISTS recharge_finance_logs (
    log_id            VARCHAR(32) NOT NULL,
    record_no         VARCHAR(64),
    member_id         VARCHAR(32),
    member_name       VARCHAR(50),
    finance_type      VARCHAR(20) NOT NULL,
    principal_change  BIGINT NOT NULL DEFAULT 0,
    bonus_change      BIGINT NOT NULL DEFAULT 0,
    revenue_change    BIGINT NOT NULL DEFAULT 0,
    balance_after     BIGINT NOT NULL DEFAULT 0,
    principal_after   BIGINT NOT NULL DEFAULT 0,
    bonus_after       BIGINT NOT NULL DEFAULT 0,
    remark            VARCHAR(500),
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_recharge_finance_logs PRIMARY KEY (log_id)
);

CREATE INDEX IF NOT EXISTS idx_recharge_finance_member ON recharge_finance_logs(member_id);
CREATE INDEX IF NOT EXISTS idx_recharge_finance_type ON recharge_finance_logs(finance_type);
CREATE INDEX IF NOT EXISTS idx_recharge_finance_record_no ON recharge_finance_logs(record_no);
CREATE INDEX IF NOT EXISTS idx_recharge_finance_time ON recharge_finance_logs(create_time);
CREATE INDEX IF NOT EXISTS idx_recharge_finance_deleted ON recharge_finance_logs(deleted);

COMMENT ON TABLE recharge_finance_logs IS '储值财务流水表（预留财务系统对接）';
COMMENT ON COLUMN recharge_finance_logs.finance_type IS '流水类型: recharge-充值 consume-消费 refund-退款 bonus_expire-赠送过期 bonus_grant-赠送发放';
COMMENT ON COLUMN recharge_finance_logs.principal_change IS '本金变动（分，正=增加负=减少）';
COMMENT ON COLUMN recharge_finance_logs.bonus_change IS '赠送变动（分）';
COMMENT ON COLUMN recharge_finance_logs.revenue_change IS '收入变动（分）';

ALTER TABLE recharge_finance_logs ADD CONSTRAINT chk_recharge_finance_type
    CHECK (finance_type IN ('recharge', 'consume', 'refund', 'bonus_expire', 'bonus_grant'));

-- =============================================
-- 6. 储值系统设置表 (recharge_system_settings) - 单行配置
-- =============================================
CREATE TABLE IF NOT EXISTS recharge_system_settings (
    setting_id    VARCHAR(32) NOT NULL,
    config_json   TEXT NOT NULL,
    create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_recharge_system_settings PRIMARY KEY (setting_id)
);

COMMENT ON TABLE recharge_system_settings IS '储值系统设置表（单行配置，config_json存储完整设置JSON）';

-- =============================================
-- 7. 异常交易告警表 (anomaly_alerts)
-- =============================================
CREATE TABLE IF NOT EXISTS anomaly_alerts (
    alert_id        VARCHAR(32) NOT NULL,
    member_id       VARCHAR(32),
    member_name     VARCHAR(50),
    alert_type      VARCHAR(50) NOT NULL,
    description     VARCHAR(1000),
    trigger_amount  BIGINT NOT NULL DEFAULT 0,
    alert_time      TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    handler         VARCHAR(50),
    handle_time     TIMESTAMP,
    handle_remark   VARCHAR(500),
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_anomaly_alerts PRIMARY KEY (alert_id)
);

CREATE INDEX IF NOT EXISTS idx_anomaly_alerts_member ON anomaly_alerts(member_id);
CREATE INDEX IF NOT EXISTS idx_anomaly_alerts_type ON anomaly_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_anomaly_alerts_status ON anomaly_alerts(status);
CREATE INDEX IF NOT EXISTS idx_anomaly_alerts_time ON anomaly_alerts(alert_time);
CREATE INDEX IF NOT EXISTS idx_anomaly_alerts_deleted ON anomaly_alerts(deleted);

COMMENT ON TABLE anomaly_alerts IS '异常交易告警表';
COMMENT ON COLUMN anomaly_alerts.alert_type IS '告警类型: frequent_recharge/fast_consume/high_refund_rate/new_member_high_recharge/multi_device/over_limit';
COMMENT ON COLUMN anomaly_alerts.trigger_amount IS '触发金额（分）';
COMMENT ON COLUMN anomaly_alerts.status IS '状态: pending-待处理 handled-已处理 ignored-已忽略';

ALTER TABLE anomaly_alerts ADD CONSTRAINT chk_anomaly_alerts_status
    CHECK (status IN ('pending', 'handled', 'ignored'));

-- =============================================
-- 8. 会员等级表 (member_levels) - 新版，字符串主键
-- =============================================
CREATE TABLE IF NOT EXISTS member_levels (
    level_id                VARCHAR(32) NOT NULL,
    level_name              VARCHAR(50) NOT NULL,
    level_code              VARCHAR(20) NOT NULL,
    level_color             VARCHAR(50),
    level_icon              VARCHAR(50),
    min_consumption         BIGINT NOT NULL DEFAULT 0,
    discount_rate           DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    points_rate             DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    birthday_discount_rate  DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    birthday_bonus_points   INTEGER NOT NULL DEFAULT 0,
    benefits_description    TEXT,
    sort_order              INTEGER NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'active',
    member_count            INTEGER NOT NULL DEFAULT 0,
    create_time             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_member_levels PRIMARY KEY (level_id),
    CONSTRAINT uk_member_levels_code UNIQUE (level_code)
);

CREATE INDEX IF NOT EXISTS idx_member_levels_status ON member_levels(status);
CREATE INDEX IF NOT EXISTS idx_member_levels_sort ON member_levels(sort_order);
CREATE INDEX IF NOT EXISTS idx_member_levels_deleted ON member_levels(deleted);

COMMENT ON TABLE member_levels IS '会员等级表（新版，字符串主键）';
COMMENT ON COLUMN member_levels.min_consumption IS '升级所需最低累计消费（分）';
COMMENT ON COLUMN member_levels.discount_rate IS '折扣率（1.0=无折扣）';
COMMENT ON COLUMN member_levels.points_rate IS '积分倍率（1.0=标准）';
COMMENT ON COLUMN member_levels.status IS '状态: active-启用 inactive-停用';

ALTER TABLE member_levels ADD CONSTRAINT chk_member_levels_status
    CHECK (status IN ('active', 'inactive'));

-- 初始化默认等级数据（仅在表为空时插入）
INSERT INTO member_levels (level_id, level_name, level_code, level_color, level_icon, min_consumption, discount_rate, points_rate, birthday_discount_rate, birthday_bonus_points, benefits_description, sort_order, status)
VALUES ('1', '普通会员', 'NORMAL', 'var(--fts-info)', 'star', 0, 1.00, 1.00, 1.00, 0, '基础会员权益', 1, 'active')
ON CONFLICT (level_code) DO NOTHING;

INSERT INTO member_levels (level_id, level_name, level_code, level_color, level_icon, min_consumption, discount_rate, points_rate, birthday_discount_rate, birthday_bonus_points, benefits_description, sort_order, status)
VALUES ('2', '银卡会员', 'SILVER', 'var(--fts-text-secondary)', 'star', 100000, 0.98, 1.20, 0.95, 100, '98折优惠，积分1.2倍', 2, 'active')
ON CONFLICT (level_code) DO NOTHING;

INSERT INTO member_levels (level_id, level_name, level_code, level_color, level_icon, min_consumption, discount_rate, points_rate, birthday_discount_rate, birthday_bonus_points, benefits_description, sort_order, status)
VALUES ('3', '金卡会员', 'GOLD', 'var(--fts-warning)', 'crown', 500000, 0.95, 1.50, 0.90, 300, '95折优惠，积分1.5倍，生日双倍积分', 3, 'active')
ON CONFLICT (level_code) DO NOTHING;

INSERT INTO member_levels (level_id, level_name, level_code, level_color, level_icon, min_consumption, discount_rate, points_rate, birthday_discount_rate, birthday_bonus_points, benefits_description, sort_order, status)
VALUES ('4', '钻石会员', 'DIAMOND', 'var(--fts-primary)', 'crown', 2000000, 0.90, 2.00, 0.85, 500, '9折优惠，积分2倍，专属客服', 4, 'active')
ON CONFLICT (level_code) DO NOTHING;
