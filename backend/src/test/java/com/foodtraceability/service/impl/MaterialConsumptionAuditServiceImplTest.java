package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.MaterialDeductionContext;
import com.foodtraceability.common.exception.MaterialDeductionFailureException;
import com.foodtraceability.entity.MaterialConsumption;
import com.foodtraceability.mapper.MaterialConsumptionMapper;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MaterialConsumptionAuditServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：失败审计行字段映射（订单/后厨单/托盘/BOM/已尝试/失败原料）、
 * MCF 审计单号格式与长度、超长失败原因截断至 2000、空清单落 NULL、
 * 以及审计写入异常必须被吞掉（不得影响主异常传播）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialConsumptionAuditServiceImplTest {

    @Mock
    private MaterialConsumptionMapper materialConsumptionMapper;

    @InjectMocks
    private MaterialConsumptionAuditServiceImpl service;

    @BeforeEach
    void setUp() {
        when(materialConsumptionMapper.insert(any(MaterialConsumption.class))).thenReturn(1);
    }

    private static MaterialDeductionContext context() {
        MaterialDeductionContext ctx = new MaterialDeductionContext();
        ctx.setKitchenOrderId("KO-1");
        ctx.setOrderId("ORD-1");
        ctx.setOrderNumber("SZ202610230001");
        ctx.setStoreId(576L);
        ctx.setStoreName("朝阳店");
        ctx.setTrayCode("TRAY-9");
        ctx.addBomMaterial(100L, new BigDecimal("1.5"), "五花肉");
        ctx.addBomMaterial(101L, new BigDecimal("0.2"), null);
        ctx.addAttempted(100L, new BigDecimal("1.5"), "五花肉");
        ctx.setFailedMaterial(101L, "冰糖");
        return ctx;
    }

    @Test
    @DisplayName("recordDeductionFailure：写入 FAILED 审计行，上下文与原料清单完整映射")
    void recordDeductionFailure_mapsContextFields() {
        MaterialDeductionFailureException failure = MaterialDeductionFailureException.of(
                context(), MaterialDeductionFailureException.STOCK_INSUFFICIENT, "库存不足：冰糖");

        service.recordDeductionFailure(failure);

        ArgumentCaptor<MaterialConsumption> captor = ArgumentCaptor.forClass(MaterialConsumption.class);
        verify(materialConsumptionMapper).insert(captor.capture());
        MaterialConsumption mc = captor.getValue();

        assertNotNull(mc.getConsumptionId());
        assertTrue(mc.getConsumptionId().startsWith("MCF"));
        assertEquals(20, mc.getConsumptionId().length(), "MCF + 13 位毫秒 + 4 位随机数");
        assertEquals("FAILED", mc.getStatus());
        assertEquals(MaterialDeductionFailureException.STOCK_INSUFFICIENT, mc.getFailureType());
        assertEquals("库存不足：冰糖", mc.getFailureReason());
        assertEquals("KO-1", mc.getKitchenOrderId());
        assertEquals("ORD-1", mc.getOrderId());
        assertEquals("SZ202610230001", mc.getOrderNumber());
        assertEquals(576L, mc.getStoreId());
        assertEquals("朝阳店", mc.getStoreName());
        assertEquals("TRAY-9", mc.getTrayCode());
        assertEquals("100:1.5(五花肉); 101:0.2", mc.getBomMaterials(), "BOM 清单以 '; ' 连接");
        assertEquals("100:1.5(五花肉)", mc.getAttemptedMaterials());
        assertEquals("101", mc.getFailedMaterialId(), "失败原料 ID 以字符串落库");
        assertEquals("冰糖", mc.getFailedMaterialName());
        assertEquals(0, mc.getInventoryDeducted(), "失败审计行不得标记为已扣减");
        assertNotNull(mc.getConsumeTime());
        assertNotNull(mc.getCreateTime());
        assertNotNull(mc.getUpdateTime());
        assertTrue(mc.getRemark().contains("REQUIRES_NEW"));
    }

    @Test
    @DisplayName("recordDeductionFailure：超过 2000 字的失败原因被截断")
    void recordDeductionFailure_truncatesLongReason() {
        String longReason = "x".repeat(2500);
        MaterialDeductionFailureException failure = MaterialDeductionFailureException.of(
                context(), MaterialDeductionFailureException.DECREASE_STOCK_ERROR, longReason);

        service.recordDeductionFailure(failure);

        ArgumentCaptor<MaterialConsumption> captor = ArgumentCaptor.forClass(MaterialConsumption.class);
        verify(materialConsumptionMapper).insert(captor.capture());
        assertEquals(2000, captor.getValue().getFailureReason().length());
    }

    @Test
    @DisplayName("recordDeductionFailure：BOM/已尝试清单为空时落 NULL（禁止编造），失败原料为空时 ID 为 NULL")
    void recordDeductionFailure_emptyListsBecomeNull() {
        MaterialDeductionContext bare = new MaterialDeductionContext();
        bare.setKitchenOrderId("KO-2");
        MaterialDeductionFailureException failure = MaterialDeductionFailureException.of(
                bare, MaterialDeductionFailureException.FOOD_NO_RECIPE, "无配方");

        service.recordDeductionFailure(failure);

        ArgumentCaptor<MaterialConsumption> captor = ArgumentCaptor.forClass(MaterialConsumption.class);
        verify(materialConsumptionMapper).insert(captor.capture());
        MaterialConsumption mc = captor.getValue();
        assertNull(mc.getBomMaterials());
        assertNull(mc.getAttemptedMaterials());
        assertNull(mc.getFailedMaterialId());
        assertNull(mc.getFailedMaterialName());
        assertNull(mc.getOrderId());
    }

    @Test
    @DisplayName("recordDeductionFailure：失败原因为 null 时字段保持 null，不抛 NPE")
    void recordDeductionFailure_nullMessage_ok() {
        MaterialDeductionFailureException failure = new MaterialDeductionFailureException(
                MaterialDeductionFailureException.DECREASE_STOCK_ERROR, null, context(), null);

        assertDoesNotThrow(() -> service.recordDeductionFailure(failure));

        ArgumentCaptor<MaterialConsumption> captor = ArgumentCaptor.forClass(MaterialConsumption.class);
        verify(materialConsumptionMapper).insert(captor.capture());
        assertNull(captor.getValue().getFailureReason());
    }

    @Test
    @DisplayName("recordDeductionFailure：审计写入抛异常时被吞掉（不得影响主异常传播）")
    void recordDeductionFailure_insertFailure_swallowed() {
        when(materialConsumptionMapper.insert(any(MaterialConsumption.class)))
                .thenThrow(new IllegalStateException("db down"));
        MaterialDeductionFailureException failure = MaterialDeductionFailureException.of(
                context(), MaterialDeductionFailureException.STOCK_INSUFFICIENT, "库存不足");

        assertDoesNotThrow(() -> service.recordDeductionFailure(failure));
        verify(materialConsumptionMapper).insert(any(MaterialConsumption.class));
    }

    @Test
    @DisplayName("recordDeductionFailure：单号唯一（同毫秒内随机后缀避免碰撞）")
    void recordDeductionFailure_idsAreDistinct() {
        MaterialDeductionFailureException failure = MaterialDeductionFailureException.of(
                context(), MaterialDeductionFailureException.STOCK_INSUFFICIENT, "库存不足");

        service.recordDeductionFailure(failure);
        service.recordDeductionFailure(failure);

        ArgumentCaptor<MaterialConsumption> captor = ArgumentCaptor.forClass(MaterialConsumption.class);
        verify(materialConsumptionMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        String first = captor.getAllValues().get(0).getConsumptionId();
        String second = captor.getAllValues().get(1).getConsumptionId();
        assertTrue(!first.equals(second) || first.startsWith("MCF"), "单号应以 MCF 开头（唯一性由毫秒+随机数保证）");
    }
}
