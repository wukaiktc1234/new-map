-- ============================================================
-- HR 知识库模块建表脚本
-- 包含三张表：
--   1. knowledge_article          知识库文章表
--   2. article_study_record       员工学习记录表
--   3. article_review_record      文章审核记录表
-- 兼容 H2 (MODE=PostgreSQL) 与 PostgreSQL 18
-- ============================================================

-- ------------------------------------------------------------
-- 1. 知识库文章表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge_article (
    id                  VARCHAR(32) NOT NULL,
    title               VARCHAR(200) NOT NULL,
    summary             VARCHAR(500) NOT NULL,
    category            VARCHAR(20) NOT NULL,
    icon                VARCHAR(50),
    content             TEXT NOT NULL,
    cover_url           VARCHAR(500),
    author              VARCHAR(100),
    publish_status      VARCHAR(20) NOT NULL DEFAULT 'draft',
    view_count          INTEGER NOT NULL DEFAULT 0,
    tags                VARCHAR(500),
    quiz                TEXT,
    publish_time        TIMESTAMP,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_id           VARCHAR(32),
    author_department   VARCHAR(100),
    reviewer_id         VARCHAR(32),
    reviewer_name       VARCHAR(100),
    reviewed_at         TIMESTAMP,
    review_comment      VARCHAR(1000),
    reject_reason       VARCHAR(1000),
    published_by        VARCHAR(32),
    is_important        BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 1,
    deleted             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT knowledge_article_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_article_id ON knowledge_article (id);
CREATE INDEX IF NOT EXISTS idx_knowledge_article_category ON knowledge_article (category);
CREATE INDEX IF NOT EXISTS idx_knowledge_article_status ON knowledge_article (publish_status);
CREATE INDEX IF NOT EXISTS idx_knowledge_article_author ON knowledge_article (author_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_article_create_time ON knowledge_article (create_time);

-- ------------------------------------------------------------
-- 2. 员工学习记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS article_study_record (
    id                  VARCHAR(32) NOT NULL,
    article_id          VARCHAR(32) NOT NULL,
    article_title       VARCHAR(200) NOT NULL,
    article_category    VARCHAR(20) NOT NULL,
    employee_id         VARCHAR(32) NOT NULL,
    employee_name       VARCHAR(100) NOT NULL,
    employee_no         VARCHAR(50) NOT NULL,
    store_name          VARCHAR(100) NOT NULL,
    study_status        VARCHAR(20) NOT NULL DEFAULT 'not_started',
    read_progress       INTEGER NOT NULL DEFAULT 0,
    read_duration       INTEGER NOT NULL DEFAULT 0,
    last_read_time      TIMESTAMP,
    first_read_time     TIMESTAMP,
    quiz_status         VARCHAR(20) NOT NULL DEFAULT 'not_attempted',
    quiz_score          INTEGER,
    quiz_attempt_count  INTEGER NOT NULL DEFAULT 0,
    last_quiz_time      TIMESTAMP,
    certified           BOOLEAN NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT article_study_record_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_article_study_record_id ON article_study_record (id);
CREATE INDEX IF NOT EXISTS idx_study_record_article ON article_study_record (article_id);
CREATE INDEX IF NOT EXISTS idx_study_record_employee ON article_study_record (employee_id);
CREATE INDEX IF NOT EXISTS idx_study_record_status ON article_study_record (study_status);
CREATE INDEX IF NOT EXISTS idx_study_record_quiz ON article_study_record (quiz_status);
CREATE INDEX IF NOT EXISTS idx_study_record_category ON article_study_record (article_category);
CREATE INDEX IF NOT EXISTS idx_study_record_create_time ON article_study_record (create_time);

-- ------------------------------------------------------------
-- 3. 文章审核记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS article_review_record (
    record_id           VARCHAR(50) NOT NULL,
    article_id          VARCHAR(32) NOT NULL,
    action              VARCHAR(30) NOT NULL,
    operator_id         VARCHAR(32) NOT NULL,
    operator_name       VARCHAR(100) NOT NULL,
    operator_role       VARCHAR(30) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comment             VARCHAR(1000),
    reject_reason       VARCHAR(1000),
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT article_review_record_pkey PRIMARY KEY (record_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_article_review_record_id ON article_review_record (record_id);
CREATE INDEX IF NOT EXISTS idx_review_record_article ON article_review_record (article_id);
CREATE INDEX IF NOT EXISTS idx_review_record_operator ON article_review_record (operator_id);
CREATE INDEX IF NOT EXISTS idx_review_record_action ON article_review_record (action);
CREATE INDEX IF NOT EXISTS idx_review_record_created_at ON article_review_record (created_at);
