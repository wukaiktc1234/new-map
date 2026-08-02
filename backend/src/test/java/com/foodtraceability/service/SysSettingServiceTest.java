package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.SysSetting;
import com.foodtraceability.mapper.SysSettingMapper;
import com.foodtraceability.service.impl.SysSettingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SysSettingServiceTest {

    @Mock
    private SysSettingMapper sysSettingMapper;

    @InjectMocks
    private SysSettingServiceImpl sysSettingService;

    private SysSetting testSetting;

    @BeforeEach
    void setUp() {
        testSetting = new SysSetting();
        testSetting.setId(1L);
        testSetting.setSettingKey("test_key");
        testSetting.setSettingValue("test_value");
        testSetting.setSettingGroup("basic");
        testSetting.setValueType("string");
        testSetting.setSettingName("测试设置");
        testSetting.setScopeType("global");
        testSetting.setIsEnabled(1);
        testSetting.setIsSystem(0);
        testSetting.setSortOrder(0);
        testSetting.setCreateTime(LocalDateTime.now());
        testSetting.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void testGetSettingsByGroup() {
        List<SysSetting> expectedSettings = Arrays.asList(testSetting);
        when(sysSettingMapper.selectByGroup("basic")).thenReturn(expectedSettings);

        var result = sysSettingService.getSettingsByGroup("basic");

        assertTrue(result.isSuccess());
        assertEquals(expectedSettings, result.getData());
        verify(sysSettingMapper, times(1)).selectByGroup("basic");
    }

    @Test
    void testGetSettingValue() {
        when(sysSettingMapper.selectGlobalSetting("test_key")).thenReturn(testSetting);

        var result = sysSettingService.getSettingValue("test_key", null, null);

        assertTrue(result.isSuccess());
        assertEquals("test_value", result.getData());
        verify(sysSettingMapper, times(1)).selectGlobalSetting("test_key");
    }

    @Test
    void testSetGlobalSetting() {
        when(sysSettingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(sysSettingMapper.insert(any(SysSetting.class))).thenReturn(1);

        var result = sysSettingService.setGlobalSetting("test_key", "new_value", "test_user");

        assertTrue(result.isSuccess());
        verify(sysSettingMapper, times(1)).insert(any(SysSetting.class));
    }

    @Test
    void testInitDefaultSettings() {
        when(sysSettingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysSettingMapper.insert(any(SysSetting.class))).thenReturn(1);

        var result = sysSettingService.initDefaultSettings();

        assertTrue(result.isSuccess());
        verify(sysSettingMapper, atLeastOnce()).insert(any(SysSetting.class));
    }
}
