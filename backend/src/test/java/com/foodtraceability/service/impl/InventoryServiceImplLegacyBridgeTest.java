package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryDeductDTO;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.InventoryMovement;
import com.foodtraceability.entity.Location;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.service.InventoryMovementService;
import com.foodtraceability.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-A 卡（P1-INVENTORY-LEGACY-WRITEPATH-001）单测：legacy 写路径收编到统一账。
 * <p>覆盖：未映射仓显式拒绝（宪法 §三.4）、sourceType/referenceNo 必填（§III.4/§IV.4）、
 * 合法入账委派统一账（location 维度 + 批次号 + source_ref）、legacy"可用量=数量−锁定数量"语义保留。</p>
 * <p>模式沿 {@link InventoryServiceImplLocationStockTest}（纯 Mockito，无 Spring 上下文）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceImplLegacyBridgeTest {

    private static final Long WAREHOUSE_ID = 1L;
    private static final Long LOCATION_ID = 5L;
    private static final Long MATERIAL_ID = 3L;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryMovementService inventoryMovementService;

    @Mock
    private LocationService locationService;

    private InventoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(inventoryMapper, inventoryMovementService, locationService);
        // ServiceImpl.getById 依赖 baseMapper（deductInventory 经 inventoryId 定位）
        ReflectionTestUtils.setField(service, "baseMapper", inventoryMapper);
    }

    private Location location(Long id) {
        Location l = new Location();
        l.setLocationId(id);
        return l;
    }

    private Inventory row(Long inventoryId, Long locationId, String qty, String locked) {
        Inventory inv = new Inventory();
        inv.setInventoryId(inventoryId);
        inv.setLocationId(locationId);
        inv.setMaterialId(MATERIAL_ID);
        inv.setMaterialName("生菜");
        inv.setQuantity(new BigDecimal(qty));
        inv.setLockedQuantity(locked == null ? null : new BigDecimal(locked));
        inv.setUnitCost(300L);
        inv.setTotalCost(3000L);
        return inv;
    }

    private InventoryIncreaseDTO increaseDTO(String sourceType, String referenceNo) {
        InventoryIncreaseDTO dto = new InventoryIncreaseDTO();
        dto.setMaterialId(MATERIAL_ID);
        dto.setWarehouseId(WAREHOUSE_ID);
        dto.setQuantity(new BigDecimal("5"));
        dto.setUnitCost(300L);
        dto.setBatchNo("B20260930");
        dto.setReferenceType("purchase_stockin");
        dto.setReferenceNo(referenceNo);
        dto.setSourceType(sourceType);
        return dto;
    }

    // ============ 1. 未映射仓显式拒绝（宪法 §三.4） ============

    @Test
    @DisplayName("收编A：仓库未映射 → 显式拒绝，零写账零流水")
    void increaseRejectsUnmappedWarehouse() {
        InventoryIncreaseDTO dto = increaseDTO("OTHER", "REF-1");
        dto.setWarehouseId(99L);
        when(locationService.resolveByWarehouseId(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.increaseInventory(dto));

        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), ex.getCode());
        verify(inventoryMapper, never()).insert(any(Inventory.class));
        verify(inventoryMapper, never()).updateById(any(Inventory.class));
        verify(inventoryMovementService, never()).recordMovement(any());
    }

    // ============ 2. sourceType / referenceNo 必填（禁止兜底） ============

    @Test
    @DisplayName("收编B：sourceType 缺失 → PARAM_ERROR 显式拒绝，不解析位置")
    void increaseRejectsBlankSourceType() {
        InventoryIncreaseDTO dto = increaseDTO(null, "REF-1");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.increaseInventory(dto));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(locationService, never()).resolveByWarehouseId(any());
        verify(inventoryMapper, never()).insert(any(Inventory.class));
    }

    @Test
    @DisplayName("收编C：referenceNo 缺失 → PARAM_ERROR（统一流水禁无来源）")
    void increaseRejectsBlankReferenceNo() {
        InventoryIncreaseDTO dto = increaseDTO("OTHER", "   ");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.increaseInventory(dto));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(inventoryMapper, never()).insert(any(Inventory.class));
    }

    // ============ 3. 合法入账：委派统一账（location + 批次号 + source_ref） ============

    @Test
    @DisplayName("收编D：已映射仓 → 按 map 解析 location 入账，写 IN 流水（source_ref=referenceType:referenceNo）")
    void increaseDelegatesToLocationDimension() {
        when(locationService.resolveByWarehouseId(WAREHOUSE_ID)).thenReturn(location(LOCATION_ID));
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID)).thenReturn(null);
        when(inventoryMapper.insert(any(Inventory.class))).thenReturn(1);

        service.increaseInventory(increaseDTO("PURCHASE_STOCKIN", "SI202609300001"));

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).insert(captor.capture());
        Inventory inserted = captor.getValue();
        assertEquals(LOCATION_ID, inserted.getLocationId());
        assertEquals(MATERIAL_ID, inserted.getMaterialId());
        assertEquals(new BigDecimal("5"), inserted.getQuantity());
        assertEquals(300L, inserted.getUnitCost());
        assertEquals("B20260930", inserted.getBatchNo()); // 批次号现状语义保留

        ArgumentCaptor<InventoryMovement> mvCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementService).recordMovement(mvCaptor.capture());
        InventoryMovement mv = mvCaptor.getValue();
        assertEquals("IN", mv.getMovementType());
        assertEquals("PURCHASE_STOCKIN", mv.getSourceType());
        assertEquals("purchase_stockin:SI202609300001", mv.getSourceRef());
        assertEquals(new BigDecimal("5"), mv.getChangeQty());
    }

    // ============ 4. legacy"可用量=数量−锁定数量"语义保留 ============

    @Test
    @DisplayName("收编E：减少库存时锁定数量计入可用量（10−8=2 < 请求5）→ 库存不足")
    void decreaseRespectsLockedQuantity() {
        when(locationService.resolveByWarehouseId(WAREHOUSE_ID)).thenReturn(location(LOCATION_ID));
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID))
                .thenReturn(row(7L, LOCATION_ID, "10", "8"));

        InventoryDecreaseDTO dto = new InventoryDecreaseDTO();
        dto.setMaterialId(MATERIAL_ID);
        dto.setWarehouseId(WAREHOUSE_ID);
        dto.setQuantity(new BigDecimal("5"));
        dto.setReferenceType("inventory_loss");
        dto.setReferenceNo("LS202609300001");
        dto.setSourceType("LOSS");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.decreaseInventory(dto));

        assertEquals(ErrorCode.INVENTORY_INSUFFICIENT.getCode(), ex.getCode());
        verify(inventoryMapper, never()).updateById(any(Inventory.class));
        verify(inventoryMovementService, never()).recordMovement(any());
    }

    // ============ 5. deductInventory 按 inventoryId 定位后委派 ============

    @Test
    @DisplayName("收编F：扣减库存（按 inventoryId）→ 定位该行 location 后走统一账，写 OUT 流水")
    void deductDelegatesByInventoryId() {
        when(inventoryMapper.selectById(7L)).thenReturn(row(7L, LOCATION_ID, "10", "0"));
        when(inventoryMapper.selectByMaterialAndLocation(LOCATION_ID, MATERIAL_ID))
                .thenReturn(row(7L, LOCATION_ID, "10", "0"));
        when(inventoryMapper.updateById(any(Inventory.class))).thenReturn(1);

        InventoryDeductDTO dto = new InventoryDeductDTO();
        dto.setInventoryId(7L);
        dto.setQuantity(new BigDecimal("2"));
        dto.setReferenceNo("MANUAL-1");
        dto.setSourceType("OTHER");

        service.deductInventory(dto);

        ArgumentCaptor<InventoryMovement> mvCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementService).recordMovement(mvCaptor.capture());
        InventoryMovement mv = mvCaptor.getValue();
        assertEquals("OUT", mv.getMovementType());
        assertEquals("OTHER", mv.getSourceType());
        assertEquals("OTHER:MANUAL-1", mv.getSourceRef());
        assertEquals(new BigDecimal("-2"), mv.getChangeQty());
    }

    @Test
    @DisplayName("收编G：扣减库存 inventoryId 不存在 → NOT_FOUND，零流水")
    void deductMissingRowRejected() {
        when(inventoryMapper.selectById(404L)).thenReturn(null);

        InventoryDeductDTO dto = new InventoryDeductDTO();
        dto.setInventoryId(404L);
        dto.setQuantity(new BigDecimal("1"));
        dto.setReferenceNo("MANUAL-2");
        dto.setSourceType("OTHER");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deductInventory(dto));

        assertEquals(ErrorCode.INVENTORY_NOT_FOUND.getCode(), ex.getCode());
        verify(inventoryMovementService, never()).recordMovement(any());
    }
}
