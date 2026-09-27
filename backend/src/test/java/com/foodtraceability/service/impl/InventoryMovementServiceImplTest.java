package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.InventoryMovement;
import com.foodtraceability.mapper.InventoryMovementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M3-M4 S5 单测：InventoryMovementService 统一流水严格校验。
 * 覆盖：无来源/缺字段拒绝（宪法 §IV.4 红线 + §III.4 禁止默认兜底）、
 * happy path（createTime 默认、单次 insert）、异常向上传播（由调用方降级）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryMovementServiceImplTest {

    @Mock
    private InventoryMovementMapper inventoryMovementMapper;

    private InventoryMovementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventoryMovementServiceImpl(inventoryMovementMapper);
    }

    private InventoryMovement validMovement() {
        InventoryMovement mv = new InventoryMovement();
        mv.setLocationId(1L);
        mv.setMaterialId(100L);
        mv.setChangeQty(new BigDecimal("5"));
        mv.setBalanceAfter(new BigDecimal("5"));
        mv.setMovementType("IN");
        mv.setSourceType("PURCHASE_STOCKIN");
        mv.setSourceRef("SI202609280001");
        mv.setUnitCost(300L);
        mv.setTotalCost(1500L);
        return mv;
    }

    private BusinessException expectRejected(InventoryMovement mv, String msgPart) {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.recordMovement(mv));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        if (msgPart != null) {
            assertNotNull(ex.getMessage());
        }
        verify(inventoryMovementMapper, never()).insert(any(InventoryMovement.class));
        return ex;
    }

    @Test
    @DisplayName("拒绝：流水记录为 null")
    void rejectsNullMovement() {
        expectRejected(null, "流水记录为空");
    }

    @Test
    @DisplayName("拒绝：location_id 缺失")
    void rejectsMissingLocationId() {
        InventoryMovement mv = validMovement();
        mv.setLocationId(null);
        expectRejected(mv, "location_id");
    }

    @Test
    @DisplayName("拒绝：material_id 缺失")
    void rejectsMissingMaterialId() {
        InventoryMovement mv = validMovement();
        mv.setMaterialId(null);
        expectRejected(mv, "material_id");
    }

    @Test
    @DisplayName("拒绝：change_qty 缺失")
    void rejectsMissingChangeQty() {
        InventoryMovement mv = validMovement();
        mv.setChangeQty(null);
        expectRejected(mv, "change_qty");
    }

    @Test
    @DisplayName("拒绝：movement_type 空白")
    void rejectsBlankMovementType() {
        InventoryMovement mv = validMovement();
        mv.setMovementType("  ");
        expectRejected(mv, "movement_type");
    }

    @Test
    @DisplayName("拒绝：source_type 缺失（宪法 §IV.4 无来源流水红线）")
    void rejectsMissingSourceType() {
        InventoryMovement mv = validMovement();
        mv.setSourceType(null);
        expectRejected(mv, "source_type");
    }

    @Test
    @DisplayName("拒绝：source_ref 空白（宪法 §IV.4 无来源流水红线）")
    void rejectsBlankSourceRef() {
        InventoryMovement mv = validMovement();
        mv.setSourceRef("   ");
        expectRejected(mv, "source_ref");
    }

    @Test
    @DisplayName("happy path：合法流水写入一次，createTime 缺省补当前时间，返回同一实例")
    void recordsValidMovement() {
        when(inventoryMovementMapper.insert(any(InventoryMovement.class))).thenReturn(1);
        InventoryMovement mv = validMovement();

        InventoryMovement result = service.recordMovement(mv);

        assertSame(mv, result);
        assertNotNull(mv.getCreateTime());
        verify(inventoryMovementMapper, times(1)).insert(mv);
    }

    @Test
    @DisplayName("异常传播：mapper insert 抛异常时向上传播（调用方 writeMovement 负责降级）")
    void propagatesMapperException() {
        when(inventoryMovementMapper.insert(any(InventoryMovement.class)))
                .thenThrow(new RuntimeException("DB down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.recordMovement(validMovement()));
        assertEquals("DB down", ex.getMessage());
        verify(inventoryMovementMapper, times(1)).insert(any(InventoryMovement.class));
    }
}
