-- =====================================================================
-- V20261028_001__create_position_code_rules.sql
-- 部署缺口修补（与 bug-⑦/⑧ 同族）：position_code_rules 在生产库靠历史手工建表，
-- 迁移里只有 ALTER/UPDATE，**从零建的库（CI / 测试库 / 新环境）没有这张表**。
--   证据：food_traceability 有 ｜ food_traceability_test 没有
--   影响：PositionCodeRuleMapper 一旦真实调用即 PSQLException: 关系不存在
--
-- 列定义逐列对齐生产库 \d position_code_rules（bigint identity + 11 列 + 3 索引）。
-- IF NOT EXISTS 保证对已存在该表的环境（生产/开发库）无副作用。
-- =====================================================================
CREATE TABLE IF NOT EXISTS position_code_rules (
    id              bigint       GENERATED ALWAYS AS IDENTITY,
    keyword         varchar(100) NOT NULL,
    code            varchar(20)  NOT NULL,
    format_template varchar(100),
    sort_order      integer      DEFAULT 0,
    created_by      varchar(50),
    updated_by      varchar(50),
    create_time     timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time     timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted         smallint     DEFAULT 0,
    version         integer      DEFAULT 0,
    CONSTRAINT position_code_rules_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_position_code_rules_code ON position_code_rules (code);
CREATE INDEX IF NOT EXISTS idx_position_code_rules_keyword ON position_code_rules (keyword);

COMMENT ON TABLE position_code_rules IS '岗位编码规则（关键词-编码映射）；V20261028_001 补建的部署缺口表，生产库此前为手工建表';
