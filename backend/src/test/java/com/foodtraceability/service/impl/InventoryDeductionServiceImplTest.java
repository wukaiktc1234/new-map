package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.InventoryDeductionDTO;
import com.foodtraceability.entity.DishRecipeNew;
import com.foodtraceability.entity.FoodTraceCode;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.entity.MaterialUsageRecord;
import com.foodtraceability.mapper.DishRecipeNewMapper;
import com.foodtraceability.mapper.FoodTraceCodeMapper;
import com.foodtraceability.mapper.MaterialTraceCodeMapper;
import com.foodtraceability.mapper.MaterialUsageRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventoryDeductionServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：模式A（扫追溯码，单次最多扣 1）的可用量回退链（available→quantity→weight）、
 * 扣完置 used + useTime、使用流水落库；模式B（按配方）逐原料折算并回写菜品追溯码 JSON；
 * 混合模式A（按批次跨码消耗、不足仅告警）；混合模式B（按名称登记）；以及内存扣减记录的读写。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryDeductionServiceImplTest {

    @Mock
    private MaterialTraceCodeMapper materialTraceCodeMapper;

    @Mock
    private DishRecipeNewMapper dishRecipeNewMapper;

    @Mock
    private FoodTraceCodeMapper foodTraceCodeMapper;

    @Mock
    private MaterialUsageRecordMapper materialUsageRecordMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InventoryDeductionServiceImpl service;

    @BeforeEach
    void setUp() {
        when(materialTraceCodeMapper.selectOne(any())).thenReturn(null);
        when(materialTraceCodeMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(materialTraceCodeMapper.updateById(any(MaterialTraceCode.class))).thenReturn(1);
        when(dishRecipeNewMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(foodTraceCodeMapper.selectOne(any())).thenReturn(null);
        when(foodTraceCodeMapper.updateById(any(FoodTraceCode.class))).thenReturn(1);
        when(materialUsageRecordMapper.insert(any(MaterialUsageRecord.class))).thenReturn(1);
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        } catch (Exception ignored) {
            // 序列化桩失败不影响测试构造
        }
    }

    private static MaterialTraceCode trace(String code, String available, String quantity, String weight) {
        MaterialTraceCode t = new MaterialTraceCode();
        t.setTraceCodeId("TC-1");
        t.setTraceCode(code);
        t.setMaterialId("100");
        t.setMaterialName("五花肉");
        t.setUnit("kg");
        t.setWeightUnit("g");
        t.setStatus("available");
        t.setAvailableQuantity(available == null ? null : new BigDecimal(available));
        t.setQuantity(quantity == null ? null : new BigDecimal(quantity));
        t.setWeight(weight == null ? null : new BigDecimal(weight));
        return t;
    }

    private static DishRecipeNew recipe(String ingredientId, String name, String qty, String unit) {
        DishRecipeNew r = new DishRecipeNew();
        r.setFoodId(1L);
        try {
            r.setMaterialId(Long.valueOf(ingredientId));
        } catch (NumberFormatException e) {
            r.setMaterialId(0L);
        }
        r.setMaterialName(name);
        r.setRequiredQuantity(new java.math.BigDecimal(qty));
        r.setUnit(unit);
        return r;
    }

    // ==================== 模式A：按追溯码 ====================

    @Test
    @DisplayName("deductByTraceCode：追溯码不存在 / 已用尽 / 无可用数量 三种拒绝")
    void deductByTraceCode_rejections() {
        RuntimeException notFound = assertThrows(RuntimeException.class,
                () -> service.deductByTraceCode("MISS", "KO-1", "U1", "张三"));
        assertTrue(notFound.getMessage().contains("追溯码不存在"));

        MaterialTraceCode used = trace("T1", "5", null, null);
        used.setStatus("used");
        when(materialTraceCodeMapper.selectOne(any())).thenReturn(used);
        RuntimeException alreadyUsed = assertThrows(RuntimeException.class,
                () -> service.deductByTraceCode("T1", "KO-1", "U1", "张三"));
        assertTrue(alreadyUsed.getMessage().contains("已使用完毕"));

        when(materialTraceCodeMapper.selectOne(any())).thenReturn(trace("T2", "0", null, null));
        RuntimeException noQty = assertThrows(RuntimeException.class,
                () -> service.deductByTraceCode("T2", "KO-1", "U1", "张三"));
        assertTrue(noQty.getMessage().contains("无可用数量"));

        verify(materialTraceCodeMapper, never()).updateById(any(MaterialTraceCode.class));
        verify(materialUsageRecordMapper, never()).insert(any(MaterialUsageRecord.class));
    }

    @Test
    @DisplayName("deductByTraceCode：可用 3 → 本次扣 1，剩余 2，状态不变，流水与内存记录同步")
    void deductByTraceCode_partialDeduction() {
        MaterialTraceCode t = trace("T1", "3", null, null);
        when(materialTraceCodeMapper.selectOne(any())).thenReturn(t);

        service.deductByTraceCode("T1", "KO-1", "U1", "张三");

        assertEquals(0, new BigDecimal("2").compareTo(t.getAvailableQuantity()));
        assertEquals(0, BigDecimal.ONE.compareTo(t.getUsedQuantity()));
        assertEquals("available", t.getStatus());
        assertNull(t.getUseTime());
        verify(materialTraceCodeMapper).updateById(t);

        ArgumentCaptor<MaterialUsageRecord> captor = ArgumentCaptor.forClass(MaterialUsageRecord.class);
        verify(materialUsageRecordMapper).insert(captor.capture());
        MaterialUsageRecord record = captor.getValue();
        assertTrue(record.getRecordId().startsWith("UR"));
        assertEquals("TC-1", record.getTraceCodeId());
        assertEquals("T1", record.getTraceCode());
        assertEquals("100", record.getMaterialId());
        assertEquals("五花肉", record.getMaterialName());
        assertEquals(0, BigDecimal.ONE.compareTo(record.getUsedQuantity()));
        assertEquals("kg", record.getUnit());
        assertEquals("KO-1", record.getKitchenOrderId());
        assertEquals("U1", record.getOperatorId());
        assertNotNull(record.getUsageTime());

        InventoryDeductionDTO deduction = service.getDeductionRecord("KO-1");
        assertNotNull(deduction);
        assertEquals(1, deduction.getModeAItems().size());
        assertEquals(0, BigDecimal.ONE.compareTo(deduction.getModeAItems().get(0).getQuantity()));
    }

    @Test
    @DisplayName("deductByTraceCode：可用 0.5 → 扣完置 used 并写 useTime")
    void deductByTraceCode_exhausted_marksUsed() {
        MaterialTraceCode t = trace("T1", "0.5", null, null);
        when(materialTraceCodeMapper.selectOne(any())).thenReturn(t);

        service.deductByTraceCode("T1", "KO-1", "U1", "张三");

        assertEquals(0, BigDecimal.ZERO.compareTo(t.getAvailableQuantity()));
        assertEquals("used", t.getStatus());
        assertNotNull(t.getUseTime());
    }

    @Test
    @DisplayName("deductByTraceCode：available 为空时回退 quantity，再回退 weight")
    void deductByTraceCode_quantityAndWeightFallback() {
        MaterialTraceCode fromQuantity = trace("T1", null, "2", null);
        when(materialTraceCodeMapper.selectOne(any())).thenReturn(fromQuantity);
        service.deductByTraceCode("T1", "KO-1", "U1", "张三");
        assertEquals(0, BigDecimal.ONE.compareTo(fromQuantity.getAvailableQuantity()));

        MaterialTraceCode fromWeight = trace("T2", null, null, "0.8");
        when(materialTraceCodeMapper.selectOne(any())).thenReturn(fromWeight);
        service.deductByTraceCode("T2", "KO-2", "U1", "张三");
        assertEquals(0, BigDecimal.ZERO.compareTo(fromWeight.getAvailableQuantity()));
        assertEquals("used", fromWeight.getStatus());
    }

    @Test
    @DisplayName("deductByTraceCode：unit 为空时流水单位回退 weightUnit")
    void deductByTraceCode_unitFallsBackToWeightUnit() {
        MaterialTraceCode t = trace("T1", "3", null, null);
        t.setUnit(null);
        when(materialTraceCodeMapper.selectOne(any())).thenReturn(t);

        service.deductByTraceCode("T1", "KO-1", "U1", "张三");

        ArgumentCaptor<MaterialUsageRecord> captor = ArgumentCaptor.forClass(MaterialUsageRecord.class);
        verify(materialUsageRecordMapper).insert(captor.capture());
        assertEquals("g", captor.getValue().getUnit());
    }

    // ==================== 模式B：按配方 ====================

    @Test
    @DisplayName("deductByRecipe：无配方时直接返回，不产生任何写入")
    void deductByRecipe_noRecipes_returnsEarly() {
        service.deductByRecipe("1", 2, "KO-1", "FTC-1");

        verify(materialUsageRecordMapper, never()).insert(any(MaterialUsageRecord.class));
        verify(foodTraceCodeMapper, never()).updateById(any(FoodTraceCode.class));
        assertNull(service.getDeductionRecord("KO-1"));
    }

    @Test
    @DisplayName("deductByRecipe：按 配方用量×份数 登记明细，并回写菜品追溯码 JSON")
    void deductByRecipe_computesQuantitiesAndPersistsJson() {
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(
                recipe("100", "五花肉", "0.2", "kg"),
                recipe("101", "冰糖", "0.05", "kg")
        ));
        FoodTraceCode foodTraceCode = new FoodTraceCode();
        foodTraceCode.setTraceCodeId("FTC-1");
        when(foodTraceCodeMapper.selectOne(any())).thenReturn(foodTraceCode);

        service.deductByRecipe("1", 3, "KO-1", "FTC-1");

        InventoryDeductionDTO deduction = service.getDeductionRecord("KO-1");
        assertNotNull(deduction);
        assertEquals(2, deduction.getModeBItems().size());
        assertEquals(0, new BigDecimal("0.6").compareTo(deduction.getModeBItems().get(0).getQuantity()), "0.2×3");
        assertEquals(0, new BigDecimal("0.15").compareTo(deduction.getModeBItems().get(1).getQuantity()), "0.05×3");

        ArgumentCaptor<FoodTraceCode> captor = ArgumentCaptor.forClass(FoodTraceCode.class);
        verify(foodTraceCodeMapper).updateById(captor.capture());
        assertEquals("{}", captor.getValue().getInventoryDeduction());
        assertEquals(deduction, service.getDeductionRecord("FTC-1"), "回写后按菜品追溯码也可查到");
    }

    @Test
    @DisplayName("deductByRecipe：菜品追溯码不存在时只登记内存记录，不写库")
    void deductByRecipe_missingFoodTraceCode_onlyRecordsInMemory() {
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(recipe("100", "五花肉", "1", "kg")));
        when(foodTraceCodeMapper.selectOne(any())).thenReturn(null);

        service.deductByRecipe("1", 1, "KO-9", "FTC-X");

        verify(foodTraceCodeMapper, never()).updateById(any(FoodTraceCode.class));
        assertNotNull(service.getDeductionRecord("KO-9"));
    }

    // ==================== 混合模式 ====================

    @Test
    @DisplayName("deductByBatchCode：无可用批次抛异常")
    void deductByBatchCode_noAvailableBatch_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.deductByBatchCode("100", "B1", BigDecimal.ONE, "KO-1", "U1", "张三"));

        assertTrue(ex.getMessage().contains("未找到可用的批次"));
    }

    @Test
    @DisplayName("deductByBatchCode：跨多个追溯码依次扣减直到满足数量，全部扣完各自置 used")
    void deductByBatchCode_consumesAcrossTraceCodes() {
        MaterialTraceCode first = trace("T1", "2", null, null);
        MaterialTraceCode second = trace("T2", "5", null, null);
        second.setTraceCodeId("TC-2");
        when(materialTraceCodeMapper.selectList(any())).thenReturn(List.of(first, second));

        service.deductByBatchCode("100", "B1", new BigDecimal("4"), "KO-1", "U1", "张三");

        assertEquals(0, BigDecimal.ZERO.compareTo(first.getAvailableQuantity()));
        assertEquals("used", first.getStatus());
        assertEquals(0, new BigDecimal("3").compareTo(second.getAvailableQuantity()));
        assertEquals("available", second.getStatus());
        verify(materialTraceCodeMapper, times(2)).updateById(any(MaterialTraceCode.class));
        verify(materialUsageRecordMapper, times(2)).insert(any(MaterialUsageRecord.class));

        InventoryDeductionDTO deduction = service.getDeductionRecord("KO-1");
        assertEquals(2, deduction.getModeAItems().size());
    }

    @Test
    @DisplayName("deductByBatchCode：批次合计不足时抛业务异常（P1-1 不再假成功），异常带缺口数量")
    void deductByBatchCode_insufficient_throws() {
        when(materialTraceCodeMapper.selectList(any())).thenReturn(List.of(trace("T1", "1", null, null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deductByBatchCode("100", "B1", new BigDecimal("5"), "KO-1", "U1", "张三"));
        assertTrue(ex.getMessage().contains("批次库存不足"), "异常信息必须说明库存不足，实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("4"), "异常信息应带出缺口数量 4，实际: " + ex.getMessage());
    }

    @Test
    @DisplayName("deductByBatchCode：可用量为 0 的追溯码被跳过")
    void deductByBatchCode_skipsZeroAvailable() {
        MaterialTraceCode zero = trace("T0", "0", null, null);
        MaterialTraceCode ok = trace("T1", "2", null, null);
        when(materialTraceCodeMapper.selectList(any())).thenReturn(List.of(zero, ok));

        service.deductByBatchCode("100", "B1", BigDecimal.ONE, "KO-1", "U1", "张三");

        verify(materialTraceCodeMapper, times(1)).updateById(ok);
        verify(materialUsageRecordMapper, times(1)).insert(any(MaterialUsageRecord.class));
    }

    @Test
    @DisplayName("deductByMaterialName：仅登记一条模式B明细，materialId 为空、单位占位「单位」")
    void deductByMaterialName_recordsOnly() {
        service.deductByMaterialName("土豆", new BigDecimal("2.5"), "KO-7", "U1", "张三");

        InventoryDeductionDTO deduction = service.getDeductionRecord("KO-7");
        assertNotNull(deduction);
        assertEquals(1, deduction.getModeBItems().size());
        assertNull(deduction.getModeBItems().get(0).getMaterialId());
        assertEquals("土豆", deduction.getModeBItems().get(0).getMaterialName());
        assertEquals("单位", deduction.getModeBItems().get(0).getUnit());
        assertEquals(0, new BigDecimal("2.5").compareTo(deduction.getModeBItems().get(0).getQuantity()));
        verify(materialUsageRecordMapper, never()).insert(any(MaterialUsageRecord.class));
    }

    @Test
    @DisplayName("getDeductionRecord：未登记的 key 返回 null")
    void getDeductionRecord_missing_returnsNull() {
        assertNull(service.getDeductionRecord("NOPE"));
    }
}
