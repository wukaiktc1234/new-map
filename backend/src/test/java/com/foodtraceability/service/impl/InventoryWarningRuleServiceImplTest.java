package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryWarningRule;
import com.foodtraceability.mapper.InventoryWarningRuleMapper;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventoryWarningRuleServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：创建时补齐时间戳、按 ID 查询/更新/删除的委托、分页筛选条件拼装
 * （rule_name 模糊 / warning_type 等值 / is_enabled 布尔转换）、启用规则列表、
 * 以及切换启用状态的空值与布尔映射边界。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryWarningRuleServiceImplTest {

    @Mock
    private InventoryWarningRuleMapper warningRuleMapper;

    @InjectMocks
    private InventoryWarningRuleServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, InventoryWarningRule.class);
    }

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 由父类持有，构造注入不覆盖，需手动补齐
        ReflectionTestUtils.setField(service, "baseMapper", warningRuleMapper);
        when(warningRuleMapper.insert(any(InventoryWarningRule.class))).thenReturn(1);
        when(warningRuleMapper.updateById(any(InventoryWarningRule.class))).thenReturn(1);
        when(warningRuleMapper.deleteById(any(Long.class))).thenReturn(1);
    }

    private static InventoryWarningRule rule(Long id, String name, boolean enabled) {
        InventoryWarningRule r = new InventoryWarningRule();
        r.setRuleId(id);
        r.setRuleName(name);
        r.setWarningType(1);
        r.setThresholdValue(new BigDecimal("10.00"));
        r.setIsEnabled(enabled);
        return r;
    }

    @Test
    @DisplayName("createWarningRule：补齐 createTime/updateTime 并落库")
    void createWarningRule_setsTimestampsAndSaves() {
        InventoryWarningRule input = rule(null, "低库存预警", true);

        InventoryWarningRule result = service.createWarningRule(input);

        assertSame(input, result);
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        verify(warningRuleMapper).insert(input);
    }

    @Test
    @DisplayName("getWarningRuleById：委托 getById")
    void getWarningRuleById_delegates() {
        InventoryWarningRule stored = rule(1L, "规则A", true);
        when(warningRuleMapper.selectById(1L)).thenReturn(stored);

        assertSame(stored, service.getWarningRuleById(1L));
    }

    @Test
    @DisplayName("updateWarningRule：强制覆盖主键与 updateTime，更新后回查")
    void updateWarningRule_setsIdAndTimestampThenReloads() {
        InventoryWarningRule payload = rule(null, "新名称", false);
        InventoryWarningRule reloaded = rule(9L, "新名称", false);
        when(warningRuleMapper.selectById(9L)).thenReturn(reloaded);

        InventoryWarningRule result = service.updateWarningRule(9L, payload);

        assertEquals(9L, payload.getRuleId(), "路径 ID 必须覆盖请求体里的主键");
        assertNotNull(payload.getUpdateTime());
        assertSame(reloaded, result);
        verify(warningRuleMapper).updateById(payload);
    }

    @Test
    @DisplayName("deleteWarningRule：委托 removeById")
    void deleteWarningRule_delegates() {
        service.deleteWarningRule(3L);

        verify(warningRuleMapper).deleteById(3L);
    }

    @Test
    @DisplayName("getWarningRulePage：ruleName 模糊 + warningLevel→warning_type 等值 + enabled→is_enabled 布尔")
    void getWarningRulePage_buildsFiltersAndMapsEnabledToBoolean() {
        Page<InventoryWarningRule> page = new Page<>(1, 10);
        page.setRecords(List.of(rule(1L, "规则A", true)));
        when(warningRuleMapper.selectPage(any(), any())).thenReturn(page);

        IPage<InventoryWarningRule> result = service.getWarningRulePage(page, "低库存", "库存", 2, 1);

        assertSame(page, result);
        assertEquals(1, result.getRecords().size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<InventoryWarningRule>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(warningRuleMapper).selectPage(any(), captor.capture());
        QueryWrapper<InventoryWarningRule> wrapper = (QueryWrapper<InventoryWarningRule>) captor.getValue();
        wrapper.getSqlSegment();
        List<Object> values = new ArrayList<>(wrapper.getParamNameValuePairs().values());

        assertTrue(values.contains("%低库存%"), "rule_name 应为 LIKE %kw%");
        assertTrue(values.contains(2), "warningLevel 应写入 warning_type");
        assertTrue(values.contains(Boolean.TRUE), "enabled=1 应转换为 is_enabled=true（不是整数 1）");
        assertFalse(values.contains(1), "不应把 int 1 直接写进 is_enabled 条件");
    }

    @Test
    @DisplayName("getWarningRulePage：enabled=0 → is_enabled=false；category 参数当前被忽略")
    void getWarningRulePage_disabledMapsToFalseAndIgnoresCategory() {
        Page<InventoryWarningRule> page = new Page<>(1, 10);
        when(warningRuleMapper.selectPage(any(), any())).thenReturn(page);

        service.getWarningRulePage(page, null, "任意类型", null, 0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<InventoryWarningRule>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(warningRuleMapper).selectPage(any(), captor.capture());
        QueryWrapper<InventoryWarningRule> wrapper = (QueryWrapper<InventoryWarningRule>) captor.getValue();
        wrapper.getSqlSegment();
        List<Object> values = new ArrayList<>(wrapper.getParamNameValuePairs().values());

        assertTrue(values.contains(Boolean.FALSE), "enabled=0 应转换为 is_enabled=false");
        assertEquals(1, values.size(), "其余筛选条件为 null/空白时不应产生参数（category 未实现）");
    }

    @Test
    @DisplayName("getAllEnabledRules：只查 is_enabled = true")
    void getAllEnabledRules_queriesEnabledOnly() {
        List<InventoryWarningRule> enabled = List.of(rule(1L, "A", true));
        when(warningRuleMapper.selectList(any())).thenReturn(enabled);

        assertSame(enabled, service.getAllEnabledRules());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<InventoryWarningRule>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(warningRuleMapper).selectList(captor.capture());
        QueryWrapper<InventoryWarningRule> wrapper = (QueryWrapper<InventoryWarningRule>) captor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Boolean.TRUE));
    }

    @Test
    @DisplayName("toggleRuleStatus：置为启用/停用并写入 updateTime")
    void toggleRuleStatus_updatesBooleanFlag() {
        InventoryWarningRule stored = rule(5L, "规则B", true);
        when(warningRuleMapper.selectById(5L)).thenReturn(stored);

        InventoryWarningRule result = service.toggleRuleStatus(5L, 0);

        assertSame(stored, result);
        assertEquals(Boolean.FALSE, stored.getIsEnabled());
        assertNotNull(stored.getUpdateTime());
        verify(warningRuleMapper).updateById(stored);
    }

    @Test
    @DisplayName("toggleRuleStatus：规则不存在时返回 null 且不写库")
    void toggleRuleStatus_missing_returnsNullWithoutUpdate() {
        when(warningRuleMapper.selectById(404L)).thenReturn(null);

        assertNull(service.toggleRuleStatus(404L, 1));
        verify(warningRuleMapper, never()).updateById(any(InventoryWarningRule.class));
    }
}
