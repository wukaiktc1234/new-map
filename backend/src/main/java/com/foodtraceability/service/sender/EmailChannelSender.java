package com.foodtraceability.service.sender;

import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * EMAIL 渠道发送器
 *
 * <p>调用 {@link EmailService#sendEmail} 真实发送邮件。
 * 适用于 EMAIL 渠道的所有业务通知场景。</p>
 *
 * <p>对应 spec F-002 / plan.md ADR-004：渠道抽象 Strategy 模式。</p>
 *
 * <p>状态转换：
 * <ul>
 *   <li>成功：record.sendStatus = SUCCESS（2）</li>
 *   <li>失败：抛出 Exception，由 NotificationMessageConsumer 触发重试/死信</li>
 * </ul></p>
 */
@Component
public class EmailChannelSender implements MessageChannelSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailChannelSender.class);

    /** EMAIL 渠道标识 */
    private static final String CHANNEL_EMAIL = "EMAIL";

    /** 发送状态：成功 */
    private static final int STATUS_SUCCESS = 2;

    private final EmailService emailService;

    /**
     * 构造函数注入 EmailService。
     *
     * @param emailService 邮件发送服务
     */
    public EmailChannelSender(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public String getChannel() {
        return CHANNEL_EMAIL;
    }

    @Override
    public void send(MsgSendRecord record) throws Exception {
        String recipient = record.getRecipient();
        String subject = record.getSubject();
        String content = record.getContent();

        logger.info("EMAIL 渠道发送开始: recipient={}, subject={}, recordId={}",
                recipient, subject, record.getRecordId());

        try {
            emailService.sendEmail(recipient, subject, content);
            record.setSendStatus(STATUS_SUCCESS);
            logger.info("EMAIL 渠道发送成功: recipient={}, recordId={}",
                    recipient, record.getRecordId());
        } catch (Exception e) {
            logger.error("EMAIL 渠道发送失败: recipient={}, recordId={}, error={}",
                    recipient, record.getRecordId(), e.getMessage(), e);
            throw e;
        }
    }
}
