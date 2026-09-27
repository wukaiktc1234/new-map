package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.InventoryMovement;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.InventoryMovementMapper;
import com.foodtraceability.service.LocationService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M3-M4 S4b 单测：InventoryService 位置维度方法（统一账直算）。
 * 五类覆盖：入建 / 出抛 / 乐观锁重试 / 成本结转 / source 约束。
 * 模式沿 OrderNewServiceImplDeductTest（纯 Mockito，无 Spring 上下文）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceImplLocationStockTest {

    private static final Long LOCATION_ID = 1L;
    private static final Long MATERIAL_ID = 100L;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryMovementMapper inventoryMovementMapper;

    @Mock
    private LocationService locationService;

    private InventoryServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        // LambdaQueryWrapper 需要实体 TableInfo（getLowStockAtLocation 路径）
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(InventoryMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Inventory.class);
    }

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(inventoryMapper, null, inventoryMovementMapper, locationService);
    }

    private Inventory row(Long inventoryId, String qty, Long unitCost, Long totalCost) {
        Inventory inv = new Inventory();
        inv.setInventoryId(inventoryId);
        inv.setLocationId(LOCATION_ID);
        inv.setMaterialId(MATERIAL_ID);
        inv.setMaterialName("测试物料");
        inv.setQuantity(new BigDecimal(qty));
        inv.setUnitCost(unitCost);
        inv.setTotalCost(totalCost);
        return inv;
    }

    // ============ 1. 入建：increase 不存在则 INSERT ============

    @Test
    @DisplayName("入建：无库存行时 increase 新建行并写 IN 流水")
    void increaseCreatesRowWhenAbsent() {
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID)).thenReturn(null);
        when(inventoryMapper.insert(any(Inventory.class))).thenAnswer(inv -> {
            Inventory saved = inv.getArgument(0);
            saved.setInventoryId(99L); // 模拟 DB identity
            return 1;
        });

        service.increaseStockAtLocation(LOCATION_ID, MATERIAL_ID, "新物料",
                new BigDecimal("5"), "斤", 300L, 1, "SI202609280001");

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).insert(captor.capture());
        Inventory inserted = captor.getValue();
        assertEquals(LOCATION_ID, inserted.getLocationId());
        assertEquals(MATERIAL_ID, inserted.getMaterialId());
        assertEquals(new BigDecimal("5"), inserted.getQuantity());
        assertEquals(300L, inserted.getUnitCost());
        assertEquals(1500L, inserted.getTotalCost());
        assertNotNull(inserted.getInventoryId());

        ArgumentCaptor<InventoryMovement> mvCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementMapper).insert(mvCaptor.capture());
        assertEquals("IN", mvCaptor.getValue().getMovementType());
        assertEquals(LOCATION_ID, mvCaptor.getValue().getLocationId());
        assertEquals("SI202609280001", mvCaptor.getValue().getSourceRef());
    }

    // ============ 2. 出抛：decrease 无行/不足时抛异常，不建行 ============

    @Test
    @DisplayName("出抛A：库存行不存在抛 NOT_FOUND，且不 INSERT 新行")
    void decreaseThrowsNotFoundWhenAbsent() {
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.decreaseStockAtLocation(LOCATION_ID, MATERIAL_ID, new BigDecimal("1"), 2, "T1"));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        verify(inventoryMapper, never()).insert(any(Inventory.class));
        verify(inventoryMapper, never()).updateById(any(Inventory.class));
    }

    @Test
    @DisplayName("出抛B：库存不足抛 INVENTORY_INSUFFICIENT")
    void decreaseThrowsInsufficient() {
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID))
                .thenReturn(row(10L, "2", 500L, 1000L));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.decreaseStockAtLocation(LOCATION_ID, MATERIAL_ID, new BigDecimal("5"), 2, "T2"));

        assertEquals(ErrorCode.INVENTORY_INSUFFICIENT.getCode(), ex.getCode());
        verify(inventoryMapper, never()).updateById(any(Inventory.class));
    }

    // ============ 3. 乐观锁重试：updateById 返回 0 后重读重试 ============

    @Test
    @DisplayName("乐观锁：首次 updateById 冲突返回 0，重试读取最新数据后成功")
    void decreaseRetriesOnOptimisticLockConflict() {
        Inventory v1 = row(10L, "10", 500L, 5000L);
        Inventory v2 = row(10L, "9", 500L, 4500L); // 并发被其他事务扣了 1
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID))
                .thenReturn(v1)   // 第一次尝试
                .thenReturn(v2);  // 重试时重读最新数据
        when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(0).thenReturn(1);

        Long outgoingCost = service.decreaseStockAtLocation(LOCATION_ID, MATERIAL_ID,
                new BigDecimal("3"), 2, "T3");

        assertEquals(1500L, outgoingCost);
        verify(inventoryMapper, times(2)).updateById(any(Inventory.class));
        // 出库成本按重试后最新行的单位成本 500 结转：3 × 500 = 1500
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper, atLeastOnce()).updateById(captor.capture());
        assertEquals(new BigDecimal("6"), captor.getValue().getQuantity()); // 9 - 3
    }

    // ============ 4. 成本结转：decrease 按当前单位成本结转并回写总成本 ============

    @Test
    @DisplayName("成本结转：出库总成本=当前单位成本×数量，totalCost 同步扣减，流水带成本")
    void decreaseCarriesCostByCurrentUnitCost() {
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID))
                .thenReturn(row(10L, "10", 400L, 4000L));
        when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(1);

        Long outgoingCost = service.decreaseStockAtLocation(LOCATION_ID, MATERIAL_ID,
                new BigDecimal("2"), 2, "T20260928001");

        assertEquals(800L, outgoingCost);
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).updateById(captor.capture());
        assertEquals(new BigDecimal("8"), captor.getValue().getQuantity());
        assertEquals(3200L, captor.getValue().getTotalCost()); // 4000 - 800
        // 单位成本保持不变（按当前单位成本出库）
        assertEquals(400L, captor.getValue().getUnitCost());

        ArgumentCaptor<InventoryMovement> mvCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementMapper).insert(mvCaptor.capture());
        assertEquals("OUT", mvCaptor.getValue().getMovementType());
        assertEquals(new BigDecimal("2"), mvCaptor.getValue().getChangeQty());
        assertEquals(new BigDecimal("8"), mvCaptor.getValue().getBalanceAfter());
        assertEquals(400L, mvCaptor.getValue().getUnitCost());
        assertEquals(800L, mvCaptor.getValue().getTotalCost());
    }

    // ============ 5. source 约束：无 sourceRef 时降级填充，流水写失败不回滚库存 ============

    @Test
    @DisplayName("source约束A：sourceRef 为空时降级填 UNSPECIFIED- 前缀，不抛异常")
    void increaseWithNullSourceRefFillsFallbackRef() {
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID)).thenReturn(null);
        when(inventoryMapper.insert(any(Inventory.class))).thenReturn(1);

        service.increaseStockAtLocation(LOCATION_ID, MATERIAL_ID, "物料",
                new BigDecimal("1"), "斤", null, 1, null);

        ArgumentCaptor<InventoryMovement> mvCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementMapper).insert(mvCaptor.capture());
        assertTrue(mvCaptor.getValue().getSourceRef().startsWith("UNSPECIFIED-"));
        assertEquals(0L, mvCaptor.getValue().getUnitCost()); // unitCost=null → 0（§6-6 现状迁移）
    }

    @Test
    @DisplayName("source约束B：流水写入失败（约束/异常）被降级吞掉，库存变更不受影响")
    void movementFailureDoesNotRollbackStock() {
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID))
                .thenReturn(row(10L, "10", 400L, 4000L));
        when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(1);
        // 模拟 NOT NULL/约束违规：流水 insert 抛异常
        when(inventoryMovementMapper.insert(any(InventoryMovement.class)))
                .thenThrow(new RuntimeException("NOT NULL violation: source_ref"));

        // 不抛异常 = 库存变更照常提交（§6-3 尽力而为现状迁移）
        Long outgoingCost = service.decreaseStockAtLocation(LOCATION_ID, MATERIAL_ID,
                new BigDecimal("1"), 2, null);

        assertEquals(400L, outgoingCost);
        verify(inventoryMapper).updateById(any(Inventory.class));
        verify(inventoryMovementMapper, times(1)).insert(any(InventoryMovement.class));
    }

    // ============ 附：increase 已有行走累加分支（非入建） ============

    @Test
    @DisplayName("入账已有行：最新入库价格法覆盖单位成本，累加数量与总成本")
    void increaseExistingRowUsesLatestInPrice() {
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID))
                .thenReturn(row(10L, "10", 999L, 9990L));
        when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(1);

        service.increaseStockAtLocation(LOCATION_ID, MATERIAL_ID, "测试物料",
                new BigDecimal("4"), "斤", 300L, 1, "SI-2");

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).updateById(captor.capture());
        assertEquals(new BigDecimal("14"), captor.getValue().getQuantity());
        assertEquals(300L, captor.getValue().getUnitCost()); // 最新入库价格法
        assertEquals(11190L, captor.getValue().getTotalCost()); // 9990 + 4×300
        verify(inventoryMapper, never()).insert(any(Inventory.class));
    }

    // ============ 附：resolveLocationIdByStoreId ============

    @Test
    @DisplayName("resolveLocationIdByStoreId：经 LocationService 解析；解析失败返回 null")
    void resolveLocationIdDelegatesToLocationService() {
        com.foodtraceability.entity.Location location = new com.foodtraceability.entity.Location();
        location.setLocationId(7L);
        when(locationService.resolveByStoreId(1L)).thenReturn(location);
        when(locationService.resolveByStoreId(99L)).thenReturn(null);

        assertEquals(Long.valueOf(7L), service.resolveLocationIdByStoreId(1L));
        assertNull(service.resolveLocationIdByStoreId(99L));
        assertNull(service.resolveLocationIdByStoreId(null));
        verify(locationService, times(2)).resolveByStoreId(anyLong());
    }
}
