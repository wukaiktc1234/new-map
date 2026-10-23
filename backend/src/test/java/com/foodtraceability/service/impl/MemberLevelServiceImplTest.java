package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.MemberLevel;
import com.foodtraceability.mapper.MemberLevelBaseMapper;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * MemberLevelServiceImpl 单元测试（会员等级：启用列表、按编码查、升级判定、等级分布）。
 *
 * <p>重点覆盖 {@code calculateUpgradeLevel} 的匹配语义：
 * 按 minPoints 从高到低找第一个「积分够 且 消费额够」的等级；阈值 null 视为无约束。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberLevelServiceImplTest {

    @Mock
    private MemberLevelBaseMapper memberLevelBaseMapper;

    @InjectMocks
    private MemberLevelServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, MemberLevel.class);
    }

    /**
     * ServiceImpl 的 baseMapper 是泛型字段，Mockito 的 @InjectMocks 注入不到父类泛型字段，
     * 必须反射塞入，否则 service.list(wrapper) 会 NPE。
     */
    @BeforeEach
    void injectBaseMapper() throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, memberLevelBaseMapper);
    }

    private static MemberLevel level(Long id, String code, Integer minPoints, Long minConsumption, Integer sortOrder) {
        MemberLevel l = new MemberLevel();
        l.setLevelId(id);
        l.setLevelCode(code);
        l.setLevelName("等级" + code);
        l.setMinPoints(minPoints);
        l.setMinConsumption(minConsumption);
        l.setSortOrder(sortOrder);
        l.setStatus(1);
        return l;
    }

    // ==================== listEnabledLevels ====================

    @Test
    @DisplayName("listEnabledLevels：只查启用状态并按 sortOrder 升序")
    void listEnabled_appliesStatusAndOrder() {
        when(memberLevelBaseMapper.selectList(any())).thenReturn(List.of(level(1L, "SILVER", 0, 0L, 1)));

        List<MemberLevel> result = service.listEnabledLevels();

        assertEquals(1, result.size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MemberLevel>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(memberLevelBaseMapper).selectList(captor.capture());
        String sql = captor.getValue().getCustomSqlSegment().toLowerCase();
        assertTrue(sql.contains("status"), "必须带 status 条件，实际: " + sql);
        assertTrue(sql.contains("sort_order"), "必须按 sort_order 排序，实际: " + sql);
        assertTrue(sql.contains("asc"), "sortOrder 必须升序，实际: " + sql);
    }

    // ==================== getByCode ====================

    @Test
    @DisplayName("getByCode：按 levelCode 等值查，命中返回实体")
    void getByCode_delegates() {
        MemberLevel gold = level(2L, "GOLD", 500, 1000L, 2);
        // ServiceImpl.getOne(wrapper) 实际调用 baseMapper.selectOne(wrapper, throwEx) 双参重载
        when(memberLevelBaseMapper.selectOne(any(), anyBoolean())).thenReturn(gold);
        when(memberLevelBaseMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(gold)));

        MemberLevel result = service.getByCode("GOLD");

        assertNotNull(result);
        assertEquals(2L, result.getLevelId());
    }

    // ==================== calculateUpgradeLevel ====================

    private static List<MemberLevel> threeTiers() {
        List<MemberLevel> levels = new ArrayList<>();
        levels.add(level(1L, "SILVER", 0, 0L, 1));
        levels.add(level(2L, "GOLD", 500, 1000L, 2));
        levels.add(level(3L, "DIAMOND", 2000, 5000L, 3));
        return levels;
    }

    @Test
    @DisplayName("calculateUpgradeLevel：积分与消费都达标的最高等级胜出")
    void upgrade_picksHighestMatching() {
        when(memberLevelBaseMapper.selectList(any())).thenReturn(threeTiers());

        // 600 分 / 2000 元 → GOLD（DIAMOND 要 2000 分 / 5000 元）
        assertEquals(2L, service.calculateUpgradeLevel(600, 2000L));
    }

    @Test
    @DisplayName("calculateUpgradeLevel：消费额不达标时降级到满足的下层")
    void upgrade_consumptionGatesLevel() {
        when(memberLevelBaseMapper.selectList(any())).thenReturn(threeTiers());

        // 积分够 DIAMOND，但消费只有 100 元 → 只能 SILVER（GOLD 要 1000 元）
        assertEquals(1L, service.calculateUpgradeLevel(3000, 100L));
    }

    @Test
    @DisplayName("calculateUpgradeLevel：都不达标返回 null（不臆造默认等级）")
    void upgrade_noMatch_returnsNull() {
        List<MemberLevel> levels = new ArrayList<>();
        levels.add(level(2L, "GOLD", 500, 1000L, 2));
        levels.add(level(3L, "DIAMOND", 2000, 5000L, 3));
        when(memberLevelBaseMapper.selectList(any())).thenReturn(levels);

        assertNull(service.calculateUpgradeLevel(0, 0L));
    }

    @Test
    @DisplayName("calculateUpgradeLevel：minPoints/minConsumption 为 null 视为无门槛")
    void upgrade_nullThresholdsAreUnconstrained() {
        List<MemberLevel> levels = new ArrayList<>();
        levels.add(level(9L, "OPEN", null, null, 1));
        when(memberLevelBaseMapper.selectList(any())).thenReturn(levels);

        assertEquals(9L, service.calculateUpgradeLevel(0, null),
                "阈值 null 表示不限，应命中该等级");
    }

    @Test
    @DisplayName("calculateUpgradeLevel：没有启用等级时返回 null")
    void upgrade_noLevels_returnsNull() {
        when(memberLevelBaseMapper.selectList(any())).thenReturn(new ArrayList<>());
        assertNull(service.calculateUpgradeLevel(9999, 999999L));
    }

    // ==================== getLevelDistribution ====================

    @Test
    @DisplayName("getLevelDistribution：分布来自 mapper，total 取启用等级数")
    void distribution_combinesMapperAndEnabledCount() {
        Map<String, Object> row = Map.of("levelCode", "GOLD", "memberCount", 12);
        when(memberLevelBaseMapper.selectLevelDistribution()).thenReturn(List.of(row));
        when(memberLevelBaseMapper.selectList(any())).thenReturn(threeTiers());

        Map<String, Object> result = service.getLevelDistribution();

        assertNotNull(result.get("distribution"));
        assertEquals(1, ((List<?>) result.get("distribution")).size());
        assertEquals(3, result.get("total"), "total 应为启用等级数量");
        verify(memberLevelBaseMapper).selectLevelDistribution();
    }
}
