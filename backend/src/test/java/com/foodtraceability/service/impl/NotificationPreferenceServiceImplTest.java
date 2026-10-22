package com.foodtraceability.service.impl;

import com.foodtraceability.entity.NotificationUserPreference;
import com.foodtraceability.mapper.NotificationUserPreferenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationPreferenceServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：查询委托、保存的「存在则更新 / 不存在则插入」双分支、并发唯一键冲突的
 * DuplicateKeyException 回退（能查到则更新、查不到则重抛）、通知默认开启语义、
 * 初始化默认偏好（16 条、仅当无记录时写入）、批量更新逐条委托。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationPreferenceServiceImplTest {

    @Mock
    private NotificationUserPreferenceMapper preferenceMapper;

    @InjectMocks
    private NotificationPreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        when(preferenceMapper.insert(any(NotificationUserPreference.class))).thenReturn(1);
        when(preferenceMapper.updateById(any(NotificationUserPreference.class))).thenReturn(1);
        when(preferenceMapper.selectByUserId(anyLong())).thenReturn(new ArrayList<>());
    }

    private static NotificationUserPreference pref(int enabled, String frequency) {
        NotificationUserPreference p = new NotificationUserPreference();
        p.setUserId(1L);
        p.setNotificationType("TASK_ASSIGN");
        p.setChannel("EMAIL");
        p.setIsEnabled(enabled);
        p.setFrequency(frequency);
        return p;
    }

    @Test
    @DisplayName("getUserPreferences / getPreference：直接委托 mapper")
    void queries_delegate() {
        List<NotificationUserPreference> rows = List.of(pref(1, "REALTIME"));
        when(preferenceMapper.selectByUserId(1L)).thenReturn(rows);
        assertSame(rows, service.getUserPreferences(1L));

        NotificationUserPreference one = pref(0, "DAILY");
        when(preferenceMapper.selectByUserAndTypeAndChannel(1L, "TASK_ASSIGN", "EMAIL")).thenReturn(one);
        assertSame(one, service.getPreference(1L, "TASK_ASSIGN", "EMAIL"));
    }

    // ==================== 保存/更新 ====================

    @Test
    @DisplayName("saveOrUpdatePreference：已存在则更新启用状态，frequency 为空保留原值")
    void save_existing_updatesWithoutInsert() {
        NotificationUserPreference existing = pref(1, "DAILY");
        existing.setUserId(1L);
        when(preferenceMapper.selectByUserAndTypeAndChannel(1L, "TASK_ASSIGN", "EMAIL")).thenReturn(existing);

        NotificationUserPreference result = service.saveOrUpdatePreference(1L, "TASK_ASSIGN", "EMAIL", 0, null);

        assertSame(existing, result);
        assertEquals(0, result.getIsEnabled());
        assertEquals("DAILY", result.getFrequency(), "frequency 为 null 时不得清空");
        assertNotNull(result.getUpdateTime());
        verify(preferenceMapper).updateById(existing);
        verify(preferenceMapper, never()).insert(any(NotificationUserPreference.class));
    }

    @Test
    @DisplayName("saveOrUpdatePreference：不存在则插入，默认频率 REALTIME")
    void save_new_insertsWithDefaultFrequency() {
        when(preferenceMapper.selectByUserAndTypeAndChannel(anyLong(), anyString(), anyString())).thenReturn(null);

        NotificationUserPreference result = service.saveOrUpdatePreference(2L, "ORDER_STATUS", "SYSTEM", 1, null);

        assertEquals(2L, result.getUserId());
        assertEquals("ORDER_STATUS", result.getNotificationType());
        assertEquals("SYSTEM", result.getChannel());
        assertEquals(1, result.getIsEnabled());
        assertEquals("REALTIME", result.getFrequency());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        verify(preferenceMapper).insert(result);
    }

    @Test
    @DisplayName("saveOrUpdatePreference：显式 frequency 优先于默认值")
    void save_new_explicitFrequency() {
        when(preferenceMapper.selectByUserAndTypeAndChannel(anyLong(), anyString(), anyString())).thenReturn(null);

        assertEquals("WEEKLY", service.saveOrUpdatePreference(2L, "TASK_ASSIGN", "EMAIL", 1, "WEEKLY").getFrequency());
    }

    @Test
    @DisplayName("saveOrUpdatePreference：并发唯一键冲突时回退为更新")
    void save_duplicateKey_fallsBackToUpdate() {
        when(preferenceMapper.selectByUserAndTypeAndChannel(1L, "TASK_ASSIGN", "EMAIL"))
                .thenReturn(null)          // 首次查询：不存在
                .thenReturn(pref(1, "REALTIME")); // 冲突后重查：已存在
        when(preferenceMapper.insert(any(NotificationUserPreference.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        NotificationUserPreference result = service.saveOrUpdatePreference(1L, "TASK_ASSIGN", "EMAIL", 0, "DAILY");

        assertEquals(0, result.getIsEnabled());
        assertEquals("DAILY", result.getFrequency());
        verify(preferenceMapper).updateById(result);
    }

    @Test
    @DisplayName("saveOrUpdatePreference：冲突后仍查不到则原样重抛（不静默吞掉）")
    void save_duplicateKeyWithoutRecord_rethrows() {
        when(preferenceMapper.selectByUserAndTypeAndChannel(anyLong(), anyString(), anyString())).thenReturn(null);
        when(preferenceMapper.insert(any(NotificationUserPreference.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThrows(DuplicateKeyException.class,
                () -> service.saveOrUpdatePreference(1L, "TASK_ASSIGN", "EMAIL", 1, null));
    }

    // ==================== 开关语义 ====================

    @Test
    @DisplayName("isNotificationEnabled：无偏好默认开启；enabled=1 开；enabled=0 关；enabled=null 关")
    void isNotificationEnabled_semantics() {
        when(preferenceMapper.selectByUserAndTypeAndChannel(anyLong(), anyString(), anyString())).thenReturn(null);
        assertTrue(service.isNotificationEnabled(1L, "TASK_ASSIGN", "EMAIL"), "未配置视为默认开启");

        when(preferenceMapper.selectByUserAndTypeAndChannel(anyLong(), anyString(), anyString())).thenReturn(pref(1, null));
        assertTrue(service.isNotificationEnabled(1L, "TASK_ASSIGN", "EMAIL"));

        when(preferenceMapper.selectByUserAndTypeAndChannel(anyLong(), anyString(), anyString())).thenReturn(pref(0, null));
        assertFalse(service.isNotificationEnabled(1L, "TASK_ASSIGN", "EMAIL"));

        NotificationUserPreference nullEnabled = pref(1, null);
        nullEnabled.setIsEnabled(null);
        when(preferenceMapper.selectByUserAndTypeAndChannel(anyLong(), anyString(), anyString())).thenReturn(nullEnabled);
        assertFalse(service.isNotificationEnabled(1L, "TASK_ASSIGN", "EMAIL"), "is_enabled 为 null 视为关闭");
    }

    // ==================== 初始化 / 批量 ====================

    @Test
    @DisplayName("initUserPreferences：已有记录时不重复初始化")
    void init_existing_doesNothing() {
        when(preferenceMapper.selectByUserId(1L)).thenReturn(List.of(pref(1, "REALTIME")));

        service.initUserPreferences(1L);

        verify(preferenceMapper, never()).insert(any(NotificationUserPreference.class));
    }

    @Test
    @DisplayName("initUserPreferences：无记录时写入 16 条默认偏好（全部启用 + REALTIME）")
    void init_empty_writesDefaults() {
        service.initUserPreferences(9L);

        ArgumentCaptor<NotificationUserPreference> captor = ArgumentCaptor.forClass(NotificationUserPreference.class);
        verify(preferenceMapper, times(16)).insert(captor.capture());
        List<NotificationUserPreference> all = captor.getAllValues();
        assertTrue(all.stream().allMatch(p -> p.getUserId().equals(9L)));
        assertTrue(all.stream().allMatch(p -> Integer.valueOf(1).equals(p.getIsEnabled())));
        assertTrue(all.stream().allMatch(p -> "REALTIME".equals(p.getFrequency())));
        assertTrue(all.stream().anyMatch(p -> "ORDER_STATUS".equals(p.getNotificationType())));
        assertTrue(all.stream().allMatch(p -> p.getCreateTime() != null && p.getUpdateTime() != null));
    }

    @Test
    @DisplayName("batchUpdatePreferences：逐条保存并返回更新后的列表")
    void batchUpdate_loopsThrough() {
        when(preferenceMapper.selectByUserAndTypeAndChannel(anyLong(), anyString(), anyString())).thenReturn(null);

        List<NotificationUserPreference> input = List.of(pref(0, "DAILY"), pref(1, "REALTIME"));
        input.get(0).setNotificationType("A");
        input.get(1).setNotificationType("B");

        List<NotificationUserPreference> result = service.batchUpdatePreferences(3L, input);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getUserId().equals(3L)));
        verify(preferenceMapper, times(2)).insert(any(NotificationUserPreference.class));
    }
}
