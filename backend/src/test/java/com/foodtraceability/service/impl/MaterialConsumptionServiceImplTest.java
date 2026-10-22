package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.foodtraceability.dto.MaterialScanConsumeDTO;
import com.foodtraceability.entity.MaterialConsumption;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.mapper.MaterialConsumptionMapper;
import com.foodtraceability.service.MaterialTraceCodeService;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MaterialConsumptionServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：扫码消耗记录的 ID 生成（MC+yyyyMMdd+4 位序号，跨日/首条边界）、溯源码不存在时返回 null、
 * 产品 ID 解析容错、消耗数量与门店名回退、消耗后回写溯源码、费用聚合委托、
 * 以及库存扣减的幂等闸门（未扣减才置 1）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialConsumptionServiceImplTest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Mock
    private MaterialConsumptionMapper materialConsumptionMapper;

    @Mock
    private ObjectProvider<MaterialTraceCodeService> materialTraceCodeServiceProvider;

    @Mock
    private MaterialTraceCodeService materialTraceCodeService;

    @InjectMocks
    private MaterialConsumptionServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, MaterialConsumption.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", materialConsumptionMapper);
        when(materialTraceCodeServiceProvider.getObject()).thenReturn(materialTraceCodeService);
        when(materialConsumptionMapper.insert(any(MaterialConsumption.class))).thenReturn(1);
        when(materialConsumptionMapper.updateById(any(MaterialConsumption.class))).thenReturn(1);
        when(materialConsumptionMapper.selectList(any())).thenReturn(List.of());
    }

    private static MaterialTraceCode traceCode() {
        MaterialTraceCode t = new MaterialTraceCode();
        t.setTraceCodeId("TC-1");
        t.setTraceCode("TRACE-001");
        t.setMaterialId("9527");
        t.setMaterialName("五花肉");
        t.setBatchNumber("B20261001");
        t.setQuantity(new BigDecimal("2.50"));
        t.setUnit("kg");
        t.setStorageLocation("冷藏库A");
        return t;
    }

    private static MaterialScanConsumeDTO dto() {
        MaterialScanConsumeDTO d = new MaterialScanConsumeDTO();
        d.setMaterialTraceCode("TRACE-001");
        d.setKitchenOrderId("KO-1");
        d.setOrderId("ORD-1");
        d.setDishId("D-1");
        d.setDishName("红烧肉");
        d.setOperatorId(7L);
        d.setOperatorName("张三");
        d.setScanDevice("PDA-01");
        d.setRemark("正常出餐");
        return d;
    }

    private static MaterialConsumption previous(String id) {
        MaterialConsumption c = new MaterialConsumption();
        c.setConsumptionId(id);
        return c;
    }

    // ==================== record ====================

    @Test
    @DisplayName("record：溯源码不存在时返回 null，不落库、不回写溯源码")
    void record_unknownTraceCode_returnsNull() {
        when(materialTraceCodeService.getByTraceCode("MISS")).thenReturn(null);

        MaterialScanConsumeDTO d = dto();
        d.setMaterialTraceCode("MISS");
        assertNull(service.record(d));

        verify(materialConsumptionMapper, never()).insert(any(MaterialConsumption.class));
        verify(materialTraceCodeService, never()).useTraceCode(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("record：生成 MC+日期+4 位序号（已有 0003 则下一条 0004），并回写溯源码")
    void record_generatesNextSequenceAndWritesBack() {
        String today = LocalDate.now().format(DATE_FORMAT);
        when(materialTraceCodeService.getByTraceCode("TRACE-001")).thenReturn(traceCode());
        when(materialConsumptionMapper.selectList(any())).thenReturn(List.of(previous("MC" + today + "0003")));

        MaterialConsumption result = service.record(dto());

        assertNotNull(result);
        assertEquals("MC" + today + "0004", result.getConsumptionId());
        assertEquals("KO-1", result.getKitchenOrderId());
        assertEquals("ORD-1", result.getOrderId());
        assertEquals("TC-1", result.getMaterialTraceCodeId());
        assertEquals(9527L, result.getProductId(), "materialId 为数字字符串时应转 Long");
        assertEquals("五花肉", result.getProductName());
        assertEquals("B20261001", result.getProductCode());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getUnitCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalCost()));
        assertEquals(0, result.getInventoryDeducted());
        assertNotNull(result.getConsumeTime());
        assertNotNull(result.getCreateTime());
        verify(materialConsumptionMapper).insert(result);
        verify(materialTraceCodeService).useTraceCode("TRACE-001", result.getConsumptionId(), 7L, "张三");
    }

    @Test
    @DisplayName("record：无历史记录时序号从 0001 起")
    void record_firstRecordOfDay_startsAt0001() {
        String today = LocalDate.now().format(DATE_FORMAT);
        when(materialTraceCodeService.getByTraceCode("TRACE-001")).thenReturn(traceCode());

        MaterialConsumption result = service.record(dto());

        assertEquals("MC" + today + "0001", result.getConsumptionId());
    }

    @Test
    @DisplayName("record：dto 未传数量时回退溯源码数量；门店名回退库位")
    void record_fallsBackToTraceQuantityAndStorageLocation() {
        when(materialTraceCodeService.getByTraceCode("TRACE-001")).thenReturn(traceCode());

        MaterialConsumption result = service.record(dto());

        assertEquals(0, new BigDecimal("2.50").compareTo(result.getConsumeQuantity()));
        assertEquals("冷藏库A", result.getStoreName());
        assertEquals("kg", result.getUnit());
    }

    @Test
    @DisplayName("record：dto 显式数量/门店名优先于溯源码")
    void record_dtoValuesWin() {
        when(materialTraceCodeService.getByTraceCode("TRACE-001")).thenReturn(traceCode());
        MaterialScanConsumeDTO d = dto();
        d.setConsumeQuantity(new BigDecimal("0.75"));
        d.setStoreId(576L);
        d.setStoreName("朝阳店");

        MaterialConsumption result = service.record(d);

        assertEquals(0, new BigDecimal("0.75").compareTo(result.getConsumeQuantity()));
        assertEquals(576L, result.getStoreId());
        assertEquals("朝阳店", result.getStoreName());
    }

    @Test
    @DisplayName("record：materialId 非数字时 productId 置 null（不抛异常）")
    void record_nonNumericMaterialId_setsNullProductId() {
        MaterialTraceCode t = traceCode();
        t.setMaterialId("MAT-ABC");
        when(materialTraceCodeService.getByTraceCode("TRACE-001")).thenReturn(t);

        MaterialConsumption result = service.record(dto());

        assertNull(result.getProductId());
        assertEquals("五花肉", result.getProductName());
    }

    // ==================== 查询/费用委托 ====================

    @Test
    @DisplayName("getByKitchenOrderId / getByOrderId / 费用聚合 全部委托 mapper")
    void queryAndCostMethods_delegate() {
        List<MaterialConsumption> byKitchen = List.of(previous("MC1"));
        List<MaterialConsumption> byOrder = List.of(previous("MC2"));
        when(materialConsumptionMapper.selectByKitchenOrderId("KO-1")).thenReturn(byKitchen);
        when(materialConsumptionMapper.selectByOrderId("ORD-1")).thenReturn(byOrder);
        when(materialConsumptionMapper.sumCostByOrderId("ORD-1")).thenReturn(new BigDecimal("12.34"));
        when(materialConsumptionMapper.sumCostByStoreAndTime(eq(576L), any(), any())).thenReturn(new BigDecimal("99.99"));
        when(materialConsumptionMapper.selectUndeducted()).thenReturn(List.of(previous("MC3")));

        assertSame(byKitchen, service.getByKitchenOrderId("KO-1"));
        assertSame(byOrder, service.getByOrderId("ORD-1"));
        assertEquals(new BigDecimal("12.34"), service.calculateOrderCost("ORD-1"));
        assertEquals(new BigDecimal("99.99"), service.calculateStoreCost(576L, null, null));
        assertEquals(1, service.getUndeductedRecords().size());
    }

    // ==================== 库存扣减 ====================

    @Test
    @DisplayName("deductInventory：记录不存在返回 false，不写库")
    void deductInventory_missing_returnsFalse() {
        when(materialConsumptionMapper.selectById("MC404")).thenReturn(null);

        assertFalse(service.deductInventory("MC404"));
        verify(materialConsumptionMapper, never()).updateById(any(MaterialConsumption.class));
    }

    @Test
    @DisplayName("deductInventory：已扣减（inventoryDeducted=1）幂等返回 false")
    void deductInventory_alreadyDeducted_returnsFalse() {
        MaterialConsumption c = previous("MC1");
        c.setInventoryDeducted(1);
        when(materialConsumptionMapper.selectById("MC1")).thenReturn(c);

        assertFalse(service.deductInventory("MC1"));
        verify(materialConsumptionMapper, never()).updateById(any(MaterialConsumption.class));
    }

    @Test
    @DisplayName("deductInventory：首次扣减置 1 并写扣减时间，返回 updateById 结果")
    void deductInventory_firstTime_marksDeducted() {
        MaterialConsumption c = previous("MC1");
        c.setInventoryDeducted(0);
        when(materialConsumptionMapper.selectById("MC1")).thenReturn(c);

        assertTrue(service.deductInventory("MC1"));

        assertEquals(1, c.getInventoryDeducted());
        assertNotNull(c.getInventoryDeductTime());
        assertNotNull(c.getUpdateTime());
        ArgumentCaptor<MaterialConsumption> captor = ArgumentCaptor.forClass(MaterialConsumption.class);
        verify(materialConsumptionMapper).updateById(captor.capture());
        assertSame(c, captor.getValue());
    }

    @Test
    @DisplayName("deductInventory：updateById 返回 false 时向上返回 false")
    void deductInventory_updateFails_returnsFalse() {
        MaterialConsumption c = previous("MC1");
        c.setInventoryDeducted(0);
        when(materialConsumptionMapper.selectById("MC1")).thenReturn(c);
        when(materialConsumptionMapper.updateById(any(MaterialConsumption.class))).thenReturn(0);

        assertFalse(service.deductInventory("MC1"));
    }

    @Test
    @DisplayName("batchDeductInventory：逐条扣减，单条失败不影响其余，整体恒返回 true（当前实现）")
    void batchDeductInventory_continuesOnMissingRecords() {
        MaterialConsumption ok = previous("MC1");
        ok.setInventoryDeducted(0);
        when(materialConsumptionMapper.selectById("MC1")).thenReturn(ok);
        when(materialConsumptionMapper.selectById("MC2")).thenReturn(null);

        assertTrue(service.batchDeductInventory(List.of("MC1", "MC2", "MC1")));

        verify(materialConsumptionMapper, org.mockito.Mockito.times(3)).selectById(anyString());
    }
}
