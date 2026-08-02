package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.product.StockForecastItemVO;
import com.foodtraceability.dto.product.StockForecastVO;
import com.foodtraceability.entity.DishRecipeNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.DishRecipeNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.StockForecastMapper;
import com.foodtraceability.service.StockForecastService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存预测与消耗差异分析服务实现（方案D+/E）
 */
@Service
public class StockForecastServiceImpl implements StockForecastService {

    private static final Logger log = LoggerFactory.getLogger(StockForecastServiceImpl.class);

    /** 订单状态=已完成 */
    private static final int ORDER_COMPLETED = 2;

    private final FoodNewMapper foodNewMapper;
    private final DishRecipeNewMapper dishRecipeNewMapper;
    private final StockForecastMapper forecastMapper;

    public StockForecastServiceImpl(FoodNewMapper foodNewMapper,
                                    DishRecipeNewMapper dishRecipeNewMapper,
                                    StockForecastMapper forecastMapper) {
        this.foodNewMapper = foodNewMapper;
        this.dishRecipeNewMapper = dishRecipeNewMapper;
        this.forecastMapper = forecastMapper;
    }

    @Override
    public StockForecastVO forecast(Long dishId, Integer days) {
        int window = days == null || days < 1 ? 7 : Math.min(days, 90);
        LocalDateTime since = LocalDateTime.now().minusDays(window);
        String storeId = SecurityUtils.getCurrentUserStoreId();
        Long storeIdLong = parseStoreIdLong(storeId);

        FoodNew food = foodNewMapper.selectById(dishId);
        List<DishRecipeNew> recipes = dishRecipeNewMapper.selectList(
                new LambdaQueryWrapper<DishRecipeNew>()
                        .eq(DishRecipeNew::getFoodId, dishId)
                        .eq(DishRecipeNew::getDeleted, 0));

        StockForecastVO result = new StockForecastVO();
        result.setDishId(dishId);
        result.setDishName(food != null ? food.getFoodName() : "");
        result.setDataWindowDays(window);
        result.setItems(new ArrayList<>());

        boolean anyData = false;
        int minActual = Integer.MAX_VALUE;
        int minTheoretical = Integer.MAX_VALUE;
        BigDecimal bottleneckSafety = BigDecimal.ZERO;
        String bottleneckName = "";

        for (DishRecipeNew recipe : recipes) {
            if (recipe.getMaterialId() == null) {
                continue;
            }
            Long materialId = recipe.getMaterialId();
            BigDecimal recipeQty = recipe.getRequiredQuantity() != null ? recipe.getRequiredQuantity() : BigDecimal.ZERO;
            BigDecimal available = forecastMapper.getAvailableStock(materialId, storeId);
            BigDecimal safety = forecastMapper.getSafetyStock(materialId, storeId);
            BigDecimal actualConsumed = forecastMapper.getActualConsumed(materialId, since, storeId);
            BigDecimal sold = forecastMapper.getSoldServings(materialId, since, storeIdLong);

            boolean sufficient = sold != null && sold.compareTo(BigDecimal.ZERO) > 0
                    && actualConsumed != null && actualConsumed.compareTo(BigDecimal.ZERO) > 0;
            if (sufficient) {
                anyData = true;
            }

            BigDecimal actualPerServing = sufficient
                    ? actualConsumed.divide(sold, 6, RoundingMode.HALF_UP)
                    : recipeQty; // 数据不足回退理论用量

            BigDecimal lossRate = BigDecimal.ZERO;
            if (sufficient && recipeQty != null && recipeQty.compareTo(BigDecimal.ZERO) > 0) {
                lossRate = actualPerServing.divide(recipeQty, 6, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
            }

            int maxActual = 0;
            int maxTheoretical = 0;
            if (available != null && available.compareTo(BigDecimal.ZERO) > 0) {
                if (actualPerServing.compareTo(BigDecimal.ZERO) > 0) {
                    maxActual = available.divide(actualPerServing, 0, RoundingMode.FLOOR).intValue();
                }
                if (recipeQty.compareTo(BigDecimal.ZERO) > 0) {
                    maxTheoretical = available.divide(recipeQty, 0, RoundingMode.FLOOR).intValue();
                }
            }

            minActual = Math.min(minActual, maxActual);
            minTheoretical = Math.min(minTheoretical, maxTheoretical);
            if (maxActual <= minActual) {
                bottleneckSafety = safety != null ? safety : BigDecimal.ZERO;
                bottleneckName = recipe.getMaterialName() != null ? recipe.getMaterialName() : "";
            }

            StockForecastItemVO item = new StockForecastItemVO();
            item.setMaterialId(materialId);
            item.setMaterialName(recipe.getMaterialName());
            item.setUnit(recipe.getUnit());
            item.setAvailableStock(available != null ? available : BigDecimal.ZERO);
            item.setSafetyStock(safety != null ? safety : BigDecimal.ZERO);
            item.setRecipeQty(recipeQty);
            item.setActualQtyPerServing(actualPerServing);
            item.setImpliedLossRate(lossRate);
            item.setMaxServingsByThis(maxActual);
            result.getItems().add(item);
        }

        // 标记瓶颈
        for (StockForecastItemVO item : result.getItems()) {
            item.setIsBottleneck(item.getMaxServingsByThis() != null
                    && item.getMaxServingsByThis() == minActual);
        }

        int maxServings = minActual == Integer.MAX_VALUE ? 0 : minActual;
        int theoreticalServings = minTheoretical == Integer.MAX_VALUE ? 0 : minTheoretical;

        // 日均销量（该菜品）
        BigDecimal dishSold = forecastMapper.getDishSoldServings(dishId, since, storeIdLong);
        BigDecimal avgDailySales = dishSold != null
                ? dishSold.divide(BigDecimal.valueOf(window), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        result.setCanMake(maxServings > 0);
        result.setMaxServings(maxServings);
        result.setTheoreticalServings(theoreticalServings);
        result.setHasSufficientData(anyData);
        result.setAvgDailySales(avgDailySales);
        result.setBottleneckMaterialName(bottleneckName);

        // 预计售罄天数 = 可做份数 / 日均销量（日均>0 才计算）
        if (avgDailySales.compareTo(BigDecimal.ZERO) > 0) {
            result.setDaysToSellout(BigDecimal.valueOf(maxServings)
                    .divide(avgDailySales, 1, RoundingMode.HALF_UP));
        } else {
            result.setDaysToSellout(BigDecimal.ZERO);
        }

        // 建议补货量：瓶颈原料补到安全库存（库存不足安全库存时）
        if (bottleneckName.isEmpty()) {
            result.setSuggestedRestock(0);
        } else {
            BigDecimal available = forecastMapper.getAvailableStock(getBottleneckMaterialId(result), storeId);
            BigDecimal shortage = bottleneckSafety.subtract(available != null ? available : BigDecimal.ZERO);
            result.setSuggestedRestock(shortage.compareTo(BigDecimal.ZERO) > 0 ? shortage.intValue() : 0);
        }

        return result;
    }

    private Long getBottleneckMaterialId(StockForecastVO vo) {
        if (vo.getItems() == null) {
            return null;
        }
        for (StockForecastItemVO item : vo.getItems()) {
            if (Boolean.TRUE.equals(item.getIsBottleneck())) {
                return item.getMaterialId();
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> varianceAnalysis(Integer days, Integer limit) {
        int window = days == null || days < 1 ? 7 : Math.min(days, 90);
        int topN = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        LocalDateTime since = LocalDateTime.now().minusDays(window);
        String storeId = SecurityUtils.getCurrentUserStoreId();
        Long storeIdLong = parseStoreIdLong(storeId);

        List<Map<String, Object>> rows = forecastMapper.getTheoreticalConsumedGrouped(since, storeIdLong);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Long materialId = toLong(row.get("materialId"));
            if (materialId == null) {
                continue;
            }
            BigDecimal theoretical = toDecimal(row.get("theoreticalConsumed"));
            BigDecimal sold = toDecimal(row.get("soldServings"));
            BigDecimal actual = forecastMapper.getActualConsumed(materialId, since, storeId);
            if (actual == null) {
                actual = BigDecimal.ZERO;
            }

            BigDecimal varianceQty = actual.subtract(theoretical);
            BigDecimal varianceRate = theoretical != null && theoretical.compareTo(BigDecimal.ZERO) > 0
                    ? varianceQty.divide(theoretical, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> item = new HashMap<>();
            item.put("materialId", materialId);
            item.put("materialName", str(row.get("materialName")));
            item.put("unit", str(row.get("unit")));
            item.put("theoreticalConsumed", theoretical);
            item.put("actualConsumed", actual);
            item.put("varianceQty", varianceQty);
            item.put("varianceRate", varianceRate);
            item.put("soldServings", sold);
            result.add(item);
        }

        // 按差异量降序（超耗最多在前）
        result.sort(Comparator.comparing((Map<String, Object> m) -> toDecimal(m.get("varianceQty")).abs())
                .reversed());
        return result.size() > topN ? result.subList(0, topN) : result;
    }

    private Long parseStoreIdLong(String storeId) {
        if (storeId == null || storeId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(storeId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        if (v instanceof Number) {
            return BigDecimal.valueOf(((Number) v).doubleValue());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String str(Object v) {
        return v == null ? "" : v.toString();
    }
}
