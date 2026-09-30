package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.InventoryTransfer;
import com.foodtraceability.entity.Location;
import com.foodtraceability.entity.enums.InventoryTransferStatus;
import com.foodtraceability.mapper.InventoryTransferMapper;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.mapper.WarehouseMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-B 卡（P1-TRANSFER-UNMAPPED-REJECT-001）单测：调拨未映射仓显式拒绝。
 * <p>验收要点：①未映射源/目标仓 → 显式拒绝且<b>零副作用</b>（不写账、不写流水、单据不得置"已完成"）；
 * ②双侧已映射 → 正常双侧入账，`source_ref` 携带调拨单号。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryTransferServiceImplTest {

    private static final Long FROM_LOCATION = 5L;
    private static final Long TO_LOCATION = 4L;
    private static final Long MATERIAL_ID = 3L;

    @Mock
    private InventoryTransferMapper inventoryTransferMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private LocationService locationService;

    private InventoryTransferServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventoryTransferServiceImpl(inventoryTransferMapper, productMapper,
                warehouseMapper, inventoryService, locationService);
        ReflectionTestUtils.setField(service, "baseMapper", inventoryTransferMapper);
    }

    private InventoryTransfer approvedTransfer() {
        InventoryTransfer t = new InventoryTransfer();
        t.setTransferId(1L);
        t.setId(1L);
        t.setTransferCode("TR20260930001");
        t.setFromWarehouseId(1L);
        t.setToWarehouseId(2L);
        t.setProductId(MATERIAL_ID);
        t.setProductName("生菜");
        t.setTransferQuantity(new BigDecimal("10"));
        t.setTransferStatus(InventoryTransferStatus.APPROVED.getCode());
        return t;
    }

    private Location location(Long id) {
        Location l = new Location();
        l.setLocationId(id);
        return l;
    }

    // ============ 1. 双侧已映射：正常调拨（回归保护） ============

    @Test
    @DisplayName("2a 回归：双侧已映射 → 调出扣减/调入增加各一次，流水带调拨单号，单据置已完成")
    void executeBothMappedWritesBothSides() {
        InventoryTransfer t = approvedTransfer();
        when(inventoryTransferMapper.selectById(1L)).thenReturn(t);
        when(locationService.resolveByWarehouseId(1L)).thenReturn(location(FROM_LOCATION));
        when(locationService.resolveByWarehouseId(2L)).thenReturn(location(TO_LOCATION));
        when(inventoryTransferMapper.updateById(any(InventoryTransfer.class))).thenReturn(1);

        service.executeInventoryTransfer(1L);

        verify(inventoryService).decreaseStockAtLocation(eq(FROM_LOCATION), eq(MATERIAL_ID),
                eq(new BigDecimal("10")), eq("TRANSFER_OUT"), contains("TR20260930001"));
        verify(inventoryService).increaseStockAtLocation(eq(TO_LOCATION), eq(MATERIAL_ID), any(),
                eq(new BigDecimal("10")), any(), any(), eq("TRANSFER_IN"), contains("TR20260930001"));
        assertEquals(InventoryTransferStatus.COMPLETED.getCode(), t.getTransferStatus());
    }

    // ============ 2. 未映射源仓：显式拒绝 + 零副作用 ============

    @Test
    @DisplayName("2b：源仓未映射 → 显式拒绝，零写账零流水，单据不得置已完成")
    void executeUnmappedSourceRejected() {
        InventoryTransfer t = approvedTransfer();
        t.setFromWarehouseId(6L); // 默认主仓库，无 location_id_map 映射
        when(inventoryTransferMapper.selectById(1L)).thenReturn(t);
        when(locationService.resolveByWarehouseId(6L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.executeInventoryTransfer(1L));

        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), ex.getCode());
        verify(inventoryService, never()).decreaseStockAtLocation(any(), any(), any(), anyString(), anyString());
        verify(inventoryService, never()).increaseStockAtLocation(any(), any(), any(), any(), any(), any(), anyString(), anyString());
        verify(inventoryTransferMapper, never()).updateById(any(InventoryTransfer.class));
        assertEquals(InventoryTransferStatus.APPROVED.getCode(), t.getTransferStatus(), "单据须保持已审批，不得置已完成");
    }

    // ============ 3. 未映射目标仓：同样先拒绝、后写账（调出侧也不得扣减） ============

    @Test
    @DisplayName("2c：目标仓未映射 → 显式拒绝，调出侧也不得扣减（先拒绝后写账）")
    void executeUnmappedTargetRejected() {
        InventoryTransfer t = approvedTransfer();
        t.setToWarehouseId(3L); // 未映射仓库（与门店 3 同 ID）
        when(inventoryTransferMapper.selectById(1L)).thenReturn(t);
        when(locationService.resolveByWarehouseId(1L)).thenReturn(location(FROM_LOCATION));
        when(locationService.resolveByWarehouseId(3L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.executeInventoryTransfer(1L));

        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), ex.getCode());
        verify(inventoryService, never()).decreaseStockAtLocation(any(), any(), any(), anyString(), anyString());
        verify(inventoryService, never()).increaseStockAtLocation(any(), any(), any(), any(), any(), any(), anyString(), anyString());
        verify(inventoryTransferMapper, never()).updateById(any(InventoryTransfer.class));
    }

    // ============ 4. 缺物料：不得"静默跳过 + 置已完成" ============

    @Test
    @DisplayName("缺 productId → 显式拒绝（禁止假成功：跳过写账却把单据置已完成）")
    void executeNullProductRejected() {
        InventoryTransfer t = approvedTransfer();
        t.setProductId(null);
        when(inventoryTransferMapper.selectById(1L)).thenReturn(t);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.executeInventoryTransfer(1L));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(inventoryTransferMapper, never()).updateById(any(InventoryTransfer.class));
    }
}
