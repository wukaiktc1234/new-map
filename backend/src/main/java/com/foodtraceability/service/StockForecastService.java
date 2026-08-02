package com.foodtraceability.service;

import com.foodtraceability.dto.product.StockForecastVO;

import java.util.List;
import java.util.Map;

/**
 * 库存预测与消耗差异分析服务（方案D+/E）
 * 基于近 N 天真实经营数据（订单销量 + 门店库存流水）自校准"实际每份用量"，
 * 预测可做份数/售罄时间/补货量，并输出理论 vs 实际消耗差异。
 */
public interface StockForecastService {

    /**
     * 菜品可做份数预测（方案D+）
     * @param dishId 菜品ID
     * @param days 数据窗口（天，默认7）
     */
    StockForecastVO forecast(Long dishId, Integer days);

    /**
     * 原料消耗差异分析（方案E）
     * @param days 数据窗口（天，默认7）
     * @param limit 返回条数（按差异量降序）
     */
    List<Map<String, Object>> varianceAnalysis(Integer days, Integer limit);
}
