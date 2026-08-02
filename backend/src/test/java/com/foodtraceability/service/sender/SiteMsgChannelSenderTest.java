package com.foodtraceability.service.sender;

import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.entity.Notification;
import com.foodtraceability.service.SiteNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SiteMsgChannelSender 单元测试
 * 对应 spec F-003 验收标准（SITE_MSG 委托 SiteNotificationService）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SITE_MSG 渠道发送器单元测试")
class SiteMsgChannelSenderTest {

    @Mock
    private SiteNotificationService siteNotificationService;

    private SiteMsgChannelSender sender;

    @BeforeEach
    void setUp() {
        sender = new SiteMsgChannelSender(siteNotificationService);
    }

    @Test
    @DisplayName("getChannel 返回 SITE_MSG")
    void testGetChannel() {
        assertEquals("SITE_MSG", sender.getChannel());
    }

    @Test
    @DisplayName("发送成功：回填 notificationId 并更新状态为 SUCCESS")
    void testSendSuccessBackfillsNotificationId() throws Exception {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        record.setRecipient("100");
        record.setSubject("测试主题");
        record.setContent("测试内容");
        record.setCreateUserId(200L);
        record.setCreateUsername("管理员");

        Notification notification = new Notification();
        notification.setId(999L);

        // businessType 可能为 null,使用 any() 而非 anyString()
        when(siteNotificationService.createNotification(
                eq(100L), eq("测试主题"), eq("测试内容"),
                anyString(), anyString(), any(), any(),
                eq(200L), eq("管理员")))
                .thenReturn(notification);

        sender.send(record);

        assertEquals(999L, record.getNotificationId());
        assertEquals(2, record.getSendStatus()); // SUCCESS
    }

    @Test
    @DisplayName("发送失败：recipient 非 userId 格式时抛出 IllegalArgumentException")
    void testSendInvalidRecipientFormat() {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        record.setRecipient("not-a-number");

        assertThrows(IllegalArgumentException.class, () -> sender.send(record));
    }

    @Test
    @DisplayName("发送失败：FIX-004 recipient 为 null 时抛出 IllegalArgumentException")
    void testSendNullRecipient() {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        // recipient 为 null
        record.setRecipient(null);

        assertThrows(IllegalArgumentException.class, () -> sender.send(record));
    }

    @Test
    @DisplayName("发送失败：SiteNotificationService 返回 null 时抛出 IllegalStateException")
    void testSendNullNotificationReturned() {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        record.setRecipient("100");
        record.setSubject("测试");
        record.setContent("内容");

        // businessType/senderName 等可能为 null,统一使用 any()
        when(siteNotificationService.createNotification(
                anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(), any()))
                .thenReturn(null);

        assertThrows(IllegalStateException.class, () -> sender.send(record));
    }

    @Test
    @DisplayName("bizId 非数字时 businessId 为 null，不影响发送")
    void testSendNonNumericBizId() throws Exception {
        MsgSendRecord record = new MsgSendRecord();
        record.setRecordId(1L);
        record.setRecipient("100");
        record.setSubject("测试");
        record.setContent("内容");
        record.setBizId("non-numeric-id");

        Notification notification = new Notification();
        notification.setId(999L);

        // businessType/senderId/senderName 可能为 null,使用 any()/isNull()
        when(siteNotificationService.createNotification(
                eq(100L), anyString(), anyString(), anyString(), anyString(),
                isNull(), any(), any(), any()))
                .thenReturn(notification);

        sender.send(record);

        assertEquals(999L, record.getNotificationId());
        assertEquals(2, record.getSendStatus());
    }
}
