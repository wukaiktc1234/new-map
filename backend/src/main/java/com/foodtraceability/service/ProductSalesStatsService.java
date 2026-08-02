package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.entity.ProductSalesStats;
import com.foodtraceability.entity.SalesTrendData;
import com.foodtraceability.entity.TopProductData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ProductSalesStatsService {
    
    /**
     * 获取产品销售分析分页数据
     */
    IPage<ProductSalesStats> getSalesStatsPage(int page, int size, String productName, 
                                                String category, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取销售趋势数据
     */
    SalesTrendData getSalesTrend(LocalDate startDate, LocalDate endDate, String type);
    
    /**
     * 获取热销产品TOP5
     */
    List<TopProductData> getTopProducts(int topN, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取销售统计概览
     */
    Map<String, Object> getSalesOverview(LocalDate date);
    
    /**
     * 按分类统计销售数据
     */
    List<Map<String, Object>> getSalesStatsByCategory(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取销售增长率数据
     */
    List<Map<String, Object>> getSalesGrowthRate(LocalDate startDate, LocalDate endDate);
}
