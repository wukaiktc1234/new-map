package com.foodtraceability.service.sender;

import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * EmailChannelSender 单元测试
 * 对应 spec F-002 验收标准
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EMAIL 渠道发送器单元测试")
class EmailChannelSenderTest {

    @Mock
    private EmailService emailService;

    private EmailChannelSender sender;

    @BeforeEach
    void setUp() {
        sender = new EmailChannelSender(emailService);
    }

    @Test
    @DisplayName("getChannel 返回 EMAIL")
    void testGetChannel() {
        assertEquals("EMAIL", sender.getChannel());
    }

    @Test
    @DisplayName("发送成功：状态更新为 SUCCESS(2)")
    void testSendSuccess() throws Exception {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        record.setRecipient("user@example.com");
        record.setSubject("测试主题");
        record.setContent("测试内容");

        sender.send(record);

        verify(emailService, times(1)).sendEmail("user@example.com", "测试主题", "测试内容");
        assertEquals(2, record.getSendStatus()); // SUCCESS
    }

    @Test
    @DisplayName("发送失败：抛出异常，状态未更新为 SUCCESS")
    void testSendFailureThrowsException() throws Exception {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        record.setRecipient("user@example.com");
        record.setSubject("测试主题");
        record.setContent("测试内容");

        doThrow(new RuntimeException("SMTP 连接失败"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        Exception ex = assertThrows(RuntimeException.class, () -> sender.send(record));
        assertEquals("SMTP 连接失败", ex.getMessage());
        assertNotEquals(2, record.getSendStatus());
    }
}
