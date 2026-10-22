package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.foodtraceability.dto.InventorySettingQueryDTO;
import com.foodtraceability.dto.InventorySettingUpdateDTO;
import com.foodtraceability.entity.InventorySetting;
import com.foodtraceability.mapper.InventorySettingMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventorySettingServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：设置的读取与类型转换（boolean / integer / json / string）、按键查询、
 * 列表筛选条件、更新时的「不存在则创建 / 存在则更新」双分支、类型推断、
 * 以及批量更新的循环写入与回读。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventorySettingServiceImplTest {

    @Mock
    private InventorySettingMapper inventorySettingMapper;

    @InjectMocks
    private InventorySettingServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, InventorySetting.class);
    }

    @BeforeEach
    void setUp() {
        // ServiceImpl.list() 走父类 baseMapper，@InjectMocks 只注入了构造参数，需手动补
        ReflectionTestUtils.setField(service, "baseMapper", inventorySettingMapper);
        when(inventorySettingMapper.insert(any(InventorySetting.class))).thenReturn(1);
        when(inventorySettingMapper.updateById(any(InventorySetting.class))).thenReturn(1);
    }

    private static InventorySetting setting(String key, String value, String type) {
        InventorySetting s = new InventorySetting();
        s.setSettingKey(key);
        s.setSettingValue(value);
        s.setSettingType(type);
        return s;
    }

    private static InventorySettingUpdateDTO updateDTO(String key, String value, String description) {
        InventorySettingUpdateDTO dto = new InventorySettingUpdateDTO();
        dto.setSettingKey(key);
        dto.setSettingValue(value);
        dto.setDescription(description);
        return dto;
    }

    // ==================== 读取与类型转换 ====================

    @Test
    @DisplayName("getAllSettingsAsMap：按实体类型转换值，且保持插入顺序")
    void getAllSettingsAsMap_convertsByDeclaredTypeAndKeepsOrder() {
        when(inventorySettingMapper.selectList(any())).thenReturn(List.of(
                setting("stock.warning.enabled", "true", "boolean"),
                setting("stock.warning.threshold", "10", "integer"),
                setting("stock.alert.config", "{\"level\":1}", "json"),
                setting("stock.note", "人工备注", "string")
        ));

        Map<String, Object> result = service.getAllSettingsAsMap();

        assertEquals(4, result.size());
        assertEquals(List.of("stock.warning.enabled", "stock.warning.threshold",
                        "stock.alert.config", "stock.note"),
                new ArrayList<>(result.keySet()), "应保持数据库返回顺序（LinkedHashMap）");
        assertEquals(Boolean.TRUE, result.get("stock.warning.enabled"));
        assertEquals(10L, result.get("stock.warning.threshold"));
        assertEquals("{\"level\":1}", result.get("stock.alert.config"));
        assertEquals("人工备注", result.get("stock.note"));
    }

    @Test
    @DisplayName("getAllSettingsAsMap：值为 null 时返回 null；integer 类型解析失败回退原始字符串")
    void getAllSettingsAsMap_handlesNullAndBrokenInteger() {
        when(inventorySettingMapper.selectList(any())).thenReturn(List.of(
                setting("stock.null", null, "integer"),
                setting("stock.broken", "not-a-number", "integer")
        ));

        Map<String, Object> result = service.getAllSettingsAsMap();

        assertTrue(result.containsKey("stock.null"));
        assertNull(result.get("stock.null"));
        assertEquals("not-a-number", result.get("stock.broken"), "解析失败必须回退原值而不是抛错");
    }

    @Test
    @DisplayName("getSettingValue：命中返回值，未命中返回 null")
    void getSettingValue_hitAndMiss() {
        when(inventorySettingMapper.selectOne(any())).thenReturn(setting("stock.warning.enabled", "false", "boolean"));
        assertEquals("false", service.getSettingValue("stock.warning.enabled"));

        when(inventorySettingMapper.selectOne(any())).thenReturn(null);
        assertNull(service.getSettingValue("missing.key"));
    }

    @Test
    @DisplayName("getByKey：返回 mapper 查询到的实体")
    void getByKey_returnsEntity() {
        InventorySetting s = setting("k", "v", "string");
        when(inventorySettingMapper.selectOne(any())).thenReturn(s);

        assertSame(s, service.getByKey("k"));
    }

    // ==================== 列表筛选 ====================

    @Test
    @DisplayName("listSettings：queryDTO 为 null 时只按 setting_id 升序，不加筛选")
    void listSettings_nullQuery_noFilters() {
        List<InventorySetting> rows = List.of(setting("a", "1", "integer"));
        when(inventorySettingMapper.selectList(any())).thenReturn(rows);

        assertSame(rows, service.listSettings(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<InventorySetting>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(inventorySettingMapper).selectList(captor.capture());
        LambdaQueryWrapper<InventorySetting> wrapper = (LambdaQueryWrapper<InventorySetting>) captor.getValue();
        assertTrue(wrapper.getParamNameValuePairs().isEmpty(), "无筛选条件时不应有查询参数");
    }

    @Test
    @DisplayName("listSettings：settingKey 模糊 + settingType 精确筛选同时生效")
    void listSettings_appliesKeyLikeAndTypeEq() {
        when(inventorySettingMapper.selectList(any())).thenReturn(List.of());

        InventorySettingQueryDTO query = new InventorySettingQueryDTO();
        query.setSettingKey("warning");
        query.setSettingType("boolean");
        service.listSettings(query);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<InventorySetting>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(inventorySettingMapper).selectList(captor.capture());
        LambdaQueryWrapper<InventorySetting> wrapper = (LambdaQueryWrapper<InventorySetting>) captor.getValue();
        wrapper.getSqlSegment();
        List<Object> values = new ArrayList<>(wrapper.getParamNameValuePairs().values());

        assertTrue(values.contains("%warning%"), "settingKey 应走 LIKE 且两侧带通配符");
        assertTrue(values.contains("boolean"), "settingType 应走等值匹配");
    }

    // ==================== 更新 / 创建 ====================

    @Test
    @DisplayName("updateSetting：键不存在 → 新建，类型推断为 integer，补齐创建/更新时间")
    void updateSetting_missing_createsWithInferredType() {
        when(inventorySettingMapper.selectOne(any())).thenReturn(null);

        InventorySetting result = service.updateSetting(updateDTO("stock.warning.threshold", "42", "预警阈值"));

        ArgumentCaptor<InventorySetting> captor = ArgumentCaptor.forClass(InventorySetting.class);
        verify(inventorySettingMapper).insert(captor.capture());
        InventorySetting saved = captor.getValue();
        assertSame(saved, result);
        assertEquals("stock.warning.threshold", saved.getSettingKey());
        assertEquals("42", saved.getSettingValue());
        assertEquals("integer", saved.getSettingType());
        assertEquals("预警阈值", saved.getDescription());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(inventorySettingMapper, never()).updateById(any(InventorySetting.class));
    }

    @Test
    @DisplayName("updateSetting：键已存在 → 更新，description 为 null 时保留原值")
    void updateSetting_existing_updatesAndKeepsDescriptionWhenNull() {
        InventorySetting existing = setting("stock.warning.enabled", "true", "boolean");
        existing.setDescription("旧描述");
        existing.setSettingId(7L);
        when(inventorySettingMapper.selectOne(any())).thenReturn(existing);

        InventorySetting result = service.updateSetting(updateDTO("stock.warning.enabled", "FALSE", null));

        assertSame(existing, result);
        assertEquals("FALSE", existing.getSettingValue());
        assertEquals("boolean", existing.getSettingType(), "true/false 不区分大小写，应推断为 boolean");
        assertEquals("旧描述", existing.getDescription(), "description 为 null 时不得清空");
        assertNotNull(existing.getUpdateTime());
        verify(inventorySettingMapper).updateById(existing);
        verify(inventorySettingMapper, never()).insert(any(InventorySetting.class));
    }

    @Test
    @DisplayName("updateSetting：已存在且带 description → 覆盖描述")
    void updateSetting_existing_overwritesDescription() {
        InventorySetting existing = setting("k", "v", "string");
        existing.setDescription("旧");
        when(inventorySettingMapper.selectOne(any())).thenReturn(existing);

        service.updateSetting(updateDTO("k", "{}", "新"));

        assertEquals("新", existing.getDescription());
        assertEquals("json", existing.getSettingType(), "以 { 开头的值应推断为 json");
    }

    @Test
    @DisplayName("inferType 边界：数字/布尔/JSON/普通字符串/null → integer/boolean/json/string/string")
    void updateSetting_inferTypeBoundaries() {
        when(inventorySettingMapper.selectOne(any())).thenReturn(null);

        service.updateSetting(updateDTO("k1", "100", null));
        service.updateSetting(updateDTO("k2", "True", null));
        service.updateSetting(updateDTO("k3", "[]", null));
        service.updateSetting(updateDTO("k4", "abc", null));
        service.updateSetting(updateDTO("k5", null, null));

        ArgumentCaptor<InventorySetting> captor = ArgumentCaptor.forClass(InventorySetting.class);
        verify(inventorySettingMapper, times(5)).insert(captor.capture());
        List<InventorySetting> saved = captor.getAllValues();
        assertEquals("integer", saved.get(0).getSettingType());
        assertEquals("boolean", saved.get(1).getSettingType());
        assertEquals("json", saved.get(2).getSettingType());
        assertEquals("string", saved.get(3).getSettingType());
        assertEquals("string", saved.get(4).getSettingType());
    }

    // ==================== 批量更新 ====================

    @Test
    @DisplayName("batchUpdateSettings：逐个转字符串写入后回读全量设置")
    void batchUpdateSettings_writesEachThenReloads() {
        when(inventorySettingMapper.selectOne(any())).thenReturn(null);
        when(inventorySettingMapper.selectList(any())).thenReturn(List.of(
                setting("stock.warning.enabled", "true", "boolean"),
                setting("stock.warning.threshold", "5", "integer")
        ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stock.warning.enabled", true);
        payload.put("stock.warning.threshold", 5);

        Map<String, Object> result = service.batchUpdateSettings(payload);

        ArgumentCaptor<InventorySetting> captor = ArgumentCaptor.forClass(InventorySetting.class);
        verify(inventorySettingMapper, times(2)).insert(captor.capture());
        assertEquals("true", captor.getAllValues().get(0).getSettingValue());
        assertEquals("5", captor.getAllValues().get(1).getSettingValue());
        assertEquals(Boolean.TRUE, result.get("stock.warning.enabled"));
        assertEquals(5L, result.get("stock.warning.threshold"));
        assertFalse(result.isEmpty());
    }
}
