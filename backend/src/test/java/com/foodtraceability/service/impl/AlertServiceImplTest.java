package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.config.AlertConfig;
import com.foodtraceability.entity.AlertEntity;
import com.foodtraceability.mapper.AlertEntityMapper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AlertServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：告警创建（ACTIVE + alertTime + 入库）、分页/状态/级别/活跃列表查询委托、
 * 按 ID 查询的 Optional 语义、确认与解决的状态迁移（含不存在时不写库）、
 * 删除按影响行数返回布尔、阈值判定矩阵（全局开关 / 规则缺失 / 规则关闭 / 大于 / 等于 / 小于）、
 * 以及计数委托。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlertServiceImplTest {

    @Mock
    private AlertEntityMapper alertEntityMapper;

    @Mock
    private AlertConfig alertConfig;

    @InjectMocks
    private AlertServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, AlertEntity.class);
    }

    @BeforeEach
    void setUp() {
        when(alertEntityMapper.insert(any(AlertEntity.class))).thenReturn(1);
        when(alertEntityMapper.updateById(any(AlertEntity.class))).thenReturn(1);
        when(alertEntityMapper.deleteById(anyLong())).thenReturn(1);
    }

    private static AlertEntity alert(Long id, String status) {
        AlertEntity a = new AlertEntity();
        a.setAlertId(id);
        a.setName("库存告警");
        a.setDescription("库存低于阈值");
        a.setSeverity("HIGH");
        a.setStatus(status);
        a.setValue(12.5);
        a.setThreshold(20.0);
        return a;
    }

    private static AlertConfig config(boolean enabled, Map<String, AlertConfig.AlertRule> rules) {
        AlertConfig c = new AlertConfig();
        c.setEnabled(enabled);
        c.setRules(rules);
        return c;
    }

    private static AlertConfig.AlertRule rule(boolean enabled, double threshold) {
        AlertConfig.AlertRule r = new AlertConfig.AlertRule();
        r.setEnabled(enabled);
        r.setThreshold(threshold);
        return r;
    }

    // ==================== 创建 / 查询 ====================

    @Test
    @DisplayName("createAlert：置 ACTIVE + alertTime 后入库")
    void createAlert_setsActiveStatusAndTime() {
        AlertEntity input = alert(null, null);

        AlertEntity result = service.createAlert(input);

        assertSame(input, result);
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getAlertTime());
        verify(alertEntityMapper).insert(input);
    }

    @Test
    @DisplayName("getAlertPage / getActiveAlerts：直接委托 mapper")
    void pageAndActive_delegate() {
        Page<AlertEntity> page = new Page<>(1, 10);
        when(alertEntityMapper.selectAlertPage(page, "HIGH", "ACTIVE", "KDS")).thenReturn(page);
        assertSame(page, service.getAlertPage(page, "HIGH", "ACTIVE", "KDS"));

        List<AlertEntity> active = List.of(alert(1L, "ACTIVE"));
        when(alertEntityMapper.selectActiveAlerts()).thenReturn(active);
        assertSame(active, service.getActiveAlerts());
    }

    @Test
    @DisplayName("getAllAlerts：按 alert_time 倒序并限制 500 条")
    void getAllAlerts_ordersAndLimits() {
        when(alertEntityMapper.selectList(any())).thenReturn(new ArrayList<>());

        service.getAllAlerts();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<AlertEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(alertEntityMapper).selectList(captor.capture());
        LambdaQueryWrapper<AlertEntity> wrapper = (LambdaQueryWrapper<AlertEntity>) captor.getValue();
        String sql = wrapper.getCustomSqlSegment().toLowerCase();
        assertTrue(sql.contains("order by"), "应带排序，实际: " + sql);
        assertTrue(sql.contains("limit 500"), "应限制 500 条，实际: " + sql);
    }

    @Test
    @DisplayName("getAlertById：不存在返回空 Optional；存在返回实体")
    void getAlertById_optionalSemantics() {
        when(alertEntityMapper.selectById(404L)).thenReturn(null);
        assertEquals(Optional.empty(), service.getAlertById(404L));

        AlertEntity stored = alert(1L, "ACTIVE");
        when(alertEntityMapper.selectById(1L)).thenReturn(stored);
        assertEquals(Optional.of(stored), service.getAlertById(1L));
    }

    @Test
    @DisplayName("getAlertsByStatus / getAlertsBySeverity：条件等值查询")
    void byStatusAndSeverity_filter() {
        List<AlertEntity> rows = List.of(alert(1L, "ACTIVE"));
        when(alertEntityMapper.selectList(any())).thenReturn(rows);

        assertSame(rows, service.getAlertsByStatus("ACTIVE"));
        assertSame(rows, service.getAlertsBySeverity("HIGH"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<AlertEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(alertEntityMapper, times(2)).selectList(captor.capture());
        LambdaQueryWrapper<AlertEntity> byStatus = (LambdaQueryWrapper<AlertEntity>) captor.getAllValues().get(0);
        LambdaQueryWrapper<AlertEntity> bySeverity = (LambdaQueryWrapper<AlertEntity>) captor.getAllValues().get(1);
        byStatus.getCustomSqlSegment();
        bySeverity.getCustomSqlSegment();
        assertTrue(byStatus.getParamNameValuePairs().containsValue("ACTIVE"));
        assertTrue(bySeverity.getParamNameValuePairs().containsValue("HIGH"));
    }

    // ==================== 状态迁移 ====================

    @Test
    @DisplayName("acknowledgeAlert：不存在返回 null 且不写库；存在置 ACKNOWLEDGED")
    void acknowledgeAlert_transitions() {
        when(alertEntityMapper.selectById(404L)).thenReturn(null);
        assertNull(service.acknowledgeAlert(404L, "张三"));
        verify(alertEntityMapper, never()).updateById(any(AlertEntity.class));

        AlertEntity active = alert(1L, "ACTIVE");
        when(alertEntityMapper.selectById(1L)).thenReturn(active);
        assertSame(active, service.acknowledgeAlert(1L, "张三"));
        assertEquals("ACKNOWLEDGED", active.getStatus());
        assertEquals("张三", active.getAcknowledgedBy());
        assertNotNull(active.getAcknowledgeTime());
        verify(alertEntityMapper).updateById(active);
    }

    @Test
    @DisplayName("resolveAlert：不存在返回 null；存在置 RESOLVED 并写处理说明")
    void resolveAlert_transitions() {
        when(alertEntityMapper.selectById(404L)).thenReturn(null);
        assertNull(service.resolveAlert(404L, "李四", "已补货"));

        AlertEntity ack = alert(2L, "ACKNOWLEDGED");
        when(alertEntityMapper.selectById(2L)).thenReturn(ack);
        service.resolveAlert(2L, "李四", "已补货");

        assertEquals("RESOLVED", ack.getStatus());
        assertEquals("李四", ack.getResolvedBy());
        assertEquals("已补货", ack.getResolveDescription());
        assertNotNull(ack.getResolveTime());
    }

    @Test
    @DisplayName("deleteAlert：影响行数 >0 返回 true，否则 false")
    void deleteAlert_returnsBoolean() {
        assertTrue(service.deleteAlert(1L));

        when(alertEntityMapper.deleteById(anyLong())).thenReturn(0);
        assertFalse(service.deleteAlert(2L));
    }

    // ==================== 阈值判定 ====================

    @Test
    @DisplayName("checkAlertCondition：全局关闭 / 规则缺失返回 false（不查规则阈值）")
    void checkAlertCondition_disabledOrMissingRule() {
        when(alertConfig.isEnabled()).thenReturn(false);
        assertFalse(service.checkAlertCondition("cpu", 99));

        when(alertConfig.isEnabled()).thenReturn(true);
        when(alertConfig.getRules()).thenReturn(new HashMap<>());
        assertFalse(service.checkAlertCondition("cpu", 99));
    }

    @Test
    @DisplayName("checkAlertCondition：规则关闭返回 false；超过阈值 true；等于/低于阈值 false")
    void checkAlertCondition_thresholdMatrix() {
        Map<String, AlertConfig.AlertRule> rules = new HashMap<>();
        rules.put("cpu", rule(true, 80.0));
        rules.put("mem", rule(false, 10.0));
        when(alertConfig.isEnabled()).thenReturn(true);
        when(alertConfig.getRules()).thenReturn(rules);

        assertFalse(service.checkAlertCondition("mem", 99), "规则未启用应直接 false");
        assertTrue(service.checkAlertCondition("cpu", 80.1));
        assertFalse(service.checkAlertCondition("cpu", 80.0), "等于阈值不算触发");
        assertFalse(service.checkAlertCondition("cpu", 10.0));
    }

    @Test
    @DisplayName("countBySeverity / countByStatus：委托 mapper")
    void counts_delegate() {
        when(alertEntityMapper.countBySeverity("HIGH")).thenReturn(3L);
        when(alertEntityMapper.countByStatus("ACTIVE")).thenReturn(7L);

        assertEquals(3L, service.countBySeverity("HIGH"));
        assertEquals(7L, service.countByStatus("ACTIVE"));
    }
}
