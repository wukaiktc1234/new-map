package com.foodtraceability.service.sender;

import com.foodtraceability.entity.MsgSendRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SMS 渠道发送器
 *
 * <p>Sprint 1 范围内 SMS 渠道尚未接入真实短信服务商（如阿里云短信、腾讯云短信），
 * 此实现采用"跳过"策略：不抛异常，仅标记 record.sendStatus = SKIPPED（5）。</p>
 *
 * <p>对应 spec F-004 边界条件 / plan.md ADR-005（未实现渠道跳过策略）。</p>
 *
 * <p>后续 Sprint 接入真实 SMS 服务时：
 * <ul>
 *   <li>替换本类 send() 方法体为调用真实短信 API</li>
 *   <li>无需改动 NotificationMessageConsumer / NotificationEventBus</li>
 * </ul></p>
 */
@Component
public class SmsChannelSender implements MessageChannelSender {

    private static final Logger logger = LoggerFactory.getLogger(SmsChannelSender.class);

    /** SMS 渠道标识 */
    private static final String CHANNEL_SMS = "SMS";

    /** 发送状态：跳过（未实现渠道） */
    private static final int STATUS_SKIPPED = 5;

    @Override
    public String getChannel() {
        return CHANNEL_SMS;
    }

    @Override
    public void send(MsgSendRecord record) throws Exception {
        logger.info("SMS 渠道 Sprint 1 未实现, 跳过发送: recipient={}, recordId={}",
                record.getRecipient(), record.getRecordId());
        record.setSendStatus(STATUS_SKIPPED);
    }
}
