-- ============================================================
-- HR 培训模块建表脚本
-- 包含三张表：
--   1. training_course            培训课程表
--   2. training_study_record      培训学习记录表
--   3. training_certificate       培训证书表
-- 兼容 H2 (MODE=PostgreSQL) 与 PostgreSQL 18
-- ============================================================

-- ------------------------------------------------------------
-- 1. 培训课程表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS training_course (
    id                          VARCHAR(32) NOT NULL,
    title                       VARCHAR(200) NOT NULL,
    description                 TEXT,
    course_type                 VARCHAR(20) NOT NULL,
    related_article_id          VARCHAR(32),
    related_article_title       VARCHAR(200),
    instructor                  VARCHAR(100),
    total_lessons               INTEGER,
    duration                    INTEGER,
    deadline                    DATE,
    certificate_eligible        BOOLEAN NOT NULL DEFAULT FALSE,
    certificate_validity_days   INTEGER,
    publish_status              VARCHAR(20) NOT NULL DEFAULT 'draft',
    assigned_count              INTEGER NOT NULL DEFAULT 0,
    completed_count             INTEGER NOT NULL DEFAULT 0,
    average_score               INTEGER,
    create_time                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                     INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT training_course_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_training_course_id ON training_course (id);
CREATE INDEX IF NOT EXISTS idx_training_course_type ON training_course (course_type);
CREATE INDEX IF NOT EXISTS idx_training_course_status ON training_course (publish_status);
CREATE INDEX IF NOT EXISTS idx_training_course_article ON training_course (related_article_id);
CREATE INDEX IF NOT EXISTS idx_training_course_create_time ON training_course (create_time);

-- ------------------------------------------------------------
-- 2. 培训学习记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS training_study_record (
    id                  VARCHAR(32) NOT NULL,
    employee_id         VARCHAR(32) NOT NULL,
    employee_name       VARCHAR(100),
    employee_code       VARCHAR(50),
    department_name     VARCHAR(100),
    article_id          VARCHAR(32),
    article_title       VARCHAR(200),
    course_id           VARCHAR(32),
    course_title        VARCHAR(200),
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    duration_seconds    INTEGER,
    completed           BOOLEAN NOT NULL DEFAULT FALSE,
    score               INTEGER,
    certificate_id      VARCHAR(32),
    certificate_expiry  TIMESTAMP,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT training_study_record_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_training_study_record_id ON training_study_record (id);
CREATE INDEX IF NOT EXISTS idx_study_record_course ON training_study_record (course_id);
CREATE INDEX IF NOT EXISTS idx_study_record_employee ON training_study_record (employee_id);
CREATE INDEX IF NOT EXISTS idx_study_record_completed ON training_study_record (completed);
CREATE INDEX IF NOT EXISTS idx_study_record_article ON training_study_record (article_id);
CREATE INDEX IF NOT EXISTS idx_study_record_create_time ON training_study_record (create_time);

-- ------------------------------------------------------------
-- 3. 培训证书表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS training_certificate (
    id              VARCHAR(32) NOT NULL,
    employee_id     VARCHAR(32) NOT NULL,
    employee_name   VARCHAR(100),
    course_id       VARCHAR(32),
    course_title    VARCHAR(200),
    issue_date      DATE,
    expiry_date     DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'valid',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT training_certificate_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_training_certificate_id ON training_certificate (id);
CREATE INDEX IF NOT EXISTS idx_certificate_employee ON training_certificate (employee_id);
CREATE INDEX IF NOT EXISTS idx_certificate_course ON training_certificate (course_id);
CREATE INDEX IF NOT EXISTS idx_certificate_status ON training_certificate (status);
CREATE INDEX IF NOT EXISTS idx_certificate_issue_date ON training_certificate (issue_date);
CREATE INDEX IF NOT EXISTS idx_certificate_expiry_date ON training_certificate (expiry_date);
