package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.foodtraceability.entity.ComboInventory;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.mapper.ComboInventoryMapper;
import com.foodtraceability.service.InventoryService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ComboInventoryServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：套餐库存关联的查询/新增（库存不存在闸门、名称回填、单位回退库存单位）/更新
 * （非空字段覆盖、缺失关联抛错）/删除的布尔回传，以及套餐成本聚合的委托。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComboInventoryServiceImplTest {

    @Mock
    private ComboInventoryMapper comboInventoryMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ComboInventoryServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, ComboInventory.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", comboInventoryMapper);
        when(comboInventoryMapper.insert(any(ComboInventory.class))).thenReturn(1);
        when(comboInventoryMapper.updateById(any(ComboInventory.class))).thenReturn(1);
        when(comboInventoryMapper.deleteById(any(Long.class))).thenReturn(1);
    }

    private static Inventory inventory(String name, String unit) {
        Inventory inv = new Inventory();
        inv.setMaterialName(name);
        inv.setUnit(unit);
        return inv;
    }

    private static ComboInventory relation(BigDecimal qty, String unit, String remark) {
        ComboInventory c = new ComboInventory();
        c.setComboId(1L);
        c.setInventoryId(100L);
        c.setQuantity(qty);
        c.setUnit(unit);
        c.setRemark(remark);
        return c;
    }

    @Test
    @DisplayName("getComboInventories：委托 mapper.selectByComboId")
    void getByComboId_delegates() {
        List<ComboInventory> rows = List.of(relation(BigDecimal.ONE, "份", null));
        when(comboInventoryMapper.selectByComboId(1L)).thenReturn(rows);

        assertSame(rows, service.getComboInventories(1L));
    }

    @Test
    @DisplayName("addComboInventory：库存产品不存在时抛异常，不写库")
    void add_missingInventory_throws() {
        when(inventoryService.getById(100L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.addComboInventory(1L, 100L, BigDecimal.ONE, "份", null));

        assertTrue(ex.getMessage().contains("库存产品不存在"));
        verify(comboInventoryMapper, org.mockito.Mockito.never()).insert(any(ComboInventory.class));
    }

    @Test
    @DisplayName("addComboInventory：回填库存名称，unit 未传时回退库存单位")
    void add_fillsNameAndFallsBackToInventoryUnit() {
        when(inventoryService.getById(100L)).thenReturn(inventory("五花肉", "kg"));

        ComboInventory saved = service.addComboInventory(1L, 100L, new BigDecimal("2"), null, "套餐含肉");

        assertEquals(1L, saved.getComboId());
        assertEquals(100L, saved.getInventoryId());
        assertEquals("五花肉", saved.getInventoryName());
        assertEquals(new BigDecimal("2"), saved.getQuantity());
        assertEquals("kg", saved.getUnit());
        assertEquals("套餐含肉", saved.getRemark());
        verify(comboInventoryMapper).insert(saved);
    }

    @Test
    @DisplayName("addComboInventory：显式 unit 优先于库存单位")
    void add_explicitUnitWins() {
        when(inventoryService.getById(100L)).thenReturn(inventory("五花肉", "kg"));

        ComboInventory saved = service.addComboInventory(1L, 100L, new BigDecimal("1"), "份", null);

        assertEquals("份", saved.getUnit());
    }

    @Test
    @DisplayName("updateComboInventory：关联不存在抛异常")
    void update_missing_throws() {
        when(comboInventoryMapper.selectById(404L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateComboInventory(404L, BigDecimal.ONE, null, null));

        assertTrue(ex.getMessage().contains("套餐库存关联不存在"));
    }

    @Test
    @DisplayName("updateComboInventory：只覆盖非空字段，更新成功返回 true")
    void update_partialUpdate() {
        ComboInventory existing = relation(new BigDecimal("5"), "kg", "旧备注");
        when(comboInventoryMapper.selectById(7L)).thenReturn(existing);

        assertTrue(service.updateComboInventory(7L, new BigDecimal("8"), null, null));

        ArgumentCaptor<ComboInventory> captor = ArgumentCaptor.forClass(ComboInventory.class);
        verify(comboInventoryMapper).updateById(captor.capture());
        ComboInventory saved = captor.getValue();
        assertEquals(new BigDecimal("8"), saved.getQuantity());
        assertEquals("kg", saved.getUnit(), "unit 传 null 时保持原值");
        assertEquals("旧备注", saved.getRemark());
    }

    @Test
    @DisplayName("updateComboInventory：影响行数为 0 返回 false")
    void update_noRows_returnsFalse() {
        when(comboInventoryMapper.selectById(7L)).thenReturn(relation(BigDecimal.ONE, "袋", null));
        when(comboInventoryMapper.updateById(any(ComboInventory.class))).thenReturn(0);

        assertFalse(service.updateComboInventory(7L, BigDecimal.TEN, "份", "改备注"));
    }

    @Test
    @DisplayName("deleteComboInventory：按影响行数返回布尔")
    void delete_returnsBoolean() {
        assertTrue(service.deleteComboInventory(1L));

        when(comboInventoryMapper.deleteById(anyLong())).thenReturn(0);
        assertFalse(service.deleteComboInventory(2L));
    }

    @Test
    @DisplayName("calculateComboCost：委托 mapper 聚合，null 原样返回")
    void calculateCost_delegates() {
        when(comboInventoryMapper.calculateComboCost(1L)).thenReturn(new BigDecimal("88.50"));
        assertEquals(new BigDecimal("88.50"), service.calculateComboCost(1L));

        when(comboInventoryMapper.calculateComboCost(2L)).thenReturn(null);
        assertNull(service.calculateComboCost(2L));
    }
}
