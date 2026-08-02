package com.foodtraceability.service.sender;

import com.foodtraceability.entity.MsgSendRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * WEBHOOK 渠道发送器
 *
 * <p>Sprint 1 范围内 WEBHOOK 渠道尚未接入真实 Webhook 接收端（如钉钉、企业微信机器人），
 * 此实现采用"跳过"策略：不抛异常，仅标记 record.sendStatus = SKIPPED（5）。</p>
 *
 * <p>对应 spec F-004 边界条件 / plan.md ADR-005（未实现渠道跳过策略）。</p>
 *
 * <p>后续 Sprint 接入真实 Webhook 时：
 * <ul>
 *   <li>替换本类 send() 方法体为 HTTP POST 调用 Webhook URL</li>
 *   <li>无需改动 NotificationMessageConsumer / NotificationEventBus</li>
 * </ul></p>
 */
@Component
public class WebhookChannelSender implements MessageChannelSender {

    private static final Logger logger = LoggerFactory.getLogger(WebhookChannelSender.class);

    /** WEBHOOK 渠道标识 */
    private static final String CHANNEL_WEBHOOK = "WEBHOOK";

    /** 发送状态：跳过（未实现渠道） */
    private static final int STATUS_SKIPPED = 5;

    @Override
    public String getChannel() {
        return CHANNEL_WEBHOOK;
    }

    @Override
    public void send(MsgSendRecord record) throws Exception {
        logger.info("WEBHOOK 渠道 Sprint 1 未实现, 跳过发送: recipient={}, recordId={}",
                record.getRecipient(), record.getRecordId());
        record.setSendStatus(STATUS_SKIPPED);
    }
}
