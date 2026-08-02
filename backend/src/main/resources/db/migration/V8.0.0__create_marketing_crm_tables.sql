-- ============================================================
-- 营销与客户关系管理系统(CRM) - 数据库表结构
-- 版本: V8.0.0
-- 说明: 会员体系、积分管理、优惠券、促销活动、客户分析、充值管理
-- ============================================================

-- =============================================
-- 1. 会员等级表 (member_level)
-- =============================================
CREATE TABLE IF NOT EXISTS member_level (
    level_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    level_name        VARCHAR(50) NOT NULL,              -- 等级名称：普通会员/银卡/金卡/钻石
    level_code        VARCHAR(20) NOT NULL UNIQUE,      -- 等级编码：NORMAL/SILVER/GOLD/DIAMOND
    min_points        INTEGER DEFAULT 0,                 -- 升级所需最低积分
    min_consumption   BIGINT DEFAULT 0,                  -- 升级所需最低消费金额（分）
    points_rate       DECIMAL(4,2) DEFAULT 1.00,         -- 积分倍率
    discount_rate     DECIMAL(4,2) DEFAULT 1.00,         -- 折扣率（1.0=无折扣，0.95=95折）
    birthday_bonus    INTEGER DEFAULT 0,                 -- 生日额外积分
    benefits_description TEXT,                           -- 等级权益说明
    icon_url          VARCHAR(500),                     -- 等级图标URL
    sort_order        INTEGER DEFAULT 0,                -- 排序序号
    status            INTEGER DEFAULT 1,                -- 1启用 0停用
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER DEFAULT 0
);

COMMENT ON TABLE member_level IS '会员等级表';
COMMENT ON COLUMN member_level.level_id IS '等级ID';
COMMENT ON COLUMN member_level.level_name IS '等级名称';
COMMENT ON COLUMN member_level.level_code IS '等级编码';
COMMENT ON COLUMN member_level.min_points IS '升级所需最低积分';
COMMENT ON COLUMN member_level.min_consumption IS '升级所需最低消费（分）';
COMMENT ON COLUMN member_level.points_rate IS '积分倍率';
COMMENT ON COLUMN member_level.discount_rate IS '折扣率';
COMMENT ON COLUMN member_level.birthday_bonus IS '生日额外积分';

-- 创建索引
CREATE INDEX idx_member_level_status ON member_level(status) WHERE deleted = 0;
CREATE INDEX idx_member_level_sort ON member_level(sort_order) WHERE deleted = 0;

-- 初始化默认等级数据
INSERT INTO member_level (level_name, level_code, min_points, min_consumption, points_rate, discount_rate, birthday_bonus, benefits_description, sort_order, status) VALUES
('普通会员', 'NORMAL', 0, 0, 1.00, 1.00, 0, '基础会员权益', 1, 1),
('银卡会员', 'SILVER', 1000, 100000, 1.20, 0.98, 100, '享受98折优惠，积分1.2倍加速', 2, 1),
('金卡会员', 'GOLD', 5000, 500000, 1.50, 0.95, 300, '享受95折优惠，积分1.5倍加速，生日双倍积分', 3, 1),
('钻石会员', 'DIAMOND', 20000, 2000000, 2.00, 0.90, 500, '享受9折优惠，积分2倍加速，专属客服', 4, 1);


-- =============================================
-- 2. 会员表 (members) - 营销CRM专用完整版
-- =============================================
CREATE TABLE IF NOT EXISTS members (
    member_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_no            VARCHAR(32) UNIQUE NOT NULL,     -- 会员卡号
    phone                VARCHAR(20) UNIQUE NOT NULL,     -- 手机号（登录账号）
    password             VARCHAR(200),                    -- 登录密码（加密存储）
    nickname             VARCHAR(50),                     -- 昵称
    avatar_url           VARCHAR(500),                   -- 头像URL
    gender               INTEGER DEFAULT 0,              -- 0未知 1男 2女
    birthday             DATE,
    email                VARCHAR(100),
    member_level_id      BIGINT DEFAULT 1,               -- 会员等级ID（默认普通会员）
    points               INTEGER DEFAULT 0,              -- 当前可用积分
    total_points_earned  INTEGER DEFAULT 0,              -- 历史累计获得积分
    total_points_used    INTEGER DEFAULT 0,              -- 历史累计使用积分
    balance              BIGINT DEFAULT 0,               -- 余额（分），充值剩余
    total_recharge       BIGINT DEFAULT 0,               -- 累计充值金额（分）
    total_consume        BIGINT DEFAULT 0,               -- 累计消费金额（分）
    order_count          INTEGER DEFAULT 0,              -- 订单数量
    last_order_time      TIMESTAMP,                      -- 最后消费时间
    last_visit_time      TIMESTAMP,                      -- 最后到店时间
    register_channel     INTEGER DEFAULT 1,              -- 1注册APP 2收银台注册 3扫码 4导入 5小程序
    status               INTEGER DEFAULT 1,              -- 1正常 2冻结 3黑名单 4注销
    tags                 JSONB DEFAULT '[]',             -- 标签数组 ["VIP","高频","生日月"...]
    remark               TEXT,
    r_score              INTEGER DEFAULT 0,              -- RFM-R最近消费得分(1-5)
    f_score              INTEGER DEFAULT 0,              -- RFM-F消费频率得分(1-5)
    m_score              INTEGER DEFAULT 0,              -- RFM-M消费金额得分(1-5)
    customer_segment     VARCHAR(20),                    -- 客户分层标签
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0,

    -- 外键约束
    CONSTRAINT fk_member_level FOREIGN KEY (member_level_id) REFERENCES member_level(level_id)
);

COMMENT ON TABLE members IS '会员信息表（营销CRM）';
COMMENT ON COLUMN members.member_id IS '会员ID';
COMMENT ON COLUMN members.member_no IS '会员卡号';
COMMENT ON COLUMN members.phone IS '手机号';
COMMENT ON COLUMN members.points IS '当前可用积分';
COMMENT ON COLUMN members.balance IS '账户余额（分）';
COMMENT ON COLUMN members.total_consume IS '累计消费（分）';
COMMENT ON COLUMN members.tags IS '会员标签JSON数组';
COMMENT ON COLUMN members.r_score IS 'RFM最近消费得分';
COMMENT ON COLUMN members.f_score IS 'RFM消费频率得分';
COMMENT ON COLUMN members.m_score IS 'RFM消费金额得分';

-- 创建索引
CREATE UNIQUE INDEX uk_members_member_no ON members(member_no) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_members_phone ON members(phone) WHERE deleted = 0;
CREATE INDEX idx_members_level ON members(member_level_id) WHERE deleted = 0;
CREATE INDEX idx_members_status ON members(status) WHERE deleted = 0;
CREATE INDEX idx_members_points ON members(points) WHERE deleted = 0;
CREATE INDEX idx_members_last_visit ON members(last_visit_time) WHERE deleted = 0;
CREATE INDEX idx_members_segment ON members(customer_segment) WHERE deleted = 0;


-- =============================================
-- 3. 积分变动日志表 (member_points_log)
-- =============================================
CREATE TABLE IF NOT EXISTS member_points_log (
    log_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id         BIGINT NOT NULL,                   -- 会员ID
    change_type       INTEGER NOT NULL,                  -- 1消费获得 2使用抵扣 3签到奖励 4活动赠送 5管理员调整 6过期清零 7充值赠送 8推荐奖励
    change_points     INTEGER NOT NULL,                  -- 变动积分（正数增加，负数减少）
    balance_after     INTEGER NOT NULL,                  -- 变动后余额
    reference_no      VARCHAR(64),                       -- 关联单据号: ORDERxxx/ACTxxx/RECxxx
    reference_type    INTEGER,                           -- 1订单 2活动 3签到 4管理员 5充值 6推荐
    operator_id       BIGINT,                            -- 操作人ID（管理员调整时）
    operator_name     VARCHAR(50),                       -- 操作人名称
    remark            TEXT,                              -- 备注
    expire_time       TIMESTAMP,                         -- 过期时间（NULL=永不过期）
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 外键约束
    CONSTRAINT fk_points_log_member FOREIGN KEY (member_id) REFERENCES members(member_id)
);

COMMENT ON TABLE member_points_log IS '会员积分变动日志表';
COMMENT ON COLUMN member_points_log.log_id IS '日志ID';
COMMENT ON COLUMN member_points_log.member_id IS '会员ID';
COMMENT ON COLUMN member_points_log.change_type IS '变动类型';
COMMENT ON COLUMN member_points_log.change_points IS '变动积分数';
COMMENT ON COLUMN member_points_log.balance_after IS '变动后余额';
COMMENT ON COLUMN member_points_log.reference_no IS '关联单据号';

-- 创建索引
CREATE INDEX idx_points_log_member ON member_points_log(member_id);
CREATE INDEX idx_points_log_type ON member_points_log(change_type);
CREATE INDEX idx_points_log_reference ON member_points_log(reference_no);
CREATE INDEX idx_points_log_create_time ON member_points_log(create_time);
CREATE INDEX idx_points_log_expire ON member_points_log(expire_time) WHERE expire_time IS NOT NULL;


-- =============================================
-- 4. 优惠券模板表 (coupon_template)
-- =============================================
CREATE TABLE IF NOT EXISTS coupon_template (
    template_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_name       VARCHAR(100) NOT NULL,            -- 模板名称
    template_code       VARCHAR(32) UNIQUE NOT NULL,      -- 模板编码
    coupon_type         INTEGER NOT NULL,                 -- 1满减券 2折扣券 3兑换券 4代金券 5新人券
    discount_type       INTEGER NOT NULL,                 -- 1固定金额减免 2百分比折扣
    discount_value      BIGINT NOT NULL,                  -- 优惠值（分或百分比*100，如95折存为9500）
    min_consumption     BIGINT DEFAULT 0,                 -- 最低消费门槛（分）
    max_discount        BIGINT,                          -- 最大优惠金额上限（分），防止无限折扣
    total_quantity      INTEGER DEFAULT -1,              -- 发放总量（-1表示不限）
    per_person_limit    INTEGER DEFAULT 1,               -- 每人限领数量
    valid_start_date    TIMESTAMP NOT NULL,              -- 可领取开始时间
    valid_expiry_date   TIMESTAMP NOT NULL,              -- 领取截止时间
    valid_days          INTEGER DEFAULT 30,              -- 领取后有效天数
    applicable_scope    INTEGER DEFAULT 1,               -- 1全场通用 2指定品类 3指定商品 4指定渠道
    scope_config        JSONB,                           -- 适用范围配置JSON
    usage_rules         TEXT,                            -- 使用规则说明
    status              INTEGER DEFAULT 1,               -- 1启用 0停用
    created_count       INTEGER DEFAULT 0,               -- 已领取数量
    used_count          INTEGER DEFAULT 0,               -- 已使用数量
    create_user_id      BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER DEFAULT 0
);

COMMENT ON TABLE coupon_template IS '优惠券模板表';
COMMENT ON COLUMN coupon_template.template_id IS '模板ID';
COMMENT ON COLUMN coupon_template.coupon_type IS '优惠券类型';
COMMENT ON COLUMN coupon_template.discount_type IS '折扣类型';
COMMENT ON COLUMN coupon_template.discount_value IS '优惠值';
COMMENT ON COLUMN coupon_template.min_consumption IS '最低消费（分）';
COMMENT ON COLUMN coupon_template.total_quantity IS '发放总量';
COMMENT ON COLUMN coupon_template.per_person_limit IS '每人限领';

-- 创建索引
CREATE INDEX idx_coupon_template_status ON coupon_template(status) WHERE deleted = 0;
CREATE INDEX idx_coupon_template_type ON coupon_template(coupon_type) WHERE deleted = 0;
CREATE INDEX idx_coupon_template_valid ON coupon_template(valid_start_date, valid_expiry_date) WHERE deleted = 0 AND status = 1;


-- =============================================
-- 5. 用户优惠券表 (member_coupon)
-- =============================================
CREATE TABLE IF NOT EXISTS member_coupon (
    coupon_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id         BIGINT NOT NULL,                 -- 优惠券模板ID
    member_id           BIGINT NOT NULL,                 -- 会员ID
    coupon_no           VARCHAR(32) UNIQUE NOT NULL,      -- 优惠券唯一号码
    status              INTEGER DEFAULT 0,               -- 0未使用 1已使用 2已过期 3已作废
    receive_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 领取时间
    use_time            TIMESTAMP,                       -- 使用时间
    expiry_time         TIMESTAMP NOT NULL,              -- 过期时间
    order_id            BIGINT,                          -- 使用时的订单ID
    order_no            VARCHAR(64),                     -- 关联订单号

    -- 外键约束
    CONSTRAINT fk_coupon_template FOREIGN KEY (template_id) REFERENCES coupon_template(template_id),
    CONSTRAINT fk_coupon_member FOREIGN KEY (member_id) REFERENCES members(member_id),

    -- 公共字段
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER DEFAULT 0
);

COMMENT ON TABLE member_coupon IS '用户优惠券表';
COMMENT ON COLUMN member_coupon.coupon_id IS '优惠券记录ID';
COMMENT ON COLUMN member_coupon.template_id IS '模板ID';
COMMENT ON COLUMN member_coupon.member_id IS '会员ID';
COMMENT ON COLUMN member_coupon.coupon_no IS '优惠券号码';
COMMENT ON COLUMN member_coupon.status IS '状态：0未使用 1已使用 2已过期 3已作废';
COMMENT ON COLUMN member_coupon.create_time IS '创建时间';
COMMENT ON COLUMN member_coupon.update_time IS '更新时间';
COMMENT ON COLUMN member_coupon.deleted IS '逻辑删除：0未删 1已删';

-- 创建索引
CREATE INDEX idx_member_coupon_member ON member_coupon(member_id) WHERE deleted = 0;
CREATE INDEX idx_member_coupon_template ON member_coupon(template_id) WHERE deleted = 0;
CREATE INDEX idx_member_coupon_status ON member_coupon(status) WHERE deleted = 0;
CREATE INDEX idx_member_coupon_no ON member_coupon(coupon_no) WHERE deleted = 0;
CREATE INDEX idx_member_coupon_expiry ON member_coupon(expiry_time) WHERE deleted = 0 AND status = 0;


-- =============================================
-- 6. 促销活动表 (marketing_promotion)
-- =============================================
CREATE TABLE IF NOT EXISTS marketing_promotion (
    promotion_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    promotion_name          VARCHAR(100) NOT NULL,        -- 活动名称
    promotion_code          VARCHAR(32) UNIQUE NOT NULL,  -- 活动编码
    promotion_type          INTEGER NOT NULL,             -- 1满减 2折扣 3买赠 4第二件半价 5限时秒杀 6会员日双倍 7新人专享
    discount_rule           JSONB NOT NULL,              -- 优惠规则JSON（复杂条件表达式）
    start_time              TIMESTAMP NOT NULL,          -- 活动开始时间
    end_time                TIMESTAMP NOT NULL,          -- 活动结束时间
    target_audience         INTEGER DEFAULT 1,           -- 1全部会员 2指定等级 3新会员 4沉睡唤醒 5高价值客户
    target_level_ids        JSONB DEFAULT '[]',          -- 目标等级ID列表
    participation_condition JSONB,                        -- 参与条件JSON（最低消费、首次消费等）
    budget_total            BIGINT DEFAULT 0,             -- 活动预算总额（分）
    budget_used             BIGINT DEFAULT 0,             -- 已使用预算（分）
    participant_count       INTEGER DEFAULT 0,            -- 参与人数
    success_count           INTEGER DEFAULT 0,            -- 成功成交笔数
    sales_amount            BIGINT DEFAULT 0,             -- 活动销售额（分）
    discount_amount         BIGINT DEFAULT 0,             -- 优惠总额（分）
    status                  INTEGER DEFAULT 1,            -- 1草稿 2进行中 3已结束 4已暂停 5已作废
    description             TEXT,                         -- 活动描述
    create_user_id          BIGINT,
    create_time             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted                 INTEGER DEFAULT 0
);

COMMENT ON TABLE marketing_promotion IS '促销活动表';
COMMENT ON COLUMN marketing_promotion.promotion_id IS '活动ID';
COMMENT ON COLUMN marketing_promotion.promotion_type IS '活动类型';
COMMENT ON COLUMN marketing_promotion.discount_rule IS '优惠规则JSON';
COMMENT ON COLUMN marketing_promotion.target_audience IS '目标人群';
COMMENT ON COLUMN marketing_promotion.budget_total IS '活动预算（分）';
COMMENT ON COLUMN marketing_promotion.status IS '状态：1草稿 2进行中 3已结束 4暂停 5作废';

-- 创建索引
CREATE INDEX idx_marketing_promotion_status ON marketing_promotion(status) WHERE deleted = 0;
CREATE INDEX idx_marketing_promotion_type ON marketing_promotion(promotion_type) WHERE deleted = 0;
CREATE INDEX idx_marketing_promotion_time ON marketing_promotion(start_time, end_time) WHERE deleted = 0;
CREATE INDEX idx_marketing_promotion_code ON marketing_promotion(promotion_code) WHERE deleted = 0;


-- =============================================
-- 7. 充值方案表 (recharge_plan)
-- =============================================
CREATE TABLE IF NOT EXISTS recharge_plan (
    plan_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_name          VARCHAR(100) NOT NULL,            -- 方案名称
    recharge_amount    BIGINT NOT NULL,                  -- 充值金额（分）
    bonus_amount       BIGINT DEFAULT 0,                -- 赠送金额（分）
    bonus_type         INTEGER DEFAULT 1,               -- 1赠送余额 2赠送积分 3两者都有
    bonus_points       INTEGER DEFAULT 0,               -- 赠送积分数
    validity_days      INTEGER DEFAULT 365,             -- 有效期天数（-1表示永久有效）
    is_recommended     BOOLEAN DEFAULT FALSE,            -- 是否推荐
    sort_order         INTEGER DEFAULT 0,               -- 排序序号
    status             INTEGER DEFAULT 1,               -- 1启用 0停用
    description        TEXT,                            -- 方案描述
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER DEFAULT 0
);

COMMENT ON TABLE recharge_plan IS '充值方案表';
COMMENT ON COLUMN recharge_plan.plan_id IS '方案ID';
COMMENT ON COLUMN recharge_plan.recharge_amount IS '充值金额（分）';
COMMENT ON COLUMN recharge_plan.bonus_amount IS '赠送金额（分）';
COMMENT ON COLUMN recharge_plan.bonus_type IS '赠送类型';
COMMENT ON COLUMN recharge_plan.bonus_points IS '赠送积分';

-- 创建索引
CREATE INDEX idx_recharge_plan_status ON recharge_plan(status) WHERE deleted = 0;
CREATE INDEX idx_recharge_plan_sort ON recharge_plan(sort_order, is_recommended) WHERE deleted = 0 AND status = 1;

-- 初始化默认充值方案
INSERT INTO recharge_plan (plan_name, recharge_amount, bonus_amount, bonus_type, bonus_points, validity_days, is_recommended, sort_order, status, description) VALUES
('体验充值', 10000, 500, 1, 0, 365, FALSE, 1, 1, '充10元送5元'),
('超值充值', 50000, 8000, 1, 0, 365, TRUE, 2, 1, '充50元送80元'),
('尊享充值', 100000, 25000, 1, 0, 365, TRUE, 3, 1, '充100元送250元'),
('豪华充值', 300000, 100000, 3, 5000, 730, FALSE, 4, 1, '充300元送100元+5000积分');


-- =============================================
-- 8. 充值记录表 (recharge_record)
-- =============================================
CREATE TABLE IF NOT EXISTS recharge_record (
    record_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    record_no          VARCHAR(32) UNIQUE NOT NULL,      -- 充值流水号
    member_id          BIGINT NOT NULL,                  -- 会员ID
    plan_id            BIGINT,                           -- 充值方案ID（NULL表示自定义金额）
    recharge_amount    BIGINT NOT NULL,                  -- 实充金额（分）
    bonus_amount       BIGINT DEFAULT 0,                -- 赠送金额（分）
    bonus_points       INTEGER DEFAULT 0,               -- 赠送积分
    payment_method     INTEGER DEFAULT 1,               -- 1微信 2支付宝 3现金 4银行卡 5余额支付
    payment_status     INTEGER DEFAULT 0,               -- 0待支付 1已支付 2已退款 3支付失败
    transaction_no     VARCHAR(64),                      -- 第三方支付流水号
    paid_time          TIMESTAMP,                        -- 支付完成时间
    refund_time        TIMESTAMP,                        -- 退款时间
    refund_amount      BIGINT DEFAULT 0,                -- 退款金额（分）
    operate_user_id    BIGINT,                           -- 操作员ID
    operate_user_name  VARCHAR(50),                      -- 操作员姓名
    remark             TEXT,
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER DEFAULT 0,

    -- 外键约束
    CONSTRAINT fk_recharge_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    CONSTRAINT fk_recharge_plan FOREIGN KEY (plan_id) REFERENCES recharge_plan(plan_id)
);

COMMENT ON TABLE recharge_record IS '充值记录表';
COMMENT ON COLUMN recharge_record.record_id IS '记录ID';
COMMENT ON COLUMN recharge_record.record_no IS '充值流水号';
COMMENT ON COLUMN recharge_record.member_id IS '会员ID';
COMMENT ON COLUMN recharge_record.recharge_amount IS '实充金额（分）';
COMMENT ON COLUMN recharge_record.payment_method IS '支付方式';
COMMENT ON COLUMN recharge_record.payment_status IS '支付状态';

-- 创建索引
CREATE INDEX idx_recharge_record_member ON recharge_record(member_id) WHERE deleted = 0;
CREATE INDEX idx_recharge_record_status ON recharge_record(payment_status) WHERE deleted = 0;
CREATE INDEX idx_recharge_record_time ON recharge_record(create_time) WHERE deleted = 0;
CREATE INDEX idx_recharge_record_no ON recharge_record(record_no) WHERE deleted = 0;
CREATE INDEX idx_recharge_record_transaction ON recharge_record(transaction_no) WHERE transaction_no IS NOT NULL AND deleted = 0;


-- =============================================
-- 9. 积分规则配置表 (points_rule_config)
-- =============================================
CREATE TABLE IF NOT EXISTS points_rule_config (
    rule_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_key           VARCHAR(50) NOT NULL UNIQUE,      -- 规则键名
    rule_name          VARCHAR(100) NOT NULL,            -- 规则名称
    rule_group         VARCHAR(30) NOT NULL,             -- 规则分组：earn/use/expire
    rule_value         VARCHAR(200) NOT NULL,            -- 规则值（支持数字和JSON）
    rule_description   TEXT,                             -- 规则说明
    status             INTEGER DEFAULT 1,               -- 1启用 0禁用
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER DEFAULT 0
);

COMMENT ON TABLE points_rule_config IS '积分规则配置表';
COMMENT ON COLUMN points_rule_config.rule_key IS '规则键名';
COMMENT ON COLUMN points_rule_config.rule_value IS '规则值';

-- 创建索引
CREATE INDEX idx_points_rule_group ON points_rule_config(rule_group) WHERE deleted = 0 AND status = 1;

-- 初始化默认积分规则
INSERT INTO points_rule_config (rule_key, rule_name, rule_group, rule_value, rule_description, status) VALUES
-- ===== 获取积分规则 =====
('base_points_rate', '基础积分倍率', 'earn', '1.0', '消费1元获得的积分倍率', 1),
('dine_in_rate', '堂食消费倍率', 'earn', '1.0', '堂食场景积分倍率', 1),
('takeout_rate', '外卖消费倍率', 'earn', '0.8', '外卖场景积分倍率（因平台扣点）', 1),
('review_bonus', '评价奖励积分', 'earn', '50', '评价后奖励的积分数', 1),
('referral_bonus', '推荐好友奖励积分', 'earn', '200', '成功推荐好友注册奖励积分', 1),
('signup_bonus', '注册奖励积分', 'earn', '100', '新用户注册奖励积分', 1),
('daily_checkin_max', '每日签到最大次数', 'earn', '1', '每天最多签到次数', 1),
('checkin_points', '签到奖励积分', 'earn', '5', '每次签到获得的积分', 1),

-- ===== 使用积分规则 =====
('points_deduct_max_percent', '积分抵扣最大比例', 'use', '30', '单笔订单最多抵扣比例（百分比）', 1),
('points_exchange_rate', '积分汇率', 'use', '100', '多少积分兑换1元（分）', 1),
('min_points_deduct', '最低抵扣积分', 'use', '100', '单次最低使用积分', 1),

-- ===== 过期规则 =====
('points_valid_months', '积分有效期（月）', 'expire', '12', '积分获得后的有效月数', 1),
('expire_check_day', '过期检查日期', 'expire', '1', '每月几号执行过期清理', 1),
('expire_advance_notice', '提前通知天数', 'expire', '30', '积分到期前多少天提醒', 1);


-- =============================================
-- 10. 营销消息模板表 (marketing_message_template)
-- =============================================
CREATE TABLE IF NOT EXISTS marketing_message_template (
    template_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_code      VARCHAR(32) NOT NULL UNIQUE,      -- 模板编码
    template_name      VARCHAR(100) NOT NULL,            -- 模板名称
    message_type       INTEGER NOT NULL,                 -- 1短信 2微信模板消息 3站内信 4App推送
    channel            INTEGER NOT NULL,                 -- 1阿里云短信 2腾讯云短信 3微信公众号
    content_template   TEXT NOT NULL,                    -- 内容模板（支持变量占位符）
    variables          JSONB DEFAULT '[]',               -- 变量定义列表
    status             INTEGER DEFAULT 1,               -- 1启用 0停用
    remark             TEXT,
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER DEFAULT 0
);

COMMENT ON TABLE marketing_message_template IS '营销消息模板表';

-- 初始化常用消息模板
INSERT INTO marketing_message_template (template_code, template_name, message_type, channel, content_template, variables, status) VALUES
('POINTS_EXPIRE_NOTICE', '积分到期提醒', 1, 1, '【食品溯源】尊敬的{nickname}，您有{points}积分将于{expire_date}过期，快来使用吧！回T退订', '["nickname", "points", "expire_date"]', 1),
('COUPON_NEW_NOTICE', '新优惠券提醒', 1, 1, '【食品溯源】尊敬的{nickname}，您有一张{coupon_name}待领取，满{min_amount}可用，点击查看详情！回T退订', '["nickname", "coupon_name", "min_amount"]', 1),
('RECHAGE_SUCCESS', '充值成功通知', 1, 1, '【食品溯源】尊敬的{nickname}，您已成功充值{amount}元，当前余额{balance}元。回T退订', '["nickname", "amount", "balance"]', 1),
('CHURN_WARNING', '流失预警关怀', 1, 1, '【食品溯源】尊敬的{nickname}，好久不见！我们为您准备了专属优惠{coupon_info}，期待您的光临！回T退订', '["nickname", "coupon_info"]', 1),
('BIRTHDAY_BLESSING', '生日祝福', 1, 1, '【食品溯源】亲爱的{nickname}，祝您生日快乐！特赠您{bonus_points}积分+生日专属优惠券，生日当月到店享双倍积分！回T退订', '["nickname", "bonus_points"]', 1),
('LEVEL_UPGRADE', '会员升级通知', 1, 1, '【食品溯源】恭喜{nickname}升级为{level_name}！享有{benefits}权益。回T退订', '["nickname", "level_name", "benefits"]', 1);


-- =============================================
-- 11. RFM计算记录表 (rfm_calculation_log)
-- =============================================
CREATE TABLE IF NOT EXISTS rfm_calculation_log (
    log_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    calculation_date   DATE NOT NULL,                    -- 计算日期
    total_members      INTEGER DEFAULT 0,               -- 总会员数
    vip_value_count    INTEGER DEFAULT 0,               -- 重要价值客户数
    vip_develop_count  INTEGER DEFAULT 0,               -- 重要发展客户数
    vip_keep_count     INTEGER DEFAULT 0,               -- 重要保持客户数
    general_count      INTEGER DEFAULT 0,               -- 一般客户数
    churn_risk_count   INTEGER DEFAULT 0,               -- 流失风险客户数
    execution_time_ms  INTEGER DEFAULT 0,               -- 执行耗时（毫秒）
    status             INTEGER DEFAULT 1,               -- 1成功 0失败
    error_message      TEXT,
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE rfm_calculation_log IS 'RFM模型计算日志表';


-- =============================================
-- 12. 创建序列（用于生成业务编号）
-- =============================================

-- 会员卡号序列
CREATE SEQUENCE IF NOT EXISTS seq_member_no
    START WITH 10000001
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 20;

-- 充值流水号序列
CREATE SEQUENCE IF NOT EXISTS seq_recharge_no
    START WITH 10000001
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 20;

-- 优惠券号码序列
CREATE SEQUENCE IF NOT EXISTS seq_coupon_no
    START WITH 10000001
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 50;
