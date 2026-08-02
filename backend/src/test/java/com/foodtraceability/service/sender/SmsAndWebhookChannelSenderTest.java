package com.foodtraceability.service.sender;

import com.foodtraceability.entity.MsgSendRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SmsChannelSender / WebhookChannelSender 单元测试
 * 对应 spec F-004 验收标准（SMS/WEBHOOK 跳过策略）
 */
@DisplayName("SMS / WEBHOOK 渠道发送器单元测试")
class SmsAndWebhookChannelSenderTest {

    private SmsChannelSender smsSender;
    private WebhookChannelSender webhookSender;

    @BeforeEach
    void setUp() {
        smsSender = new SmsChannelSender();
        webhookSender = new WebhookChannelSender();
    }

    @Test
    @DisplayName("SmsChannelSender getChannel 返回 SMS")
    void testSmsGetChannel() {
        assertEquals("SMS", smsSender.getChannel());
    }

    @Test
    @DisplayName("WebhookChannelSender getChannel 返回 WEBHOOK")
    void testWebhookGetChannel() {
        assertEquals("WEBHOOK", webhookSender.getChannel());
    }

    @Test
    @DisplayName("SMS 渠道跳过：status=SKIPPED(5)，不抛异常")
    void testSmsSkippedWithoutException() throws Exception {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        record.setRecipient("13800138000");

        smsSender.send(record);

        assertEquals(5, record.getSendStatus()); // SKIPPED
    }

    @Test
    @DisplayName("WEBHOOK 渠道跳过：status=SKIPPED(5)，不抛异常")
    void testWebhookSkippedWithoutException() throws Exception {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        record.setRecipient("https://example.com/webhook");

        webhookSender.send(record);

        assertEquals(5, record.getSendStatus()); // SKIPPED
    }
}
