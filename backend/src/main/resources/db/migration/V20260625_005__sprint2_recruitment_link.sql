-- ============================================================
-- Sprint 2 招聘链路 - 数据库迁移脚本
-- 版本: V20260625_005__sprint2_recruitment_link.sql
-- 数据库: PostgreSQL 18（兼容 H2 MODE=PostgreSQL）
--
-- 变更内容:
-- 1. 新建表 recruitment_quotas（招聘名额表，Ch3.1）
-- 2. 新建表 recruitment_feedback（招聘反馈表，Ch3.2）
-- 3. 新建表 job_offers（录用Offer表，Ch3.3）
-- 4. 扩展表 interviews 新增 5 字段（门店面评，Ch3.2）
-- 5. 扩展表 recruitment_requirements 新增 quota_id 字段（Ch3.1 联动反查）
-- 6. 注册 18 个通知模板（Ch3.1 名额8个 + Ch3.2 协同6个 + Ch3.3 Offer4个）
--
-- 对应文档:
-- - spec.md BC-001/BC-002（名额校验联动）
-- - spec.md BC-005（事务安全）
-- - plan.md 第 4 节（DDL）
-- - plan.md ADR-003（VARCHAR(32) 雪花ID）
-- - plan.md ADR-004（recruitment_requirements 扩展 quota_id）
--
-- 注意:
-- - requirement_id / resume_id / interview_id 使用 VARCHAR(32) 雪花（Sprint 1.5 已迁移）
-- - 薪资字段使用 BIGINT（单位:分，遵循 project_rules.md 金额存储规范）
-- - 18 个模板使用 ON CONFLICT (template_code) DO NOTHING 幂等插入
-- ============================================================

-- ============================================================
-- 1. 新建表：recruitment_quotas（招聘名额表）
-- 子模块: Ch3.1 招聘名额系统
-- 主键: quota_id BIGINT GENERATED ALWAYS AS IDENTITY（自增）
-- ============================================================
CREATE TABLE IF NOT EXISTS recruitment_quotas (
    quota_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    year                INTEGER                     NOT NULL,
    quarter             INTEGER                     NOT NULL,
    store_id            BIGINT                      NOT NULL,
    store_name          VARCHAR(100)                NOT NULL,
    position_id         BIGINT                      NOT NULL,
    position_name       VARCHAR(100)                NOT NULL,
    headcount           INTEGER                     NOT NULL,
    used_count          INTEGER                     NOT NULL DEFAULT 0,
    status              VARCHAR(32)                 NOT NULL DEFAULT 'draft',
    issued_by           BIGINT,
    issued_time         TIMESTAMP,
    confirmed_by        BIGINT,
    confirmed_time      TIMESTAMP,
    expire_date         DATE,
    remark              VARCHAR(500),
    create_time         TIMESTAMP                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER                     NOT NULL DEFAULT 0,
    CONSTRAINT ck_quota_quarter CHECK (quarter IN (1, 2, 3, 4)),
    CONSTRAINT ck_quota_headcount CHECK (headcount > 0),
    CONSTRAINT ck_quota_used_count CHECK (used_count >= 0)
);

COMMENT ON TABLE  recruitment_quotas IS '招聘名额表-HR按年度季度向门店下发招聘名额';
COMMENT ON COLUMN recruitment_quotas.quota_id IS '主键ID(自增)';
COMMENT ON COLUMN recruitment_quotas.year IS '年度';
COMMENT ON COLUMN recruitment_quotas.quarter IS '季度(1-4)';
COMMENT ON COLUMN recruitment_quotas.store_id IS '门店ID';
COMMENT ON COLUMN recruitment_quotas.store_name IS '门店名称(冗余,避免关联查询)';
COMMENT ON COLUMN recruitment_quotas.position_id IS '岗位ID';
COMMENT ON COLUMN recruitment_quotas.position_name IS '岗位名称(冗余)';
COMMENT ON COLUMN recruitment_quotas.headcount IS '名额数(>0)';
COMMENT ON COLUMN recruitment_quotas.used_count IS '已用名额数(>=0,入职成功时+1)';
COMMENT ON COLUMN recruitment_quotas.status IS '状态(draft/issued/active/exhausted/closed/rejected/adjustment_requested)';
COMMENT ON COLUMN recruitment_quotas.issued_by IS '下发人ID(HR用户ID)';
COMMENT ON COLUMN recruitment_quotas.issued_time IS '下发时间';
COMMENT ON COLUMN recruitment_quotas.confirmed_by IS '确认人ID(门店店长用户ID)';
COMMENT ON COLUMN recruitment_quotas.confirmed_time IS '确认时间';
COMMENT ON COLUMN recruitment_quotas.expire_date IS '名额有效期';
COMMENT ON COLUMN recruitment_quotas.remark IS '备注';
COMMENT ON COLUMN recruitment_quotas.create_time IS '创建时间';
COMMENT ON COLUMN recruitment_quotas.update_time IS '更新时间';
COMMENT ON COLUMN recruitment_quotas.deleted IS '逻辑删除:0=未删除 1=已删除';

-- 索引（按 project_rules.md 第八章 idx_{table}_{field} 命名规范）
CREATE INDEX IF NOT EXISTS idx_recruitment_quotas_store        ON recruitment_quotas(store_id);
CREATE INDEX IF NOT EXISTS idx_recruitment_quotas_status       ON recruitment_quotas(status);
CREATE INDEX IF NOT EXISTS idx_recruitment_quotas_year_quarter ON recruitment_quotas(year, quarter);
CREATE INDEX IF NOT EXISTS idx_recruitment_quotas_position     ON recruitment_quotas(position_id);
CREATE INDEX IF NOT EXISTS idx_recruitment_quotas_issued_by    ON recruitment_quotas(issued_by);
-- 唯一索引：同门店同岗位同年同季度仅允许一条未删除名额（部分唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS uk_recruitment_quotas_store_pos_year_quarter
    ON recruitment_quotas(store_id, position_id, year, quarter) WHERE deleted = 0;

-- ============================================================
-- 2. 新建表：recruitment_feedback（招聘反馈表）
-- 子模块: Ch3.2 门店↔HR 招聘协同反馈
-- 主键: feedback_id BIGINT GENERATED ALWAYS AS IDENTITY（自增）
-- requirement_id 类型: VARCHAR(32)（雪花算法，Sprint 1.5 已将 recruitment_requirements.id 迁移为 VARCHAR(32) 雪花）
-- ============================================================
CREATE TABLE IF NOT EXISTS recruitment_feedback (
    feedback_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    requirement_id      VARCHAR(32)                 NOT NULL,
    feedback_type       VARCHAR(20)                 NOT NULL,
    content             TEXT                        NOT NULL,
    reviewer_id         BIGINT                      NOT NULL,
    reviewer_name       VARCHAR(100)                NOT NULL,
    review_time         TIMESTAMP                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time         TIMESTAMP                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER                     NOT NULL DEFAULT 0,
    CONSTRAINT ck_feedback_type CHECK (feedback_type IN ('approve', 'feedback', 'reject'))
);

COMMENT ON TABLE  recruitment_feedback IS '招聘反馈表-HR对门店提报的招聘需求反馈';
COMMENT ON COLUMN recruitment_feedback.feedback_id IS '主键ID(自增)';
COMMENT ON COLUMN recruitment_feedback.requirement_id IS '招聘需求ID(关联recruitment_requirements.id,VARCHAR(32)雪花)';
COMMENT ON COLUMN recruitment_feedback.feedback_type IS '反馈类型:approve=通过 feedback=需修改 reject=驳回';
COMMENT ON COLUMN recruitment_feedback.content IS '反馈内容';
COMMENT ON COLUMN recruitment_feedback.reviewer_id IS '审核人ID(HR用户ID)';
COMMENT ON COLUMN recruitment_feedback.reviewer_name IS '审核人姓名';
COMMENT ON COLUMN recruitment_feedback.review_time IS '审核时间';
COMMENT ON COLUMN recruitment_feedback.create_time IS '创建时间';
COMMENT ON COLUMN recruitment_feedback.update_time IS '更新时间';
COMMENT ON COLUMN recruitment_feedback.deleted IS '逻辑删除:0=未删除 1=已删除';

CREATE INDEX IF NOT EXISTS idx_recruitment_feedback_requirement ON recruitment_feedback(requirement_id);
CREATE INDEX IF NOT EXISTS idx_recruitment_feedback_reviewer    ON recruitment_feedback(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_recruitment_feedback_type        ON recruitment_feedback(feedback_type);

-- ============================================================
-- 3. 新建表：job_offers（录用 Offer 表）
-- 子模块: Ch3.3 录用 Offer 持久化
-- 主键: offer_id BIGINT GENERATED ALWAYS AS IDENTITY（自增）
-- requirement_id / resume_id / interview_id 类型: VARCHAR(32)（雪花算法，Sprint 1.5 已迁移）
-- 薪资字段: BIGINT（单位:分，遵循 project_rules.md 金额存储规范）
-- ============================================================
CREATE TABLE IF NOT EXISTS job_offers (
    offer_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    requirement_id      VARCHAR(32)                 NOT NULL,
    resume_id           VARCHAR(32)                 NOT NULL,
    interview_id        VARCHAR(32),
    candidate_name      VARCHAR(100)                NOT NULL,
    candidate_email     VARCHAR(100),
    position_id         BIGINT                      NOT NULL,
    position_name       VARCHAR(100)                NOT NULL,
    store_id            BIGINT,
    store_name          VARCHAR(100),
    proposed_salary     BIGINT                      NOT NULL,
    probation_months    INTEGER                     NOT NULL DEFAULT 3,
    probation_salary    BIGINT,
    status              VARCHAR(32)                 NOT NULL DEFAULT 'pending',
    send_time           TIMESTAMP,
    accept_time         TIMESTAMP,
    reject_time         TIMESTAMP,
    reject_reason       VARCHAR(500),
    entry_date          DATE,
    remark              VARCHAR(500),
    create_time         TIMESTAMP                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER                     NOT NULL DEFAULT 0,
    CONSTRAINT ck_offer_status CHECK (status IN ('pending', 'sent', 'accepted', 'onboarded', 'rejected', 'withdrawn')),
    CONSTRAINT ck_offer_probation CHECK (probation_months >= 0 AND probation_months <= 6),
    CONSTRAINT ck_offer_salary CHECK (proposed_salary > 0)
);

COMMENT ON TABLE  job_offers IS '录用Offer表-持久化录用数据并联动入职流程';
COMMENT ON COLUMN job_offers.offer_id IS '主键ID(自增)';
COMMENT ON COLUMN job_offers.requirement_id IS '招聘需求ID(关联recruitment_requirements.id,VARCHAR(32)雪花)';
COMMENT ON COLUMN job_offers.resume_id IS '简历ID(关联resumes.id,VARCHAR(32)雪花)';
COMMENT ON COLUMN job_offers.interview_id IS '面试ID(关联interviews.id,VARCHAR(32)雪花,可空)';
COMMENT ON COLUMN job_offers.candidate_name IS '候选人姓名';
COMMENT ON COLUMN job_offers.candidate_email IS '候选人邮箱(发送Offer邮件用)';
COMMENT ON COLUMN job_offers.position_id IS '岗位ID';
COMMENT ON COLUMN job_offers.position_name IS '岗位名称(冗余)';
COMMENT ON COLUMN job_offers.store_id IS '门店ID(可空,HR通用岗位无门店)';
COMMENT ON COLUMN job_offers.store_name IS '门店名称(冗余)';
COMMENT ON COLUMN job_offers.proposed_salary IS '拟定薪资(单位:分,BIGINT)';
COMMENT ON COLUMN job_offers.probation_months IS '试用期月数(0-6,默认3)';
COMMENT ON COLUMN job_offers.probation_salary IS '试用期薪资(单位:分,可空)';
COMMENT ON COLUMN job_offers.status IS '状态:pending=待发送 sent=已发送 accepted=已接受 onboarded=已入职 rejected=已拒绝 withdrawn=已撤回';
COMMENT ON COLUMN job_offers.send_time IS '发送时间';
COMMENT ON COLUMN job_offers.accept_time IS '接受时间';
COMMENT ON COLUMN job_offers.reject_time IS '拒绝时间';
COMMENT ON COLUMN job_offers.reject_reason IS '拒绝原因';
COMMENT ON COLUMN job_offers.entry_date IS '预期入职日期';
COMMENT ON COLUMN job_offers.remark IS '备注';
COMMENT ON COLUMN job_offers.create_time IS '创建时间';
COMMENT ON COLUMN job_offers.update_time IS '更新时间';
COMMENT ON COLUMN job_offers.deleted IS '逻辑删除:0=未删除 1=已删除';

CREATE INDEX IF NOT EXISTS idx_job_offers_requirement  ON job_offers(requirement_id);
CREATE INDEX IF NOT EXISTS idx_job_offers_resume       ON job_offers(resume_id);
CREATE INDEX IF NOT EXISTS idx_job_offers_candidate    ON job_offers(candidate_name);
CREATE INDEX IF NOT EXISTS idx_job_offers_status       ON job_offers(status);
CREATE INDEX IF NOT EXISTS idx_job_offers_position     ON job_offers(position_id);
CREATE INDEX IF NOT EXISTS idx_job_offers_store        ON job_offers(store_id);
CREATE INDEX IF NOT EXISTS idx_job_offers_entry_date   ON job_offers(entry_date);

-- ============================================================
-- 4. 扩展表：interviews（ALTER TABLE 新增 5 字段）
-- 子模块: Ch3.2 门店协同面试面评
-- store_interviewer_id 类型: VARCHAR(32)（关联 users.id BIGINT，以字符串存储；32 字符足以容纳 BIGINT 数字字符串）
-- 注: interviews.interviewer_id 现有类型为 String（预存在技术债务），store_interviewer_id 跟随现状用 VARCHAR
-- ============================================================
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS store_interviewer_id      VARCHAR(32);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS store_interviewer_name    VARCHAR(100);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS store_evaluation          TEXT;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS store_evaluation_time     TIMESTAMP;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS store_evaluation_score    INTEGER;

COMMENT ON COLUMN interviews.store_interviewer_id     IS '门店面试官ID(用户ID,字符串兼容)';
COMMENT ON COLUMN interviews.store_interviewer_name   IS '门店面试官姓名';
COMMENT ON COLUMN interviews.store_evaluation         IS '门店面评内容';
COMMENT ON COLUMN interviews.store_evaluation_time    IS '门店面评提交时间';
COMMENT ON COLUMN interviews.store_evaluation_score   IS '门店面评评分(1-5)';

-- 添加 CHECK 约束（评分范围 1-5，允许 NULL 表示未提交面评）
ALTER TABLE interviews ADD CONSTRAINT ck_interview_store_score
    CHECK (store_evaluation_score IS NULL OR (store_evaluation_score >= 1 AND store_evaluation_score <= 5));

CREATE INDEX IF NOT EXISTS idx_interviews_store_interviewer ON interviews(store_interviewer_id);

-- ============================================================
-- 5. 扩展表：recruitment_requirements（ALTER TABLE 新增 quota_id）
-- 子模块: Ch3.1 联动反查
-- 用途: 入职成功时通过 requirement_id → quota_id 反查名额并 incrementUsedCount
-- 类型: BIGINT（关联 recruitment_quotas.quota_id）
-- 可空: HR 端提报需求不需要名额校验,quota_id 可为 NULL
-- ============================================================
ALTER TABLE recruitment_requirements ADD COLUMN IF NOT EXISTS quota_id BIGINT;

COMMENT ON COLUMN recruitment_requirements.quota_id IS '关联招聘名额ID(可空,门店提报时必填,HR提报时可空)';

CREATE INDEX IF NOT EXISTS idx_recruitment_requirements_quota ON recruitment_requirements(quota_id);

-- ============================================================
-- 6. 通知模板注册（18 个 INSERT）
-- 语法: {varName}（Sprint 1 NotificationEventBus 渲染语法）
-- status=1 启用
-- template_type: 1=EMAIL 2=SITE_MSG（按主渠道设置）
-- 渠道在事件发布时通过 BusinessEvent.channels 指定,模板的 channel 字段为主渠道
-- 使用 ON CONFLICT (template_code) DO NOTHING 幂等插入
-- ============================================================

-- ---------- Ch3.1 招聘名额 8 个模板 ----------

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.quota.issued',
    '招聘名额-下发通知',
    '【招聘名额】您收到 {year}年第{quarter}季度招聘名额',
    '您收到新的招聘名额：年度={year}，季度={quarter}，门店={storeName}，岗位={positionName}，名额数={headcount}，有效期={expireDate}。请及时确认。',
    2, 'SITE_MSG', 1,
    '{"year":"年度","quarter":"季度","storeName":"门店名","positionName":"岗位名","headcount":"名额数","expireDate":"有效期"}'::jsonb,
    '{"year":"2026","quarter":"3","storeName":"上海人民广场店","positionName":"服务员","headcount":"5","expireDate":"2026-09-30"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.quota.confirmed',
    '招聘名额-门店确认通知',
    '【招聘名额】{storeName} 已确认接收名额',
    '门店 {storeName} 已确认接收 {positionName} 岗位的招聘名额。确认时间：{confirmedTime}。',
    2, 'SITE_MSG', 1,
    '{"storeName":"门店名","positionName":"岗位名","confirmedTime":"确认时间"}'::jsonb,
    '{"storeName":"上海人民广场店","positionName":"服务员","confirmedTime":"2026-06-25 14:30:00"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.quota.rejected',
    '招聘名额-门店拒绝通知',
    '【招聘名额】{storeName} 拒绝接收名额',
    '门店 {storeName} 拒绝接收 {positionName} 岗位的招聘名额。拒绝原因：{rejectReason}。',
    2, 'SITE_MSG', 1,
    '{"storeName":"门店名","positionName":"岗位名","rejectReason":"拒绝原因"}'::jsonb,
    '{"storeName":"上海人民广场店","positionName":"服务员","rejectReason":"人员已满"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.quota.adjustment_requested',
    '招聘名额-门店申请追加通知',
    '【招聘名额】{storeName} 申请追加名额',
    '门店 {storeName} 申请追加 {positionName} 岗位招聘名额。追加数量：{additionalCount}，原因：{reason}。',
    2, 'SITE_MSG', 1,
    '{"storeName":"门店名","positionName":"岗位名","additionalCount":"追加数","reason":"原因"}'::jsonb,
    '{"storeName":"上海人民广场店","positionName":"服务员","additionalCount":"2","reason":"周末客流增加"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.quota.adjustment_result',
    '招聘名额-追加审核结果通知',
    '【招聘名额】您的追加申请已审核',
    '您提交的 {storeName} {positionName} 岗位名额追加申请已审核。审核结果：{approved}，备注：{remark}。',
    2, 'SITE_MSG', 1,
    '{"storeName":"门店名","positionName":"岗位名","approved":"审核结果","remark":"备注"}'::jsonb,
    '{"storeName":"上海人民广场店","positionName":"服务员","approved":"通过","remark":"同意追加"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.quota.exhausting',
    '招聘名额-即将用完通知(80%)',
    '【招聘名额】{storeName} {positionName} 名额即将用完',
    '门店 {storeName} 的 {positionName} 岗位招聘名额即将用完。已用：{usedCount}，总数：{headcount}，剩余：{remaining}。',
    2, 'SITE_MSG', 1,
    '{"storeName":"门店名","positionName":"岗位名","usedCount":"已用数","headcount":"总数","remaining":"剩余数"}'::jsonb,
    '{"storeName":"上海人民广场店","positionName":"服务员","usedCount":"4","headcount":"5","remaining":"1"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.quota.exhausted',
    '招聘名额-已用完通知',
    '【招聘名额】{storeName} {positionName} 名额已用完',
    '门店 {storeName} 的 {positionName} 岗位招聘名额已用完（{usedCount}/{headcount}）。如有需求请申请新的名额。',
    2, 'SITE_MSG', 1,
    '{"storeName":"门店名","positionName":"岗位名","usedCount":"已用数","headcount":"总数"}'::jsonb,
    '{"storeName":"上海人民广场店","positionName":"服务员","usedCount":"5","headcount":"5"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.quota.closed',
    '招聘名额-已关闭通知',
    '【招聘名额】{storeName} {positionName} 名额已关闭',
    '门店 {storeName} 的 {positionName} 岗位招聘名额已被 HR 关闭。如有疑问请联系 HR。',
    2, 'SITE_MSG', 1,
    '{"storeName":"门店名","positionName":"岗位名"}'::jsonb,
    '{"storeName":"上海人民广场店","positionName":"服务员"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

-- ---------- Ch3.2 招聘协同 6 个模板 ----------

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.requirement.submitted',
    '招聘需求-门店提报通知',
    '【招聘需求】门店 {storeName} 提报了新的招聘需求',
    '门店 {storeName} 提报了 {positionName} 岗位招聘需求。需求数量：{requirementNum}，请及时审核。',
    2, 'SITE_MSG', 1,
    '{"storeName":"门店名","positionName":"岗位名","requirementNum":"需求数"}'::jsonb,
    '{"storeName":"上海人民广场店","positionName":"服务员","requirementNum":"3"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.feedback.given',
    '招聘需求-HR反馈通知',
    '【招聘需求】您的招聘需求需修改',
    '您提报的 {positionName} 岗位招聘需求需修改。HR 反馈：{feedbackContent}。请修改后重新提交。',
    2, 'SITE_MSG', 1,
    '{"positionName":"岗位名","feedbackContent":"反馈内容"}'::jsonb,
    '{"positionName":"服务员","feedbackContent":"薪资范围需调整"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.approved',
    '招聘需求-HR审核通过通知',
    '【招聘需求】您的招聘需求已通过审核',
    '您提报的 {positionName} 岗位招聘需求已通过 HR 审核，开始招聘流程。',
    2, 'SITE_MSG', 1,
    '{"positionName":"岗位名"}'::jsonb,
    '{"positionName":"服务员"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.rejected',
    '招聘需求-HR驳回通知',
    '【招聘需求】您的招聘需求已驳回',
    '您提报的 {positionName} 岗位招聘需求已被 HR 驳回。驳回原因：{rejectReason}。',
    2, 'SITE_MSG', 1,
    '{"positionName":"岗位名","rejectReason":"驳回原因"}'::jsonb,
    '{"positionName":"服务员","rejectReason":"岗位编制已满"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'interview.store_evaluation.submitted',
    '面试-门店面评提交通知',
    '【面试】门店已提交面评',
    '门店面试官 {storeInterviewerName} 已提交 {candidateName} 的面试面评。评分：{score}，评价：{evaluation}。',
    2, 'SITE_MSG', 1,
    '{"storeInterviewerName":"面试官名","candidateName":"候选人名","score":"评分","evaluation":"评价"}'::jsonb,
    '{"storeInterviewerName":"李店长","candidateName":"张三","score":"4","evaluation":"沟通能力好"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'recruitment.requirement.resubmitted',
    '招聘需求-重新提交通知',
    '【招聘需求】门店已重新提交招聘需求',
    '门店已修改并重新提交 {positionName} 岗位招聘需求，请及时审核。',
    2, 'SITE_MSG', 1,
    '{"positionName":"岗位名"}'::jsonb,
    '{"positionName":"服务员"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

-- ---------- Ch3.3 Offer 4 个模板 ----------

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'offer.sent',
    'Offer-发送通知',
    '【录用通知】{companyName} 录用 Offer',
    '尊敬的 {candidateName}：恭喜您通过面试！{companyName} 拟录用您为 {positionName}。薪资：{proposedSalary} 元/月，试用期 {probationMonths} 个月。预期入职日期：{entryDate}。请在 {expireDate} 前回复是否接受。',
    1, 'EMAIL', 1,
    '{"candidateName":"候选人名","companyName":"公司名","positionName":"岗位名","proposedSalary":"薪资(元)","probationMonths":"试用期月数","entryDate":"入职日期","expireDate":"回复截止"}'::jsonb,
    '{"candidateName":"张三","companyName":"食品溯源公司","positionName":"服务员","proposedSalary":"8000","probationMonths":"3","entryDate":"2026-07-01","expireDate":"2026-06-28"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'offer.accepted',
    'Offer-候选人接受通知',
    '【录用通知】候选人 {candidateName} 已接受 Offer',
    '候选人 {candidateName} 已接受 {positionName} 岗位的录用 Offer。请及时办理转入职手续。',
    2, 'SITE_MSG', 1,
    '{"candidateName":"候选人名","positionName":"岗位名"}'::jsonb,
    '{"candidateName":"张三","positionName":"服务员"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'offer.rejected',
    'Offer-候选人拒绝通知',
    '【录用通知】候选人 {candidateName} 拒绝了 Offer',
    '候选人 {candidateName} 拒绝了 {positionName} 岗位的录用 Offer。拒绝原因：{rejectReason}。',
    2, 'SITE_MSG', 1,
    '{"candidateName":"候选人名","positionName":"岗位名","rejectReason":"拒绝原因"}'::jsonb,
    '{"candidateName":"张三","positionName":"服务员","rejectReason":"已接受其他offer"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data)
VALUES
(
    'offer.withdrawn',
    'Offer-HR撤回通知',
    '【录用通知】{companyName} 已撤回 Offer',
    '尊敬的 {candidateName}：很抱歉，{companyName} 已撤回 {positionName} 岗位的录用 Offer。如有疑问请联系 HR。',
    1, 'EMAIL', 1,
    '{"candidateName":"候选人名","companyName":"公司名","positionName":"岗位名"}'::jsonb,
    '{"candidateName":"张三","companyName":"食品溯源公司","positionName":"服务员"}'::jsonb
) ON CONFLICT (template_code) DO NOTHING;
