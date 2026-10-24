package com.foodtraceability.service.impl;

import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.dto.product.StockForecastVO;
import com.foodtraceability.entity.DishRecipeNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.DishRecipeNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.LocationIdMapMapper;
import com.foodtraceability.mapper.StockForecastMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 库存预测的库位口径（整改：location_code 软匹配 → location_id）。
 *
 * <p>覆盖：门店映射命中时 Mapper 只收 locationId、无映射时空结果 + 不查全量、
 * Mapper SQL 中不再出现 location_code 与 storeId 的比较。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockForecastLocationScopeTest {

    private static final long DISH_ID = 800L;
    private static final long MATERIAL_ID = 900L;
    private static final long LOCATION_ID = 5L;

    @Mock private FoodNewMapper foodNewMapper;
    @Mock private DishRecipeNewMapper dishRecipeNewMapper;
    @Mock private StockForecastMapper forecastMapper;
    @Mock private LocationIdMapMapper locationIdMapMapper;

    private LocationIdBridge locationIdBridge;

    /** 可注入当前用户门店/库位的测试子类 */
    private static class TestableService extends StockForecastServiceImpl {
        Long locationId;
        String storeId;

        TestableService(FoodNewMapper foodMapper, DishRecipeNewMapper recipeMapper,
                        StockForecastMapper forecastMapper, LocationIdBridge bridge) {
            super(foodMapper, recipeMapper, forecastMapper, bridge);
        }

        @Override
        protected Long currentUserLocationId() {
            return locationId;
        }

        @Override
        protected String currentUserStoreId() {
            return storeId;
        }
    }

    private TestableService service;

    @BeforeEach
    void setUp() {
        locationIdBridge = new LocationIdBridge(locationIdMapMapper);
        service = new TestableService(foodNewMapper, dishRecipeNewMapper, forecastMapper, locationIdBridge);

        FoodNew food = new FoodNew();
        food.setFoodId(DISH_ID);
        food.setFoodName("QA-测试菜");
        when(foodNewMapper.selectById(DISH_ID)).thenReturn(food);

        DishRecipeNew recipe = new DishRecipeNew();
        recipe.setFoodId(DISH_ID);
        recipe.setMaterialId(MATERIAL_ID);
        recipe.setMaterialName("QA-土豆");
        recipe.setRequiredQuantity(new BigDecimal("0.200"));
        when(dishRecipeNewMapper.selectList(any())).thenReturn(List.of(recipe));

        when(forecastMapper.getAvailableStock(anyLong(), any())).thenReturn(new BigDecimal("10.000"));
        when(forecastMapper.getSafetyStock(anyLong(), any())).thenReturn(new BigDecimal("5.000"));
        when(forecastMapper.getActualConsumed(anyLong(), any(), any())).thenReturn(new BigDecimal("2.000"));
        when(forecastMapper.getSoldServings(anyLong(), any(), any())).thenReturn(new BigDecimal("10"));
        when(forecastMapper.getDishSoldServings(anyLong(), any(), any())).thenReturn(new BigDecimal("10"));
    }

    @Test
    @DisplayName("a) 有门店归属 → Mapper 收到 locationId（ID 映射结果），不再是字符串 storeId")
    void mapperReceivesResolvedLocationId() {
        service.locationId = LOCATION_ID;
        service.storeId = "1";

        StockForecastVO vo = service.forecast(DISH_ID, 7);

        assertNotNull(vo);
        // forecast() 会在「逐原料」与「瓶颈补货」两处取可用库存，故用 atLeastOnce
        verify(forecastMapper, org.mockito.Mockito.atLeastOnce()).getAvailableStock(eq(MATERIAL_ID), eq(LOCATION_ID));
        verify(forecastMapper).getSafetyStock(eq(MATERIAL_ID), eq(LOCATION_ID));
        verify(forecastMapper).getActualConsumed(eq(MATERIAL_ID), any(), eq(LOCATION_ID));
        // 关键：所有库存类查询都必须带 locationId，不存在「按 storeId 字符串」的调用形态
        verify(forecastMapper, never()).getAvailableStock(anyLong(), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    @DisplayName("b) store_id=1 经 location_id_map('stores_new') 映射到 location 5")
    void storeIdMapsToLocationId() {
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, 1L))
                .thenReturn(LOCATION_ID);

        assertEquals(LOCATION_ID, service.resolveStoreLocationId("1"));
    }

    @Test
    @DisplayName("b2) 无映射 → 空结果 + 不查全量（Mapper 零调用）")
    void unmappedStoreYieldsEmptyResultWithoutFullQuery() {
        service.locationId = null;      // 无 users.location_id
        service.storeId = "1";          // 但声明了门店
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, 1L))
                .thenReturn(null);      // 映射不到库位

        StockForecastVO vo = service.forecast(DISH_ID, 7);

        assertNotNull(vo);
        assertTrue(vo.getItems() == null || vo.getItems().isEmpty(), "无映射必须返回空结果");
        assertFalse(Boolean.TRUE.equals(vo.getCanMake()));
        // 关键：既没有用 storeId 软匹配，也没有回退成全量查询
        verifyNoInteractions(forecastMapper);
    }

    @Test
    @DisplayName("b3) 差异分析无映射 → 返回空列表且不查全量")
    void varianceAnalysisEmptyWhenUnmapped() {
        service.locationId = null;
        service.storeId = "1";
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, 1L)).thenReturn(null);

        List<Map<String, Object>> rows = service.varianceAnalysis(7, 20);

        assertNotNull(rows);
        assertTrue(rows.isEmpty());
        verifyNoInteractions(forecastMapper);
    }

    @Test
    @DisplayName("c) Mapper SQL 不再出现 location_code 与 storeId 的比较，改用 location_id")
    void mapperSqlUsesLocationIdOnly() throws Exception {
        Path mapper = Path.of("src/main/java/com/foodtraceability/mapper/StockForecastMapper.java");
        assertTrue(Files.exists(mapper), "应能定位到 StockForecastMapper.java，实际工作目录=" + Path.of("").toAbsolutePath());
        // 只看代码行（去掉注释行），避免把「解释历史缺陷」的注释误判成实现
        StringBuilder codeOnly = new StringBuilder();
        for (String line : Files.readAllLines(mapper, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                continue;
            }
            codeOnly.append(line).append('\n');
        }
        String sql = codeOnly.toString();

        assertFalse(sql.contains("location_code = #{storeId"), "禁止 location_code 与 storeId 做字符串比较");
        assertFalse(sql.contains("#{storeId, jdbcType=VARCHAR}"), "禁止再传入 storeId 字符串参数");
        int hits = sql.split("l\\.location_id = #\\{locationId", -1).length - 1;
        assertTrue(hits >= 3, "三处库存查询都应改为 l.location_id = #{locationId}，实际命中=" + hits);
    }

    @Test
    @DisplayName("d) 无任何门店归属（storeId 为空）→ 保持全量口径（locationId=null）")
    void noStoreScopeFallsBackToAllScope() {
        service.locationId = null;
        service.storeId = null;

        service.forecast(DISH_ID, 7);

        verify(forecastMapper, org.mockito.Mockito.atLeastOnce())
                .getAvailableStock(eq(MATERIAL_ID), org.mockito.ArgumentMatchers.isNull());
        assertNull(service.resolveStoreLocationId(null));
    }
}
