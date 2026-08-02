package com.foodtraceability.constant;

import java.util.List;

/**
 * 招聘链路相关常量
 * 集中管理 Sprint 2 招聘链路（名额/协同/Offer）的状态、事件类型、渠道、阈值等常量。
 *
 * <p>遵循 ADR-002：状态常量集中管理，避免散落在各 ServiceImpl。</p>
 */
public class RecruitmentConstants {

    private RecruitmentConstants() {
    }

    // ============ Ch3.1 招聘名额状态（7 状态） ============

    /** 名额状态：草稿 */
    public static final String QUOTA_DRAFT = "draft";

    /** 名额状态：已下发 */
    public static final String QUOTA_ISSUED = "issued";

    /** 名额状态：已激活（门店确认后） */
    public static final String QUOTA_ACTIVE = "active";

    /** 名额状态：已用完 */
    public static final String QUOTA_EXHAUSTED = "exhausted";

    /** 名额状态：已关闭 */
    public static final String QUOTA_CLOSED = "closed";

    /** 名额状态：已拒绝（门店拒绝） */
    public static final String QUOTA_REJECTED = "rejected";

    /** 名额状态：申请追加中 */
    public static final String QUOTA_ADJUSTMENT_REQUESTED = "adjustment_requested";

    // ============ Ch3.3 JobOffer 状态（6 状态） ============

    /** Offer 状态：待发送 */
    public static final String OFFER_PENDING = "pending";

    /** Offer 状态：已发送 */
    public static final String OFFER_SENT = "sent";

    /** Offer 状态：已接受 */
    public static final String OFFER_ACCEPTED = "accepted";

    /** Offer 状态：已入职 */
    public static final String OFFER_ONBOARDED = "onboarded";

    /** Offer 状态：已拒绝 */
    public static final String OFFER_REJECTED = "rejected";

    /** Offer 状态：已撤回 */
    public static final String OFFER_WITHDRAWN = "withdrawn";

    // ============ 招聘需求状态（兼容现状 + 新增反馈流） ============

    /** 需求状态：待处理（等价于 spec 中 "submitted"） */
    public static final String REQ_OPEN = "open";

    /** 需求状态：已提交 */
    public static final String REQ_SUBMITTED = "submitted";

    /** 需求状态：已反馈（HR 给出 feedback 后） */
    public static final String REQ_FEEDBACK_GIVEN = "feedback_given";

    /** 需求状态：已通过（HR approve） */
    public static final String REQ_APPROVED = "approved";

    /** 需求状态：已拒绝（HR reject） */
    public static final String REQ_REJECTED = "rejected";

    /** 需求状态：已关闭 */
    public static final String REQ_CLOSED = "closed";

    /** 需求状态：已满足（招满） */
    public static final String REQ_FILLED = "filled";

    // ============ 反馈类型 ============

    /** 反馈类型：通过 */
    public static final String FEEDBACK_APPROVE = "approve";

    /** 反馈类型：需补充（给出反馈意见） */
    public static final String FEEDBACK_FEEDBACK = "feedback";

    /** 反馈类型：拒绝 */
    public static final String FEEDBACK_REJECT = "reject";

    // ============ 事件类型常量（18 个事件编码，对应 msg_template.template_code） ============

    // ---- Ch3.1 招聘名额 8 个事件 ----

    /** 事件：名额下发 */
    public static final String EVENT_QUOTA_ISSUED = "recruitment.quota.issued";

    /** 事件：名额确认（门店确认） */
    public static final String EVENT_QUOTA_CONFIRMED = "recruitment.quota.confirmed";

    /** 事件：名额拒绝（门店拒绝） */
    public static final String EVENT_QUOTA_REJECTED = "recruitment.quota.rejected";

    /** 事件：申请名额追加 */
    public static final String EVENT_QUOTA_ADJUSTMENT_REQUESTED = "recruitment.quota.adjustment_requested";

    /** 事件：名额追加审核结果 */
    public static final String EVENT_QUOTA_ADJUSTMENT_RESULT = "recruitment.quota.adjustment_result";

    /** 事件：名额即将用完（80% 阈值） */
    public static final String EVENT_QUOTA_EXHAUSTING = "recruitment.quota.exhausting";

    /** 事件：名额已用完（100%） */
    public static final String EVENT_QUOTA_EXHAUSTED = "recruitment.quota.exhausted";

    /** 事件：名额关闭 */
    public static final String EVENT_QUOTA_CLOSED = "recruitment.quota.closed";

    // ---- Ch3.2 招聘协同 6 个事件 ----

    /** 事件：招聘需求提交 */
    public static final String EVENT_REQUIREMENT_SUBMITTED = "recruitment.requirement.submitted";

    /** 事件：HR 给出反馈 */
    public static final String EVENT_FEEDBACK_GIVEN = "recruitment.feedback.given";

    /** 事件：招聘需求通过 */
    public static final String EVENT_REQUIREMENT_APPROVED = "recruitment.approved";

    /** 事件：招聘需求拒绝 */
    public static final String EVENT_REQUIREMENT_REJECTED = "recruitment.rejected";

    /** 事件：门店面评提交 */
    public static final String EVENT_STORE_EVALUATION_SUBMITTED = "interview.store_evaluation.submitted";

    /** 事件：招聘需求重新提交 */
    public static final String EVENT_REQUIREMENT_RESUBMITTED = "recruitment.requirement.resubmitted";

    // ---- Ch3.3 Offer 4 个事件 ----

    /** 事件：Offer 发送 */
    public static final String EVENT_OFFER_SENT = "offer.sent";

    /** 事件：Offer 接受 */
    public static final String EVENT_OFFER_ACCEPTED = "offer.accepted";

    /** 事件：Offer 拒绝 */
    public static final String EVENT_OFFER_REJECTED = "offer.rejected";

    /** 事件：Offer 撤回 */
    public static final String EVENT_OFFER_WITHDRAWN = "offer.withdrawn";

    // ============ 通知渠道常量 ============

    /** 渠道：站内信 */
    public static final String CHANNEL_SITE_MSG = "SITE_MSG";

    /** 渠道：邮件 */
    public static final String CHANNEL_EMAIL = "EMAIL";

    /** 站内信+邮件组合渠道 */
    public static final List<String> CHANNELS_SITE_AND_EMAIL = List.of(CHANNEL_SITE_MSG, CHANNEL_EMAIL);

    /** 仅站内信渠道 */
    public static final List<String> CHANNELS_SITE_ONLY = List.of(CHANNEL_SITE_MSG);

    /** 仅邮件渠道 */
    public static final List<String> CHANNELS_EMAIL_ONLY = List.of(CHANNEL_EMAIL);

    // ============ 名额阈值 ============

    /** 名额即将用完阈值（已用数 / 总数 >= 0.8 时触发 exhausting 事件） */
    public static final double QUOTA_EXHAUSTING_THRESHOLD = 0.8;
}
