package com.foodtraceability.service.impl;

import com.foodtraceability.entity.report.ReportConfig;
import com.foodtraceability.mapper.ReportConfigMapper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ReportConfigServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：配置读取的本地缓存（首次查库、二次命中、返回副本不污染缓存、按报表类型隔离）、
 * 保存的「存在则更新 / 不存在则插入」双分支、保存/批量/删除后的缓存失效、
 * 默认配置的 5 个固定键。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportConfigServiceImplTest {

    @Mock
    private ReportConfigMapper reportConfigMapper;

    @InjectMocks
    private ReportConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        when(reportConfigMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(reportConfigMapper.insert(any(ReportConfig.class))).thenReturn(1);
        when(reportConfigMapper.updateById(any(ReportConfig.class))).thenReturn(1);
        when(reportConfigMapper.delete(any())).thenReturn(1);
    }

    private static ReportConfig config(String key, String value) {
        ReportConfig c = new ReportConfig();
        c.setConfigId(1L);
        c.setUserId(1L);
        c.setReportType(1);
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    // ==================== 读取与缓存 ====================

    @Test
    @DisplayName("getUserConfig：首次查库并映射 key/value，第二次命中缓存不再查库")
    void getUserConfig_cachesAfterFirstRead() {
        when(reportConfigMapper.selectList(any())).thenReturn(List.of(
                config("date_range", "last_7_days"),
                config("channel", "online")
        ));

        Map<String, String> first = service.getUserConfig(1L, 1);
        Map<String, String> second = service.getUserConfig(1L, 1);

        assertEquals(2, first.size());
        assertEquals("last_7_days", first.get("date_range"));
        assertEquals("online", second.get("channel"));
        verify(reportConfigMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("getUserConfig：返回副本，外部修改不污染缓存")
    void getUserConfig_returnsDefensiveCopy() {
        when(reportConfigMapper.selectList(any())).thenReturn(List.of(config("k", "v")));

        Map<String, String> first = service.getUserConfig(1L, 1);
        first.put("k", "hacked");

        Map<String, String> second = service.getUserConfig(1L, 1);
        assertEquals("v", second.get("k"));
        assertNotSame(first, second);
    }

    @Test
    @DisplayName("getUserConfig：按 reportType 隔离缓存")
    void getUserConfig_isolatesByReportType() {
        when(reportConfigMapper.selectList(any()))
                .thenReturn(List.of(config("k", "type1")))
                .thenReturn(List.of(config("k", "type2")));

        assertEquals("type1", service.getUserConfig(1L, 1).get("k"));
        assertEquals("type2", service.getUserConfig(1L, 2).get("k"));
        verify(reportConfigMapper, times(2)).selectList(any());
    }

    // ==================== 保存 / 删除 ====================

    @Test
    @DisplayName("saveConfig：已存在则更新值，不新增")
    void saveConfig_existing_updates() {
        ReportConfig existing = config("date_range", "last_30_days");
        when(reportConfigMapper.selectOne(any())).thenReturn(existing);

        service.saveConfig(1L, 1, "date_range", "last_7_days");

        assertEquals("last_7_days", existing.getConfigValue());
        verify(reportConfigMapper).updateById(existing);
        verify(reportConfigMapper, never()).insert(any(ReportConfig.class));
    }

    @Test
    @DisplayName("saveConfig：不存在则新增并带上 userId/reportType/key/value")
    void saveConfig_missing_inserts() {
        when(reportConfigMapper.selectOne(any())).thenReturn(null);

        service.saveConfig(2L, 3, "channel", "offline");

        ArgumentCaptor<ReportConfig> captor = ArgumentCaptor.forClass(ReportConfig.class);
        verify(reportConfigMapper).insert(captor.capture());
        ReportConfig saved = captor.getValue();
        assertEquals(2L, saved.getUserId());
        assertEquals(3, saved.getReportType());
        assertEquals("channel", saved.getConfigKey());
        assertEquals("offline", saved.getConfigValue());
    }

    @Test
    @DisplayName("saveConfig：保存后缓存失效，下次读取重新查库")
    void saveConfig_invalidatesCache() {
        when(reportConfigMapper.selectList(any()))
                .thenReturn(List.of(config("k", "old")))
                .thenReturn(List.of(config("k", "new")));
        when(reportConfigMapper.selectOne(any())).thenReturn(null);

        assertEquals("old", service.getUserConfig(1L, 1).get("k"));
        service.saveConfig(1L, 1, "k", "new");
        assertEquals("new", service.getUserConfig(1L, 1).get("k"));
        verify(reportConfigMapper, times(2)).selectList(any());
    }

    @Test
    @DisplayName("batchSaveConfig：逐条保存（每条都走一次存在性查询）")
    void batchSaveConfig_savesEachEntry() {
        when(reportConfigMapper.selectOne(any())).thenReturn(null);
        Map<String, String> configs = new HashMap<>();
        configs.put("date_range", "last_7_days");
        configs.put("channel", "all");

        service.batchSaveConfig(1L, 1, configs);

        verify(reportConfigMapper, times(2)).insert(any(ReportConfig.class));
    }

    @Test
    @DisplayName("deleteConfig：按条件删除并清缓存")
    void deleteConfig_removesByCondition() {
        when(reportConfigMapper.selectList(any())).thenReturn(List.of(config("k", "v")));
        service.getUserConfig(1L, 1);

        service.deleteConfig(1L, 1, "k");

        verify(reportConfigMapper).delete(any());
        service.getUserConfig(1L, 1);
        verify(reportConfigMapper, times(2)).selectList(any());
    }

    @Test
    @DisplayName("getDefaultConfig：固定 5 个默认键")
    void getDefaultConfig_hasFixedKeys() {
        Map<String, String> defaults = service.getDefaultConfig(1);

        assertEquals(5, defaults.size());
        assertEquals("last_30_days", defaults.get("date_range"));
        assertEquals("all", defaults.get("store_ids"));
        assertEquals("all", defaults.get("channel"));
        assertEquals("yoy", defaults.get("compare_type"));
        assertTrue(defaults.get("kpi_order").contains("revenue"));
    }
}
