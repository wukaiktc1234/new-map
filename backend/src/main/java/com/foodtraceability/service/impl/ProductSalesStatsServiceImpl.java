package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Food;
import com.foodtraceability.entity.ProductSalesStats;
import com.foodtraceability.entity.SalesTrendData;
import com.foodtraceability.entity.TopProductData;
import com.foodtraceability.mapper.FoodMapper;
import com.foodtraceability.service.ProductSalesStatsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 产品销售统计服务实现
 *
 * 注意：当前订单管理中心尚未完成开发，order_items 表无真实销售数据。
 * 本实现的统计方法返回基于 Food 表的零销售数据（销售额=0，销量=0），
 * 待订单管理中心开发完成后，需接入真实订单数据查询。
 */
@Service
public class ProductSalesStatsServiceImpl implements ProductSalesStatsService {

    public ProductSalesStatsServiceImpl(FoodMapper foodMapper) {
        this.foodMapper = foodMapper;
    }

    private final FoodMapper foodMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public IPage<ProductSalesStats> getSalesStatsPage(int page, int size, String productName,
                                                       String category, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Food> queryWrapper = new LambdaQueryWrapper<>();
        if (productName != null && !productName.isEmpty()) {
            queryWrapper.like(Food::getFoodName, productName);
        }
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq(Food::getFoodCategory, category);
        }
        queryWrapper.orderByDesc(Food::getCreatedAt);

        Page<Food> foodPage = foodMapper.selectPage(new Page<>(page, size), queryWrapper);

        List<ProductSalesStats> statsList = foodPage.getRecords().stream()
                .map(this::buildZeroSalesStats)
                .collect(Collectors.toList());

        Page<ProductSalesStats> resultPage = new Page<>(page, size);
        resultPage.setTotal(foodPage.getTotal());
        resultPage.setRecords(statsList);

        return resultPage;
    }

    @Override
    public SalesTrendData getSalesTrend(LocalDate startDate, LocalDate endDate, String type) {
        List<String> dates = new ArrayList<>();
        List<BigDecimal> salesAmounts = new ArrayList<>();
        List<Integer> salesVolumes = new ArrayList<>();

        // 防御性处理：未传日期范围时返回空数据，避免 NPE
        if (startDate == null || endDate == null) {
            return new SalesTrendData(dates, salesAmounts, salesVolumes);
        }

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current.format(DATE_FORMATTER));
            salesAmounts.add(BigDecimal.ZERO);
            salesVolumes.add(0);
            current = current.plusDays(1);
        }

        return new SalesTrendData(dates, salesAmounts, salesVolumes);
    }

    @Override
    public List<TopProductData> getTopProducts(int topN, LocalDate startDate, LocalDate endDate) {
        // 订单模块未开发完成，返回 Food 表中前 topN 个菜品（销量=0、销售额=0）
        List<Food> allFoods = foodMapper.selectList(null);

        return allFoods.stream()
                .limit(topN)
                .map(food -> new TopProductData(
                        food.getFoodCode(),
                        food.getFoodName(),
                        0,
                        BigDecimal.ZERO
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getSalesOverview(LocalDate date) {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalSales", BigDecimal.ZERO);
        overview.put("totalVolume", 0);
        overview.put("avgOrderAmount", BigDecimal.ZERO);
        overview.put("orderCount", 0);
        overview.put("growthRate", BigDecimal.ZERO);
        overview.put("date", date != null ? date.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));
        return overview;
    }

    @Override
    public List<Map<String, Object>> getSalesStatsByCategory(LocalDate startDate, LocalDate endDate) {
        // 按菜品分类聚合，销售额=0、销量=0
        List<Food> allFoods = foodMapper.selectList(null);
        Map<String, List<Food>> grouped = allFoods.stream()
                .filter(f -> f.getFoodCategory() != null)
                .collect(Collectors.groupingBy(Food::getFoodCategory));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Food>> entry : grouped.entrySet()) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("categoryName", entry.getKey());
            stats.put("salesAmount", BigDecimal.ZERO);
            stats.put("salesVolume", 0);
            stats.put("productCount", entry.getValue().size());
            stats.put("growthRate", BigDecimal.ZERO);
            result.add(stats);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getSalesGrowthRate(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> growthData = new ArrayList<>();

        // 防御性处理：未传日期范围时返回空数据，避免 NPE
        if (startDate == null || endDate == null) {
            return growthData;
        }

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", current.format(DATE_FORMATTER));
            dayData.put("growthRate", BigDecimal.ZERO);
            dayData.put("salesAmount", BigDecimal.ZERO);
            dayData.put("salesVolume", 0);
            growthData.add(dayData);
            current = current.plusDays(1);
        }

        return growthData;
    }

    /**
     * 基于 Food 表构建零销售统计（订单模块未完成开发）
     */
    private ProductSalesStats buildZeroSalesStats(Food food) {
        BigDecimal avgPrice = food.getFoodPrice() != null
                ? food.getFoodPrice().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new ProductSalesStats(
                null,
                food.getFoodCode(),
                food.getFoodName(),
                food.getFoodCategory(),
                0,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                avgPrice,
                0.0,
                java.time.LocalDateTime.now(),
                "daily"
        );
    }
}
