package com.foodtraceability.service.event;

import com.foodtraceability.constant.RecruitmentConstants;
import java.util.List;
import java.util.Map;

/**
 * 招聘链路业务事件（统一子类，18 个静态工厂方法）。
 *
 * <p>遵循 ADR-005：每个工厂方法对应一个事件类型（msg_template.template_code），
 * 通过 {@link BusinessEvent} 父类的 eventType 字段区分，而非为每个事件创建独立子类。</p>
 *
 * <p>对应 spec 8.1 事件覆盖映射表中的 10 类核心事件 + 8 个额外事件：</p>
 * <ul>
 *   <li>Ch3.1 招聘名额 8 个事件</li>
 *   <li>Ch3.2 招聘协同 6 个事件</li>
 *   <li>Ch3.3 Offer 4 个事件</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaIssued(
 *     this, operatorId, variables, recipientUserIds));
 * }</pre></p>
 */
public class RecruitmentNotificationEvent extends BusinessEvent {

    // ============ Ch3.1 招聘名额 8 个事件 ============

    /**
     * 名额下发事件（HR 下发名额 → 门店店长）。
     * 渠道：站内信 + 邮件
     */
    public static RecruitmentNotificationEvent quotaIssued(Object source, Long operatorId,
                                                            Map<String, Object> variables,
                                                            List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_QUOTA_ISSUED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_AND_EMAIL);
    }

    /**
     * 名额确认事件（门店确认 → 下发 HR）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent quotaConfirmed(Object source, Long operatorId,
                                                              Map<String, Object> variables,
                                                              List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_QUOTA_CONFIRMED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 名额拒绝事件（门店拒绝 → 下发 HR）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent quotaRejected(Object source, Long operatorId,
                                                             Map<String, Object> variables,
                                                             List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_QUOTA_REJECTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 申请名额追加事件（门店申请追加 → 下发 HR）。
     * 渠道：站内信 + 邮件
     */
    public static RecruitmentNotificationEvent quotaAdjustmentRequested(Object source, Long operatorId,
                                                                        Map<String, Object> variables,
                                                                        List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_QUOTA_ADJUSTMENT_REQUESTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_AND_EMAIL);
    }

    /**
     * 名额追加审核结果事件（HR 审核 → 门店店长）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent quotaAdjustmentResult(Object source, Long operatorId,
                                                                     Map<String, Object> variables,
                                                                     List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_QUOTA_ADJUSTMENT_RESULT,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 名额即将用完事件（80% 阈值首次触发 → 门店店长 + HR）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent quotaExhausting(Object source, Long operatorId,
                                                               Map<String, Object> variables,
                                                               List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_QUOTA_EXHAUSTING,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 名额已用完事件（100% 触发 → 门店店长 + HR）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent quotaExhausted(Object source, Long operatorId,
                                                              Map<String, Object> variables,
                                                              List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_QUOTA_EXHAUSTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 名额关闭事件（HR 关闭 → 门店店长）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent quotaClosed(Object source, Long operatorId,
                                                           Map<String, Object> variables,
                                                           List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_QUOTA_CLOSED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    // ============ Ch3.2 招聘协同 6 个事件 ============

    /**
     * 招聘需求提交事件（门店提报 → HR 审核员）。
     * 渠道：站内信 + 邮件
     */
    public static RecruitmentNotificationEvent requirementSubmitted(Object source, Long operatorId,
                                                                    Map<String, Object> variables,
                                                                    List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_REQUIREMENT_SUBMITTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_AND_EMAIL);
    }

    /**
     * HR 给出反馈事件（HR feedback → 门店）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent feedbackGiven(Object source, Long operatorId,
                                                             Map<String, Object> variables,
                                                             List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_FEEDBACK_GIVEN,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 招聘需求通过事件（HR approve → 门店）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent requirementApproved(Object source, Long operatorId,
                                                                   Map<String, Object> variables,
                                                                   List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_REQUIREMENT_APPROVED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 招聘需求拒绝事件（HR reject → 门店）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent requirementRejected(Object source, Long operatorId,
                                                                   Map<String, Object> variables,
                                                                   List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_REQUIREMENT_REJECTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 门店面评提交事件（门店面试官提交 → HR）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent storeEvaluationSubmitted(Object source, Long operatorId,
                                                                        Map<String, Object> variables,
                                                                        List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_STORE_EVALUATION_SUBMITTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * 招聘需求重新提交事件（门店修改后重新提交 → HR）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent requirementResubmitted(Object source, Long operatorId,
                                                                      Map<String, Object> variables,
                                                                      List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_REQUIREMENT_RESUBMITTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    // ============ Ch3.3 Offer 4 个事件 ============

    /**
     * Offer 发送事件（HR 发送 → 候选人）。
     * 渠道：邮件
     */
    public static RecruitmentNotificationEvent offerSent(Object source, Long operatorId,
                                                         Map<String, Object> variables,
                                                         List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_OFFER_SENT,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_EMAIL_ONLY);
    }

    /**
     * Offer 接受事件（候选人接受 → HR）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent offerAccepted(Object source, Long operatorId,
                                                             Map<String, Object> variables,
                                                             List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_OFFER_ACCEPTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * Offer 拒绝事件（候选人拒绝 → HR）。
     * 渠道：站内信
     */
    public static RecruitmentNotificationEvent offerRejected(Object source, Long operatorId,
                                                             Map<String, Object> variables,
                                                             List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_OFFER_REJECTED,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_SITE_ONLY);
    }

    /**
     * Offer 撤回事件（HR 撤回 → 候选人）。
     * 渠道：邮件
     */
    public static RecruitmentNotificationEvent offerWithdrawn(Object source, Long operatorId,
                                                              Map<String, Object> variables,
                                                              List<Long> recipientUserIds) {
        return new RecruitmentNotificationEvent(source, RecruitmentConstants.EVENT_OFFER_WITHDRAWN,
                operatorId, variables, recipientUserIds, RecruitmentConstants.CHANNELS_EMAIL_ONLY);
    }

    /**
     * 私有构造器（仅工厂方法可调用）。
     *
     * @param source            事件源（通常为 this）
     * @param eventType         事件类型（对应 msg_template.template_code）
     * @param operatorId        操作人 ID（可为 null）
     * @param variables         模板变量（可为 null）
     * @param recipientUserIds  接收人列表（可为 null）
     * @param channels          渠道列表
     */
    private RecruitmentNotificationEvent(Object source, String eventType, Long operatorId,
                                         Map<String, Object> variables,
                                         List<Long> recipientUserIds, List<String> channels) {
        super(source, eventType, operatorId, variables, recipientUserIds, channels);
    }
}
