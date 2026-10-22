package com.foodtraceability.service.impl;

import com.foodtraceability.dto.product.StockForecastItemVO;
import com.foodtraceability.dto.product.StockForecastVO;
import com.foodtraceability.entity.DishRecipeNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.DishRecipeNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.StockForecastMapper;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link StockForecastServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：可做份数（实际用量 vs 理论用量双口径）、瓶颈原料标记、数据不足时回退理论用量、
 * 窗口天数上下限钳制（1..90）、售罄天数与建议补货量、以及消耗差异分析的排序/截断/脏数据容错。</p>
 *
 * <p>说明：单测环境无登录态，{@code SecurityUtils.getCurrentUserStoreId()} 返回 null，
 * 因此 mapper 以 storeId=null 调用——这正是「未归属门店」分支，属于需要覆盖的边界。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockForecastServiceImplTest {

    @Mock
    private FoodNewMapper foodNewMapper;

    @Mock
    private DishRecipeNewMapper dishRecipeNewMapper;

    @Mock
    private StockForecastMapper forecastMapper;

    @InjectMocks
    private StockForecastServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        when(foodNewMapper.selectById(any())).thenReturn(null);
        when(dishRecipeNewMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(forecastMapper.getDishSoldServings(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(forecastMapper.getAvailableStock(any(), any())).thenReturn(BigDecimal.ZERO);
        when(forecastMapper.getSafetyStock(any(), any())).thenReturn(BigDecimal.ZERO);
        when(forecastMapper.getActualConsumed(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(forecastMapper.getSoldServings(any(), any(), any())).thenReturn(BigDecimal.ZERO);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static DishRecipeNew recipe(Long materialId, String name, String qty, String unit) {
        DishRecipeNew r = new DishRecipeNew();
        r.setFoodId(100L);
        r.setMaterialId(materialId);
        r.setMaterialName(name);
        r.setRequiredQuantity(qty == null ? null : new BigDecimal(qty));
        r.setUnit(unit);
        r.setDeleted(0);
        return r;
    }

    private static FoodNew food(String name) {
        FoodNew f = new FoodNew();
        f.setFoodName(name);
        return f;
    }

    // ==================== forecast ====================

    @Test
    @DisplayName("forecast：实际用量口径优先，瓶颈取最小可做份数，差异率=实际/理论-1")
    void forecast_withSufficientData_computesBottleneckAndLossRate() {
        when(foodNewMapper.selectById(100L)).thenReturn(food("红烧肉"));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(
                recipe(1L, "五花肉", "0.2", "kg"),
                recipe(2L, "冰糖", "0.5", "kg")
        ));
        // 原料1：库存 100，安全库存 30，实际消耗 25，售出 100 份 → 单份 0.25kg，损耗率 +25%
        when(forecastMapper.getAvailableStock(eq(1L), any())).thenReturn(new BigDecimal("100"));
        when(forecastMapper.getSafetyStock(eq(1L), any())).thenReturn(new BigDecimal("30"));
        when(forecastMapper.getActualConsumed(eq(1L), any(), any())).thenReturn(new BigDecimal("25"));
        when(forecastMapper.getSoldServings(eq(1L), any(), any())).thenReturn(new BigDecimal("100"));
        // 原料2：库存 10，安全库存 5，实际消耗 60，售出 100 份 → 单份 0.6kg → 只能做 16 份（瓶颈）
        when(forecastMapper.getAvailableStock(eq(2L), any())).thenReturn(new BigDecimal("10"));
        when(forecastMapper.getSafetyStock(eq(2L), any())).thenReturn(new BigDecimal("5"));
        when(forecastMapper.getActualConsumed(eq(2L), any(), any())).thenReturn(new BigDecimal("60"));
        when(forecastMapper.getSoldServings(eq(2L), any(), any())).thenReturn(new BigDecimal("100"));
        when(forecastMapper.getDishSoldServings(eq(100L), any(), any())).thenReturn(new BigDecimal("32"));

        StockForecastVO vo = service.forecast(100L, 7);

        assertEquals(100L, vo.getDishId());
        assertEquals("红烧肉", vo.getDishName());
        assertEquals(7, vo.getDataWindowDays());
        assertEquals(2, vo.getItems().size());
        assertTrue(vo.getCanMake());
        assertTrue(vo.getHasSufficientData());
        assertEquals(16, vo.getMaxServings(), "瓶颈原料决定可做份数");
        assertEquals(20, vo.getTheoreticalServings(), "理论口径按配方用量：10/0.5=20");
        assertEquals("冰糖", vo.getBottleneckMaterialName());
        assertEquals(new BigDecimal("4.57"), vo.getAvgDailySales(), "32 份 / 7 天，保留 2 位");
        assertEquals(new BigDecimal("3.5"), vo.getDaysToSellout(), "16 / 4.57 保留 1 位");
        assertEquals(0, vo.getSuggestedRestock(), "瓶颈库存 10 > 安全库存 5，无需补货");

        StockForecastItemVO pork = vo.getItems().get(0);
        assertEquals(0, new BigDecimal("0.25").compareTo(pork.getActualQtyPerServing()));
        assertEquals(0, new BigDecimal("0.25").compareTo(pork.getImpliedLossRate()), "0.25/0.2-1 = 0.25");
        assertEquals(400, pork.getMaxServingsByThis());
        assertFalse(pork.getIsBottleneck());

        StockForecastItemVO sugar = vo.getItems().get(1);
        assertEquals(16, sugar.getMaxServingsByThis());
        assertTrue(sugar.getIsBottleneck());
    }

    @Test
    @DisplayName("forecast：销量/消耗为 0 时回退理论用量，hasSufficientData=false，损耗率 0")
    void forecast_insufficientData_fallsBackToRecipeQty() {
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(recipe(1L, "盐", "0.01", "kg")));
        when(forecastMapper.getAvailableStock(eq(1L), any())).thenReturn(new BigDecimal("1"));
        when(forecastMapper.getSoldServings(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(forecastMapper.getActualConsumed(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);

        StockForecastVO vo = service.forecast(100L, 7);

        assertFalse(vo.getHasSufficientData());
        StockForecastItemVO item = vo.getItems().get(0);
        assertEquals(0, new BigDecimal("0.01").compareTo(item.getActualQtyPerServing()), "数据不足时按配方用量");
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getImpliedLossRate()));
        assertEquals(100, item.getMaxServingsByThis(), "1 / 0.01 = 100");
    }

    @Test
    @DisplayName("forecast：无配方 → 空明细、canMake=false、各汇总为 0、瓶颈名为空")
    void forecast_noRecipes_returnsEmptyResult() {
        when(foodNewMapper.selectById(100L)).thenReturn(food("空配方菜品"));

        StockForecastVO vo = service.forecast(100L, 7);

        assertTrue(vo.getItems().isEmpty());
        assertFalse(vo.getCanMake());
        assertEquals(0, vo.getMaxServings());
        assertEquals(0, vo.getTheoreticalServings());
        assertEquals(0, vo.getSuggestedRestock());
        assertEquals("", vo.getBottleneckMaterialName());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getDaysToSellout()));
        assertNull(vo.getBottleneckMaterialName() == null ? null : null);
    }

    @Test
    @DisplayName("forecast：菜品不存在时 dishName 为空串；库存为 null 时按 0 处理且不可制作")
    void forecast_missingDishAndNullStock_handled() {
        when(foodNewMapper.selectById(100L)).thenReturn(null);
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(recipe(1L, "盐", "0.01", "kg")));
        when(forecastMapper.getAvailableStock(eq(1L), any())).thenReturn(null);
        when(forecastMapper.getSafetyStock(eq(1L), any())).thenReturn(new BigDecimal("5"));

        StockForecastVO vo = service.forecast(100L, 7);

        assertEquals("", vo.getDishName());
        assertFalse(vo.getCanMake());
        assertEquals(0, vo.getMaxServings());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getItems().get(0).getAvailableStock()));
        assertEquals(5, vo.getSuggestedRestock(), "库存 0、安全库存 5 → 建议补货 5");
    }

    @Test
    @DisplayName("forecast：配方材料 ID 为 null 的脏数据被跳过")
    void forecast_nullMaterialIdRecipe_skipped() {
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(
                recipe(null, "脏数据", "1", "kg"),
                recipe(2L, "正常", "1", "kg")
        ));

        StockForecastVO vo = service.forecast(100L, 7);

        assertEquals(1, vo.getItems().size());
        assertEquals(2L, vo.getItems().get(0).getMaterialId());
    }

    @Test
    @DisplayName("forecast：窗口天数钳制在 1..90（null/0 → 7；超大 → 90）")
    void forecast_windowClamped() {
        assertEquals(7, service.forecast(100L, null).getDataWindowDays());
        assertEquals(7, service.forecast(100L, 0).getDataWindowDays());
        assertEquals(7, service.forecast(100L, -5).getDataWindowDays());
        assertEquals(1, service.forecast(100L, 1).getDataWindowDays());
        assertEquals(90, service.forecast(100L, 365).getDataWindowDays());
        assertEquals(30, service.forecast(100L, 30).getDataWindowDays());
    }

    // ==================== varianceAnalysis ====================

    @Test
    @DisplayName("varianceAnalysis：差异量=（实际-理论），按绝对值降序，超耗在前")
    void varianceAnalysis_sortsByAbsoluteVariance() {
        when(forecastMapper.getTheoreticalConsumedGrouped(any(), any())).thenReturn(List.of(
                row(1L, "猪肉", "kg", "20", "100"),
                row(2L, "牛肉", "kg", "10", "50")
        ));
        when(forecastMapper.getActualConsumed(eq(1L), any(), any())).thenReturn(new BigDecimal("25"));
        when(forecastMapper.getActualConsumed(eq(2L), any(), any())).thenReturn(BigDecimal.ZERO);

        List<Map<String, Object>> result = service.varianceAnalysis(7, 20);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).get("materialId"), "|0-10|=10 大于 |25-20|=5，牛肉排前");
        assertEquals(new BigDecimal("-10"), result.get(0).get("varianceQty"));
        assertEquals(1L, result.get(1).get("materialId"));
        assertEquals(new BigDecimal("5"), result.get(1).get("varianceQty"));
        assertEquals(0, new BigDecimal("0.25").compareTo((BigDecimal) result.get(1).get("varianceRate")));
        assertEquals("猪肉", result.get(1).get("materialName"));
        assertEquals("kg", result.get(1).get("unit"));
    }

    @Test
    @DisplayName("varianceAnalysis：limit 截断（默认 20，上限 100），理论为 0 时差异率为 0")
    void varianceAnalysis_appliesLimitAndZeroTheoreticalGuard() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            rows.add(row((long) i, "m" + i, "kg", "0", "0"));
        }
        when(forecastMapper.getTheoreticalConsumedGrouped(any(), any())).thenReturn(rows);
        when(forecastMapper.getActualConsumed(anyLong(), any(), any())).thenReturn(new BigDecimal("5"));

        assertEquals(10, service.varianceAnalysis(7, 10).size());
        assertEquals(20, service.varianceAnalysis(7, 0).size(), "limit<1 回退 20");
        assertEquals(30, service.varianceAnalysis(7, 1000).size(), "上限 100，但只有 30 行");
        assertEquals(BigDecimal.ZERO, service.varianceAnalysis(7, 10).get(0).get("varianceRate"));
    }

    @Test
    @DisplayName("varianceAnalysis：materialId 为 null/非数字的行被跳过，字段缺失用零值兜底")
    void varianceAnalysis_skipsDirtyRowsAndDefaultsNulls() {
        Map<String, Object> dirty = new HashMap<>();
        dirty.put("materialId", null);
        dirty.put("theoreticalConsumed", "10");

        Map<String, Object> nonNumericId = new HashMap<>();
        nonNumericId.put("materialId", "abc");

        Map<String, Object> sparse = new HashMap<>();
        sparse.put("materialId", 7);
        sparse.put("theoreticalConsumed", null);
        sparse.put("materialName", null);
        sparse.put("unit", null);
        sparse.put("soldServings", null);

        when(forecastMapper.getTheoreticalConsumedGrouped(any(), any()))
                .thenReturn(List.of(dirty, nonNumericId, sparse));
        when(forecastMapper.getActualConsumed(eq(7L), any(), any())).thenReturn(null);

        List<Map<String, Object>> result = service.varianceAnalysis(7, 20);

        assertEquals(1, result.size());
        Map<String, Object> only = result.get(0);
        assertEquals(7L, only.get("materialId"));
        assertEquals("", only.get("materialName"));
        assertEquals("", only.get("unit"));
        assertEquals(BigDecimal.ZERO, only.get("theoreticalConsumed"));
        assertEquals(BigDecimal.ZERO, only.get("actualConsumed"));
        assertEquals(BigDecimal.ZERO, only.get("varianceQty"));
        assertEquals(BigDecimal.ZERO, only.get("varianceRate"));
    }

    @Test
    @DisplayName("varianceAnalysis：时间窗口按 days 计算，days<1 回退 7 天")
    void varianceAnalysis_usesClampedWindowInQuery() {
        when(forecastMapper.getTheoreticalConsumedGrouped(any(), any())).thenReturn(List.of());

        service.varianceAnalysis(0, 10);

        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(forecastMapper).getTheoreticalConsumedGrouped(sinceCaptor.capture(), any());
        LocalDateTime since = sinceCaptor.getValue();
        assertNotNull(since);
        assertTrue(since.isBefore(LocalDateTime.now().minusDays(6)), "应回退为 7 天前，实际: " + since);
        assertTrue(since.isAfter(LocalDateTime.now().minusDays(8)), "不应超出 7 天窗口，实际: " + since);
    }

    private static Map<String, Object> row(Long materialId, String name, String unit, String theoretical, String sold) {
        Map<String, Object> m = new HashMap<>();
        m.put("materialId", materialId);
        m.put("materialName", name);
        m.put("unit", unit);
        m.put("theoreticalConsumed", new BigDecimal(theoretical));
        m.put("soldServings", new BigDecimal(sold));
        return m;
    }
}
