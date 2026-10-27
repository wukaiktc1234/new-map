-- =====================================================================
-- V20261028_004__frontend_reachable_tables.sql
--
-- 【重要事实】本迁移里的 3 张表**由实体反推生成**（生产库无结构可抄）。
--   coupon / budget / contract_template 的生产库中均不存在；而这三条入口
--   **前端确有调用**（frontend-pos/src/api/posApi.ts、frontend/src/api/finance/budget.ts、
--   frontend/src/views/hr/ContractDashboard.vue 等），即功能已交付用户，必须让表存在。
--   字段以实体声明为准；后续如产品给出正式设计，需按设计复核本文件。
--
-- 类型推断规则（Lead 要求逐列对照）：
--   String→varchar / text（长文本）｜Long→bigint｜BigDecimal→numeric(14,2)｜
--   LocalDateTime→timestamp without time zone｜Integer→integer｜Boolean→无（本批未出现）
--   项目惯例列：create_time/update_time(timestamp, 默认 CURRENT_TIMESTAMP)、deleted(integer 默认 0)
-- =====================================================================

-- 1) coupon（实体 com.foodtraceability.entity.Coupon，@TableId(type=INPUT) → 主键由应用生成）
CREATE TABLE IF NOT EXISTS coupon (
    coupon_id       varchar(64)  NOT NULL,              -- String couponId（IdType.INPUT）
    code            varchar(64),                        -- String code
    name            varchar(128),                       -- String name
    discount_type   varchar(32),                        -- String discountType
    discount_value  numeric(14,2),                      -- BigDecimal discountValue
    min_order_amount numeric(14,2),                     -- BigDecimal minOrderAmount
    max_discount    numeric(14,2),                      -- BigDecimal maxDiscount
    valid_from      timestamp without time zone,        -- LocalDateTime validFrom
    valid_to        timestamp without time zone,        -- LocalDateTime validTo
    usage_limit     integer,                            -- Integer usageLimit
    used_count      integer DEFAULT 0,                  -- Integer usedCount
    status          varchar(32),                        -- String status
    store_id        bigint,                             -- Long storeId
    store_name      varchar(128),                       -- String storeName
    create_time     timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time     timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted         integer NOT NULL DEFAULT 0,
    CONSTRAINT coupon_pkey PRIMARY KEY (coupon_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_coupon_code ON coupon (code) WHERE deleted = 0;   -- 核券按 code 查/判重
CREATE INDEX IF NOT EXISTS idx_coupon_store ON coupon (store_id) WHERE deleted = 0;    -- 门店维度列表
CREATE INDEX IF NOT EXISTS idx_coupon_valid ON coupon (valid_from, valid_to);          -- 有效期过滤

-- 2) budget（实体 com.foodtraceability.entity.Budget，@TableId(value=budget_id, ASSIGN_ID)）
CREATE TABLE IF NOT EXISTS budget (
    budget_id        varchar(64) NOT NULL,              -- String budgetId（IdType.ASSIGN_ID）
    budget_name      varchar(128),                      -- String budgetName
    budget_amount    numeric(14,2),                     -- BigDecimal budgetAmount
    used_amount      numeric(14,2),                     -- BigDecimal usedAmount
    remaining_amount numeric(14,2),                     -- BigDecimal remainingAmount
    budget_period    varchar(20),                       -- String budgetPeriod
    status           varchar(32),                       -- String status
    create_time      timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time      timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    create_by        varchar(64),                       -- String createBy
    update_by        varchar(64),                       -- String updateBy
    deleted          integer NOT NULL DEFAULT 0,
    CONSTRAINT budget_pkey PRIMARY KEY (budget_id)
);
-- 说明：Lead 建议的 budget.store_id 复合索引——**实体无 store_id 字段**，故按实体实际查询维度建索引；
--       若产品确认预算需按门店隔离，应先加实体字段，再补迁移（不在本次反推范围）。
CREATE INDEX IF NOT EXISTS idx_budget_period_status ON budget (budget_period, status) WHERE deleted = 0;

-- 3) contract_template（实体 com.foodtraceability.entity.ContractTemplate，@TableId(type=AUTO)）
CREATE TABLE IF NOT EXISTS contract_template (
    id                 bigint GENERATED ALWAYS AS IDENTITY,   -- Long id（IdType.AUTO）
    template_name      varchar(128),                          -- String templateName
    template_code      varchar(64),                           -- String templateCode
    contract_type      varchar(64),                           -- String contractType
    template_content   text,                                  -- String templateContent
    template_variables text,                                  -- String templateVariables（JSON 串）
    version            varchar(32),                           -- String version（注意：实体里是 String，非乐观锁整数）
    status             varchar(32),                           -- String status
    description        varchar(500),                          -- String description
    create_by          bigint,                                -- Long createBy
    create_time        timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time        timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted            integer NOT NULL DEFAULT 0,
    CONSTRAINT contract_template_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_contract_template_code ON contract_template (template_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_contract_template_type ON contract_template (contract_type) WHERE deleted = 0;

COMMENT ON TABLE coupon IS '优惠券；V20261028_004 由实体反推生成（生产库无结构可抄），字段以实体为准，待产品正式设计复核';
COMMENT ON TABLE budget IS '预算；V20261028_004 由实体反推生成（生产库无结构可抄），字段以实体为准，待产品正式设计复核';
COMMENT ON TABLE contract_template IS '合同模板；V20261028_004 由实体反推生成（生产库无结构可抄），字段以实体为准，待产品正式设计复核';
