package com.foodtraceability.service;

import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.entity.MsgTemplate;
import com.foodtraceability.entity.Notification;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.MsgSendRecordMapper;
import com.foodtraceability.service.event.BusinessEvent;
import com.foodtraceability.service.event.TestBusinessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationEventBus 单元测试
 *
 * <p>测试核心逻辑：模板渲染、接收人校验、渠道分发。
 * 对应 spec F-001 / F-003 / F-005 验收标准。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知事件总线单元测试")
class NotificationEventBusTest {

    @Mock
    private SiteNotificationService siteNotificationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private MsgSendRecordMapper msgSendRecordMapper;

    private NotificationEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new NotificationEventBus(
                siteNotificationService, notificationService,
                userService, msgSendRecordMapper);
    }

    @Test
    @DisplayName("模板渲染：{varName} 占位符替换")
    void testRenderTemplate() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("eventName", "测试事件");
        variables.put("operatorName", "张三");

        String template = "事件：{eventName}，操作人：{operatorName}";
        String result = eventBus.renderTemplate(template, variables);

        assertEquals("事件：测试事件，操作人：张三", result);
    }

    @Test
    @DisplayName("模板渲染：缺失变量保留原占位符")
    void testRenderTemplateMissingVariable() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("eventName", "测试事件");

        String template = "事件：{eventName}，操作人：{operatorName}";
        String result = eventBus.renderTemplate(template, variables);

        assertEquals("事件：测试事件，操作人：{operatorName}", result);
    }

    @Test
    @DisplayName("模板渲染：空变量返回原模板")
    void testRenderTemplateEmptyVariables() {
        String template = "事件：{eventName}";
        String result = eventBus.renderTemplate(template, new HashMap<>());

        assertEquals(template, result);
    }

    @Test
    @DisplayName("模板渲染：null 模板返回 null")
    void testRenderTemplateNull() {
        String result = eventBus.renderTemplate(null, new HashMap<>());
        assertNull(result);
    }

    @Test
    @DisplayName("接收人校验：过滤不存在的用户")
    void testValidateRecipientsFiltersInvalidUsers() {
        // FIX-018: 改为批量查询 listByIds
        User user1 = new User();
        user1.setId(1L);
        User user3 = new User();
        user3.setId(3L);
        when(userService.listByIds(List.of(1L, 2L, 3L)))
                .thenReturn(java.util.Arrays.asList(user1, user3));

        List<Long> result = eventBus.validateRecipients(List.of(1L, 2L, 3L));

        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(3L));
        assertFalse(result.contains(2L));
    }

    @Test
    @DisplayName("接收人校验：查询异常时返回空列表")
    void testValidateRecipientsSkipsOnException() {
        // FIX-018: 批量查询抛异常时降级为空列表
        when(userService.listByIds(anyList())).thenThrow(new RuntimeException("DB error"));

        List<Long> result = eventBus.validateRecipients(List.of(1L, 2L));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("接收人校验：空列表返回空列表")
    void testValidateRecipientsEmptyList() {
        List<Long> result = eventBus.validateRecipients(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("事件处理：模板不存在时跳过分发")
    void testHandleBusinessEventTemplateNotFound() {
        BusinessEvent event = new TestBusinessEvent(
                this, 1L, new HashMap<>(), List.of(1L), List.of("SITE_MSG"));

        when(notificationService.getTemplateByCode("sprint1.test.event")).thenReturn(null);

        eventBus.handleBusinessEvent(event);

        verify(siteNotificationService, never()).createNotification(
                anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("事件处理：模板未启用时跳过分发")
    void testHandleBusinessEventTemplateDisabled() {
        MsgTemplate template = new MsgTemplate();
        template.setStatus(0); // DISABLED

        BusinessEvent event = new TestBusinessEvent(
                this, 1L, new HashMap<>(), List.of(1L), List.of("SITE_MSG"));

        when(notificationService.getTemplateByCode("sprint1.test.event")).thenReturn(template);

        eventBus.handleBusinessEvent(event);

        verify(siteNotificationService, never()).createNotification(
                anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("事件处理：无有效接收人时跳过分发")
    void testHandleBusinessEventNoValidRecipients() {
        MsgTemplate template = new MsgTemplate();
        template.setStatus(1); // ENABLED
        template.setTemplateCode("sprint1.test.event");

        BusinessEvent event = new TestBusinessEvent(
                this, 1L, new HashMap<>(), List.of(999L), List.of("SITE_MSG"));

        when(notificationService.getTemplateByCode("sprint1.test.event")).thenReturn(template);
        // FIX-018: 批量查询返回空列表(999 不存在)
        when(userService.listByIds(List.of(999L))).thenReturn(java.util.Collections.emptyList());

        eventBus.handleBusinessEvent(event);

        verify(siteNotificationService, never()).createNotification(
                anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("事件处理：SITE_MSG 渠道成功分发并写 msg_send_record")
    void testHandleBusinessEventSiteMsgChannelSuccess() {
        MsgTemplate template = new MsgTemplate();
        template.setStatus(1);
        template.setTemplateId(100L);
        template.setTemplateCode("sprint1.test.event");
        template.setTemplateName("测试事件模板");
        template.setSubjectPattern("测试事件：{eventName}");
        template.setContentPattern("操作人：{operatorName}");

        Map<String, Object> variables = new HashMap<>();
        variables.put("eventName", "验收测试");
        variables.put("operatorName", "管理员");

        BusinessEvent event = new TestBusinessEvent(
                this, 1L, variables, List.of(10L), List.of("SITE_MSG"));

        Notification notification = new Notification();
        notification.setId(200L);

        // FIX-018: 批量查询返回 id=10 的 User
        User user10 = new User();
        user10.setId(10L);
        user10.setUsername("操作员10");

        when(notificationService.getTemplateByCode("sprint1.test.event")).thenReturn(template);
        when(userService.listByIds(List.of(10L))).thenReturn(java.util.Collections.singletonList(user10));
        when(siteNotificationService.createNotification(
                eq(10L), eq("测试事件：验收测试"), eq("操作人：管理员"),
                anyString(), anyString(), any(), anyString(), eq(1L), any()))
                .thenReturn(notification);

        eventBus.handleBusinessEvent(event);

        verify(siteNotificationService, times(1)).createNotification(
                eq(10L), eq("测试事件：验收测试"), eq("操作人：管理员"),
                anyString(), anyString(), any(), anyString(), eq(1L), any());
        verify(msgSendRecordMapper, times(1)).insert(any(MsgSendRecord.class));
    }
}
