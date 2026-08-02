package com.foodtraceability.service.sender;

import com.foodtraceability.entity.MsgSendRecord;

/**
 * 消息渠道发送器接口（Strategy 模式）
 *
 * <p>各渠道（EMAIL / SITE_MSG / SMS / WEBHOOK）独立实现，由 NotificationMessageConsumer
 * 通过 Map<String, MessageChannelSender> 路由调用。</p>
 *
 * <p>对应 plan.md ADR-004（渠道抽象设计，Strategy 模式）。
 * 新增渠道（如钉钉、企业微信）只需新增 Sender 实现，不修改消费者。</p>
 *
 * <p>实现类：
 * <ul>
 *   <li>{@link EmailChannelSender} - EMAIL 渠道，调用 EmailService 真实发送</li>
 *   <li>{@link SiteMsgChannelSender} - SITE_MSG 渠道，委托 SiteNotificationService 落库 + WebSocket</li>
 *   <li>{@link SmsChannelSender} - SMS 渠道，Sprint 1 跳过（status=SKIPPED）</li>
 *   <li>{@link WebhookChannelSender} - WEBHOOK 渠道，Sprint 1 跳过（status=SKIPPED）</li>
 * </ul></p>
 */
public interface MessageChannelSender {

    /**
     * 获取渠道标识。
     *
     * @return 渠道标识：EMAIL / SITE_MSG / SMS / WEBHOOK
     */
    String getChannel();

    /**
     * 发送消息。
     *
     * <p>实现要求：
     * <ul>
     *   <li>成功：更新 record.sendStatus = SUCCESS（2），SITE_MSG 渠道还需回填 notificationId</li>
     *   <li>跳过（SMS/WEBHOOK 未实现）：更新 record.sendStatus = SKIPPED（5），不抛异常</li>
     *   <li>失败：抛出 Exception，由 NotificationMessageConsumer 统一处理重试/死信</li>
     * </ul></p>
     *
     * @param record 发送记录（包含 recipient/subject/content/channel 等字段）
     * @throws Exception 发送失败时抛出（触发重试机制）
     */
    void send(MsgSendRecord record) throws Exception;
}
