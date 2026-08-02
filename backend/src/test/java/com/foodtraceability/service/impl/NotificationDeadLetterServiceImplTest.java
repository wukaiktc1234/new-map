package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.foodtraceability.dto.DeadLetterQueryDTO;
import com.foodtraceability.dto.DeadLetterVO;
import com.foodtraceability.entity.NotificationDeadLetter;
import com.foodtraceability.entity.User;
import com.foodtraceability.service.NotificationService;
import com.foodtraceability.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationDeadLetterServiceImpl 单元测试
 * 对应 spec NC-006 验收标准（死信队列处理）
 *
 * <p>注：由于 ServiceImpl 父类方法难以 mock，本测试聚焦于
 * VO 转换逻辑和依赖调用验证。完整集成测试见 T-017。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("死信管理服务单元测试")
class NotificationDeadLetterServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    private NotificationDeadLetterServiceImpl deadLetterService;

    @BeforeEach
    void setUp() {
        deadLetterService = spy(new NotificationDeadLetterServiceImpl(
                userService, notificationService));
    }

    @Test
    @DisplayName("查询死信详情：返回 VO 并关联查询处理人用户名")
    void testGetDeadLetterByIdReturnsVoWithResolvedByName() {
        NotificationDeadLetter entity = new NotificationDeadLetter();
        entity.setDeadLetterId(1L);
        entity.setOriginalRecordId(100L);
        entity.setChannel("EMAIL");
        entity.setRecipient("user@example.com");
        entity.setSubject("测试主题");
        entity.setResolved(1);
        entity.setResolvedBy(200L);
        entity.setResolveRemark("已处理");
        entity.setDeadLetterTime(LocalDateTime.now());

        User operator = new User();
        operator.setUsername("管理员");

        doReturn(entity).when(deadLetterService).getById(1L);
        when(userService.getById(200L)).thenReturn(operator);

        DeadLetterVO vo = deadLetterService.getDeadLetterById(1L);

        assertNotNull(vo);
        assertEquals(1L, vo.getDeadLetterId());
        assertEquals(100L, vo.getOriginalRecordId());
        assertEquals("EMAIL", vo.getChannel());
        assertEquals(1, vo.getResolved());
        assertEquals(200L, vo.getResolvedBy());
        assertEquals("管理员", vo.getResolvedByUsername());
        assertEquals("已处理", vo.getResolveRemark());
    }

    @Test
    @DisplayName("查询死信详情：记录不存在时返回 null")
    void testGetDeadLetterByIdNotFound() {
        doReturn(null).when(deadLetterService).getById(999L);

        DeadLetterVO vo = deadLetterService.getDeadLetterById(999L);

        assertNull(vo);
    }

    @Test
    @DisplayName("查询死信详情：resolvedBy 为 null 时不查询用户名")
    void testGetDeadLetterByIdNullResolvedBy() {
        NotificationDeadLetter entity = new NotificationDeadLetter();
        entity.setDeadLetterId(1L);
        entity.setResolved(0);
        entity.setResolvedBy(null);

        doReturn(entity).when(deadLetterService).getById(1L);

        DeadLetterVO vo = deadLetterService.getDeadLetterById(1L);

        assertNotNull(vo);
        assertNull(vo.getResolvedBy());
        assertNull(vo.getResolvedByUsername());
        verify(userService, never()).getById(anyLong());
    }

    @Test
    @DisplayName("标记死信已处理：更新 resolved/resolvedBy/resolvedTime/resolveRemark")
    void testResolveDeadLetterUpdatesFields() {
        NotificationDeadLetter entity = new NotificationDeadLetter();
        entity.setDeadLetterId(1L);
        entity.setResolved(0);

        doReturn(entity).when(deadLetterService).getById(1L);
        doReturn(true).when(deadLetterService).updateById(any(NotificationDeadLetter.class));

        boolean success = deadLetterService.resolveDeadLetter(1L, 200L, "已人工处理");

        assertTrue(success);
        assertEquals(1, entity.getResolved());
        assertEquals(200L, entity.getResolvedBy());
        assertEquals("已人工处理", entity.getResolveRemark());
        assertNotNull(entity.getResolvedTime());
    }

    @Test
    @DisplayName("标记死信已处理：记录不存在时返回 false")
    void testResolveDeadLetterNotFound() {
        doReturn(null).when(deadLetterService).getById(999L);

        boolean success = deadLetterService.resolveDeadLetter(999L, 200L, "备注");

        assertFalse(success);
    }

    @Test
    @DisplayName("重试死信：CAS 占位 + 调用 NotificationService.retryMessage + 更新 RETRY_SUCCESS")
    void testRetryDeadLetterCallsNotificationService() {
        NotificationDeadLetter entity = new NotificationDeadLetter();
        entity.setDeadLetterId(1L);
        entity.setOriginalRecordId(100L);

        User operator = new User();
        operator.setId(200L);
        operator.setUsername("管理员");

        // FIX-007: CAS 占位 update 返回 true
        doReturn(true).when(deadLetterService)
                .update(any(NotificationDeadLetter.class), any(Wrapper.class));
        // CAS 占位后再次查询死信记录
        doReturn(entity).when(deadLetterService).getById(1L);
        when(userService.getById(200L)).thenReturn(operator);
        when(notificationService.retryMessage(100L, 200L, "管理员")).thenReturn(true);

        boolean success = deadLetterService.retryDeadLetter(1L, 200L);

        assertTrue(success);
        verify(notificationService, times(1)).retryMessage(100L, 200L, "管理员");
    }

    @Test
    @DisplayName("重试死信：CAS 占位成功但 originalRecordId 为 null 时回滚占位并返回 false")
    void testRetryDeadLetterNullOriginalRecordId() {
        NotificationDeadLetter entity = new NotificationDeadLetter();
        entity.setDeadLetterId(1L);
        entity.setOriginalRecordId(null);

        // FIX-007: CAS 占位成功
        doReturn(true).when(deadLetterService)
                .update(any(NotificationDeadLetter.class), any(Wrapper.class));
        // 但 entity 的 originalRecordId 为 null
        doReturn(entity).when(deadLetterService).getById(1L);

        boolean success = deadLetterService.retryDeadLetter(1L, 200L);

        assertFalse(success);
        verify(notificationService, never()).retryMessage(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("查询参数：DeadLetterQueryDTO 默认值正确")
    void testDeadLetterQueryDTODefaultValues() {
        DeadLetterQueryDTO dto = new DeadLetterQueryDTO();

        assertEquals(1, dto.getCurrent());
        assertEquals(20, dto.getSize());
    }
}
